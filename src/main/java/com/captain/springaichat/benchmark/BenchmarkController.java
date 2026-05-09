package com.captain.springaichat.benchmark;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * Runs the full benchmark and returns a cost report.
     * Takes ~30-60 seconds on first run (cold cache).
     * Run it twice — second run shows the cache effect clearly.
     *
     * POST /benchmark
     */
    @PostMapping
    public ResponseEntity<BenchmarkService.BenchmarkResult> runBenchmark() {
        return ResponseEntity.ok(benchmarkService.run());
    }
}
