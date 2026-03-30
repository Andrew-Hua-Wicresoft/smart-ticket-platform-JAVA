package com.ticket.zhigong.entity;

import com.ticket.zhigong.enums.KbArticleStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KbArticleStatus status = KbArticleStatus.DRAFT;

    @Column(name = "source_ticket_id")
    private Long sourceTicketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public KnowledgeBase() {}

    public KnowledgeBase(String title, String content, KbArticleStatus status, Long sourceTicketId) {
        this.title = title;
        this.content = content;
        this.status = status;
        this.sourceTicketId = sourceTicketId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public KbArticleStatus getStatus() { return status; }
    public void setStatus(KbArticleStatus status) { this.status = status; }
    public Long getSourceTicketId() { return sourceTicketId; }
    public void setSourceTicketId(Long sourceTicketId) { this.sourceTicketId = sourceTicketId; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
