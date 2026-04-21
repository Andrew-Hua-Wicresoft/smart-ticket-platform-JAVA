package com.ticket.zhigong.messaging;

import com.ticket.zhigong.ai.InternalAiClient;
import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.service.AuditService;
import com.ticket.zhigong.service.KnowledgeBaseService;
import com.ticket.zhigong.service.SanitizationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiWorkflowListener {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowListener.class);

    private final InternalAiClient internalAiClient;
    private final TicketRepository ticketRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final TicketEventPublisher ticketEventPublisher;
    private final AuditService auditService;
    private final SanitizationService sanitizationService;

    public AiWorkflowListener(InternalAiClient internalAiClient,
                              TicketRepository ticketRepository,
                              KnowledgeBaseRepository knowledgeBaseRepository,
                              KnowledgeBaseService knowledgeBaseService,
                              TicketEventPublisher ticketEventPublisher,
                              AuditService auditService,
                              SanitizationService sanitizationService) {
        this.internalAiClient = internalAiClient;
        this.ticketRepository = ticketRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.ticketEventPublisher = ticketEventPublisher;
        this.auditService = auditService;
        this.sanitizationService = sanitizationService;
    }

    @Transactional
    @RabbitListener(queues = AiWorkflowMessaging.AI_ANALYSIS_QUEUE)
    public void handleTicketCreated(AiWorkflowEvents.TicketCreated event) {
        Ticket ticket = ticketRepository.findById(event.ticketId())
                .orElseThrow(() -> new EntityNotFoundException("工单不存在"));

        InternalAiClient.AnalyzeResponse analysis = internalAiClient.analyzeTicket(
                event.ticketId(),
                event.title(),
                event.description(),
                event.userId()
        );

        TicketPriority priority = TicketPriority.MEDIUM;
        String priorityReason = "AI分析不可用，已保留默认优先级。";
        boolean degraded = true;

        if (analysis != null) {
            degraded = analysis.degraded();
            priority = resolvePriority(analysis.suggestedPriority());
            priorityReason = analysis.priorityReason() == null || analysis.priorityReason().isBlank()
                    ? "AI已完成优先级分析。"
                    : analysis.priorityReason();
        }

        ticket.setPriority(priority);
        ticket.setPriorityReason(sanitizationService.sanitize(priorityReason));
        ticketRepository.save(ticket);

        auditService.log(null,
                AuditAction.AI_ANALYSIS_COMPLETED,
                "TICKET",
                ticket.getId(),
                "AI已完成工单 #" + ticket.getId() + " 的优先级分析");
        ticketEventPublisher.publishAiAnalysisCompleted(ticket.getId(), priority.name(), priorityReason, degraded);
    }

    @Transactional
    @RabbitListener(queues = AiWorkflowMessaging.KB_DRAFT_QUEUE)
    public void handleTicketResolved(AiWorkflowEvents.TicketResolved event) {
        if (knowledgeBaseRepository.existsBySourceTicketId(event.ticketId())) {
            log.info("KB draft already exists for ticket #{}, skipping duplicate generation", event.ticketId());
            return;
        }

        InternalAiClient.TextResponse draft = internalAiClient.generateKnowledgeDraft(
                event.ticketId(),
                event.title(),
                event.description(),
                event.resolutionNotes(),
                event.engineerId()
        );

        boolean degraded = draft == null || draft.degraded();
        String articleContent = draft != null && draft.content() != null && !draft.content().isBlank()
                ? draft.content()
                : fallbackDraftContent(event.description(), event.resolutionNotes());

        KnowledgeBase article = knowledgeBaseService.createDraftArticle(
                event.ticketId(),
                event.title(),
                articleContent,
                event.engineerId()
        );

        auditService.log(null,
                AuditAction.KB_ARTICLE_DRAFT_GENERATED,
                "KNOWLEDGE_BASE",
                article.getId(),
                "AI已为工单 #" + event.ticketId() + " 生成知识库草稿");
        ticketEventPublisher.publishKnowledgeDraftGenerated(article, event.engineerId(), degraded);
    }

    private TicketPriority resolvePriority(String suggestedPriority) {
        if (suggestedPriority == null || suggestedPriority.isBlank()) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(suggestedPriority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TicketPriority.MEDIUM;
        }
    }

    private String fallbackDraftContent(String description, String resolutionNotes) {
        return "## 问题描述\n"
                + sanitizationService.sanitize(description)
                + "\n\n## 解决方案\n"
                + sanitizationService.sanitize(resolutionNotes)
                + "\n\n## 注意事项\n"
                + "AI草稿生成不可用，请工程师补充复盘与验证结果。";
    }
}
