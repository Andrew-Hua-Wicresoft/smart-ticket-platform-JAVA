package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.NotificationResponse;
import com.ticket.zhigong.entity.Notification;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.NotificationType;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.NotificationRepository;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               TicketRepository ticketRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public void notifyUsers(List<Long> recipientIds,
                            NotificationType type,
                            String title,
                            String content,
                            Long ticketId) {
        Set<Long> deduplicatedIds = new LinkedHashSet<>(recipientIds);
        if (deduplicatedIds.isEmpty()) {
            return;
        }

        Ticket ticket = ticketId != null ? ticketRepository.findById(ticketId).orElse(null) : null;
        List<User> recipients = userRepository.findAllById(deduplicatedIds);

        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setTicket(ticket);
            notificationRepository.save(notification);
        }
    }

    public Page<NotificationResponse> listNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByReadAscCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    public NotificationResponse getNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "通知不存在", HttpStatus.NOT_FOUND));
        return NotificationResponse.fromEntity(notification);
    }
}
