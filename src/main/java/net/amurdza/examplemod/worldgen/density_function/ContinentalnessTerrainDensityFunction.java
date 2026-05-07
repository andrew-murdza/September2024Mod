package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;

/**
 * Custom density function for continentalness-driven terrain.
 *
 * Evaluates to:  (surfaceHeight - blockY) / verticalScale
 *
 * where surfaceHeight is the y of the first AIR block computed by whichever
 * TerrainRange the current continentalness value falls into.
 *
 * Positive output → solid block.
 * Negative output → air.
 * Zero output     → boundary (treated as air by Minecraft's threshold).
 *
 * Registration
 * ------------
 * Register this codec in ModDensityFunctionTypes, then reference it in JSON as:
 *   "type": "yourmod:continentalness_terrain"
 *
 * Replace "yourmod" with your actual mod ID throughout.
 */
public class ContinentalnessTerrainDensityFunction implements DensityFunction {

    // ------------------------------------------------------------------
    // Codec
    // ------------------------------------------------------------------

    public static final MapCodec<ContinentalnessTerrainDensityFunction> MAP_CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                    DensityFunction.HOLDER_HELPER_CODEC
                            .fieldOf("continentalness")
                            .forGetter(f -> f.continentalness),
                    TerrainRange.CODEC.listOf()
                            .fieldOf("ranges")
                            .forGetter(f -> f.ranges),
                    Codec.FLOAT
                            .optionalFieldOf("vertical_scale", 1.0f)
                            .forGetter(f -> f.verticalScale)
            ).apply(i, ContinentalnessTerrainDensityFunction::new));

    /**
     * KeyDispatchDataCodec wrapping the MapCodec.
     * This is what codec() must return and what ModDensityFunctionTypes registers.
     */
    public static final KeyDispatchDataCodec<ContinentalnessTerrainDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MAP_CODEC);

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    /** Density function that samples the continentalness noise. y_scale should be 0. */
    private final DensityFunction continentalness;

    /**
     * Ordered list of terrain range descriptors.
     * Ranges should be non-overlapping and together cover [-1.05, 1.05].
     * The first matching range (minContinentalness <= cont < maxContinentalness) wins.
     */
    private final List<TerrainRange> ranges;

    /**
     * Divisor in (surfaceHeight - blockY) / verticalScale.
     *
     * 1.0 → perfectly sharp 1-block surface transition (ideal for stepped terrain).
     * Values > 1 soften cliff faces across multiple blocks.
     *
     * Note: this function is evaluated at the coarse interpolation grid
     * (every 4 blocks XZ, 8 blocks Y by default) and then trilinearly
     * interpolated, so the perceived sharpness is already somewhat softened.
     */
    private final float verticalScale;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public ContinentalnessTerrainDensityFunction(
            DensityFunction continentalness,
            List<TerrainRange> ranges,
            float verticalScale) {
        this.continentalness = continentalness;
        this.ranges = ranges;
        this.verticalScale = verticalScale;
    }

    // ------------------------------------------------------------------
    // DensityFunction implementation
    // ------------------------------------------------------------------

    @Override
    public double compute(FunctionContext ctx) {
        double cont = continentalness.compute(ctx);
        TerrainRange range = findRange(cont);
        if (range == null) return 0.0;

        double surfaceHeight = range.computeSurface(ctx, cont);
        return (surfaceHeight - ctx.blockY()) / verticalScale;
    }

    @Override
    public void fillArray(double[] densities, ContextProvider context) {
        // Default implementation evaluates compute() at each context position.
        // Override with a bulk path if profiling shows this is a bottleneck.
        context.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ContinentalnessTerrainDensityFunction(
                continentalness.mapAll(visitor),
                ranges.stream().map(r -> r.mapAll(visitor)).toList(),
                verticalScale
        ));
    }

    // ------------------------------------------------------------------
    // Value bounds
    // Used by the engine for optimisation.  Conservative bounds are safe.
    // ------------------------------------------------------------------

    /** Minimum possible output: deepest water (y≈43) minus build height (y=320). */
    @Override
    public double minValue() { return -320.0; }

    /** Maximum possible output: highest land (y≈174) minus bedrock floor (y=-64). */
    @Override
    public double maxValue() { return  320.0; }

    // ------------------------------------------------------------------
    // Codec registration reference
    // ------------------------------------------------------------------

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Returns the first range whose [minContinentalness, maxContinentalness)
     * interval contains {@code cont}, or null if none match.
     *
     * Ranges are checked in list order; put more specific (narrower) ranges
     * before wider catch-all ranges if they overlap.
     */
    private TerrainRange findRange(double cont) {
        for (TerrainRange r : ranges) {
            if (cont >= r.minContinentalness() && cont < r.maxContinentalness()) {
                return r;
            }
        }
        return null;
    }
}