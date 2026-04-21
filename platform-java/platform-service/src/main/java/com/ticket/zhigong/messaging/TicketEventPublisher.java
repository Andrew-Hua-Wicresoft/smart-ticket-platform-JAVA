package com.ticket.zhigong.messaging;

import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TicketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TicketEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTicketCreated(Ticket ticket, Long userId) {
        publish(
                AiWorkflowMessaging.ROUTING_TICKET_CREATED,
                new AiWorkflowEvents.TicketCreated(ticket.getId(), userId, ticket.getTitle(), ticket.getDescription())
        );
    }

    public void publishTicketResolved(Ticket ticket, Long engineerId) {
        publish(
                AiWorkflowMessaging.ROUTING_TICKET_RESOLVED,
                new AiWorkflowEvents.TicketResolved(
                        ticket.getId(),
                        engineerId,
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getResolutionNotes()
                )
        );
    }

    public void publishAiAnalysisCompleted(Long ticketId, String suggestedPriority, String priorityReason, boolean degraded) {
        publish(
                AiWorkflowMessaging.ROUTING_AI_ANALYSIS_COMPLETED,
                new AiWorkflowEvents.AiAnalysisCompleted(ticketId, suggestedPriority, priorityReason, degraded)
        );
    }

    public void publishKnowledgeDraftGenerated(KnowledgeBase article, Long engineerId, boolean degraded) {
        publish(
                AiWorkflowMessaging.ROUTING_KNOWLEDGE_DRAFT_GENERATED,
                new AiWorkflowEvents.KnowledgeDraftGenerated(
                        article.getId(),
                        article.getSourceTicketId(),
                        engineerId,
                        degraded
                )
        );
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(AiWorkflowMessaging.TICKET_EVENTS_EXCHANGE, routingKey, payload);
        } catch (Exception ex) {
            log.warn("Failed to publish workflow event [routingKey={}]: {}", routingKey, ex.getMessage());
        }
    }
}
