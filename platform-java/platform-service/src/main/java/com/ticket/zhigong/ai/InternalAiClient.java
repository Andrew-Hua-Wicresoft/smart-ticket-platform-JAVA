package com.ticket.zhigong.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public interface InternalAiClient {

    record TextResponse(
            String provider,
            String model,
            String mode,
            boolean degraded,
            String content
    ) {}

    record AnalyzeResponse(
            String provider,
            String model,
            boolean degraded,
            String summary,
            @JsonProperty("suggested_priority")
            String suggestedPriority,
            @JsonProperty("priority_reason")
            String priorityReason,
            @JsonProperty("follow_up_questions")
            List<String> followUpQuestions
    ) {}

    record SearchResult(
            @JsonProperty("source_id")
            String sourceId,
            String title,
            String content,
            double similarity
    ) {}

    record ReindexResponse(
            String provider,
            String model,
            boolean degraded,
            boolean accepted,
            @JsonProperty("entity_type")
            String entityType,
            int limit,
            @JsonProperty("processed_count")
            int processedCount
    ) {}

    List<SearchResult> searchKnowledgeBase(String query, int topK, Long userId);

    TextResponse generateFollowUpQuestions(String title, String description, Long userId);

    TextResponse suggest(Long ticketId, String title, String description, String context, Long userId);

    AnalyzeResponse analyzeTicket(Long ticketId, String title, String description, Long userId);

    TextResponse generateKnowledgeDraft(Long ticketId, String title, String description, String resolutionNotes, Long userId);

    ReindexResponse reindexKnowledgeBase(int limit);
}
