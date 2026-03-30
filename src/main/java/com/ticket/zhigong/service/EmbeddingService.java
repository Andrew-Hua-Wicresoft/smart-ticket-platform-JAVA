package com.ticket.zhigong.service;

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
 * Client for the Python FastAPI embedding sidecar.
 * POST /embed {"text": "..."} -> {"embedding": [1024 floats]}
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int MAX_CHARS_FOR_EMBEDDING = 1000;

    private final RestTemplate restTemplate;
    private final String sidecarUrl;

    public EmbeddingService(
            @Value("${embedding.sidecar.url:http://localhost:8100}") String sidecarUrl) {
        this.restTemplate = new RestTemplate();
        this.sidecarUrl = sidecarUrl;
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var body = Map.of("text", truncated);
            var request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    sidecarUrl + "/embed", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Number> embedding = (List<Number>) response.getBody().get("embedding");
                if (embedding != null) {
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    return result;
                }
            }
        } catch (RestClientException e) {
            log.warn("Embedding sidecar unavailable: {}", e.getMessage());
        }

        return null;
    }
}
