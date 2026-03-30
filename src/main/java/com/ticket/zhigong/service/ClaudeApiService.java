package com.ticket.zhigong.service;

import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Shared Claude API wrapper for all 5 call sites:
 * REFINE, SUGGEST, PRIORITY, KB_GENERATE, VISION
 */
@Service
public class ClaudeApiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiService.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final AiInteractionRepository aiInteractionRepository;

    public ClaudeApiService(
            @Value("${claude.api-key:}") String apiKey,
            @Value("${claude.model:claude-sonnet-4-5-20250514}") String model,
            AiInteractionRepository aiInteractionRepository) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.model = model;
        this.aiInteractionRepository = aiInteractionRepository;
    }

    /**
     * Call Claude API with a system prompt and user message.
     * Returns the text response, or null if unavailable (graceful degradation).
     */
    public String call(String systemPrompt, String userMessage, String type, Long userId, Long ticketId) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Claude API key not configured, AI features disabled");
            return null;
        }

        long startTime = System.currentTimeMillis();
        AiInteraction interaction = new AiInteraction();
        interaction.setType(type);
        interaction.setUserId(userId);
        interaction.setTicketId(ticketId);
        interaction.setInputText(userMessage.length() > 2000 ? userMessage.substring(0, 2000) : userMessage);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 2048,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            var request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ANTHROPIC_API_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                if (content != null && !content.isEmpty()) {
                    String text = (String) content.get(0).get("text");

                    // Log usage
                    Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                    if (usage != null) {
                        int inputTokens = ((Number) usage.getOrDefault("input_tokens", 0)).intValue();
                        int outputTokens = ((Number) usage.getOrDefault("output_tokens", 0)).intValue();
                        interaction.setTokensUsed(inputTokens + outputTokens);
                    }

                    interaction.setOutputText(text.length() > 2000 ? text.substring(0, 2000) : text);
                    interaction.setSuccess(true);
                    interaction.setLatencyMs(System.currentTimeMillis() - startTime);
                    aiInteractionRepository.save(interaction);

                    return text;
                }
            }

            interaction.setSuccess(false);
            interaction.setErrorMessage("Empty response from Claude API");
            interaction.setLatencyMs(System.currentTimeMillis() - startTime);
            aiInteractionRepository.save(interaction);
            return null;

        } catch (RestClientException e) {
            log.warn("Claude API call failed ({}): {}", type, e.getMessage());
            interaction.setSuccess(false);
            interaction.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            interaction.setLatencyMs(System.currentTimeMillis() - startTime);
            aiInteractionRepository.save(interaction);
            return null;
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
