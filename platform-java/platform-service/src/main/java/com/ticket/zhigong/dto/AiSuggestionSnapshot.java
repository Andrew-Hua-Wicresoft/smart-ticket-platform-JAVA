package com.ticket.zhigong.dto;

import java.time.LocalDateTime;

public class AiSuggestionSnapshot {

    private final boolean available;
    private final String suggestion;
    private final LocalDateTime createdAt;

    public AiSuggestionSnapshot(boolean available, String suggestion, LocalDateTime createdAt) {
        this.available = available;
        this.suggestion = suggestion;
        this.createdAt = createdAt;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
