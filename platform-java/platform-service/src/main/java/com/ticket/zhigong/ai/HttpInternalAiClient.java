package com.ticket.zhigong.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.zhigong.config.AiServiceProperties;
import com.ticket.zhigong.entity.AiInteraction;
import com.ticket.zhigong.repository.AiInteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class HttpInternalAiClient implements InternalAiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpInternalAiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiInteractionRepository aiInteractionRepository;

    public HttpInternalAiClient(RestClient.Builder restClientBuilder,
                                AiServiceProperties aiServiceProperties,
                                ObjectMapper objectMapper,
                                AiInteractionRepository aiInteractionRepository) {
        this.restClient = restClientBuilder
                .baseUrl(aiServiceProperties.getUrl())
                .build();
        this.objectMapper = objectMapper;
        this.aiInteractionRepository = aiInteractionRepository;
    }

    @Override
    public List<SearchResult> searchKnowledgeBase(String query, int topK, Long userId) {
        return post(
                "/internal/ai/search",
                new SearchRequest(query, topK),
                new ParameterizedTypeReference<List<SearchResult>>() {},
                "SEARCH",
                userId,
                null
        );
    }

    @Override
    public TextResponse generateFollowUpQuestions(String title, String description, Long userId) {
        return post(
                "/internal/ai/follow-up-questions",
                new TextGenerationRequest(title, description, null),
                TextResponse.class,
                "FOLLOW_UP_QUESTIONS",
                userId,
                null
        );
    }

    @Override
    public TextResponse suggest(Long ticketId, String title, String description, String context, Long userId) {
        return post(
                "/internal/ai/suggest",
                new TextGenerationRequest(title, description, context),
                TextResponse.class,
                "SUGGEST",
                userId,
                ticketId
        );
    }

    @Override
    public AnalyzeResponse analyzeTicket(Long ticketId, String title, String description, Long userId) {
        return post(
                "/internal/ai/analyze-ticket",
                new AnalyzeTicketRequest(title, description),
                AnalyzeResponse.class,
                "ANALYZE_TICKET",
                userId,
                ticketId
        );
    }

    @Override
    public TextResponse generateKnowledgeDraft(Long ticketId,
                                               String title,
                                               String description,
                                               String resolutionNotes,
                                               Long userId) {
        return post(
                "/internal/ai/kb-draft-generate",
                new TextGenerationRequest(title, description, resolutionNotes),
                TextResponse.class,
                "KB_DRAFT_GENERATE",
                userId,
                ticketId
        );
    }

    @Override
    public ReindexResponse reindexKnowledgeBase(int limit) {
        return post(
                "/internal/ai/reindex",
                Map.of("entity_type", "knowledge-base", "limit", limit),
                ReindexResponse.class,
                "REINDEX",
                null,
                null
        );
    }

    private <T> T post(String path,
                       Object request,
                       Class<T> responseType,
                       String type,
                       Long userId,
                       Long ticketId) {
        Instant startedAt = Instant.now();
        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            persistInteraction(type, userId, ticketId, request, response, null, startedAt);
            return response;
        } catch (Exception ex) {
            persistInteraction(type, userId, ticketId, request, null, ex, startedAt);
            log.warn("Internal AI call failed [type={}, ticketId={}]: {}", type, ticketId, ex.getMessage());
            return null;
        }
    }

    private <T> T post(String path,
                       Object request,
                       ParameterizedTypeReference<T> responseType,
                       String type,
                       Long userId,
                       Long ticketId) {
        Instant startedAt = Instant.now();
        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            persistInteraction(type, userId, ticketId, request, response, null, startedAt);
            return response;
        } catch (Exception ex) {
            persistInteraction(type, userId, ticketId, request, null, ex, startedAt);
            log.warn("Internal AI call failed [type={}, ticketId={}]: {}", type, ticketId, ex.getMessage());
            return null;
        }
    }

    private void persistInteraction(String type,
                                    Long userId,
                                    Long ticketId,
                                    Object request,
                                    Object response,
                                    Exception error,
                                    Instant startedAt) {
        try {
            AiInteraction interaction = new AiInteraction();
            interaction.setType(type);
            interaction.setUserId(userId);
            interaction.setTicketId(ticketId);
            interaction.setInputText(serialize(request));
            interaction.setOutputText(response != null ? serialize(response) : null);
            interaction.setLatencyMs(Duration.between(startedAt, Instant.now()).toMillis());
            interaction.setSuccess(error == null);
            interaction.setErrorMessage(error == null ? null : truncate(error.getMessage(), 500));
            aiInteractionRepository.save(interaction);
        } catch (Exception persistenceError) {
            log.warn("Failed to persist AI interaction log: {}", persistenceError.getMessage());
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record TextGenerationRequest(
            String title,
            String description,
            String context
    ) {}

    private record AnalyzeTicketRequest(
            String title,
            String description
    ) {}

    private record SearchRequest(
            String query,
            @JsonProperty("top_k")
            int topK
    ) {}
}
