package com.captain.springaichat.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    /**
     * ChatMemory backed by PostgreSQL via JdbcChatMemoryRepository.
     *
     * MessageWindowChatMemory keeps a sliding window of the last N messages
     * per conversationId. Older messages are dropped automatically once the
     * window is full — this keeps prompt sizes (and Bedrock costs) bounded.
     *
     * JdbcChatMemoryRepository is auto-configured by Spring AI when the
     * spring-ai-starter-chat-memory-repository-jdbc dependency is present
     * and spring.ai.chat.memory.repository.jdbc.initialize-schema=always
     * is set. We just inject it here.
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository,
                                 AppProperties props) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(props.getChat().getMaxMessages())
                .build();
    }
}
