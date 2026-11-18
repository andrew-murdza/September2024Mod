import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.amurdza.examplemod.worldgen.SavannaLakeBasins;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Actual seeded noise, production basin decisions, and Minecraft's four-block interpolation. */
public final class SavannaHeightAudit {
    private static final int MIN = -2048, WIDTH = 4096, NODES = WIDTH / 4 + 1;
    private static final long[] SEEDS = {0, 12345, 98765, -8152788754161584091L};

    private static final class Stats {
        long water;
        int boundaryLakes;
        final ArrayList<Integer> areas = new ArrayList<>();
        Map<String, Object> report() {
            Collections.sort(areas);
            return Map.of("water_blocks", water, "coverage_percent", 100.0 * water / (WIDTH * WIDTH),
                    "complete_lakes", areas.size(), "boundary_lakes", boundaryLakes,
                    "minimum_area", areas.get(0), "median_area", areas.get(areas.size() / 2),
                    "p90_area", areas.get(areas.size() * 9 / 10), "maximum_area", areas.get(areas.size() - 1),
                    "lakes_below_512_blocks", areas.stream().filter(area -> area < 512).count());
        }
    }

    private static Stats components(boolean[] water) {
        Stats result = new Stats();
        int[] queue = new int[water.length];
        for (int start = 0; start < water.length; start++) {
            if (!water[start]) continue;
            int head = 0, tail = 1;
            queue[0] = start;
            water[start] = false;
            boolean boundary = false;
            while (head < tail) {
                int cell = queue[head++], x = cell / WIDTH, z = cell % WIDTH;
                if (x == 0 || z == 0 || x == WIDTH - 1 || z == WIDTH - 1) boundary = true;
                for (int direction = 0; direction < 4; direction++) {
                    int next = switch (direction) {
                        case 0 -> x > 0 ? cell - WIDTH : -1;
                        case 1 -> x + 1 < WIDTH ? cell + WIDTH : -1;
                        case 2 -> z > 0 ? cell - 1 : -1;
                        default -> z + 1 < WIDTH ? cell + 1 : -1;
                    };
                    if (next >= 0 && water[next]) {
                        water[next] = false;
                        queue[tail++] = next;
                    }
                }
            }
            result.water += tail;
            if (boundary) result.boundaryLakes++; else result.areas.add(tail);
        }
        return result;
    }

    private static boolean[] water(double[] heights) {
        boolean[] result = new boolean[WIDTH * WIDTH];
        for (int x = 0; x < WIDTH; x++) for (int z = 0; z < WIDTH; z++) {
            int gx = x / 4, gz = z / 4;
            double tx = x % 4 / 4.0, tz = z % 4 / 4.0;
            double north = heights[gx * NODES + gz] * (1 - tx) + heights[(gx + 1) * NODES + gz] * tx;
            double south = heights[gx * NODES + gz + 1] * (1 - tx) + heights[(gx + 1) * NODES + gz + 1] * tx;
            result[x * WIDTH + z] = north * (1 - tz) + south * tz <= SavannaHeight.WATER_EDGE_HEIGHT;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); } catch (LinkageError expectedOutsideForge) { }
        double previous = SavannaHeight.height(-2);
        for (int i = -199999; i <= 200000; i++) {
            double noise = i / 100000.0, next = SavannaHeight.height(noise);
            if (next < previous || next < 55 || next >= 74) throw new AssertionError("Invalid height envelope or monotonicity");
            if (noise > SavannaHeight.PREVIOUS_SHORE_NOISE) {
                if (Math.abs(next - PreviousSavannaHeight.height(noise) - 3.5) > 1e-10)
                    throw new AssertionError("Existing dry land was not raised uniformly");
                double epsilon = 1e-7;
                if (noise - epsilon > SavannaHeight.PREVIOUS_SHORE_NOISE) {
                    double slope = (SavannaHeight.height(noise + epsilon) - SavannaHeight.height(noise - epsilon)) / (2 * epsilon);
                    double oldSlope = (PreviousSavannaHeight.height(noise + epsilon) - PreviousSavannaHeight.height(noise - epsilon)) / (2 * epsilon);
                    if (Math.abs(slope - oldSlope) > 1e-7) throw new AssertionError("Existing dry gradient changed");
                }
            }
            if (noise <= SavannaHeight.SHORE_NOISE && Math.abs(next
                    - PreviousSavannaHeight.profile(noise - SavannaHeight.WATER_NOISE_SHIFT)) > 1e-10)
                throw new AssertionError("Retained underwater bank profile changed");
            previous = next;
        }
        if (SavannaHeight.MAX_HEIGHT != 74) throw new AssertionError("Expected maximum height74");
        var rows = new ArrayList<Map<String, Object>>();
        long oldWater = 0, newWater = 0, oldLakes = 0, newLakes = 0;
        for (long seed : SEEDS) {
            var noise = NormalNoise.create(new XoroshiroRandomSource(seed).forkPositional()
                    .fromHashOf("aoemod:terrain/savanna/land"), -8, 1, .15);
            int cacheMin = MIN - 256, cacheSide = (WIDTH + 512) / 4 + 1;
            double[] samples = new double[cacheSide * cacheSide];
            for (int x = 0; x < cacheSide; x++) for (int z = 0; z < cacheSide; z++)
                samples[x * cacheSide + z] = noise.getValue((cacheMin + x * 4) * 1.25, 0, (cacheMin + z * 4) * 1.25);
            var lakes = new SavannaLakeBasins((x, z) -> {
                int gx = (int) (x - cacheMin) / 4, gz = (int) (z - cacheMin) / 4;
                if (x >= cacheMin && z >= cacheMin && gx < cacheSide && gz < cacheSide
                        && (int) x % 4 == 0 && (int) z % 4 == 0) return samples[gx * cacheSide + gz];
                return noise.getValue(x * 1.25, 0, z * 1.25);
            });
            double[] old = new double[NODES * NODES], current = new double[NODES * NODES];
            for (int x = 0; x < NODES; x++) for (int z = 0; z < NODES; z++) {
                int wx = MIN + x * 4, wz = MIN + z * 4;
                old[x * NODES + z] = PreviousSavannaHeight.height(samples[(x + 64) * cacheSide + z + 64]);
                current[x * NODES + z] = lakes.height(wx, wz);
            }
            Stats before = components(water(old)), after = components(water(current));
            oldWater += before.water; newWater += after.water;
            oldLakes += before.areas.size(); newLakes += after.areas.size();
            System.out.println("seed=" + seed + " previous=" + before.report() + " current=" + after.report());
            rows.add(Map.of("seed", seed, "previous", before.report(), "current", after.report()));
        }
        double ratio = (double) newWater / oldWater;
        System.out.printf("TOTAL waterRatio=%.8f previousLakes=%d currentLakes=%d lakeCountRatio=%.6f maximumHeight=%.1f%n",
                ratio, oldLakes, newLakes, newLakes / (double) oldLakes, SavannaHeight.MAX_HEIGHT);
        if (ratio < .35 || ratio > .45 || newLakes >= oldLakes) throw new AssertionError("Expected about0.4 water and fewer lakes");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("audit_source", "scripts/SavannaHeightAudit.java");
        report.put("block_columns_per_seed", WIDTH * WIDTH);
        report.put("region", "[-2048,2048) x [-2048,2048), every block after four-block terrain interpolation");
        report.put("maximum_height", SavannaHeight.MAX_HEIGHT);
        report.put("existing_dry_land_uplift", 3.5);
        report.put("shore_noise", SavannaHeight.SHORE_NOISE);
        report.put("basin_lattice_area_bounds", new int[]{SavannaLakeBasins.MINIMUM_AREA, SavannaLakeBasins.MAXIMUM_AREA});
        report.put("water_ratio", ratio);
        report.put("lake_count_ratio", newLakes / (double) oldLakes);
        report.put("seeds", rows);
        report.put("checks", "Monotone raw height transfer, exact3.5 uplift and unchanged dry derivatives, identical submerged reference profile; production component filter used.");
        report.put("limitations", "Finite four-seed region sample. Basin acceptance estimates area on the four-block density lattice; actual block shore area can differ. Boundary-crossing lakes count toward water coverage but are excluded from complete-lake size/count distributions. Existing chunks remain unchanged.");
        Path output = Path.of(args.length == 0 ? "scripts/savanna-bounded-lakes-calibration.json" : args[0]);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report) + System.lineSeparator());
    }

    // Frozen immediately previous height profile, independent of the production changes.
    private static final class PreviousSavannaHeight {
        public static final int SEA_LEVEL = 63;
        public static final double BOTTOM = 55;
        // A solid top at 61 leaves one water block at Y62; 61.5 is the continuous shoreline.
        public static final double WATER_EDGE_HEIGHT = SEA_LEVEL - 1.5;
        public static final double WATER_NOISE_SHIFT = 0;
        private static final double PREVIOUS_SHORE_NOISE = -.3526421518803894;
        private static final double PROFILE_MAX_HEIGHT = 79.5;
        private static final double LAND_START_HEIGHT = profile(PREVIOUS_SHORE_NOISE + WATER_NOISE_SHIFT);
        public static final double MAX_HEIGHT = WATER_EDGE_HEIGHT + (PROFILE_MAX_HEIGHT - LAND_START_HEIGHT) / 2;
    
        private PreviousSavannaHeight() { }
    
        public static double height(double noise) {
            // Move the shoreline back without scaling the submerged profile. This
            // halves the preceding revision's lake coverage, retaining its gradients
            // and Y55 bottom.
            double submerged = profile(noise - WATER_NOISE_SHIFT);
            if (submerged <= WATER_EDGE_HEIGHT) return submerged;
            // Keep the preceding land derivative at the SAME noise coordinate.
            // Re-anchoring at the smaller lake footprint raises existing land by a
            // constant and lifts the maximum to 70.5 without steepening its slopes.
            return WATER_EDGE_HEIGHT + (profile(noise) - LAND_START_HEIGHT) / 2;
        }
    
        public static double profile(double noise) {
            // Reference bank profile: derivative 80 through the main submerged
            // slope, with seven water blocks above the flat Y55 bottom.
            if (noise <= -.4525) return BOTTOM;
            if (noise < -.4225) {
                double t = (noise + .4525) / .030;
                // Integrate smoothstep: the bottom joins the bank without a crease.
                return BOTTOM + 2.4 * integralSmoothstep(t);
            }
            if (noise < -.3675) return 56.2 + 80 * (noise + .4225);
            if (noise < -.3475) {
                double t = (noise + .3675) / .020;
                return 60.6 + 1.6 * t - 1.12 * integralSmoothstep(t);
            }
            double land = noise + .3475;
            // Reference land derivative decreases gently from 24 towards hilltops.
            // The public transfer halves this derivative after re-anchoring it.
            double height = 61.64 + 20 * land / Math.hypot(land, 20.0 / 24);
            // Ease the highest tail into its reference ceiling without a flat cap.
            double taperStart = PROFILE_MAX_HEIGHT - 1.5;
            return height <= taperStart ? height
                    : taperStart + 1.5 * Math.tanh((height - taperStart) / 1.5);
        }
    
        private static double integralSmoothstep(double t) {
            return t * t * t * (1 - .5 * t);
        }
    }
}
