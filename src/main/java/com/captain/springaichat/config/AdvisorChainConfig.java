package com.captain.springaichat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import com.captain.springaichat.cache.SemanticCacheAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.model.ChatModel;

@Configuration
public class AdvisorChainConfig {

    /**
     * The advisor chain is the core of this project.
     *
     * ORDER MATTERS — advisors run in ascending order value:
     *
     * Order 1 — SemanticCacheAdvisor:
     *   Converts the user question to an embedding, searches Redis for a
     *   semantically similar past question. If similarity > threshold (0.93),
     *   returns the cached answer immediately — NO LLM call, NO memory lookup.
     *   This is the cost-saving layer.
     *
     * Order 2 — MessageChatMemoryAdvisor:
     *   Only runs if cache missed. Fetches the last N messages for this
     *   conversationId from PostgreSQL and injects them into the prompt so
     *   the LLM has context of the ongoing conversation.
     *
     * Why cache before memory?
     *   If the cache hits, we skip the DB query entirely. Putting memory first
     *   would mean always hitting PostgreSQL even for questions we already know
     *   the answer to — wasteful and slower.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 VectorStore vectorStore,
                                 ChatMemory chatMemory,
                                 AppProperties props) {

        SemanticCacheAdvisor semanticCacheAdvisor = SemanticCacheAdvisor
                .builder(vectorStore)
                .order(1)
                .similarityThreshold(props.getCache().getSimilarityThreshold())
                .build();

        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(2)
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(semanticCacheAdvisor, memoryAdvisor)
                .defaultSystem("""
                        You are a helpful customer support assistant for a software product.
                        Answer concisely and accurately.
                        If you don't know the answer, say so — don't make things up.
                        """)
                .build();
    }
}
