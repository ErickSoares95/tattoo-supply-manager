package com.ericksoares.tattoo.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
class OllamaLlmClient implements LlmClient {

    private final ChatClient chatClient;

    public OllamaLlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String prompt) {

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
