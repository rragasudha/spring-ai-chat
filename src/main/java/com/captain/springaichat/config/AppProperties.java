package com.captain.springaichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Chat chat = new Chat();
    private Cache cache = new Cache();
    private Benchmark benchmark = new Benchmark();

    public static class Chat {
        private int maxMessages = 20;
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    }

    public static class Cache {
        private double similarityThreshold = 0.93;
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double t) { this.similarityThreshold = t; }
    }

    public static class Benchmark {
        private int maxQuestions = 20;
        public int getMaxQuestions() { return maxQuestions; }
        public void setMaxQuestions(int maxQuestions) { this.maxQuestions = maxQuestions; }
    }

    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }
    public Benchmark getBenchmark() { return benchmark; }
    public void setBenchmark(Benchmark benchmark) { this.benchmark = benchmark; }
}
