package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record VerticalBiomeCompressionDensityFunction(
        DensityFunction continentalness,
        DensityFunction terrainNoCaves,
        DensityFunction terrainWithCaves,
        List<BiomeParams> biomes,
        List<BoundaryParams> boundaries
) implements DensityFunction {

    public static final MapCodec<VerticalBiomeCompressionDensityFunction> DATA_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continentalness")
                            .forGetter(VerticalBiomeCompressionDensityFunction::continentalness),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain_no_caves")
                            .forGetter(VerticalBiomeCompressionDensityFunction::terrainNoCaves),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain_with_caves")
                            .forGetter(VerticalBiomeCompressionDensityFunction::terrainWithCaves),
                    BiomeParams.CODEC.listOf().fieldOf("biomes")
                            .forGetter(VerticalBiomeCompressionDensityFunction::biomes),
                    BoundaryParams.CODEC.listOf().fieldOf("boundaries")
                            .forGetter(VerticalBiomeCompressionDensityFunction::boundaries)
            ).apply(instance, VerticalBiomeCompressionDensityFunction::new));

    public static final KeyDispatchDataCodec<VerticalBiomeCompressionDensityFunction> CODEC =
            KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();

        double c = continentalness.compute(context);

        BlendResult blend = getBlendedParams(y, c);

        double sourceY = blend.sourceY();
        double lambda = clamp(blend.lambda(), 0.0, 1.0);

        double noCaves = sampleYInterpolated(terrainNoCaves, x, sourceY, z);
        double withCaves = sampleYInterpolated(terrainWithCaves, x, sourceY, z);

        return lerp(noCaves, withCaves, lambda);
    }

    private BlendResult getBlendedParams(double outputY, double c) {
        if (biomes.isEmpty()) {
            return new BlendResult(outputY, 1.0);
        }

        if (biomes.size() == 1 || boundaries.isEmpty()) {
            BiomeParams biome = biomes.get(0);
            return new BlendResult(
                    biome.sourceY(outputY),
                    biome.lambda(outputY)
            );
        }

        int boundaryCount = Math.min(boundaries.size(), biomes.size() - 1);

        for (int i = 0; i < boundaryCount; i++) {
            BoundaryParams boundary = boundaries.get(i);

            double left = boundary.c() - boundary.deltaMinus();
            double right = boundary.c() + boundary.deltaPlus();

            if (c >= left && c <= right) {
                double denom = Math.max(right - left, 1.0E-6);
                double t = clamp((c - left) / denom, 0.0, 1.0);
                double r = smoothstep(t);

                BiomeParams a = biomes.get(i);
                BiomeParams b = biomes.get(i + 1);

                double sourceY = lerp(a.sourceY(outputY), b.sourceY(outputY), r);
                double lambda = lerp(a.lambda(outputY), b.lambda(outputY), r);

                return new BlendResult(sourceY, lambda);
            }

            if (c < left) {
                BiomeParams biome = biomes.get(i);
                return new BlendResult(
                        biome.sourceY(outputY),
                        biome.lambda(outputY)
                );
            }
        }

        BiomeParams biome = biomes.get(Math.min(boundaryCount, biomes.size() - 1));
        return new BlendResult(
                biome.sourceY(outputY),
                biome.lambda(outputY)
        );
    }

    private static double sampleYInterpolated(DensityFunction function, int x, double y, int z) {
        int y0 = (int) Math.floor(y);
        int y1 = y0 + 1;
        double t = y - y0;

        double a = function.compute(new SinglePointContext(x, y0, z));
        double b = function.compute(new SinglePointContext(x, y1, z));

        return lerp(a, b, t);
    }

    private static double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void fillArray(double @NotNull [] values, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(values, this);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new VerticalBiomeCompressionDensityFunction(
                continentalness,
                terrainNoCaves,
                terrainWithCaves,
                biomes,
                boundaries
        ));
    }

    @Override
    public double minValue() {
        return Math.min(
                terrainNoCaves.minValue(),
                terrainWithCaves.minValue()
        );
    }

    @Override
    public double maxValue() {
        return Math.max(
                terrainNoCaves.maxValue(),
                terrainWithCaves.maxValue()
        );
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private record BlendResult(double sourceY, double lambda) {}

    private record SinglePointContext(int blockX, int blockY, int blockZ)
            implements DensityFunction.FunctionContext {}

    public record BoundaryParams(
            double c,
            double deltaMinus,
            double deltaPlus
    ) {
        public static final Codec<BoundaryParams> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("c").forGetter(BoundaryParams::c),
                        Codec.DOUBLE.fieldOf("delta_minus").forGetter(BoundaryParams::deltaMinus),
                        Codec.DOUBLE.fieldOf("delta_plus").forGetter(BoundaryParams::deltaPlus)
                ).apply(instance, BoundaryParams::new));
    }

    public record BiomeParams(
            String name,
            CompressionType type,

            double y0,

            double hMin,
            double hMax,

            double HMin,
            double HMax,

            double beta,
            double L,

            boolean suppressCaves
    ) {
        public static final Codec<BiomeParams> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("name").forGetter(BiomeParams::name),
                        CompressionType.CODEC.fieldOf("type").forGetter(BiomeParams::type),

                        Codec.DOUBLE.optionalFieldOf("y0", 0.0).forGetter(BiomeParams::y0),

                        Codec.DOUBLE.optionalFieldOf("h_min", 0.0).forGetter(BiomeParams::hMin),
                        Codec.DOUBLE.optionalFieldOf("h_max", 0.0).forGetter(BiomeParams::hMax),

                        Codec.DOUBLE.optionalFieldOf("H_min", 0.0).forGetter(BiomeParams::HMin),
                        Codec.DOUBLE.optionalFieldOf("H_max", 0.0).forGetter(BiomeParams::HMax),

                        Codec.DOUBLE.optionalFieldOf("beta", 0.0).forGetter(BiomeParams::beta),
                        Codec.DOUBLE.optionalFieldOf("L", 1.0).forGetter(BiomeParams::L),

                        Codec.BOOL.optionalFieldOf("suppress_caves", false).forGetter(BiomeParams::suppressCaves)
                ).apply(instance, BiomeParams::new));

        public double sourceY(double outputY) {
            return switch (type) {
                case VANILLA -> outputY;

                case FULL -> HMin + safeDiv(
                        HMax - HMin,
                        hMax - hMin
                ) * (outputY - hMin);

                case UPPER -> {
                    if (outputY < y0) {
                        yield outputY;
                    }

                    yield y0 + safeDiv(
                            HMax - y0,
                            hMax - hMin
                    ) * (outputY - hMin);
                }
            };
        }

        public double lambda(double outputY) {
            if (!suppressCaves) {
                return 1.0;
            }

            double cutoff = hMin - beta;

            if (outputY >= cutoff) {
                return 0.0;
            }

            if (outputY <= cutoff - L) {
                return 1.0;
            }

            double t = (cutoff - outputY) / Math.max(L, 1.0E-6);
            return smoothstep(t);
        }

        private static double safeDiv(double numerator, double denominator) {
            if (Math.abs(denominator) < 1.0E-6) {
                return 0.0;
            }
            return numerator / denominator;
        }
    }

    public enum CompressionType implements StringRepresentable {
        VANILLA("vanilla"),
        FULL("full"),
        UPPER("upper");

        public static final Codec<CompressionType> CODEC =
                StringRepresentable.fromEnum(CompressionType::values);

        private final String serializedName;

        CompressionType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }
}