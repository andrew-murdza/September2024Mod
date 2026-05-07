package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Adds a smooth, noisy mountain-height density contribution controlled by 2D continentalness.
 * This is intentionally applied to initial_density_without_jaggedness, not final_density,
 * so caves, cheese, noodle caves, and preliminary surface stay much closer to vanilla behavior.
 */
public record ContinentalMountainLiftDensityFunction(
        DensityFunction input,
        DensityFunction continents,
        DensityFunction heightNoise,
        double edgeMin,
        double fullMin,
        double fullMax,
        double edgeMax,
        int minSurfaceY,
        int maxSurfaceY,
        double verticalScale,
        double strength
) implements DensityFunction {

    private static final Codec<Double> DOUBLE = Codec.DOUBLE;
    private static final Codec<Integer> INT = Codec.INT;

    public static final MapCodec<ContinentalMountainLiftDensityFunction> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(ContinentalMountainLiftDensityFunction::input),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(ContinentalMountainLiftDensityFunction::continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("height_noise").forGetter(ContinentalMountainLiftDensityFunction::heightNoise),
            DOUBLE.fieldOf("edge_min").orElse(0.08).forGetter(ContinentalMountainLiftDensityFunction::edgeMin),
            DOUBLE.fieldOf("full_min").orElse(0.14).forGetter(ContinentalMountainLiftDensityFunction::fullMin),
            DOUBLE.fieldOf("full_max").orElse(0.24).forGetter(ContinentalMountainLiftDensityFunction::fullMax),
            DOUBLE.fieldOf("edge_max").orElse(0.29).forGetter(ContinentalMountainLiftDensityFunction::edgeMax),
            INT.fieldOf("min_surface_y").orElse(140).forGetter(ContinentalMountainLiftDensityFunction::minSurfaceY),
            INT.fieldOf("max_surface_y").orElse(175).forGetter(ContinentalMountainLiftDensityFunction::maxSurfaceY),
            DOUBLE.fieldOf("vertical_scale").orElse(42.0).forGetter(ContinentalMountainLiftDensityFunction::verticalScale),
            DOUBLE.fieldOf("strength").orElse(0.38).forGetter(ContinentalMountainLiftDensityFunction::strength)
    ).apply(instance, ContinentalMountainLiftDensityFunction::new));

    public static final KeyDispatchDataCodec<ContinentalMountainLiftDensityFunction> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        double base = input.compute(context);

        double c = continents.compute(context);
        double w = mountainWeight(c, edgeMin, fullMin, fullMax, edgeMax);
        if (w <= 0.0) {
            return base;
        }

        double n = clamp(heightNoise.compute(context), -1.0, 1.0);
        double t = (n + 1.0) * 0.5;
        double targetSurface = lerp(minSurfaceY, maxSurfaceY, t);

        // Positive below targetSurface, negative above it.
        // The clamp prevents a vertical wall/flat ceiling from overpowering vanilla terrain.
        double yShape = clamp((targetSurface - context.blockY()) / verticalScale, -1.0, 1.0);
        return base + w * strength * yShape;
    }

    private static double mountainWeight(double c, double edgeMin, double fullMin, double fullMax, double edgeMax) {
        if (c <= edgeMin || c >= edgeMax) return 0.0;
        if (c >= fullMin && c <= fullMax) return 1.0;
        if (c < fullMin) return smoothstep((c - edgeMin) / (fullMin - edgeMin));
        return smoothstep((edgeMax - c) / (edgeMax - fullMax));
    }

    private static double smoothstep(double x) {
        x = clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public void fillArray(double[] values, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ContinentalMountainLiftDensityFunction(
                input.mapAll(visitor),
                continents.mapAll(visitor),
                heightNoise.mapAll(visitor),
                edgeMin, fullMin, fullMax, edgeMax,
                minSurfaceY, maxSurfaceY, verticalScale, strength
        ));
    }

    @Override
    public double minValue() {
        return input.minValue() - Math.abs(strength);
    }

    @Override
    public double maxValue() {
        return input.maxValue() + Math.abs(strength);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
