package com.captain.springaichat.cache;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

public class SemanticCacheAdvisor implements CallAdvisor {

    private static final String CACHE_ANSWER_KEY = "cache_answer";

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final int order;

    private SemanticCacheAdvisor(VectorStore vectorStore, double similarityThreshold, int order) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
        this.order = order;
    }

    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    @Override
    public String getName() {
        return "SemanticCacheAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = extractUserText(request.prompt());

        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userText)
                        .topK(1)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
        String scoreInfo = hits.isEmpty() ? "none" : String.valueOf(hits.getFirst().getScore());
        System.out.println("=== CACHE DEBUG === query: " + userText + " | hits: " + hits.size() + " | score: " + scoreInfo + " | threshold: " + similarityThreshold);

        if (!hits.isEmpty()) {
            String cachedAnswer = (String) hits.getFirst().getMetadata().get(CACHE_ANSWER_KEY);
            if (cachedAnswer != null) {
                ChatResponse cached = new ChatResponse(List.of(
                        new Generation(new AssistantMessage(cachedAnswer))
                ));
                return new ChatClientResponse(cached, request.context());
            }
        }

        ChatClientResponse response = chain.nextCall(request);

        String answer = response.chatResponse().getResult().getOutput().getText();
        vectorStore.add(List.of(
                Document.builder()
                        .text(userText)
                        .metadata(Map.of(CACHE_ANSWER_KEY, answer))
                        .build()
        ));

        return response;
    }

    private String extractUserText(Prompt prompt) {
        return prompt.getInstructions().stream()
                .filter(m -> m.getMessageType().name().equals("USER"))
                .reduce((first, second) -> second)
                .map(m -> m.getText())
                .orElse("");
    }

    public static class Builder {
        private final VectorStore vectorStore;
        private double similarityThreshold = 0.93;
        private int order = 1;

        private Builder(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public SemanticCacheAdvisor build() {
            return new SemanticCacheAdvisor(vectorStore, similarityThreshold, order);
        }
    }
}
