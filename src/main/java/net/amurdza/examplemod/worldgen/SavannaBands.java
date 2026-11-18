package net.amurdza.examplemod.worldgen;

import net.minecraft.world.level.levelgen.DensityFunction;

/** Continuous curved rivers and gently sloped dry divides with their thin highest crest lowered. */
public final class SavannaBands {
    public static final double WATER_WIDTH = 80;
    public static final double LAND_WIDTH = 400;
    public static final double LAND_HALF_WIDTH = LAND_WIDTH / 2;
    public static final double PERIOD = WATER_WIDTH + LAND_WIDTH;
    // Only the former Y73 crest loses a solid block; lower physical columns
    // retain their existing heights despite this small continuous-height cap.
    public static final double MAX_HEIGHT = 72;
    private final DensityFunction warp;

    public SavannaBands(DensityFunction warp) { this.warp = warp; }

    public double bend(int x) {
        // Match Desert's single, gently curved bend. The Savanna land resource
        // compensates for its two-lower first octave with a fourfold noise scale.
        // Only the sideways displacement changes; river width and grades do not.
        return 96 * Math.tanh(warp.compute(new DensityFunction.SinglePointContext(x * 2, 0, 0)));
    }

    public double shoreDistance(int x, double z) {
        double coordinate = z + bend(x);
        coordinate -= Math.floor((coordinate + PERIOD / 2) / PERIOD) * PERIOD;
        return Math.abs(coordinate) - WATER_WIDTH / 2;
    }

    public double height(int x, int z) { return Math.min(MAX_HEIGHT, SavannaHeight.height(shoreDistance(x, z))); }
}
