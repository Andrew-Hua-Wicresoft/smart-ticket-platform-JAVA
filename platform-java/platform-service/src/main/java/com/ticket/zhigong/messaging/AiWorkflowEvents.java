package com.ticket.zhigong.messaging;

public final class AiWorkflowEvents {

    private AiWorkflowEvents() {
    }

    public record TicketCreated(
            Long ticketId,
            Long userId,
            String title,
            String description
    ) {}

    public record TicketResolved(
            Long ticketId,
            Long engineerId,
            String title,
            String description,
            String resolutionNotes
    ) {}

    public record AiAnalysisCompleted(
            Long ticketId,
            String suggestedPriority,
            String priorityReason,
            boolean degraded
    ) {}

    public record KnowledgeDraftGenerated(
            Long articleId,
            Long ticketId,
            Long engineerId,
            boolean degraded
    ) {}
}
