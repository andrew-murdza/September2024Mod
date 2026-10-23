package net.amurdza.stackbench;

import java.util.Map;

/** Standalone Java assertions: no Minecraft runtime or test dependency required. */
public final class BenchmarkMetricsChecks {
    public static void main(String[] args) {
        var empty = BenchmarkMetrics.summarizeDurations(new long[0]);
        equal(empty, "count", 0);
        equal(empty, "p99", 0);
        var edges = BenchmarkMetrics.summarizeDurations(new long[] {
                1_000_000, 50_000_000, 50_000_001, 100_000_000, 100_000_001});
        equal(edges, "over50Ms", 3);
        equal(edges, "over100Ms", 1);
        equal(edges, "p50", 50.000001);
        long[] values = new long[100];
        for (int i = 0; i < values.length; i++) values[i] = (100 - i) * 1_000_000L;
        var ranks = BenchmarkMetrics.summarizeDurations(values);
        equal(ranks, "mean", 50.5);
        equal(ranks, "p50", 50);
        equal(ranks, "p95", 95);
        equal(ranks, "p99", 99);
        equal(ranks, "max", 100);
        if (values[0] != 100_000_000L) throw new AssertionError("Input was mutated");

        var slow = BenchmarkMetrics.slowTickSamples(new long[] {1_000_000, 50_000_000, 51_000_000, 100_000_000},
                new long[] {50_000_000, 50_000_000, 70_000_000});
        equal(slow, "totalCount", 2);
        equal(slow, "omittedCount", 0);
        var slowSamples = (java.util.List<?>) slow.get("samples");
        var secondSlow = (Map<?, ?>) slowSamples.get(1);
        if (((Number) secondSlow.get("sample_index")).intValue() != 3
                || ((Number) secondSlow.get("offset_since_first_tick_ms")).doubleValue() != 170)
            throw new AssertionError("Slow-tick offsets do not follow the completed-tick intervals");
        long[] manySlow = new long[101], manyIntervals = new long[100];
        java.util.Arrays.fill(manySlow, 51_000_000);
        java.util.Arrays.fill(manyIntervals, 50_000_000);
        manySlow[100] = 170_000_000;
        var bounded = BenchmarkMetrics.slowTickSamples(manySlow, manyIntervals);
        equal(bounded, "totalCount", 101);
        equal(bounded, "omittedCount", 1);
        var boundedSamples = (java.util.List<?>) bounded.get("samples");
        var longest = (Map<?, ?>) boundedSamples.get(99);
        if (boundedSamples.size() != 100 || ((Number) longest.get("sample_index")).intValue() != 100
                || ((Number) longest.get("duration_ms")).doubleValue() != 170
                || ((Number) longest.get("offset_since_first_tick_ms")).doubleValue() != 5000)
            throw new AssertionError("Bounded slow samples must retain the phase maximum");

        // Starting at an END handler must not claim the tick that is already running.
        BenchmarkMetrics.tickStarted();
        BenchmarkMetrics.begin("boundary-check");
        BenchmarkMetrics.tickFinished();
        if (BenchmarkMetrics.completedTicks() != 0) throw new AssertionError("Counted a partial starting tick");
        BenchmarkMetrics.tickStarted();
        BenchmarkMetrics.tickFinished();
        BenchmarkMetrics.tickStarted();
        BenchmarkMetrics.tickFinished();
        BenchmarkMetrics.tickStarted();
        Map<String, Object> phase = BenchmarkMetrics.finish();
        BenchmarkMetrics.tickFinished();
        equal(phase, "completedTicks", 2);
        if (!Boolean.TRUE.equals(phase.get("excludedInProgressTick")))
            throw new AssertionError("Counted a partial ending tick");
        if (BenchmarkMetrics.isActive()) throw new AssertionError("Finished phase remains active");
        var durations = (Map<?, ?>) phase.get("fullTickMs");
        var intervals = (Map<?, ?>) phase.get("tickStartIntervalMs");
        if (((Number) durations.get("count")).intValue() != 2
                || ((Number) intervals.get("count")).intValue() != 1)
            throw new AssertionError("Tick interval count does not match complete sample pairs");
        System.out.println("Benchmark metrics checks passed");
    }

    private static void equal(Map<String, Object> map, String key, double expected) {
        double actual = ((Number) map.get(key)).doubleValue();
        if (Math.abs(actual - expected) > 0.00000001)
            throw new AssertionError(key + ": expected " + expected + ", got " + actual);
    }
}
