package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Complete lake-to-terracotta contours; seeded noise bends bands without erasing either end. */
public record DesertBands(DensityFunction warp) implements DensityFunction {
    public static final double PERIOD = DesertBandLayout.PERIOD;
    public static final KeyDispatchDataCodec<DesertBands> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("warp").forGetter(DesertBands::warp)
            ).apply(instance, DesertBands::new)));

    @Override
    public double compute(FunctionContext context) {
        // The displacement is constant across each Z transect. Every transect
        // therefore crosses the full range, even when the noise has a weak peak.
        // Keep the previously doubled bend field exactly. Only distances within
        // lake and terracotta bands change; sand/red profiles are translated.
        double phase = DesertBandLayout.originalCoordinate(context.blockZ() + bend(context.blockX()));
        return .02 - .60 * Math.cos(2 * Math.PI * phase / DesertBandLayout.ORIGINAL_PERIOD);
    }

    public double bend(int x) {
        return 96 * Math.tanh(warp.compute(new SinglePointContext(x * 2, 0, 0)));
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) { return visitor.apply(new DesertBands(warp.mapAll(visitor))); }
    @Override public double minValue() { return -.58; }
    @Override public double maxValue() { return .62; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
