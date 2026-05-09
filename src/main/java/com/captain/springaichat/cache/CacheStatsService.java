package com.captain.springaichat.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheStatsService {

    private static final String HITS_KEY  = "stats:cache:hits";
    private static final String MISSES_KEY = "stats:cache:misses";

    private final StringRedisTemplate redis;

    public CacheStatsService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordHit()  { redis.opsForValue().increment(HITS_KEY); }
    public void recordMiss() { redis.opsForValue().increment(MISSES_KEY); }

    public CacheStats getStats() {
        String hits   = redis.opsForValue().get(HITS_KEY);
        String misses = redis.opsForValue().get(MISSES_KEY);

        long h = hits   != null ? Long.parseLong(hits)   : 0;
        long m = misses != null ? Long.parseLong(misses) : 0;
        long total = h + m;
        double hitRate = total > 0 ? (double) h / total * 100 : 0;

        return new CacheStats(h, m, total, Math.round(hitRate * 100.0) / 100.0);
    }

    public void resetStats() {
        redis.delete(HITS_KEY);
        redis.delete(MISSES_KEY);
    }

    public record CacheStats(long hits, long misses, long total, double hitRatePct) {}
}
