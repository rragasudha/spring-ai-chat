package com.captain.springaichat.benchmark;

import com.captain.springaichat.cache.CacheStatsService;
import com.captain.springaichat.chat.ChatService;
import com.captain.springaichat.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BenchmarkService {

    // Nova Micro pricing (us-east-1) as of May 2026
    private static final double INPUT_COST_PER_1K_TOKENS  = 0.000035;
    private static final double OUTPUT_COST_PER_1K_TOKENS = 0.000140;
    private static final int    AVG_INPUT_TOKENS          = 60;
    private static final int    AVG_OUTPUT_TOKENS         = 150;

    // 20 semantically similar questions about Spring AI
    // Slight rephrasing tests whether the cache recognises them as equivalent
    private static final List<String> QUESTIONS = List.of(
            "What is Spring AI?",
            "Tell me about Spring AI",
            "Explain Spring AI to me",
            "What does Spring AI do?",
            "Can you describe Spring AI?",
            "What is the Spring AI framework?",
            "Give me an overview of Spring AI",
            "What is Spring AI used for?",
            "How does Spring AI work?",
            "What problem does Spring AI solve?",
            "What is Spring AI in simple terms?",
            "Could you explain what Spring AI is?",
            "I'd like to know about Spring AI",
            "Spring AI — what is it exactly?",
            "What's the purpose of Spring AI?",
            "Describe the Spring AI project",
            "What can Spring AI help me with?",
            "Is Spring AI a framework?",
            "What does Spring AI provide?",
            "Tell me what Spring AI is"
    );

    private final ChatService chatService;
    private final CacheStatsService cacheStats;
    private final AppProperties props;

    public BenchmarkService(ChatService chatService,
                            CacheStatsService cacheStats,
                            AppProperties props) {
        this.chatService = chatService;
        this.cacheStats  = cacheStats;
        this.props       = props;
    }

    /**
     * Runs the benchmark:
     * 1. Resets cache stats counters
     * 2. Fires N questions (capped at app.benchmark.max-questions)
     * 3. Tracks latency per question to determine cache hits vs LLM calls
     * 4. Calculates estimated Bedrock cost with and without cache
     *
     * Note: first question always hits the LLM (cold cache).
     * Subsequent similar questions should hit the cache.
     */
    public BenchmarkResult run() {
        cacheStats.resetStats();

        int total     = Math.min(QUESTIONS.size(), props.getBenchmark().getMaxQuestions());
        int llmCalls  = 0;
        int cacheHits = 0;
        long totalLatency = 0;

        String benchmarkUserId = "benchmark-" + System.currentTimeMillis();

        for (int i = 0; i < total; i++) {
            var response = chatService.chat(benchmarkUserId, QUESTIONS.get(i));
            totalLatency += response.latencyMs();

            // Heuristic: fast response = cache hit, slow = LLM call
            if (response.latencyMs() < 200) {
                cacheHits++;
            } else {
                llmCalls++;
            }
        }

        double costWithoutCache = calculateCost(total);
        double costWithCache    = calculateCost(llmCalls);
        double savings          = costWithoutCache - costWithCache;
        double hitRate          = total > 0 ? (double) cacheHits / total * 100 : 0;

        return new BenchmarkResult(
                total,
                llmCalls,
                cacheHits,
                Math.round(hitRate * 100.0) / 100.0,
                totalLatency,
                total > 0 ? totalLatency / total : 0,
                Math.round(costWithoutCache * 1_000_000.0) / 1_000_000.0,
                Math.round(costWithCache    * 1_000_000.0) / 1_000_000.0,
                Math.round(savings          * 1_000_000.0) / 1_000_000.0
        );
    }

    private double calculateCost(int calls) {
        double inputCost  = (calls * AVG_INPUT_TOKENS  / 1000.0) * INPUT_COST_PER_1K_TOKENS;
        double outputCost = (calls * AVG_OUTPUT_TOKENS / 1000.0) * OUTPUT_COST_PER_1K_TOKENS;
        return inputCost + outputCost;
    }

    public record BenchmarkResult(
            int    totalQuestions,
            int    llmCalls,
            int    cacheHits,
            double cacheHitRatePct,
            long   totalLatencyMs,
            long   avgLatencyMs,
            double estimatedCostWithoutCacheUsd,
            double estimatedCostWithCacheUsd,
            double estimatedSavingsUsd
    ) {}
}
