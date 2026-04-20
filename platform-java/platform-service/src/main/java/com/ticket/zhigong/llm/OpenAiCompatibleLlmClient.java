package com.ticket.zhigong.llm;

import com.ticket.zhigong.config.LlmProperties;
import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final RestTemplate restTemplate;
    private final LlmProperties llmProperties;
    private final AiInteractionRepository aiInteractionRepository;

    public OpenAiCompatibleLlmClient(RestTemplateBuilder restTemplateBuilder,
                                     LlmProperties llmProperties,
                                     AiInteractionRepository aiInteractionRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.llmProperties = llmProperties;
        this.aiInteractionRepository = aiInteractionRepository;
    }

    @Override
    public String call(String systemPrompt, String userMessage, String type, Long userId, Long ticketId) {
        if (!isAvailable()) {
            log.warn("LLM provider {} not configured, AI features disabled", llmProperties.getProvider());
            return null;
        }

        long startTime = System.currentTimeMillis();
        AiInteraction interaction = prepareInteraction(type, userId, ticketId, userMessage);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getApiKey());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", llmProperties.getModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));

            var request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(resolveBaseUrl(), request, Map.class);

            String text = extractText(response.getBody());
            if (text != null) {
                interaction.setOutputText(text.length() > 2000 ? text.substring(0, 2000) : text);
                interaction.setSuccess(true);
                interaction.setLatencyMs(System.currentTimeMillis() - startTime);
                aiInteractionRepository.save(interaction);
                return text;
            }

            interaction.setSuccess(false);
            interaction.setErrorMessage("Empty response from OpenAI-compatible provider");
            interaction.setLatencyMs(System.currentTimeMillis() - startTime);
            aiInteractionRepository.save(interaction);
            return null;
        } catch (RestClientException e) {
            log.warn("OpenAI-compatible provider call failed ({}): {}", type, e.getMessage());
            interaction.setSuccess(false);
            interaction.setErrorMessage(truncate(e.getMessage()));
            interaction.setLatencyMs(System.currentTimeMillis() - startTime);
            aiInteractionRepository.save(interaction);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return llmProperties.getApiKey() != null
                && !llmProperties.getApiKey().isBlank()
                && resolveBaseUrl() != null
                && !resolveBaseUrl().isBlank();
    }

    private AiInteraction prepareInteraction(String type, Long userId, Long ticketId, String userMessage) {
        AiInteraction interaction = new AiInteraction();
        interaction.setType(type);
        interaction.setUserId(userId);
        interaction.setTicketId(ticketId);
        interaction.setInputText(userMessage.length() > 2000 ? userMessage.substring(0, 2000) : userMessage);
        return interaction;
    }

    private String resolveBaseUrl() {
        if (llmProperties.getBaseUrl() != null && !llmProperties.getBaseUrl().isBlank()) {
            return llmProperties.getBaseUrl();
        }

        return switch (llmProperties.getProvider() == null ? "" : llmProperties.getProvider().trim().toLowerCase()) {
            case "deepseek" -> llmProperties.getProviders().getDeepseek().getBaseUrl();
            case "openai", "openai-compatible" -> llmProperties.getProviders().getOpenai().getBaseUrl();
            default -> llmProperties.getBaseUrl();
        };
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return null;
        }

        Object choices = responseBody.get("choices");
        if (choices instanceof List<?> choicesList && !choicesList.isEmpty()) {
            Object first = choicesList.get(0);
            if (first instanceof Map<?, ?> firstChoice) {
                Object message = firstChoice.get("message");
                if (message instanceof Map<?, ?> messageMap) {
                    Object content = messageMap.get("content");
                    if (content instanceof String text) {
                        return text;
                    }
                }
            }
        }

        return null;
    }

    private String truncate(String input) {
        if (input == null) {
            return null;
        }
        return input.length() > 500 ? input.substring(0, 500) : input;
    }
}
