package com.ticket.zhigong.messaging;

public final class AiWorkflowMessaging {

    public static final String TICKET_EVENTS_EXCHANGE = "ticket.events";
    public static final String PLATFORM_DLX_EXCHANGE = "platform.dlx";

    public static final String AI_ANALYSIS_QUEUE = "ai.analysis";
    public static final String KB_DRAFT_QUEUE = "kb.draft";
    public static final String NOTIFICATION_DISPATCH_QUEUE = "notification.dispatch";

    public static final String ROUTING_TICKET_CREATED = "ticket.created";
    public static final String ROUTING_TICKET_UPDATED = "ticket.updated";
    public static final String ROUTING_TICKET_RESOLVED = "ticket.resolved";
    public static final String ROUTING_KNOWLEDGE_PUBLISHED = "knowledge.published";
    public static final String ROUTING_AI_ANALYSIS_COMPLETED = "ai.analysis.completed";
    public static final String ROUTING_KNOWLEDGE_DRAFT_GENERATED = "knowledge.draft.generated";

    private AiWorkflowMessaging() {
    }
}
