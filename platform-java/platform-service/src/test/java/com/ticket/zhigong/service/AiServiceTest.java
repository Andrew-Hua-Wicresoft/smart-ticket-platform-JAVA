package com.ticket.zhigong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.zhigong.ai.InternalAiClient;
import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private InternalAiClient internalAiClient;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private AiInteractionRepository aiInteractionRepository;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(
                internalAiClient,
                rateLimiterService,
                new SanitizationService(),
                aiInteractionRepository,
                new ObjectMapper()
        );
    }

    @Test
    void suggestConsumesOneRateLimitPermit() {
        when(internalAiClient.searchKnowledgeBase("VPN failed\nVPN timeout", 3, 42L))
                .thenReturn(List.of(new InternalAiClient.SearchResult(
                        "7",
                        "VPN troubleshooting",
                        "Restart the VPN client and verify credentials.",
                        0.91
                )));
        when(internalAiClient.suggest(eq(99L), eq("VPN failed"), eq("VPN timeout"), contains("知识库参考 #7"), eq(42L)))
                .thenReturn(new InternalAiClient.TextResponse("deepseek", "deepseek-v4-pro", "suggest", false, "Restart the client."));

        String result = aiService.suggest(99L, "VPN failed", "VPN timeout", 42L);

        assertThat(result).isEqualTo("Restart the client.");
        verify(rateLimiterService).checkRateLimit(42L);
        verifyNoMoreInteractions(rateLimiterService);
    }

    @Test
    void findSimilarTicketsConsumesOneRateLimitPermit() {
        when(internalAiClient.searchKnowledgeBase("VPN timeout", 5, 42L))
                .thenReturn(List.of(new InternalAiClient.SearchResult(
                        "8",
                        "VPN timeout duplicate",
                        "Known duplicate ticket.",
                        0.83
                )));

        assertThat(aiService.findSimilarTickets("VPN timeout", 42L)).hasSize(1);

        verify(rateLimiterService).checkRateLimit(42L);
        verifyNoMoreInteractions(rateLimiterService);
    }

    @Test
    void latestSuggestionReturnsPersistedSuggestContent() {
        AiInteraction interaction = new AiInteraction();
        interaction.setOutputText("""
                {"provider":"deepseek","model":"deepseek-v4-pro","content":"Restart the VPN client."}
                """);
        when(aiInteractionRepository.findFirstByTicketIdAndTypeAndSuccessTrueOrderByCreatedAtDesc(99L, "SUGGEST"))
                .thenReturn(Optional.of(interaction));

        var result = aiService.latestSuggestion(99L);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getSuggestion()).isEqualTo("Restart the VPN client.");
    }
}
