package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Complete, seeded mountain/lake bands, with noise bending their shared contour. */
public record MountainRidges(NoiseHolder ridge, NoiseHolder shift, double stretch, double deepWaterWidth) implements DensityFunction {
    public static final KeyDispatchDataCodec<MountainRidges> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    NoiseHolder.CODEC.fieldOf("ridge").forGetter(MountainRidges::ridge),
                    NoiseHolder.CODEC.fieldOf("shift").forGetter(MountainRidges::shift),
                    Codec.doubleRange(1, 8).fieldOf("stretch").forGetter(MountainRidges::stretch),
                    Codec.doubleRange(0, 10000).optionalFieldOf("deep_water_width", 0.0).forGetter(MountainRidges::deepWaterWidth)
            ).apply(instance, MountainRidges::new)));

    public MountainRidges(NoiseHolder ridge, NoiseHolder shift, double stretch) {
        this(ridge, shift, stretch, 0);
    }

    @Override
    public double compute(FunctionContext context) {
        // Longer, smaller bends retain curved ridgelines without the rapid
        // sideways oscillations that made the mountains look corrugated.
        double bend = bend(context.blockX());
        double coordinate = SnowyBandCompression.originalCoordinate(context.blockZ() + bend, stretch, deepWaterWidth);
        double phase = coordinate / (960 * stretch);
        phase -= Math.floor(phase);
        double triangle = 1 - 4 * Math.abs(phase - .5);
        return Math.copySign(contour(Math.abs(triangle)), triangle);
    }

    public double bend(int blockX) {
        double x = blockX * .0625 / stretch;
        return 80 * Math.tanh(ridge.getValue(x, 0, 0)) + 40 * Math.tanh(shift.getValue(x, 0, 8192));
    }

    public static double contour(double t) {
        // Preserve broad dappled shoulders while giving every ridge a snowy
        // summit. Hermite tangents keep the shoulders and summits smooth.
        if (t <= .7) return hermite(t / .7, 0, .4, .55 * .7, .7 * .7);
        return hermite((t - .7) / .3, .4, .9, .7 * .3, 0);
    }

    private static double hermite(double t, double a, double b, double da, double db) {
        return (2 * t * t * t - 3 * t * t + 1) * a
                + (t * t * t - 2 * t * t + t) * da
                + (-2 * t * t * t + 3 * t * t) * b + (t * t * t - t * t) * db;
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MountainRidges(visitor.visitNoise(ridge), visitor.visitNoise(shift), stretch, deepWaterWidth));
    }
    @Override public double minValue() { return -.9; }
    @Override public double maxValue() { return .9; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
