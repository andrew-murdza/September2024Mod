import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compares the frozen sparse-lake terrain with production octaves; never opens or edits a world. */
public final class SavannaOctaveAudit {
    private static final int SIDE = 1024;
    private static final double SHORE_HEIGHT = 61.5, SCALE = 2.35, STRETCH = 4;
    private static final long[] SEEDS = {0, 12345, 98765, -8152788754161584091L};
    private static final Path RESOURCES = Path.of("src/main/resources/data/aoemod/worldgen");

    private record NoiseSpec(int octave, double[] amplitudes) {
        NormalNoise create(net.minecraft.world.level.levelgen.PositionalRandomFactory random, String name) {
            return NormalNoise.create(random.fromHashOf("aoemod:terrain/savanna/" + name), octave, amplitudes);
        }
    }

    private static final class Stats {
        long columns, water, dry, banks;
        int lowestLand = Integer.MAX_VALUE, highestLand = Integer.MIN_VALUE, boundaryLakes;
        double lowestHeight = Double.POSITIVE_INFINITY, highestHeight = Double.NEGATIVE_INFINITY;
        double drySlopes, wetSlopes, bankSlopes;
        final List<Integer> areas = new ArrayList<>();

        void add(Stats other) {
            columns += other.columns;
            water += other.water;
            dry += other.dry;
            banks += other.banks;
            drySlopes += other.drySlopes;
            wetSlopes += other.wetSlopes;
            bankSlopes += other.bankSlopes;
            lowestLand = Math.min(lowestLand, other.lowestLand);
            highestLand = Math.max(highestLand, other.highestLand);
            lowestHeight = Math.min(lowestHeight, other.lowestHeight);
            highestHeight = Math.max(highestHeight, other.highestHeight);
            boundaryLakes += other.boundaryLakes;
            areas.addAll(other.areas);
        }

        Map<String, Object> report() {
            Collections.sort(areas);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("water_percent", water * 100.0 / columns);
            result.put("complete_lakes", areas.size());
            result.put("boundary_lakes", boundaryLakes);
            result.put("median_lake_area", areas.isEmpty() ? 0 : areas.get(areas.size() / 2));
            result.put("p90_lake_area", areas.isEmpty() ? 0 : areas.get(areas.size() * 9 / 10));
            result.put("lowest_land_block", lowestLand);
            result.put("highest_land_block", highestLand);
            result.put("lowest_continuous_height", lowestHeight);
            result.put("highest_continuous_height", highestHeight);
            result.put("mean_euclidean_dry_slope", drySlopes / dry);
            result.put("mean_euclidean_underwater_slope", wetSlopes / water);
            result.put("mean_euclidean_underwater_bank_slope", bankSlopes / banks);
            return result;
        }
    }

    private static NoiseSpec readNoise(String name, int expectedOctave) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(RESOURCES.resolve("noise/terrain/savanna/" + name + ".json"))).getAsJsonObject();
        int octave = json.get("firstOctave").getAsInt();
        double[] amplitudes = new double[json.getAsJsonArray("amplitudes").size()];
        for (int i = 0; i < amplitudes.length; i++) amplitudes[i] = json.getAsJsonArray("amplitudes").get(i).getAsDouble();
        if (octave != expectedOctave || !Arrays.equals(amplitudes, new double[]{1, .5, .25})) {
            throw new AssertionError("Unexpected production " + name + " noise: " + json);
        }
        return new NoiseSpec(octave, amplitudes);
    }

    private static int verifyRouter(JsonElement element) {
        int count = 0;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) count += verifyRouter(child);
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            if (object.has("noise") && object.get("noise").isJsonPrimitive()
                    && object.get("noise").getAsString().startsWith("aoemod:terrain/savanna/")) {
                String noise = object.get("noise").getAsString();
                if (noise.endsWith("/land") || noise.endsWith("/detail")) {
                    if (object.get("xz_scale").getAsDouble() != SCALE || object.get("y_scale").getAsDouble() != 0) {
                        throw new AssertionError("Production Savanna router scale changed: " + object);
                    }
                    count++;
                }
            }
            for (var entry : object.entrySet()) count += verifyRouter(entry.getValue());
        }
        return count;
    }

    /** Frozen pre-octave-change profile, including its capped bed, detail fade and upper lift. */
    private static double previous(double land, double detail) {
        double distance = land + .5;
        if (distance <= 0) return Math.max(55, SHORE_HEIGHT + distance * 25);
        double rise = Math.pow(Math.pow(3.0 / 145, 2.0 / 3) + distance, 1.5) * 145 - 3;
        double upper = Math.max(0, Math.min(1, (distance - .9) / .5));
        return SHORE_HEIGHT + (rise + detail * 5 * Math.min(1, distance / .08)) / 34.75
                + 2.5 * upper * upper * (3 - 2 * upper);
    }

    private static void verifyProfile() {
        for (double land : new double[]{-2, -.9, -.76, -.7, -.500001, -.5, -.499999, -.48, -.42, 0, .4, .6, .9, 1.2, 2}) {
            for (double detail : new double[]{-1, -.25, 0, .5, 1}) {
                double expected = SHORE_HEIGHT + STRETCH * (previous(land, detail) - SHORE_HEIGHT);
                if (Math.abs(SavannaHeight.height(land, detail) - expected) > 1e-10) {
                    throw new AssertionError("Profile failed uniform fourfold relief scaling at " + land + ", " + detail);
                }
            }
        }
        if (SavannaHeight.NOISE_SCALE != SCALE || SavannaHeight.SHORE_NOISE != -.5) {
            throw new AssertionError("Octave change must retain shoreline and router scale");
        }
    }

    private static double height(NormalNoise land, NormalNoise detail, double x, double z, boolean revised) {
        double first = land.getValue(x * SCALE, 0, z * SCALE);
        double second = detail.getValue(x * SCALE, 0, z * SCALE);
        return revised ? SavannaHeight.height(first, second) : previous(first, second);
    }

    private static Stats sample(long seed, boolean revised, NoiseSpec currentLand, NoiseSpec currentDetail) {
        var random = new XoroshiroRandomSource(seed).forkPositional();
        var land = (revised ? currentLand : new NoiseSpec(-9, new double[]{1, .5, .25})).create(random, "land");
        var detail = (revised ? currentDetail : new NoiseSpec(-6, new double[]{1, .5, .25})).create(random, "detail");
        int step = revised ? 16 : 4, min = -SIDE * step / 2;
        double bottom = revised ? 35.5 : 55;
        var stats = new Stats();
        boolean[] water = new boolean[SIDE * SIDE];
        for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
            double wx = min + x * step, wz = min + z * step;
            double h = height(land, detail, wx, wz, revised);
            stats.columns++;
            stats.lowestHeight = Math.min(stats.lowestHeight, h);
            stats.highestHeight = Math.max(stats.highestHeight, h);
            double epsilon = .1;
            double dx = (height(land, detail, wx + epsilon, wz, revised)
                    - height(land, detail, wx - epsilon, wz, revised)) / (2 * epsilon);
            double dz = (height(land, detail, wx, wz + epsilon, revised)
                    - height(land, detail, wx, wz - epsilon, revised)) / (2 * epsilon);
            double slope = Math.hypot(dx, dz);
            if (h <= SHORE_HEIGHT) {
                water[x * SIDE + z] = true;
                stats.water++;
                stats.wetSlopes += slope;
                if (h > bottom + 1e-8) {
                    stats.banks++;
                    stats.bankSlopes += slope;
                }
            } else {
                int top = (int) Math.ceil(h + .5) - 1;
                stats.lowestLand = Math.min(stats.lowestLand, top);
                stats.highestLand = Math.max(stats.highestLand, top);
                stats.drySlopes += slope;
                stats.dry++;
            }
        }
        int[] queue = new int[water.length];
        for (int start = 0; start < water.length; start++) {
            if (!water[start]) continue;
            int head = 0, tail = 1;
            queue[0] = start;
            water[start] = false;
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
                    if (next >= 0 && water[next]) {
                        water[next] = false;
                        queue[tail++] = next;
                    }
                }
            }
            if (boundary) stats.boundaryLakes++;
            else stats.areas.add(tail * step * step);
        }
        return stats;
    }

    private static void requireSimilarSlope(double ratio, String kind) {
        if (!Double.isFinite(ratio) || ratio < .85 || ratio > 1.15) {
            throw new AssertionError(kind + " sampled slope changed by more than 15%: " + ratio);
        }
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); } catch (LinkageError expectedOutsideForge) { }
        NoiseSpec land = readNoise("land", -11), detail = readNoise("detail", -8);
        if (verifyRouter(JsonParser.parseString(Files.readString(RESOURCES.resolve("noise_settings/savanna_dimension.json")))) != 8) {
            throw new AssertionError("Expected all eight production Savanna land/detail inputs");
        }
        verifyProfile();
        var before = new Stats();
        var after = new Stats();
        var rows = new ArrayList<Map<String, Object>>();
        for (long seed : SEEDS) {
            var old = sample(seed, false, land, detail);
            var current = sample(seed, true, land, detail);
            before.add(old);
            after.add(current);
            rows.add(Map.of("seed", seed, "previous", old.report(), "current", current.report()));
            System.out.println("seed=" + seed + " previous=" + old.report() + " current=" + current.report());
        }
        double dryRatio = (after.drySlopes / after.dry) / (before.drySlopes / before.dry);
        double wetRatio = (after.wetSlopes / after.water) / (before.wetSlopes / before.water);
        double bankRatio = (after.bankSlopes / after.banks) / (before.bankSlopes / before.banks);
        double areaRatio = ((Number) after.report().get("median_lake_area")).doubleValue()
                / ((Number) before.report().get("median_lake_area")).doubleValue();
        requireSimilarSlope(dryRatio, "Dry");
        requireSimilarSlope(wetRatio, "Underwater");
        requireSimilarSlope(bankRatio, "Underwater bank");
        if (areaRatio < 8 || areaRatio > 32) throw new AssertionError("Expected approximately sixteenfold lake area: " + areaRatio);
        if (after.highestLand <= before.highestLand + 20) throw new AssertionError("Expected substantially higher hills");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("audit_source", "scripts/SavannaOctaveAudit.java");
        result.put("previous_region_per_seed", "[-2048,2048) squared, sampled every four blocks; four seeds");
        result.put("current_region_per_seed", "[-8192,8192) squared, sampled every sixteen blocks; four seeds");
        result.put("previous_first_octaves", Map.of("land", -9, "detail", -6));
        result.put("current_first_octaves", Map.of("land", land.octave, "detail", detail.octave));
        result.put("horizontal_wavelength_factor", STRETCH);
        result.put("vertical_relief_factor", STRETCH);
        result.put("previous", before.report());
        result.put("current", after.report());
        result.put("dry_euclidean_slope_ratio", dryRatio);
        result.put("underwater_euclidean_slope_ratio", wetRatio);
        result.put("underwater_bank_euclidean_slope_ratio", bankRatio);
        result.put("median_lake_area_ratio", areaRatio);
        result.put("seeds", rows);
        result.put("limitations", "Finite seeded samples of continuous terrain on proportionately scaled lattices. Changing firstOctave changes octave-specific random permutations, so the samples are not the same lakes stretched in place. Lake areas and connectivity are lattice estimates; boundary-crossing lakes count toward water coverage but not area distributions. Euclidean slopes use central differences with epsilon 0.1 before block rounding. Uniform height-profile scaling and unchanged noise amplitudes preserve the slope scale; finite-sample slope means vary. Existing chunks remain unchanged.");
        Files.writeString(Path.of(args.length == 0 ? "scripts/savanna-octave-calibration.json" : args[0]),
                new GsonBuilder().setPrettyPrinting().create().toJson(result) + System.lineSeparator());
        System.out.printf(Locale.ROOT, "PASS dry slope ratio=%.8f underwater slope ratio=%.8f bank slope ratio=%.8f median area ratio=%.8f%n",
                dryRatio, wetRatio, bankRatio, areaRatio);
    }
}
