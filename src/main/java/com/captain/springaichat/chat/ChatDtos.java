package com.captain.springaichat.chat;

public class ChatDtos {

    public record ChatRequest(String userId, String message) {}

    public record ChatResponse(
            String answer,
            String conversationId,
            long latencyMs
    ) {}

    public record HistoryMessage(String role, String content) {}
}
