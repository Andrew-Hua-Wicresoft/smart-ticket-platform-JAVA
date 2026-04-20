package com.ticket.zhigong.config;

import com.ticket.zhigong.llm.LlmClient;
import com.ticket.zhigong.llm.NoopLlmClient;
import com.ticket.zhigong.llm.OpenAiCompatibleLlmClient;
import com.ticket.zhigong.repository.AiInteractionRepository;
import com.ticket.zhigong.service.ClaudeApiService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmClientConfiguration {

    @Bean
    public LlmClient llmClient(RestTemplateBuilder restTemplateBuilder,
                               LlmProperties llmProperties,
                               AiInteractionRepository aiInteractionRepository) {
        return switch (normalizeProvider(llmProperties.getProvider())) {
            case "anthropic" -> new ClaudeApiService(
                    restTemplateBuilder,
                    llmProperties,
                    aiInteractionRepository
            );
            case "openai", "deepseek", "openai-compatible" -> new OpenAiCompatibleLlmClient(
                    restTemplateBuilder,
                    llmProperties,
                    aiInteractionRepository
            );
            default -> new NoopLlmClient(llmProperties.getProvider());
        };
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "anthropic" : provider.trim().toLowerCase();
    }
}
