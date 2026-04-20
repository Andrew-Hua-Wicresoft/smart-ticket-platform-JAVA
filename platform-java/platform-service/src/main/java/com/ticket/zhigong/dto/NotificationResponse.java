package com.ticket.zhigong.dto;

import com.ticket.zhigong.entity.Notification;
import com.ticket.zhigong.enums.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private Long ticketId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.id = notification.getId();
        response.type = notification.getType();
        response.title = notification.getTitle();
        response.content = notification.getContent();
        response.ticketId = notification.getTicket() != null ? notification.getTicket().getId() : null;
        response.read = notification.isRead();
        response.createdAt = notification.getCreatedAt();
        response.readAt = notification.getReadAt();
        return response;
    }

    public Long getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Long getTicketId() { return ticketId; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
