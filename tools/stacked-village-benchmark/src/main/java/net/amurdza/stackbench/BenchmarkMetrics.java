package net.amurdza.stackbench;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

/** QA-only measurements. Call the phase API from the dedicated server thread. */
public final class BenchmarkMetrics {
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static Phase active;
    private static Phase inFlight;
    private static long tickStartedNanos;

    private BenchmarkMetrics() { }

    /** Arms a phase at the next tick HEAD, so setup in the current tick is excluded. */
    public static void begin(String name) {
        if (active != null) throw new IllegalStateException("A benchmark phase is already active: " + active.name);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("A phase requires a name");
        Phase phase = new Phase(name);
        // These snapshots may initialize management beans. Do that outside the
        // first timed tick so their one-time cost cannot inflate its maximum.
        phase.gcAtStart = garbageCollectionSnapshot();
        phase.heapAtStart = heapSnapshot();
        active = phase;
    }

    public static boolean isActive() { return active != null; }

    public static int completedTicks() { return active == null ? 0 : active.work.size; }

    public static double elapsedSeconds() {
        // End a phase only after a fully measured tick crossed the deadline.
        // Consulting the current clock in an END subscriber would discard the
        // very slow final tick that caused the deadline to be reached.
        return active == null || active.work.size == 0 ? 0
                : (active.lastCompletedNanos - active.startedNanos) / NANOS_PER_SECOND;
    }

    /** Record QA work inside a measured tick without subtracting it from timings. */
    public static void recordDiagnosticNanos(String kind, long nanos) {
        if (kind == null || kind.isBlank() || nanos < 0) throw new IllegalArgumentException("Invalid diagnostic sample");
        if (active == null || inFlight != active) return;
        active.diagnostics.computeIfAbsent(kind, ignored -> new LongSeries()).add(nanos);
    }

    /**
     * Ends the phase and returns a Gson-serializable report. An in-progress tick is
     * excluded: an END event subscriber runs before tickServer's RETURN injection.
     * The sample window ends at the last completed tick, not at this method call.
     */
    public static Map<String, Object> finish() {
        Phase phase = active;
        if (phase == null) throw new IllegalStateException("No benchmark phase is active");
        active = null;
        boolean excludedInFlight = inFlight == phase;
        if (excludedInFlight) inFlight = null;
        long requestedEndNanos = System.nanoTime();
        long measuredNanos = phase.work.size == 0 ? 0 : phase.lastCompletedNanos - phase.startedNanos;
        double seconds = measuredNanos / NANOS_PER_SECOND;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("phase", phase.name);
        report.put("completedTicks", phase.work.size);
        report.put("sampleWindowSeconds", seconds);
        report.put("phaseElapsedSeconds", phase.startedNanos == 0 ? 0
                : (requestedEndNanos - phase.startedNanos) / NANOS_PER_SECOND);
        report.put("completedTicksPerWallSecond", seconds == 0 ? 0 : phase.work.size / seconds);
        long[] durations = phase.work.copy(), intervals = phase.intervals.copy();
        report.put("fullTickMs", summarizeDurations(durations));
        report.put("tickStartIntervalMs", summarizeDurations(intervals));
        report.put("slowTickSamples", slowTickSamples(durations, intervals));
        report.put("excludedInProgressTick", excludedInFlight);
        report.put("incompleteTicks", phase.incompleteTicks);
        report.put("heapAtStart", phase.heapAtStart == null ? Map.of() : phase.heapAtStart);
        report.put("heapAtFinish", heapSnapshot());
        report.put("garbageCollection", garbageCollectionDelta(phase.gcAtStart));
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        phase.diagnostics.forEach((kind, values) -> {
            long[] samples = values.copy();
            Map<String, Object> summary = summarizeDurations(samples);
            double total = 0;
            for (long sample : samples) total += sample / NANOS_PER_MILLISECOND;
            summary.put("totalMs", total);
            diagnostics.put(kind, summary);
        });
        report.put("measuredDiagnosticOverheadMs", diagnostics);
        report.put("diagnosticOverheadSubtracted", false);
        report.put("timingMethod", "System.nanoTime at MinecraftServer.tickServer HEAD/RETURN; includes Forge END handlers, excludes between-tick idle wait");
        report.put("quantileMethod", "nearest rank: sorted[ceil(p * count) - 1]");
        report.put("gcScope", "GC bean deltas cover phase arming through finish; collection time is not a precise stop-the-world pause sum");
        return report;
    }

    /** Invoked only by the timing mixin. The timestamp precedes measurement bookkeeping. */
    public static void tickStarted() {
        long now = System.nanoTime();
        if (inFlight != null && inFlight == active) inFlight.incompleteTicks++;
        inFlight = active;
        tickStartedNanos = now;
        if (inFlight != null && inFlight.startedNanos == 0) {
            inFlight.startedNanos = now;
        }
    }

    /** Invoked only by the timing mixin; no file writes, entity scans or logging. */
    public static void tickFinished() {
        long now = System.nanoTime();
        Phase phase = inFlight;
        inFlight = null;
        if (phase == null || phase != active) return;
        phase.work.add(now - tickStartedNanos);
        if (phase.work.size > 1) phase.intervals.add(tickStartedNanos - phase.lastCompletedStartNanos);
        phase.lastCompletedStartNanos = tickStartedNanos;
        phase.lastCompletedNanos = now;
    }

    /** Public for independent checks with known sample arrays; input is never mutated. */
    public static Map<String, Object> summarizeDurations(long[] samples) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        double sum = 0;
        int over50 = 0, over100 = 0;
        for (long sample : sorted) {
            if (sample < 0) throw new IllegalArgumentException("Duration must be nonnegative");
            sum += sample;
            if (sample > 50_000_000L) over50++;
            if (sample > 100_000_000L) over100++;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("count", sorted.length);
        report.put("mean", sorted.length == 0 ? 0 : sum / sorted.length / NANOS_PER_MILLISECOND);
        report.put("p50", percentileMillis(sorted, .50));
        report.put("p95", percentileMillis(sorted, .95));
        report.put("p99", percentileMillis(sorted, .99));
        report.put("max", sorted.length == 0 ? 0 : sorted[sorted.length - 1] / NANOS_PER_MILLISECOND);
        report.put("over50Ms", over50);
        report.put("over100Ms", over100);
        report.put("over50MsPercent", sorted.length == 0 ? 0 : 100.0 * over50 / sorted.length);
        report.put("over100MsPercent", sorted.length == 0 ? 0 : 100.0 * over100 / sorted.length);
        return report;
    }

    /** Built only after a phase ends, from the already collected chronological samples. */
    public static Map<String, Object> slowTickSamples(long[] durations, long[] intervals) {
        if (intervals.length != Math.max(0, durations.length - 1))
            throw new IllegalArgumentException("Expected one interval between each pair of completed ticks");
        final int limit = 100;
        // Retain the longest ticks so the phase maximum remains available even
        // when more than 100 ticks exceeded the threshold. Report them in time order.
        PriorityQueue<SlowTick> retained = new PriorityQueue<>(Comparator.comparingLong(SlowTick::durationNanos)
                .thenComparing(Comparator.comparingInt(SlowTick::index).reversed()));
        int count = 0;
        long offset = 0;
        for (int index = 0; index < durations.length; index++) {
            if (index > 0) {
                if (intervals[index - 1] < 0) throw new IllegalArgumentException("Interval must be nonnegative");
                offset += intervals[index - 1];
            }
            if (durations[index] < 0) throw new IllegalArgumentException("Duration must be nonnegative");
            if (durations[index] <= 50_000_000L) continue;
            count++;
            SlowTick sample = new SlowTick(index, offset, durations[index]);
            if (retained.size() < limit) retained.add(sample);
            else if (sample.durationNanos > retained.peek().durationNanos) {
                retained.remove();
                retained.add(sample);
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("thresholdMsExclusive", 50);
        report.put("sampleIndexBase", 0);
        report.put("retention", "Longest 100 ticks, listed in chronological order; earliest retained for equal durations");
        report.put("totalCount", count);
        report.put("omittedCount", count - retained.size());
        report.put("samples", retained.stream().sorted(Comparator.comparingInt(SlowTick::index)).map(sample -> Map.of(
                "sample_index", sample.index,
                "offset_since_first_tick_ms", sample.offsetNanos / NANOS_PER_MILLISECOND,
                "duration_ms", sample.durationNanos / NANOS_PER_MILLISECOND)).toList());
        return report;
    }

    public static Map<String, Object> machineInfo() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        var runtime = Runtime.getRuntime();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("operatingSystem", os.getName());
        report.put("operatingSystemVersion", os.getVersion());
        report.put("architecture", os.getArch());
        report.put("logicalProcessors", os.getAvailableProcessors());
        report.put("javaVersion", System.getProperty("java.version"));
        report.put("javaVm", System.getProperty("java.vm.name"));
        report.put("maxHeapBytes", runtime.maxMemory());
        if (os instanceof com.sun.management.OperatingSystemMXBean extended) {
            report.put("physicalMemoryBytes", extended.getTotalMemorySize());
            report.put("freePhysicalMemoryBytes", extended.getFreeMemorySize());
            report.put("processCpuTimeNanos", extended.getProcessCpuTime());
        }
        report.put("garbageCollectors", ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName).toList());
        return report;
    }

    private static double percentileMillis(long[] sorted, double percentile) {
        return sorted.length == 0 ? 0
                : sorted[Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1)] / NANOS_PER_MILLISECOND;
    }

    private static Map<String, Object> heapSnapshot() {
        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return Map.of("usedBytes", usage.getUsed(), "committedBytes", usage.getCommitted(), "maxBytes", usage.getMax());
    }

    private static Map<String, GcSample> garbageCollectionSnapshot() {
        Map<String, GcSample> result = new LinkedHashMap<>();
        for (var bean : ManagementFactory.getGarbageCollectorMXBeans())
            result.put(bean.getName(), new GcSample(bean.getCollectionCount(), bean.getCollectionTime()));
        return result;
    }

    private static Map<String, Object> garbageCollectionDelta(Map<String, GcSample> baseline) {
        Map<String, Object> result = new LinkedHashMap<>();
        long count = 0, time = 0;
        for (var entry : garbageCollectionSnapshot().entrySet()) {
            GcSample start = baseline.get(entry.getKey()), end = entry.getValue();
            long countDelta = start == null || start.count < 0 || end.count < 0 ? -1 : end.count - start.count;
            long timeDelta = start == null || start.millis < 0 || end.millis < 0 ? -1 : end.millis - start.millis;
            result.put(entry.getKey(), Map.of("collections", countDelta, "collectionTimeMillis", timeDelta));
            if (countDelta >= 0) count += countDelta;
            if (timeDelta >= 0) time += timeDelta;
        }
        result.put("knownCollectionCount", count);
        result.put("knownCollectionTimeMillis", time);
        return result;
    }

    private record GcSample(long count, long millis) { }
    private record SlowTick(int index, long offsetNanos, long durationNanos) { }

    private static final class Phase {
        final String name;
        final LongSeries work = new LongSeries(), intervals = new LongSeries();
        final Map<String, LongSeries> diagnostics = new LinkedHashMap<>();
        long startedNanos, lastCompletedNanos, lastCompletedStartNanos;
        int incompleteTicks;
        Map<String, GcSample> gcAtStart = Map.of();
        Map<String, Object> heapAtStart;
        Phase(String name) { this.name = name; }
    }

    private static final class LongSeries {
        long[] values = new long[8192];
        int size;
        void add(long value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }
        long[] copy() { return Arrays.copyOf(values, size); }
    }
}
