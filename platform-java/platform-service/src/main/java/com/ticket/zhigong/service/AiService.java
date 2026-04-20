package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates AI features: search, refine, suggest.
 * Uses CompletableFuture to parallelize independent AI calls.
 */
@Service
public class AiService {

    private final LlmClient llmClient;
    private final KnowledgeBaseService kbService;
    private final RateLimiterService rateLimiterService;
    private final SanitizationService sanitizationService;

    public AiService(LlmClient llmClient,
                     KnowledgeBaseService kbService,
                     RateLimiterService rateLimiterService,
                     SanitizationService sanitizationService) {
        this.llmClient = llmClient;
        this.kbService = kbService;
        this.rateLimiterService = rateLimiterService;
        this.sanitizationService = sanitizationService;
    }

    /**
     * Search KB for similar articles.
     */
    public List<AiSearchResult> search(String query, int topK, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return kbService.searchSimilar(query, topK);
    }

    /**
     * Generate refinement questions for a ticket description.
     */
    public String refine(String title, String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return sanitizationService.sanitize(llmClient.call(
                "你是一个IT支持系统的智能助手。用户提交了一个工单，但描述可能不够详细。" +
                "请生成2-3个针对性的问题来帮助用户补充信息。" +
                "格式：每个问题一行，用数字编号。使用简洁的中文。",
                "工单标题: " + title + "\n问题描述: " + description,
                "REFINE", userId, null));
    }

    /**
     * Suggest solution for a ticket based on similar past resolutions.
     */
    public String suggest(Long ticketId, String title, String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);

        // Parallel: search KB + call Claude for suggestion
        CompletableFuture<List<AiSearchResult>> searchFuture = CompletableFuture.supplyAsync(
                () -> kbService.searchSimilar(description, 3));

        CompletableFuture<String> suggestionFuture = searchFuture.thenApply(results -> {
            StringBuilder context = new StringBuilder();
            for (AiSearchResult result : results) {
                context.append("--- 相似文章 (匹配度: ")
                       .append(String.format("%.0f%%", result.getSimilarity() * 100))
                       .append(") ---\n")
                       .append(result.getTitle()).append("\n")
                       .append(result.getContent()).append("\n\n");
            }

            String contextStr = context.toString();
            if (contextStr.isBlank()) {
                contextStr = "暂无相似的历史解决方案。";
            }

            return llmClient.call(
                    "你是一个IT支持工程师助手。根据工单信息和历史解决方案，给出建议的解决方案。" +
                    "如果有相似的历史案例，请参考它们。格式清晰，使用中文。",
                    "工单标题: " + title + "\n问题描述: " + description +
                    "\n\n历史相似案例:\n" + contextStr,
                    "SUGGEST", userId, ticketId);
        });

        try {
            return sanitizationService.sanitize(suggestionFuture.get());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find similar tickets for duplicate detection.
     */
    public List<AiSearchResult> findSimilarTickets(String description, Long userId) {
        rateLimiterService.checkRateLimit(userId);
        return kbService.searchSimilar(description, 5);
    }
}
