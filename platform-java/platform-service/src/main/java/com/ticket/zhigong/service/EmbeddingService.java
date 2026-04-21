package com.ticket.zhigong.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for the Python FastAPI embedding sidecar.
 * POST /embed {"text": "..."} -> {"embedding": [1024 floats]}
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int MAX_CHARS_FOR_EMBEDDING = 1000;

    private final RestClient restClient;

    public EmbeddingService(RestClient.Builder restClientBuilder,
                            @org.springframework.beans.factory.annotation.Value("${ai.service.url:${embedding.sidecar.url:http://localhost:8100}}") String aiServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(aiServiceUrl).build();
    }

    /**
     * Embed text using the Python sidecar. Truncates to first 1000 chars for embedding.
     * Returns null if the sidecar is unavailable (graceful degradation).
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return null;

        // Truncate for embedding, full text stored separately
        String truncated = text.length() > MAX_CHARS_FOR_EMBEDDING
                ? text.substring(0, MAX_CHARS_FOR_EMBEDDING)
                : text;

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/embed")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("text", truncated))
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                List<Number> embedding = (List<Number>) response.get("embedding");
                if (embedding != null) {
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    return result;
                }
            }
        } catch (Exception ex) {
            log.warn("AI service embedding endpoint unavailable: {}", ex.getMessage());
        }

        return null;
    }
}
