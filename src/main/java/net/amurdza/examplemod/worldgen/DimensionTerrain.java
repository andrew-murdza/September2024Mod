package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Seeded terrain shared by the density router and biome selection. Heights are block coordinates. */
public record DimensionTerrain(String dimension, String output, DensityFunction land, DensityFunction detail,
                               DensityFunction river, DensityFunction lake, DensityFunction caveA,
                               DensityFunction caveB) implements DensityFunction {
    public static final KeyDispatchDataCodec<DimensionTerrain> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("dimension").forGetter(DimensionTerrain::dimension),
                    Codec.STRING.optionalFieldOf("output", "density").forGetter(DimensionTerrain::output),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("land").forGetter(DimensionTerrain::land),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(DimensionTerrain::detail),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("river").forGetter(DimensionTerrain::river),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("lake").forGetter(DimensionTerrain::lake),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_a").forGetter(DimensionTerrain::caveA),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_b").forGetter(DimensionTerrain::caveB)
            ).apply(instance, DimensionTerrain::new)));

    @Override
    public double compute(FunctionContext context) {
        FunctionContext column = new SinglePointContext(context.blockX(), 0, context.blockZ());
        double continent = land.compute(column);
        double roughness = detail.compute(column);
        double drainage = Math.abs(river.compute(column));
        double basin = lake.compute(column);
        double height = surfaceHeight(dimension, continent, roughness, drainage, basin);
        if (output.equals("height")) return height;
        if (output.equals("biome")) return tropicalBiome(continent, basin, drainage, height);
        int y = context.blockY();
        if (dimension.equals("nether")) return nether(context, continent, roughness, basin);
        double density = (height + 0.5 - y) / 8.0;
        if (dimension.equals("tropical") || dimension.equals("savanna")) return density;
        if (dimension.equals("snowy")) {
            // No carver may puncture the rock between the deepest lake (Y16) and this ceiling.
            if (y >= -32 && y <= -1) {
                double floor = -26 + Mth.clamp(roughness * 4, -2, 2);
                double ceiling = floor + 22 + Mth.clamp(basin * 3, -2, 2);
                ceiling = Math.min(-3, ceiling);
                density = Math.min(density, gallery(y, floor, ceiling, continent));
            } else if (y > -90 && y < -49) {
                density = Math.min(density, tunnel(context, -90, -49));
            }
        } else if (dimension.equals("desert")) {
            if (y >= -31 && y < 0) {
                density = Math.min(density, gallery(y, -27 + Mth.clamp(roughness * 4, -2, 2),
                        -4 + Mth.clamp(basin * 2, -1, 1), continent));
            } else if (y >= 8 && y < 40) {
                density = Math.min(density, tunnel(context, 8, 40));
            }
        } else if (y > -60 && y < 40) {
            density = Math.min(density, tunnel(context, -60, 40));
        }
        return density;
    }

    /** Continuous banks avoid vertical water walls at lake and river edges. */
    public static double surfaceHeight(String dimension, double land, double detail, double river, double lake) {
        double height;
        switch (dimension) {
            case "tropical" -> {
                height = Mth.clamp(64 + (land + 0.15) * 78 + detail * 5, 24, 150);
                double channel = 1 - smooth(0.025, 0.085, river);
                height = Math.min(height, Mth.lerp(channel, height, 55));
                double pond = smooth(0.36, 0.53, -lake);
                return Math.min(height, Mth.lerp(pond, height, 54));
            }
            case "savanna" -> {
                height = Math.max(65, 72 + land * 15 + detail * 3);
                height = Mth.lerp(1 - smooth(0.022, 0.065, river), height, 56);
                return Mth.lerp(smooth(0.32, 0.48, lake), height, 52 + Mth.clamp(detail * 4, -3, 3));
            }
            case "plains" -> {
                height = Math.max(65, 74 + land * 19 + detail * 4);
                height = Mth.lerp(1 - smooth(0.020, 0.060, river), height, 57);
                return Mth.lerp(smooth(0.35, 0.51, lake), height, 51 + Mth.clamp(detail * 5, -3, 3));
            }
            case "snowy" -> {
                height = Math.max(101, 111 + Math.max(0, land) * 82 + detail * 9);
                double basinFloor = 48 - 24 * smooth(0.34, 0.64, lake) - 8 * smooth(0.70, 0.85, lake);
                height = Mth.lerp(smooth(0.06, 0.34, lake), height, basinFloor);
                height = Math.min(height, Mth.lerp(1 - smooth(0.014, 0.050, river), height, 84));
                return Math.max(16, height);
            }
            case "desert" -> {
                height = Math.max(66, 72 + Math.pow(Math.max(0, land + 0.06), 1.5) * 145 + detail * 5);
                // Rare groundwater oases; shallow connecting channels are confined to lowland basins.
                height = Mth.lerp(smooth(0.57, 0.72, lake), height, 59);
                if (land < -0.18 && lake > 0.15) {
                    height = Mth.lerp(1 - smooth(0.007, 0.025, river), height, 60);
                }
                return height;
            }
            default -> { return 128; }
        }
    }

    /** Four discrete climate values, read by DimensionBiomeSource using the same seeded router. */
    public static double tropicalBiome(double land, double wetness, double river, double height) {
        if (land < -0.15 && height < 64) return 0;
        if (river < 0.042 || wetness < -0.41 || height < 66) return 1;
        if (wetness > 0.19) return 3;
        return 2;
    }

    private double tunnel(FunctionContext context, int bottom, int top) {
        double tubes = Math.max(Math.abs(caveA.compute(context)) - 0.12,
                Math.abs(caveB.compute(context)) - 0.14) * 32;
        return Math.max(tubes, Math.max(bottom + 2 - context.blockY(), context.blockY() - top + 2));
    }

    private static double gallery(int y, double floor, double ceiling, double pillars) {
        double room = Math.max(floor + 0.5 - y, y - ceiling + 0.5);
        // Broad rooms remain linked around seeded support pillars rather than a single hollow slab.
        return Math.max(room, (pillars - 0.56) * 26) / 4;
    }

    private static double nether(FunctionContext context, double land, double detail, double basin) {
        int y = context.blockY();
        if (y <= 1 || y >= 127) return 1;
        int band = Math.floorDiv(y, 32);
        double floor = band * 32 + 6 + Mth.clamp(detail * 4, -2, 2);
        if (band == 0) floor = Mth.lerp(smooth(-0.15, 0.22, basin), 5, 18 + Mth.clamp(detail * 4, -2, 2));
        else floor = Mth.lerp(smooth(0.20, 0.42, basin), floor, band * 32 + 1);
        double corridor = netherRampDistance(context.blockZ(), detail);
        if (band > 0) {
            // A solid bank separates the ramp tunnel from the upper lava pools.
            // It remains above the fluid threshold even after four-block interpolation.
            double bank = 1 - smooth(16, 24, corridor);
            floor = Math.max(floor, Mth.lerp(bank, floor, band * 32 + 7));
        }
        double ceiling = band * 32 + 29 + Mth.clamp(basin * 3, -1, 1);
        double density = gallery(y, floor, ceiling, land);
        // The walkway needs its own solid support through an otherwise open gallery.
        // Carving alone would leave it suspended until it reached the next rock shelf.
        double rampFloor = netherRampFloor(context.blockX(), basin);
        double support = Math.min(rampFloor + 0.5 - y, 5 - corridor) / 4;
        density = Math.max(density, support);
        double ramp = Math.max(Math.max(rampFloor + 0.5 - y, y - rampFloor - 6.5), corridor - 5) / 4;
        return Math.min(density, ramp);
    }

    /** Shared with the seeded fluid picker, including the same detail-field displacement. */
    public static double netherRampDistance(int z, double detail) {
        double displaced = z + detail * 18;
        return Math.abs(displaced - 192 * Math.floor(displaced / 192.0) - 96);
    }

    public static double netherRampFloor(int x, double basin) {
        double phase = (x + basin * 32) / 368.0;
        phase -= Math.floor(phase);
        return 18 + 92 * (1 - Math.abs(2 * phase - 1));
    }

    private static double smooth(double low, double high, double value) {
        double t = Mth.clamp((value - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override public void fillArray(double[] values, ContextProvider context) { context.fillAllDirectly(values, this); }
    @Override public double minValue() { return -128; }
    @Override public double maxValue() { return 384; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        // Visiting every child is essential: RandomState installs world-seeded NormalNoise instances here.
        return visitor.apply(new DimensionTerrain(dimension, output, land.mapAll(visitor), detail.mapAll(visitor),
                river.mapAll(visitor), lake.mapAll(visitor), caveA.mapAll(visitor), caveB.mapAll(visitor)));
    }
}
