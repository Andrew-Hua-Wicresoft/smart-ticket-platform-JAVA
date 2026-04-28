package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.TicketCreateRequest;
import com.ticket.zhigong.dto.TicketListFilter;
import com.ticket.zhigong.dto.TicketResolveRequest;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.NotificationType;
import com.ticket.zhigong.enums.TicketAssigneeScope;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.messaging.TicketEventPublisher;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TicketEventPublisher ticketEventPublisher;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditService auditService,
                         TicketEventPublisher ticketEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.ticketEventPublisher = ticketEventPublisher;
    }

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest request, Long userId) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setCustomer(customer);
        ticket.setStatus(TicketStatus.OPEN);

        ticket = ticketRepository.save(ticket);
        notificationService.notifyUsers(
                userRepository.findByRoleIn(List.of(UserRole.ENGINEER, UserRole.ADMIN)).stream()
                        .map(User::getId)
                        .toList(),
                NotificationType.TICKET_CREATED,
                "有新工单待处理",
                "新工单 #" + ticket.getId() + " 已创建：" + ticket.getTitle(),
                ticket.getId()
        );
        auditService.log(userId, AuditAction.TICKET_CREATED, "TICKET", ticket.getId(),
                "创建工单 #" + ticket.getId());
        ticketEventPublisher.publishTicketCreated(ticket, userId);
        log.info("Ticket created [id={}, userId={}, priority={}]", ticket.getId(), userId, ticket.getPriority());
        return TicketResponse.fromEntity(ticket);
    }

    public Page<TicketResponse> listTickets(Long userId, UserRole role, Pageable pageable) {
        return listTickets(userId, role, pageable, TicketListFilter.empty());
    }

    public Page<TicketResponse> listTickets(Long userId, UserRole role, Pageable pageable, TicketListFilter filter) {
        TicketListFilter safeFilter = filter == null ? TicketListFilter.empty() : filter;
        List<TicketStatus> statuses = allowedStatuses(role, safeFilter);
        if (statuses.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<Ticket> spec = buildListSpecification(userId, role, safeFilter, statuses);
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);
        return tickets.map(TicketResponse::fromEntity);
    }

    List<TicketStatus> allowedStatuses(UserRole role, TicketListFilter filter) {
        List<TicketStatus> roleStatuses = switch (role) {
            case ENGINEER -> List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
            case ADMIN, CUSTOMER -> Arrays.asList(TicketStatus.values());
        };
        if (filter.getStatuses().isEmpty()) {
            return roleStatuses;
        }
        Set<TicketStatus> requested = new LinkedHashSet<>(filter.getStatuses());
        return roleStatuses.stream()
                .filter(requested::contains)
                .toList();
    }

    private Specification<Ticket> buildListSpecification(Long userId,
                                                         UserRole role,
                                                         TicketListFilter filter,
                                                         List<TicketStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(statuses));

            if (role == UserRole.CUSTOMER) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), userId));
            }

            if (!filter.getPriorities().isEmpty()) {
                predicates.add(root.get("priority").in(filter.getPriorities()));
            }

            if (filter.hasKeyword()) {
                String keyword = "%" + filter.getKeyword().toLowerCase(Locale.ROOT) + "%";
                Join<Ticket, User> customer = root.join("customer", JoinType.LEFT);
                Join<Ticket, User> assignedEngineer = root.join("assignedEngineer", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title").as(String.class)), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description").as(String.class)), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(customer.get("name").as(String.class)), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(assignedEngineer.get("name").as(String.class)), keyword)
                ));
            }

            if (filter.getAssignee() == TicketAssigneeScope.UNASSIGNED) {
                predicates.add(criteriaBuilder.isNull(root.get("assignedEngineer")));
            } else if (filter.getAssignee() == TicketAssigneeScope.ME) {
                predicates.add(criteriaBuilder.equal(root.get("assignedEngineer").get("id"), userId));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public TicketResponse getTicket(Long ticketId, Long userId, UserRole role) {
        return TicketResponse.fromEntity(loadAuthorizedTicket(ticketId, userId, role));
    }

    public Ticket loadAuthorizedTicket(Long ticketId, Long userId, UserRole role) {
        Ticket ticket = ticketRepository.findDetailedById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("工单 #" + ticketId + " 不存在"));

        if (role == UserRole.CUSTOMER && !ticket.getCustomer().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "无权限查看此工单", HttpStatus.FORBIDDEN);
        }

        return ticket;
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, Long engineerId) {
        // Verify ticket exists
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("工单 #" + ticketId + " 不存在");
        }

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new EntityNotFoundException("工程师不存在"));

        // Atomic: only assigns if status is still OPEN (prevents race condition)
        int updated = ticketRepository.assignToEngineer(ticketId, engineer);
        if (updated == 0) {
            throw new BusinessException("ALREADY_ASSIGNED",
                    "该工单已被其他工程师接取",
                    HttpStatus.CONFLICT);
        }

        // Refresh entity after atomic update
        Ticket ticket = ticketRepository.findDetailedById(ticketId).orElseThrow();
        notificationService.notifyUsers(
                List.of(ticket.getCustomer().getId()),
                NotificationType.TICKET_ASSIGNED,
                "工单已被接取",
                "工单 #" + ticketId + " 已由 " + engineer.getName() + " 接取处理",
                ticketId
        );
        auditService.log(engineerId, AuditAction.TICKET_ASSIGNED, "TICKET", ticketId,
                "接取工单 #" + ticketId);
        log.info("Ticket assigned [id={}, engineerId={}]", ticketId, engineerId);
        return TicketResponse.fromEntity(ticket);
    }

    @Transactional
    public TicketResponse resolveTicket(Long ticketId, TicketResolveRequest request, Long engineerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("工单 #" + ticketId + " 不存在"));

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new BusinessException("INVALID_STATUS",
                    "只能解决状态为 IN_PROGRESS 的工单，当前状态: " + ticket.getStatus(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (ticket.getAssignedEngineer() == null || !ticket.getAssignedEngineer().getId().equals(engineerId)) {
            throw new BusinessException("NOT_ASSIGNED",
                    "只有被分配的工程师才能解决此工单",
                    HttpStatus.FORBIDDEN);
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionNotes(request.getResolutionNotes());
        ticket.setResolvedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);
        notificationService.notifyUsers(
                List.of(ticket.getCustomer().getId()),
                NotificationType.TICKET_RESOLVED,
                "工单已解决",
                "工单 #" + ticketId + " 已解决，请查看处理说明",
                ticketId
        );
        auditService.log(engineerId, AuditAction.TICKET_RESOLVED, "TICKET", ticketId,
                "解决工单 #" + ticketId);
        ticketEventPublisher.publishTicketResolved(ticket, engineerId);
        log.info("Ticket resolved [id={}, engineerId={}]", ticketId, engineerId);

        return TicketResponse.fromEntity(ticket);
    }
}
