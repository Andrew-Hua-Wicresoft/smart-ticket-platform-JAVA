package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.TicketCommentResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.TicketComment;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.NotificationType;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.repository.TicketCommentRepository;
import com.ticket.zhigong.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SanitizationService sanitizationService;

    public TicketCommentService(TicketCommentRepository ticketCommentRepository,
                                UserRepository userRepository,
                                TicketService ticketService,
                                NotificationService notificationService,
                                AuditService auditService,
                                SanitizationService sanitizationService) {
        this.ticketCommentRepository = ticketCommentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.sanitizationService = sanitizationService;
    }

    public List<TicketCommentResponse> listComments(Long ticketId, Long userId, UserRole role) {
        ticketService.loadAuthorizedTicket(ticketId, userId, role);
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketCommentResponse::fromEntity)
                .toList();
    }

    @Transactional
    public TicketCommentResponse addComment(Long ticketId, String content, Long authorId, UserRole role) {
        Ticket ticket = ticketService.loadAuthorizedTicket(ticketId, authorId, role);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(sanitizationService.sanitize(content));
        comment = ticketCommentRepository.save(comment);

        notifyCommentParticipants(ticket, author);
        auditService.log(authorId, AuditAction.TICKET_COMMENTED, "TICKET", ticketId,
                "在工单 #" + ticketId + " 下新增评论");

        return TicketCommentResponse.fromEntity(comment);
    }

    private void notifyCommentParticipants(Ticket ticket, User author) {
        List<Long> recipients = new ArrayList<>();
        if (author.getRole() == UserRole.CUSTOMER) {
            if (ticket.getAssignedEngineer() != null) {
                recipients.add(ticket.getAssignedEngineer().getId());
            } else {
                recipients.addAll(userRepository.findByRoleIn(List.of(UserRole.ENGINEER, UserRole.ADMIN))
                        .stream()
                        .map(User::getId)
                        .toList());
            }
        } else {
            recipients.add(ticket.getCustomer().getId());
        }

        recipients.remove(author.getId());

        notificationService.notifyUsers(
                recipients,
                NotificationType.TICKET_COMMENTED,
                "工单有新评论",
                author.getName() + " 在工单 #" + ticket.getId() + " 下添加了新评论",
                ticket.getId()
        );
    }
}
