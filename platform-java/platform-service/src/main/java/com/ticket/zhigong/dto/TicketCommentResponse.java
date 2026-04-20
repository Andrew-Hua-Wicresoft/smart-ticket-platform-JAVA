package com.ticket.zhigong.dto;

import com.ticket.zhigong.entity.TicketComment;
import com.ticket.zhigong.enums.UserRole;

import java.time.LocalDateTime;

public class TicketCommentResponse {

    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorName;
    private UserRole authorRole;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketCommentResponse fromEntity(TicketComment comment) {
        TicketCommentResponse response = new TicketCommentResponse();
        response.id = comment.getId();
        response.ticketId = comment.getTicket().getId();
        response.authorId = comment.getAuthor().getId();
        response.authorName = comment.getAuthor().getName();
        response.authorRole = comment.getAuthor().getRole();
        response.content = comment.getContent();
        response.createdAt = comment.getCreatedAt();
        response.updatedAt = comment.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public UserRole getAuthorRole() { return authorRole; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
