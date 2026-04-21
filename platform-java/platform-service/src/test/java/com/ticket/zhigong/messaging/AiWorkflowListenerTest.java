package com.ticket.zhigong.messaging;

import com.ticket.zhigong.ai.InternalAiClient;
import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.service.AuditService;
import com.ticket.zhigong.service.KnowledgeBaseService;
import com.ticket.zhigong.service.SanitizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWorkflowListenerTest {

    @Mock
    private InternalAiClient internalAiClient;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private TicketEventPublisher ticketEventPublisher;

    @Mock
    private AuditService auditService;

    @Mock
    private SanitizationService sanitizationService;

    @InjectMocks
    private AiWorkflowListener listener;

    @Test
    void handleTicketCreatedUpdatesPriorityFromAiAnalysis() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sanitizationService.sanitize("问题疑似影响业务连续性或较大范围用户，需要优先处理。"))
                .thenReturn("问题疑似影响业务连续性或较大范围用户，需要优先处理。");
        when(internalAiClient.analyzeTicket(1L, "邮箱异常", "全员无法登录邮箱", 7L))
                .thenReturn(new InternalAiClient.AnalyzeResponse(
                        "anthropic",
                        "claude-sonnet-4-20250514",
                        false,
                        "summary",
                        "HIGH",
                        "问题疑似影响业务连续性或较大范围用户，需要优先处理。",
                        List.of("Q1", "Q2")
                ));

        listener.handleTicketCreated(new AiWorkflowEvents.TicketCreated(1L, 7L, "邮箱异常", "全员无法登录邮箱"));

        ArgumentCaptor<Ticket> savedTicket = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(savedTicket.capture());
        assertThat(savedTicket.getValue().getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(savedTicket.getValue().getPriorityReason()).isEqualTo("问题疑似影响业务连续性或较大范围用户，需要优先处理。");
        verify(ticketEventPublisher).publishAiAnalysisCompleted(
                1L,
                "HIGH",
                "问题疑似影响业务连续性或较大范围用户，需要优先处理。",
                false
        );
    }

    @Test
    void handleTicketResolvedFallsBackWhenAiDraftGenerationFails() {
        KnowledgeBase article = new KnowledgeBase();
        article.setId(100L);
        article.setSourceTicketId(1L);

        when(knowledgeBaseRepository.existsBySourceTicketId(1L)).thenReturn(false);
        when(sanitizationService.sanitize("员工无法连接 VPN")).thenReturn("员工无法连接 VPN");
        when(sanitizationService.sanitize("重启客户端并刷新证书后恢复")).thenReturn("重启客户端并刷新证书后恢复");
        when(knowledgeBaseService.createDraftArticle(eq(1L), eq("VPN 故障"), contains("## 问题描述"), eq(9L)))
                .thenReturn(article);

        listener.handleTicketResolved(new AiWorkflowEvents.TicketResolved(
                1L,
                9L,
                "VPN 故障",
                "员工无法连接 VPN",
                "重启客户端并刷新证书后恢复"
        ));

        verify(knowledgeBaseService).createDraftArticle(eq(1L), eq("VPN 故障"), contains("## 解决方案"), eq(9L));
        verify(ticketEventPublisher).publishKnowledgeDraftGenerated(article, 9L, true);
    }
}
