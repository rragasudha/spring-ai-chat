package com.captain.springaichat.cache;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheStatsService cacheStats;

    public CacheController(CacheStatsService cacheStats) {
        this.cacheStats = cacheStats;
    }

    @GetMapping("/stats")
    public ResponseEntity<CacheStatsService.CacheStats> stats() {
        return ResponseEntity.ok(cacheStats.getStats());
    }

    @DeleteMapping("/stats")
    public ResponseEntity<Void> resetStats() {
        cacheStats.resetStats();
        return ResponseEntity.noContent().build();
    }
}
