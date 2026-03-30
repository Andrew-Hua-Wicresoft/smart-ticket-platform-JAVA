package com.ticket.zhigong.dto;

import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.enums.KbArticleStatus;
import java.time.LocalDateTime;

public class KbArticleResponse {

    private Long id;
    private String title;
    private String content;
    private KbArticleStatus status;
    private Long sourceTicketId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KbArticleResponse fromEntity(KnowledgeBase kb) {
        KbArticleResponse response = new KbArticleResponse();
        response.id = kb.getId();
        response.title = kb.getTitle();
        response.content = kb.getContent();
        response.status = kb.getStatus();
        response.sourceTicketId = kb.getSourceTicketId();
        if (kb.getCreatedBy() != null) {
            response.createdByName = kb.getCreatedBy().getName();
        }
        response.createdAt = kb.getCreatedAt();
        response.updatedAt = kb.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public KbArticleStatus getStatus() { return status; }
    public Long getSourceTicketId() { return sourceTicketId; }
    public String getCreatedByName() { return createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
