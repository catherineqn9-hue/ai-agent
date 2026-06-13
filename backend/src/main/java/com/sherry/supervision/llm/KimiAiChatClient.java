package com.sherry.supervision.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KimiAiChatClient implements AiChatClient {

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final boolean logRequests;
    private final boolean logResponses;
    private ChatModel chatModel;

    public KimiAiChatClient(
            @Value("${langchain4j.open-ai.chat-model.api-key:}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.base-url:https://api.moonshot.cn/v1}") String baseUrl,
            @Value("${langchain4j.open-ai.chat-model.model-name:moonshot-v1-8k}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.log-requests:false}") boolean logRequests,
            @Value("${langchain4j.open-ai.chat-model.log-responses:false}") boolean logResponses) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.logRequests = logRequests;
        this.logResponses = logResponses;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("Kimi API key is not configured");
        }
        String prompt = systemPrompt + "\n\n用户问题：\n" + userMessage;
        return model().chat(prompt);
    }

    private ChatModel model() {
        if (chatModel == null) {
            chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(0.2)
                    .timeout(Duration.ofSeconds(30))
                    .maxRetries(1)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .build();
        }
        return chatModel;
    }
}
