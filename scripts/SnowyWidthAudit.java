import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.DoublePredicate;

/** Exact 1.20.1 ridge/offset noise wiring, sampled without loading a Minecraft world. */
public final class SnowyWidthAudit {
    private static final int STEP = 4;
    private static final int LENGTH = 32768;
    private static final long[] SEEDS = {0, 12345, 98765, -8152788754161584091L};

    public static void main(String[] args) throws IOException {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            // The standalone class path has no Forge event transformation. Its
            // final networking bootstrap fails after registries are initialized;
            // this numerical audit never loads or uses networking.
            if (Arrays.stream(error.getStackTrace()).noneMatch(frame -> frame.getClassName()
                    .startsWith("net.minecraftforge.network."))) throw error;
        }
        var vanilla = samples(1);
        Stats low = stats(vanilla, 0, 0.35), high = stats(vanilla, 0.35, Double.POSITIVE_INFINITY);
        System.out.println("VANILLA low=" + low + " high=" + high);
        printProfile("VANILLA_SPLIT", vanilla, .1, .35);
        var current = samples(2);
        printProfile("PREVIOUS_SHIPPED_035", current, .1, .35);
        Stats[] vanillaCurrentBands = currentProfile(vanilla);
        Stats[] currentBands = currentProfile(current);
        printCurrentProfile("VANILLA_SAME_040_CLASSIFICATION", vanillaCurrentBands);
        printCurrentProfile("SHIPPED_STRICT_040", currentBands);
        for (int habitat = 0; habitat < currentBands.length; habitat++) {
            requireWidth(currentBands[habitat], vanillaCurrentBands[habitat],
                    new String[]{"mountain lake", "dappled forest", "snowy taiga"}[habitat]);
        }
        double totalFraction = Arrays.stream(currentBands).mapToDouble(Stats::fraction).sum();
        if (Math.abs(totalFraction - 1) > 1.0e-12)
            throw new AssertionError("Current habitats must partition all samples");
        // Raising the threshold intentionally gives some former taiga to dappled
        // forest. Do not enforce the superseded vanilla-0.35 coverage target.
        if (currentBands[2].fraction >= stats(current, .35, Double.POSITIVE_INFINITY).fraction)
            throw new AssertionError("The strict 0.4 threshold must reduce sampled taiga coverage");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--report")) {
                if (i + 1 == args.length) throw new IllegalArgumentException("--report requires a path");
                writeReport(Path.of(args[++i]), vanillaCurrentBands, currentBands, current);
            }
        }
        if (!Arrays.asList(args).contains("--sweep")) return;
        printProfile("DOUBLE_BALANCED_15_40", current, .15, .4);
        printProfile("DOUBLE_BALANCED_15_45", current, .15, .45);
        List<Candidate> candidates = new ArrayList<>();
        for (double stretch : new double[]{1, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 2.75, 3}) {
            var sampled = samples(stretch);
            for (double lake = .08; lake <= .30; lake += .01) {
                Stats l = stats(sampled, 0, lake);
                for (double dappled = .35; dappled <= .55; dappled += .01) {
                    Stats d = stats(sampled, lake, dappled), h = stats(sampled, dappled, Double.POSITIVE_INFINITY);
                    // Retain substantial taiga coverage, accepting imperfect widths.
                    if (h.fraction < .2 || h.fraction > .3) continue;
                    double score = error(l, low) + error(d, low) + error(h, high);
                    candidates.add(new Candidate(score, stretch, lake, dappled, l, d, h));
                }
            }
        }
        candidates.sort(java.util.Comparator.comparingDouble(Candidate::score));
        candidates.stream().limit(16).forEach(System.out::println);
    }

    private static double error(Stats observed, Stats expected) {
        return Math.pow(Math.log(observed.mean / expected.mean), 2)
                + 0.5 * Math.pow(Math.log(observed.median / (double) expected.median), 2);
    }

    private static Stats[] currentProfile(List<double[]> samples) {
        // Match Climate.Sampler's float conversion and quantization, including
        // the exact 0.4 boundary belonging to dappled rather than taiga.
        return new Stats[]{stats(samples, value -> quantized(value) < 1000),
                stats(samples, value -> quantized(value) >= 1000 && quantized(value) <= 4000),
                stats(samples, value -> quantized(value) > 4000)};
    }

    private static long quantized(double value) {
        return Math.abs(Climate.quantizeCoord((float) value));
    }

    private static void printCurrentProfile(String name, Stats[] profile) {
        System.out.println(name + " lake=" + profile[0] + " dappled=" + profile[1] + " high=" + profile[2]);
    }

    private static void writeReport(Path path, Stats[] vanilla, Stats[] current, List<double[]> currentSamples)
            throws IOException {
        var gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonObject report = new JsonObject();
        report.addProperty("minecraft_version", "1.20.1");
        report.addProperty("audit_source", "scripts/SnowyWidthAudit.java");
        report.addProperty("historical_035_report", "scripts/snowy-width-calibration.json");
        report.addProperty("method", "Run-length transects using production MountainRidges with vanilla Xoroshiro ridge/offset seeds. Incomplete boundary runs excluded from widths; all points included in sample fractions. Both horizontal axes sampled. Current classifications use Climate.quantizeCoord after the actual float conversion.");
        report.add("seeds", gson.toJsonTree(SEEDS));
        report.add("transect_fixed_coordinates", gson.toJsonTree(new int[]{-8192, -1536, 1536, 8192}));
        report.addProperty("transects", SEEDS.length * 8);
        report.addProperty("length_per_transect_blocks", LENGTH);
        report.addProperty("sample_step_blocks", STEP);
        report.addProperty("sample_points_per_profile", SEEDS.length * 8 * (LENGTH / STEP + 1));
        JsonObject profile = new JsonObject();
        profile.addProperty("full_domain_stretch", 2.0);
        profile.addProperty("mountain_lake", "abs(quantized weirdness) < 1000");
        profile.addProperty("dappled_forest", "1000 <= abs(quantized weirdness) <= 4000");
        profile.addProperty("snowy_taiga", "abs(quantized weirdness) > 4000");
        report.add("profile", profile);
        report.add("vanilla_same_040_classification", profileJson(vanilla));
        report.add("current_040_classification", profileJson(current));
        report.add("previous_035_continuous_classification_same_samples", profileJson(new Stats[]{
                stats(currentSamples, 0, .1), stats(currentSamples, .1, .35),
                stats(currentSamples, .35, Double.POSITIVE_INFINITY)}));
        report.addProperty("interpretation", "The full ridge and domain-shift noise coordinates remain divided by two. Weirdness values and terrain vertical amplitude are unchanged. Raising the taiga threshold from 0.35 to strictly above 0.4 transfers part of the former taiga band to dappled forest; the reduced taiga share is intentional. Vanilla comparisons apply these same habitat cuts to the unstretched vanilla ridge field, not vanilla's complete multi-noise biome selection.");
        report.addProperty("limitations", "Finite transects estimate widths and coverage; they do not guarantee individual contour widths or global two-dimensional biome area. Minecraft climate quantization makes the effective strict upper threshold approximately 0.4001. Four-block sampling cannot resolve thinner runs, and client biome blending can shift visible color boundaries.");
        Files.writeString(path, gson.toJson(report) + System.lineSeparator());
    }

    private static JsonObject profileJson(Stats[] profile) {
        JsonObject result = new JsonObject();
        String[] names = {"mountain_lake", "dappled_forest", "snowy_taiga"};
        for (int i = 0; i < profile.length; i++) {
            JsonObject stats = new JsonObject();
            stats.addProperty("mean_width", profile[i].mean);
            stats.addProperty("median_width", profile[i].median);
            stats.addProperty("runs", profile[i].runs);
            stats.addProperty("sample_fraction", profile[i].fraction);
            result.add(names[i], stats);
        }
        return result;
    }

    private static void printProfile(String name, List<double[]> samples, double lakeEdge, double taigaEdge) {
        System.out.println(name + " lake=" + stats(samples, 0, lakeEdge)
                + " dappled=" + stats(samples, lakeEdge, taigaEdge)
                + " high=" + stats(samples, taigaEdge, Double.POSITIVE_INFINITY));
    }

    private static void requireWidth(Stats actual, Stats reference, String habitat) {
        double ratio = actual.mean / (2 * reference.mean);
        if (ratio < 0.9 || ratio > 1.1) throw new AssertionError(habitat + " mean width drifted: " + ratio);
    }

    private static List<double[]> samples(double stretch) {
        List<double[]> result = new ArrayList<>();
        for (long seed : SEEDS) {
            var positional = new XoroshiroRandomSource(seed).forkPositional();
            var ridgeParameters = new NormalNoise.NoiseParameters(-7, 1, 2, 1, 0, 0, 0);
            var shiftParameters = new NormalNoise.NoiseParameters(-3, 1, 1, 1, 0);
            NormalNoise ridge = NormalNoise.create(positional.fromHashOf("minecraft:ridge"), ridgeParameters);
            NormalNoise shift = NormalNoise.create(positional.fromHashOf("minecraft:offset"), shiftParameters);
            MountainRidges density = new MountainRidges(new DensityFunction.NoiseHolder(Holder.direct(ridgeParameters), ridge),
                    new DensityFunction.NoiseHolder(Holder.direct(shiftParameters), shift), stretch);
            for (int direction = 0; direction < 2; direction++) for (int line : new int[]{-8192, -1536, 1536, 8192}) {
                double[] points = new double[LENGTH / STEP + 1];
                for (int i = 0; i < points.length; i++) {
                    int x = direction == 0 ? i * STEP - LENGTH / 2 : line;
                    int z = direction == 1 ? i * STEP - LENGTH / 2 : line;
                    points[i] = Math.abs(density.compute(new DensityFunction.SinglePointContext(x, 0, z)));
                }
                result.add(points);
            }
        }
        return result;
    }

    private static Stats stats(List<double[]> lines, double minimum, double maximum) {
        return stats(lines, value -> value >= minimum && value < maximum);
    }

    private static Stats stats(List<double[]> lines, DoublePredicate contains) {
        List<Integer> widths = new ArrayList<>();
        long occupied = 0, samples = 0;
        for (double[] line : lines) {
            int start = -1;
            for (int i = 0; i < line.length; i++) {
                boolean inside = contains.test(line[i]);
                samples++;
                if (inside) { occupied++; if (start == -1) start = i; }
                else if (start != -1) { if (start > 0) widths.add((i - start) * STEP); start = -1; }
            }
        }
        int[] sorted = widths.stream().mapToInt(Integer::intValue).sorted().toArray();
        return new Stats(Arrays.stream(sorted).average().orElse(0), sorted[sorted.length / 2],
                sorted.length, occupied / (double) samples);
    }

    private record Stats(double mean, int median, int runs, double fraction) {
        @Override public String toString() { return String.format(Locale.ROOT,
                "{mean=%.2f,median=%d,runs=%d,area=%.4f}", mean, median, runs, fraction); }
    }
    private record Candidate(double score, double stretch, double lakeThreshold, double dappledThreshold,
                             Stats lake, Stats dappled, Stats high) { }
}
