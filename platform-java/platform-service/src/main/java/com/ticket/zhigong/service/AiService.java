package com.ticket.zhigong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.zhigong.ai.InternalAiClient;
import com.ticket.zhigong.dto.AiSuggestionSnapshot;
import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Public AI facade.
 * Public APIs remain on the Java side, but AI core logic now executes in the internal Python AI service.
 */
@Service
public class AiService {

    private final InternalAiClient internalAiClient;
    private final RateLimiterService rateLimiterService;
    private final SanitizationService sanitizationService;
    private final AiInteractionRepository aiInteractionRepository;
    private final ObjectMapper objectMapper;

    public AiService(InternalAiClient internalAiClient,
                     RateLimiterService rateLimiterService,
                     SanitizationService sanitizationService,
                     AiInteractionRepository aiInteractionRepository,
                     ObjectMapper objectMapper) {
        this.internalAiClient = internalAiClient;
        this.rateLimiterService = rateLimiterService;
        this.sanitizationService = sanitizationService;
        this.aiInteractionRepository = aiInteractionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Search KB for similar articles.
     */
    public List<AiSearchResult> search(String query, int topK, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return searchInternal(query, topK, userId);
    }

    private List<AiSearchResult> searchInternal(String query, int topK, Long userId) {
        List<InternalAiClient.SearchResult> results = internalAiClient.searchKnowledgeBase(query, topK, userId);
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(this::toSearchResult)
                .toList();
    }

    /**
     * Generate refinement questions for a ticket description.
     */
    public String refine(String title, String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        InternalAiClient.TextResponse response = internalAiClient.generateFollowUpQuestions(title, description, userId);
        if (response == null || response.content() == null || response.content().isBlank()) {
            return fallbackQuestions(title);
        }
        return sanitizationService.sanitize(response.content());
    }

    /**
     * Suggest solution for a ticket based on similar past resolutions.
     */
    public String suggest(Long ticketId, String title, String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        List<AiSearchResult> searchResults = searchInternal(buildSearchQuery(title, description), 3, userId);
        String context = buildSuggestionContext(searchResults);
        InternalAiClient.TextResponse response = internalAiClient.suggest(ticketId, title, description, context, userId);
        if (response == null || response.content() == null || response.content().isBlank()) {
            return "AI暂时不可用，请先检查最近变更、日志和依赖服务状态。";
        }
        return sanitizationService.sanitize(response.content());
    }

    public AiSuggestionSnapshot latestSuggestion(Long ticketId) {
        return aiInteractionRepository
                .findFirstByTicketIdAndTypeAndSuccessTrueOrderByCreatedAtDesc(ticketId, "SUGGEST")
                .map(this::toSuggestionSnapshot)
                .orElseGet(() -> new AiSuggestionSnapshot(false, null, null));
    }

    /**
     * Find similar tickets for duplicate detection.
     */
    public List<AiSearchResult> findSimilarTickets(String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return searchInternal(description, 5, userId);
    }

    private AiSearchResult toSearchResult(InternalAiClient.SearchResult result) {
        long kbId;
        try {
            kbId = Long.parseLong(result.sourceId());
        } catch (NumberFormatException ex) {
            kbId = -1L;
        }
        return new AiSearchResult(kbId, result.title(), result.content(), result.similarity());
    }

    private String buildSearchQuery(String title, String description) {
        if (title == null || title.isBlank()) {
            return description;
        }
        return title + "\n" + description;
    }

    private String buildSuggestionContext(List<AiSearchResult> results) {
        if (results.isEmpty()) {
            return "暂无相似的历史解决方案。";
        }

        StringBuilder context = new StringBuilder();
        for (AiSearchResult result : results) {
            context.append("--- 知识库参考 #")
                    .append(result.getKbId())
                    .append(" (匹配度: ")
                    .append(String.format("%.0f%%", result.getSimilarity() * 100))
                    .append(") ---\n")
                    .append(result.getTitle()).append("\n")
                    .append(truncateContext(result.getContent())).append("\n\n");
        }
        return context.toString();
    }

    private String truncateContext(String content) {
        if (content == null || content.length() <= 1600) {
            return content;
        }
        return content.substring(0, 1600) + "\n...（内容已截断）";
    }

    private AiSuggestionSnapshot toSuggestionSnapshot(AiInteraction interaction) {
        String suggestion = extractSuggestion(interaction.getOutputText());
        if (suggestion == null || suggestion.isBlank()) {
            return new AiSuggestionSnapshot(false, null, null);
        }
        return new AiSuggestionSnapshot(true, sanitizationService.sanitize(suggestion), interaction.getCreatedAt());
    }

    private String extractSuggestion(String outputText) {
        if (outputText == null || outputText.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(outputText);
            JsonNode content = node.get("content");
            if (content != null && content.isTextual()) {
                return content.asText();
            }
        } catch (Exception ignored) {
            // Older rows may contain plain text instead of the serialized AI response.
        }
        return outputText;
    }

    private String fallbackQuestions(String title) {
        String subject = title == null || title.isBlank() ? "该问题" : sanitizationService.sanitize(title);
        return "1. " + subject + " 从什么时候开始出现，最近是否有变更？\n"
                + "2. 请补充报错信息、截图以及影响范围。\n"
                + "3. 这个问题是持续存在还是间歇性出现？";
    }
}
