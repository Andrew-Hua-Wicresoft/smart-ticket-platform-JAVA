package com.ticket.zhigong.service;

import com.ticket.zhigong.config.LlmProperties;
import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.llm.LlmClient;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Shared Claude API wrapper for all 5 call sites:
 * REFINE, SUGGEST, PRIORITY, KB_GENERATE, VISION
 */
public class ClaudeApiService implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiService.class);

    private final RestTemplate restTemplate;
    private final LlmProperties llmProperties;
    private final AiInteractionRepository aiInteractionRepository;

    public ClaudeApiService(RestTemplateBuilder restTemplateBuilder,
                            LlmProperties llmProperties,
                            AiInteractionRepository aiInteractionRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.llmProperties = llmProperties;
        this.aiInteractionRepository = aiInteractionRepository;
    }

    @Override
    public String call(String systemPrompt, String userMessage, String type, Long userId, Long ticketId) {
        if (!isAvailable()) {
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
            headers.set("x-api-key", llmProperties.getApiKey());
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = Map.of(
                    "model", llmProperties.getModel(),
                    "max_tokens", 2048,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            var request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(resolveApiUrl(), request, Map.class);

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

    @Override
    public boolean isAvailable() {
        return llmProperties.getApiKey() != null
                && !llmProperties.getApiKey().isBlank()
                && resolveApiUrl() != null
                && !resolveApiUrl().isBlank();
    }

    private String resolveApiUrl() {
        if (llmProperties.getBaseUrl() != null && !llmProperties.getBaseUrl().isBlank()) {
            return llmProperties.getBaseUrl();
        }
        return llmProperties.getProviders().getAnthropic().getBaseUrl();
    }
}
