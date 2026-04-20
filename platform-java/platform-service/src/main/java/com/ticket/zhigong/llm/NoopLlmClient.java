package com.ticket.zhigong.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(NoopLlmClient.class);

    private final String provider;

    public NoopLlmClient(String provider) {
        this.provider = provider;
    }

    @Override
    public String call(String systemPrompt, String userMessage, String type, Long userId, Long ticketId) {
        log.warn("LLM provider {} is not implemented yet, falling back to disabled AI behavior", provider);
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
