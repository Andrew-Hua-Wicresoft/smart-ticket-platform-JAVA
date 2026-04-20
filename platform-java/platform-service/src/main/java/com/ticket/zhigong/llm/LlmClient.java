package com.ticket.zhigong.llm;

public interface LlmClient {

    String call(String systemPrompt, String userMessage, String type, Long userId, Long ticketId);

    boolean isAvailable();
}
