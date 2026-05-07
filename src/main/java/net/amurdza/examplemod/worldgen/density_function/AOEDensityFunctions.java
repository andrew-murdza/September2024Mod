package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;


public final class AOEDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AOEMod.MOD_ID);

    public static final RegistryObject<Codec<? extends DensityFunction>> SHIFT_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("shift", Shift.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> DISTANCE_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("distance", Distance.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> MODULUS_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("modulus", Modulus.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> DIVIDE_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("divide", Divide.CODEC::codec);


    public static final RegistryObject<Codec<? extends DensityFunction>> VERTICAL_BIOME_COMPRESSION =
            DENSITY_FUNCTION_TYPES.register(
                    "vertical_biome_compression",
                    VerticalBiomeCompressionDensityFunction.CODEC::codec
            );

    public static final RegistryObject<Codec<? extends DensityFunction>> CONTINENTALNESS_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("continentalness_terrain", ContinentalnessTerrainDensityFunction.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> CONTINENTAL_MOUNTAIN_LIFT =
            DENSITY_FUNCTION_TYPES.register(
                    "continental_mountain_lift",
                    ContinentalMountainLiftDensityFunction.CODEC::codec
            );

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }

    public static final Long2DoubleOpenHashMap heightmap = new Long2DoubleOpenHashMap();
    public static DensityFunction temperature;

    protected record Shift(DensityFunction input, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ) implements DensityFunction {
        private static final MapCodec<Shift> DATA_CODEC = RecordCodecBuilder.mapCodec((data) -> data.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(Shift::input),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(Shift::shiftX),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_y").forGetter(Shift::shiftY),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(Shift::shiftZ)
        ).apply(data, Shift::new));

        public static final KeyDispatchDataCodec<Shift> CODEC = AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            return input.compute(new DensityFunction.SinglePointContext(
                    (int) (context.blockX() + shiftX().compute(context)),
                    (int) (context.blockY() + shiftY().compute(context)),
                    (int) (context.blockZ() + shiftZ().compute(context))
            ));
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Shift(
                    input().mapAll(visitor),
                    shiftX().mapAll(visitor),
                    shiftY().mapAll(visitor),
                    shiftZ().mapAll(visitor)
            ));
        }

        @Override public double minValue() { return -1875000d; }
        @Override public double maxValue() { return  1875000d; }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * aoemod:distance
     * sqrt((x - x0)^2 + (z - z0)^2)
     */
    protected record Distance(DensityFunction x0, DensityFunction z0) implements DensityFunction {

        private static final MapCodec<Distance> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("x0").forGetter(Distance::x0),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("z0").forGetter(Distance::z0)
                ).apply(data, Distance::new));

        public static final KeyDispatchDataCodec<Distance> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double dx = context.blockX() - x0.compute(context);
            final double dz = context.blockZ() - z0.compute(context);
            return Math.sqrt(dx * dx + dz * dz);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Distance(
                    x0.mapAll(visitor),
                    z0.mapAll(visitor)
            ));
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            // Same safety bound used elsewhere
            return 6.0e7;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * aoemod:modulus
     * argument1 % argument2
     * <p>
     * Note: Java's % keeps the sign of the dividend (argument1).
     */
    protected record Modulus(DensityFunction argument1, DensityFunction argument2) implements DensityFunction {
        private static final MapCodec<Modulus> DATA_CODEC = RecordCodecBuilder.mapCodec((data) -> data.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(Modulus::argument1),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(Modulus::argument2)
        ).apply(data, Modulus::new));

        public static final KeyDispatchDataCodec<Modulus> CODEC = AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double a = argument1.compute(context);
            final double b = argument2.compute(context);

            // Java: a % 0 => NaN
            if (b == 0.0) return Double.NaN;

            return a % b;
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Modulus(argument1.mapAll(visitor), argument2.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            // Conservative bound: remainder magnitude is < |divisor| (when divisor != 0),
            // but divisor is dynamic; use its max absolute bound.
            final double dMin = argument2.minValue();
            final double dMax = argument2.maxValue();
            final double maxAbsDiv = Math.max(Math.abs(dMin), Math.abs(dMax));
            return -maxAbsDiv;
        }

        @Override
        public double maxValue() {
            final double dMin = argument2.minValue();
            final double dMax = argument2.maxValue();
            return Math.max(Math.abs(dMin), Math.abs(dMax));
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * aoemod:divide
     * argument1 / argument2
     */
    protected record Divide(DensityFunction argument1, DensityFunction argument2) implements DensityFunction {
        private static final MapCodec<Divide> DATA_CODEC = RecordCodecBuilder.mapCodec((data) -> data.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(Divide::argument1),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(Divide::argument2)
        ).apply(data, Divide::new));

        public static final KeyDispatchDataCodec<Divide> CODEC = AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double a = argument1.compute(context);
            final double b = argument2.compute(context);
            return a / b; // Java handles b==0 as +/-Infinity or NaN
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Divide(argument1.mapAll(visitor), argument2.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return estimateMinMax()[0];
        }

        @Override
        public double maxValue() {
            return estimateMinMax()[1];
        }

        private double[] estimateMinMax() {
            final double aMin = argument1.minValue();
            final double aMax = argument1.maxValue();
            final double bMin = argument2.minValue();
            final double bMax = argument2.maxValue();

            // If denominator range crosses 0, bounds can explode.
            // Use a conservative finite clamp similar to vanilla-style safety bounds.
            if (bMin <= 0.0 && bMax >= 0.0) {
                return new double[]{-1.0e9, 1.0e9};
            }

            // Otherwise compute min/max of the 4 corner ratios
            final double r1 = aMin / bMin;
            final double r2 = aMin / bMax;
            final double r3 = aMax / bMin;
            final double r4 = aMax / bMax;

            double mn = Math.min(Math.min(r1, r2), Math.min(r3, r4));
            double mx = Math.max(Math.max(r1, r2), Math.max(r3, r4));

            // Also keep sane finite bounds if weird values appear
            if (!Double.isFinite(mn) || !Double.isFinite(mx)) {
                return new double[]{-1.0e9, 1.0e9};
            }
            return new double[]{mn, mx};
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> codec) {
        return KeyDispatchDataCodec.of(codec);
    }
}
