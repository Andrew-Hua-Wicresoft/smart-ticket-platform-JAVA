package com.ticket.zhigong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deflections")
public class Deflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "matched_kb_id")
    private Long matchedKbId;

    @Column(name = "search_query", columnDefinition = "TEXT")
    private String searchQuery;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Deflection() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMatchedKbId() { return matchedKbId; }
    public void setMatchedKbId(Long matchedKbId) { this.matchedKbId = matchedKbId; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
