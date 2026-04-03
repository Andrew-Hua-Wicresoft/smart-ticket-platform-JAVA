package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.TicketCreateRequest;
import com.ticket.zhigong.dto.TicketResolveRequest;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ClaudeApiService claudeApiService;
    private final KnowledgeBaseService kbService;
    private final RateLimiterService rateLimiterService;
    private final SanitizationService sanitizationService;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         ClaudeApiService claudeApiService,
                         KnowledgeBaseService kbService,
                         RateLimiterService rateLimiterService,
                         SanitizationService sanitizationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.claudeApiService = claudeApiService;
        this.kbService = kbService;
        this.rateLimiterService = rateLimiterService;
        this.sanitizationService = sanitizationService;
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

        // AI priority assignment (async, non-blocking if Claude unavailable)
        try {
            rateLimiterService.checkRateLimit(userId);
            String priorityResult = claudeApiService.call(
                    "你是一个IT工单系统的优先级分析器。根据工单描述，返回优先级和原因。格式：PRIORITY:HIGH/MEDIUM/LOW\\nREASON:一句话原因。\\n判断标准：影响全公司=HIGH，影响个人工作=MEDIUM，非紧急请求=LOW。",
                    "工单标题: " + request.getTitle() + "\\n工单描述: " + request.getDescription(),
                    "PRIORITY", userId, null);

            if (priorityResult != null) {
                parsePriority(priorityResult, ticket);
            }
        } catch (Exception e) {
            // AI failure is non-blocking, default priority is MEDIUM
        }

        ticket = ticketRepository.save(ticket);
        log.info("Ticket created [id={}, userId={}, priority={}]", ticket.getId(), userId, ticket.getPriority());
        return TicketResponse.fromEntity(ticket);
    }

    public Page<TicketResponse> listTickets(Long userId, UserRole role, Pageable pageable) {
        Page<Ticket> tickets;

        switch (role) {
            case CUSTOMER:
                tickets = ticketRepository.findByCustomerId(userId, pageable);
                break;
            case ENGINEER:
                tickets = ticketRepository.findByStatusIn(
                        List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS), pageable);
                break;
            case ADMIN:
                tickets = ticketRepository.findAll(pageable);
                break;
            default:
                tickets = Page.empty();
        }

        return tickets.map(TicketResponse::fromEntity);
    }

    public TicketResponse getTicket(Long ticketId, Long userId, UserRole role) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("工单 #" + ticketId + " 不存在"));

        // CUSTOMER can only see their own tickets
        if (role == UserRole.CUSTOMER && !ticket.getCustomer().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "无权限查看此工单", HttpStatus.FORBIDDEN);
        }

        return TicketResponse.fromEntity(ticket);
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
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
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

        final String ticketTitle = ticket.getTitle();
        final String ticketDescription = ticket.getDescription();
        final String resolutionNotes = request.getResolutionNotes();

        ticket = ticketRepository.save(ticket);
        log.info("Ticket resolved [id={}, engineerId={}]", ticketId, engineerId);

        // Async: generate KB article from resolution notes
        final Long savedTicketId = ticket.getId();
        CompletableFuture.runAsync(() -> {
            kbService.generateKbArticleFromResolution(
                    savedTicketId,
                    ticketTitle,
                    ticketDescription,
                    resolutionNotes,
                    engineerId);
        });

        return TicketResponse.fromEntity(ticket);
    }

    private void parsePriority(String result, Ticket ticket) {
        try {
            String[] lines = result.split("\\n");
            for (String line : lines) {
                if (line.startsWith("PRIORITY:")) {
                    String priority = line.substring("PRIORITY:".length()).trim().toUpperCase();
                    ticket.setPriority(TicketPriority.valueOf(priority));
                } else if (line.startsWith("REASON:")) {
                    ticket.setPriorityReason(sanitizationService.sanitize(line.substring("REASON:".length()).trim()));
                }
            }
        } catch (Exception e) {
            // Parse failure is non-blocking, keep default MEDIUM
        }
    }
}
