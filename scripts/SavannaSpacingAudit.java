import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Historical sparse-lake revision comparison; use SavannaOctaveAudit for current production terrain. */
public final class SavannaSpacingAudit {
    private static final int SIDE = 1024, STEP = 4, MIN = -2048;
    private static final long[] SEEDS = {0, 12345, 98765, -8152788754161584091L};
    private static final double PREVIOUS_SHORE = Math.pow(1.0 / 145, 2.0 / 3) - .06 - 10.5 / 50;

    private static final class Stats {
        long columns, water, dry;
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE, boundaryLakes;
        double slopes;
        final List<Integer> areas = new ArrayList<>();

        void add(Stats other) {
            columns += other.columns; water += other.water; dry += other.dry; slopes += other.slopes;
            lowest = Math.min(lowest, other.lowest); highest = Math.max(highest, other.highest);
            boundaryLakes += other.boundaryLakes; areas.addAll(other.areas);
        }

        Map<String, Object> report() {
            Collections.sort(areas);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("water_percent", water * 100.0 / columns);
            result.put("complete_lakes", areas.size());
            result.put("boundary_lakes", boundaryLakes);
            result.put("median_lake_area", areas.get(areas.size() / 2));
            result.put("p90_lake_area", areas.get(areas.size() * 9 / 10));
            result.put("lowest_land_block", lowest);
            result.put("highest_land_block", highest);
            result.put("land_elevations", highest - lowest + 1);
            result.put("mean_euclidean_dry_slope", slopes / dry);
            return result;
        }
    }

    private static double previous(double land, double detail) {
        double distance = land - PREVIOUS_SHORE;
        if (distance <= 0) return Math.max(55, 61.5 + distance * 25);
        return 61.5 + (DesertHeight.upperContour(DesertHeight.STEEP_START_NOISE + distance) - 75
                + detail * 5 * Math.min(1, distance / .08)) / 32;
    }

    private static double height(NormalNoise land, NormalNoise detail, double x, double z, boolean revised) {
        double scale = revised ? 2.35 : 4;
        double first = land.getValue(x * scale, 0, z * scale);
        double second = detail.getValue(x * scale, 0, z * scale);
        return revised ? spacingRevision(first, second) : previous(first, second);
    }

    private static double spacingRevision(double land, double detail) {
        double distance = land + .5;
        if (distance <= 0) return Math.max(55, 61.5 + distance * 25);
        double rise = DesertHeight.upperContour(DesertHeight.STEEP_START_NOISE + distance) - 75;
        double upper = Math.max(0, Math.min(1, (distance - .9) / .5));
        return 61.5 + (rise + detail * 5 * Math.min(1, distance / .08)) / 34.75
                + 2.5 * upper * upper * (3 - 2 * upper);
    }

    private static Stats sample(long seed, boolean revised) {
        var random = new XoroshiroRandomSource(seed).forkPositional();
        var land = NormalNoise.create(random.fromHashOf("aoemod:terrain/savanna/land"), -9, 1, .5, .25);
        var detail = NormalNoise.create(random.fromHashOf("aoemod:terrain/savanna/detail"), -6, 1, .5, .25);
        var stats = new Stats();
        boolean[] water = new boolean[SIDE * SIDE];
        for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
            double wx = MIN + x * STEP, wz = MIN + z * STEP;
            double h = height(land, detail, wx, wz, revised);
            stats.columns++;
            if (h <= 61.5) { water[x * SIDE + z] = true; stats.water++; continue; }
            int top = (int) Math.ceil(h + .5) - 1;
            stats.lowest = Math.min(stats.lowest, top); stats.highest = Math.max(stats.highest, top);
            double epsilon = .1;
            double dx = (height(land, detail, wx + epsilon, wz, revised)
                    - height(land, detail, wx - epsilon, wz, revised)) / (2 * epsilon);
            double dz = (height(land, detail, wx, wz + epsilon, revised)
                    - height(land, detail, wx, wz - epsilon, revised)) / (2 * epsilon);
            stats.slopes += Math.hypot(dx, dz); stats.dry++;
        }
        int[] queue = new int[water.length];
        for (int start = 0; start < water.length; start++) {
            if (!water[start]) continue;
            int head = 0, tail = 1;
            queue[0] = start; water[start] = false;
            boolean boundary = false;
            while (head < tail) {
                int cell = queue[head++], x = cell / SIDE, z = cell % SIDE;
                if (x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1) boundary = true;
                for (int direction = 0; direction < 4; direction++) {
                    int next = switch (direction) {
                        case 0 -> x > 0 ? cell - SIDE : -1;
                        case 1 -> x + 1 < SIDE ? cell + SIDE : -1;
                        case 2 -> z > 0 ? cell - 1 : -1;
                        default -> z + 1 < SIDE ? cell + 1 : -1;
                    };
                    if (next >= 0 && water[next]) { water[next] = false; queue[tail++] = next; }
                }
            }
            if (boundary) stats.boundaryLakes++; else stats.areas.add(tail * STEP * STEP);
        }
        return stats;
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); } catch (LinkageError expectedOutsideForge) { }
        var before = new Stats(); var after = new Stats();
        var rows = new ArrayList<Map<String, Object>>();
        for (long seed : SEEDS) {
            var old = sample(seed, false); var current = sample(seed, true);
            before.add(old); after.add(current);
            rows.add(Map.of("seed", seed, "previous", old.report(), "current", current.report()));
            System.out.println("seed=" + seed + " previous=" + old.report() + " current=" + current.report());
            if (current.highest - current.lowest + 1 < 10) throw new AssertionError("Land range under ten at seed " + seed);
        }
        double slopeRatio = (after.slopes / after.dry) / (before.slopes / before.dry);
        double medianAreaRatio = ((Number) after.report().get("median_lake_area")).doubleValue()
                / ((Number) before.report().get("median_lake_area")).doubleValue();
        if (slopeRatio < .68 || slopeRatio > .72) throw new AssertionError("Expected about 30% lower Euclidean dry slope: " + slopeRatio);
        if (medianAreaRatio < .8 || medianAreaRatio > 1.2) throw new AssertionError("Typical lake area changed too much");
        if (after.areas.size() < before.areas.size() * .22 || after.areas.size() > before.areas.size() * .28 || after.water >= before.water) throw new AssertionError("Lakes did not become sparser");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("audit_source", "scripts/SavannaSpacingAudit.java");
        result.put("region_per_seed", "[-2048,2048) squared, sampled every four blocks; four seeds");
        result.put("previous", before.report()); result.put("current", after.report());
        result.put("dry_euclidean_slope_ratio", slopeRatio);
        result.put("median_lake_area_ratio", medianAreaRatio);
        result.put("lake_count_ratio", after.areas.size() / (double) before.areas.size());
        result.put("seeds", rows);
        result.put("limitations", "Finite seeded sample of continuous terrain. Lake areas and connectivity are estimated on a four-block lattice. Boundary-crossing lakes count toward water coverage but not size/count distributions. Euclidean slopes use central differences with epsilon0.1 before block rounding. Existing chunks remain unchanged.");
        Files.writeString(Path.of(args.length == 0 ? "scripts/savanna-spacing-calibration.json" : args[0]),
                new GsonBuilder().setPrettyPrinting().create().toJson(result) + System.lineSeparator());
        System.out.printf(Locale.ROOT, "PASS dry slope ratio=%.8f median area ratio=%.8f%n", slopeRatio, medianAreaRatio);
    }
}
