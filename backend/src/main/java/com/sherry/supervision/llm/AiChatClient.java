package com.sherry.supervision.llm;

public interface AiChatClient {

    boolean isAvailable();

    String chat(String systemPrompt, String userMessage);
}
