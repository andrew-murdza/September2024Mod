import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Arrays;

/** Reproducible seeded-noise coverage audit; does not create or alter a world. */
public final class TerrainNoiseRevisionAudit {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        try { net.minecraft.server.Bootstrap.bootStrap(); } catch (LinkageError ignored) { }
        long[] seeds = {0, 12345, 98765, -8152788754161584091L};
        int width = 768, cursor = 0, oldWater = 0, newWater = 0;
        double[] samples = new double[width * width * seeds.length];
        double oldEdge = Math.pow(1.0 / 145, 2.0 / 3) - .06 - 14.5 / 50;
        double target = args.length > 0 ? Double.parseDouble(args[0]) : -.46;
        for (long seed : seeds) {
            var noise = NormalNoise.create(new XoroshiroRandomSource(seed).forkPositional()
                    .fromHashOf("aoemod:terrain/desert/land"), -9, 1, .5, .25);
            int oldSeed = 0, newSeed = 0;
            for (int gx = 0; gx < width; gx++) for (int gz = 0; gz < width; gz++) {
                double value = noise.getValue((gx - width / 2) * 43 + 7, 0, (gz - width / 2) * 47 - 11);
                samples[cursor++] = value;
                if (value <= oldEdge) oldSeed++;
                if (value <= target) newSeed++;
            }
            oldWater += oldSeed; newWater += newSeed;
            System.out.printf("seed=%d oldWater=%d newWater=%d ratio=%.5f%n", seed, oldSeed, newSeed, newSeed / (double) oldSeed);
        }
        Arrays.sort(samples);
        System.out.printf("columns=%d oldEdge=%.12f halfWaterEdge=%.12f actualRatio=%.6f%n",
                cursor, oldEdge, samples[oldWater / 2], newWater / (double) oldWater);
    }
}
