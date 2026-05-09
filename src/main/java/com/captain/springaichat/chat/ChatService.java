package com.captain.springaichat.chat;

import com.captain.springaichat.cache.CacheStatsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final CacheStatsService cacheStats;

    public ChatService(ChatClient chatClient,
                       ChatMemory chatMemory,
                       CacheStatsService cacheStats) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.cacheStats = cacheStats;
    }

    /**
     * Sends a message through the full advisor chain.
     *
     * The critical line is: .param(ChatMemory.CONVERSATION_ID, userId)
     *
     * This is the Spring AI 1.0 requirement — conversation ID must be
     * passed explicitly per request. Without this, the memory advisor
     * has no way to scope messages to the right user. In older versions
     * this defaulted silently, meaning ALL users shared one memory — a
     * serious data leak in any multi-user system.
     *
     * The advisors (cache + memory) are already wired into the ChatClient
     * via AdvisorChainConfig. We don't re-specify them here — just pass
     * the conversationId param that the memory advisor reads.
     */
    public ChatDtos.ChatResponse chat(String userId, String message) {
        long start = System.currentTimeMillis();

        String answer = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();

        long latency = System.currentTimeMillis() - start;

        // Track cache hit/miss based on latency heuristic:
        // Cache hits are typically < 200ms (just a Redis lookup + embedding)
        // LLM calls are typically > 500ms
        if (latency < 200) {
            cacheStats.recordHit();
        } else {
            cacheStats.recordMiss();
        }

        return new ChatDtos.ChatResponse(answer, userId, latency);
    }

    /**
     * Retrieves conversation history for a user from PostgreSQL.
     * Returns messages in chronological order.
     */
    public java.util.List<ChatDtos.HistoryMessage> getHistory(String userId) {
        return chatMemory.get(userId)
                .stream()
                .map(msg -> new ChatDtos.HistoryMessage(
                        msg.getMessageType().name(),
                        msg.getText()
                ))
                .toList();
    }

    /**
     * Clears all conversation history for a user from PostgreSQL.
     */
    public void clearHistory(String userId) {
        chatMemory.clear(userId);
    }
}
