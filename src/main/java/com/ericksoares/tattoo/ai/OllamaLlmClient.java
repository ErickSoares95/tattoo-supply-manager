package com.ericksoares.tattoo.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class OllamaLlmClient implements LlmClient {

    private final ChatClient chatClient;

    public OllamaLlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String prompt) {

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            // Ollama down or not deployed: a 503 for the caller instead of a 500.
            log.warn("LLM call failed: {}", e.getMessage());
            throw new AssistantUnavailableException();
        }
    }
}
