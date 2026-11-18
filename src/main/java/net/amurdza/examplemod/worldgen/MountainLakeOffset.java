package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/** A valley-only change to vanilla's terrain offset; all three-dimensional terrain noise remains vanilla. */
public record MountainLakeOffset(DensityFunction offset, DensityFunction ridges,
                                 DensityFunction continents, DensityFunction erosion) implements DensityFunction {
    public static final KeyDispatchDataCodec<MountainLakeOffset> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("offset").forGetter(MountainLakeOffset::offset),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(MountainLakeOffset::ridges),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(MountainLakeOffset::continents),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(MountainLakeOffset::erosion)
            ).apply(instance, MountainLakeOffset::new)));

    public static final int SEA_LEVEL = 63;
    // Halfway in block space between the previous .25 shore and .4 forest edge
    // under MountainRidges' Hermite contour, rounded to the biome's precision.
    public static final double LAKE_WEIRDNESS = 0.3214;
    public static final long LAKE_QUANTIZED_WEIRDNESS = 3214;
    // Raising the bottom by5.8728 blocks shortens both deep ramps by20 blocks
    // combined, retaining the previous flat-bottom width at deep width130.
    public static final double MAX_LAKE_DEPTH = 44.12722453022183;
    // Actual vanilla-density columns amplify this local contour-distance slope;
    // the real-column audit calibrates it separately from the ideal straight bank.
    public static final double BANK_SLOPE = 0.5;

    @Override
    public double compute(FunctionContext context) {
        double original = offset.compute(context);
        double weirdness = Math.abs(ridges.compute(context));
        if (weirdness >= LAKE_WEIRDNESS) return original;
        int x = context.blockX(), z = context.blockZ();
        double dx = (sample(x + 4, z) - sample(x - 4, z)) / 8;
        double dz = (sample(x, z + 4) - sample(x, z - 4)) / 8;
        // The signed distance estimate gives banks a block-space slope rather
        // than a noise-value slope, which varies greatly between world seeds.
        double distance = (LAKE_WEIRDNESS - weirdness) / Math.max(0.00001, Math.hypot(dx, dz));
        double originalHeight = 128 + original * 128;
        double floor = SEA_LEVEL - MAX_LAKE_DEPTH
                + 10 * Math.max(0, continents.compute(context) - 0.4)
                + 8 * Math.max(0, -0.15 - erosion.compute(context));
        double depth = Math.max(0, originalHeight - floor);
        double descent = Math.min(depth, Math.max(0, distance) * BANK_SLOPE);
        // Ease the final four blocks into the bottom; retain the continent and
        // erosion variations and vanilla sloped-cheese noise on the lake floor.
        if (depth > 4 && descent > depth - 4) {
            double t = Mth.clamp((descent - depth + 4) / 4, 0, 1);
            descent = depth - 4 + 4 * (t + t * t - t * t * t);
        }
        return original - descent / 128;
    }

    private double sample(int x, int z) {
        return ridges.compute(new SinglePointContext(x, 0, z));
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MountainLakeOffset(offset.mapAll(visitor), ridges.mapAll(visitor),
                continents.mapAll(visitor), erosion.mapAll(visitor)));
    }
    @Override public double minValue() { return Math.min(offset.minValue(), -1); }
    @Override public double maxValue() { return offset.maxValue(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
