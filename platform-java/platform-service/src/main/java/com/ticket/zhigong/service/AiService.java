package com.ticket.zhigong.service;

import com.ticket.zhigong.ai.InternalAiClient;
import com.ticket.zhigong.dto.AiSearchResult;
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

    public AiService(InternalAiClient internalAiClient,
                     RateLimiterService rateLimiterService,
                     SanitizationService sanitizationService) {
        this.internalAiClient = internalAiClient;
        this.rateLimiterService = rateLimiterService;
        this.sanitizationService = sanitizationService;
    }

    /**
     * Search KB for similar articles.
     */
    public List<AiSearchResult> search(String query, int topK, Long userId) {
        rateLimiterService.checkRateLimit(userId);
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
        List<AiSearchResult> searchResults = search(description, 3, userId);
        String context = buildSuggestionContext(searchResults);
        InternalAiClient.TextResponse response = internalAiClient.suggest(ticketId, title, description, context, userId);
        if (response == null || response.content() == null || response.content().isBlank()) {
            return "AI暂时不可用，请先检查最近变更、日志和依赖服务状态。";
        }
        return sanitizationService.sanitize(response.content());
    }

    /**
     * Find similar tickets for duplicate detection.
     */
    public List<AiSearchResult> findSimilarTickets(String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return search(description, 5, userId);
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

    private String buildSuggestionContext(List<AiSearchResult> results) {
        if (results.isEmpty()) {
            return "暂无相似的历史解决方案。";
        }

        StringBuilder context = new StringBuilder();
        for (AiSearchResult result : results) {
            context.append("--- 相似文章 (匹配度: ")
                    .append(String.format("%.0f%%", result.getSimilarity() * 100))
                    .append(") ---\n")
                    .append(result.getTitle()).append("\n")
                    .append(result.getContent()).append("\n\n");
        }
        return context.toString();
    }

    private String fallbackQuestions(String title) {
        String subject = title == null || title.isBlank() ? "该问题" : sanitizationService.sanitize(title);
        return "1. " + subject + " 从什么时候开始出现，最近是否有变更？\n"
                + "2. 请补充报错信息、截图以及影响范围。\n"
                + "3. 这个问题是持续存在还是间歇性出现？";
    }
}
