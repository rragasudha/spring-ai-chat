package com.captain.springaichat.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Main chat endpoint. Pass userId to scope conversation memory.
     *
     * Example:
     * POST /chat
     * { "userId": "captain", "message": "What is Spring AI?" }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatDtos.ChatResponse> chat(
            @RequestBody ChatDtos.ChatRequest request) {
        ChatDtos.ChatResponse response = chatService.chat(
                request.userId(), request.message());
        return ResponseEntity.ok(response);
    }

    /**
     * View full conversation history for a user.
     * Fetched from PostgreSQL — survives app restarts.
     *
     * GET /history/captain
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatDtos.HistoryMessage>> history(
            @PathVariable String userId) {
        return ResponseEntity.ok(chatService.getHistory(userId));
    }

    /**
     * Clear conversation history for a user.
     * Useful for testing or when user wants to start fresh.
     *
     * DELETE /history/captain
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String userId) {
        chatService.clearHistory(userId);
        return ResponseEntity.noContent().build();
    }
}
