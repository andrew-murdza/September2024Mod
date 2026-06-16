package net.amurdza.examplemod.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public final class ModDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AOEMod.MOD_ID);

    static {
        DENSITY_FUNCTION_TYPES.register("continents", Continents.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("rivers", RadialRivers.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("offset", SteppedMountainOffset.CODEC::codec);
    }

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }

    protected record Continents(int biome_width, int z_0) implements DensityFunction {

        private static final MapCodec<Continents> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        Codec.INT.fieldOf("biome_width").forGetter(Continents::biome_width),
                        Codec.INT.fieldOf("z_0").forGetter(Continents::z_0)
                ).apply(data, Continents::new));

        public static final KeyDispatchDataCodec<Continents> CODEC =
                ModDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            int bandWidthBlocks = biome_width * 16;
            int periodBlocks = bandWidthBlocks * 10;

            if (bandWidthBlocks <= 0 || periodBlocks <= 0) {
                return 0.0D;
            }

            int shiftedZ = context.blockZ() - z_0;
            int wrappedZ = Math.floorMod(shiftedZ, periodBlocks);

            double bandPosition = (double) wrappedZ / (double) bandWidthBlocks;

            return 0.5D - 0.1D * Math.abs(bandPosition - 5.0D);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Continents(biome_width, z_0));
        }

        @Override
        public double minValue() {
            return 0.0D;
        }

        @Override
        public double maxValue() {
            return 0.5D;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record SteppedMountainOffset(
            DensityFunction continents,
            DensityFunction rivers,
            int biomeWidth
    ) implements DensityFunction {

        private static final double LOW_LAND_OFFSET = -0.5D;

        private static final double BADLANDS_START = 0.400D;

        private static final int DESERT_FLAT_ENTRY_BLOCKS = 48;
        private static final double DESERT_ENTRY_SLOPE = 1D/3;

        private static final MapCodec<SteppedMountainOffset> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC
                                .fieldOf("continents")
                                .forGetter(SteppedMountainOffset::continents),
                        DensityFunction.HOLDER_HELPER_CODEC
                                .fieldOf("rivers")
                                .forGetter(SteppedMountainOffset::rivers),
                        ExtraCodecs.POSITIVE_INT
                                .fieldOf("biome_width")
                                .forGetter(SteppedMountainOffset::biomeWidth)
                ).apply(data, SteppedMountainOffset::new));

        public static final KeyDispatchDataCodec<SteppedMountainOffset> CODEC =
                ModDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double c = continents.compute(context);
            final double riverValue = rivers.compute(context);

            return applyRivers(c, riverValue, biomeWidth);
        }

        private static double applyRivers(
                double c,
                double riverValue,
                int biomeWidth
        ) {
            final double negativeRiver = Math.min(riverValue, 0.0D);
            final double positiveRiver = Math.max(riverValue, 0.0D);

            double offset = SteppedMountainOffset.LOW_LAND_OFFSET;

            if (positiveRiver > 0.0D && c >= BADLANDS_START) {
                final double oneBlockContinents = 0.1D / (16 * biomeWidth);
                final double allowedEntryLift = getDesertEntryLiftLimit(c, oneBlockContinents);
                final double cappedPositiveRiver = Math.min(positiveRiver, allowedEntryLift);

                offset = Math.max(
                        SteppedMountainOffset.LOW_LAND_OFFSET,
                        LOW_LAND_OFFSET + cappedPositiveRiver
                );
            }

            if (negativeRiver >= 0.0D) {
                return offset;
            }

            return offset + negativeRiver;
        }

        private static double getDesertEntryLiftLimit(double c, double oneBlockContinents) {
            final double flatEnd = BADLANDS_START
                    + DESERT_FLAT_ENTRY_BLOCKS * oneBlockContinents;

            if (c <= flatEnd) {
                return 0.0D;
            }

            final int blocksPastFlat = continentsToWholeBlocks(c - flatEnd, oneBlockContinents);
            return (blocksPastFlat * DESERT_ENTRY_SLOPE) / 128.0D;
        }

        private static int continentsToWholeBlocks(double continentsDistance, double oneBlockContinents) {
            return (int) Math.floor(continentsDistance / oneBlockContinents);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new SteppedMountainOffset(
                    continents.mapAll(visitor),
                    rivers.mapAll(visitor),
                    biomeWidth
            ));
        }

        @Override
        public double minValue() {
            return LOW_LAND_OFFSET - (10.0D / 128.0D);
        }

        @Override
        public double maxValue() {
            return 1.0D;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }



    /**
     * aoemod:rivers
     * New behavior:
     * - x0 and z0 are chunk coordinates.
     * - x0 is no longer the center of the river.
     * - The river starts at block x = x0 * 16.
     * - The river extends width chunks in the x direction.
     * - The next river starts after x_spacing chunks.
     * Example:
     *   x0 = 0
     *   width = 6
     *   x_spacing = 30
     * First river:
     *   x = 0 through 95
     * Next river:
     *   x = 480 through 575
     */
    protected record RadialRivers(
            int z0,
            DensityFunction continents,
            int x0,
            int width,
            int biomeWidth,
            int xSpacing
    ) implements DensityFunction {


        private static RiverProfile steppedDepthRamp(
                RiverProfile start,
                RiverProfile end,
                double rampStart,
                double oneBlockContinents,
                double continents
        ) {
            int blocksPastStart = (int) Math.round(
                    (continents - rampStart) / oneBlockContinents
            );

            blocksPastStart = Math.max(0, blocksPastStart);

            double startDepth = start.maxDepth();
            double endDepth = end.maxDepth();

            double direction = Math.signum(endDepth - startDepth);
            double depthChange = Math.abs(endDepth - startDepth);

            int wholeDepthSteps = blocksPastStart / 2;

            double newDepth = startDepth + direction * Math.min(
                    depthChange,
                    wholeDepthSteps
            );

            return new RiverProfile(newDepth);
        }
        private static final double NORMAL_RIVER_START = 0;

        private static final double JUNGLE_END = 0.100D;
        private static final double SAVANNA_END = 0.200D;
        private static final double MOUNTAIN_START = 0.300D;
        private static final double BADLANDS_START = 0.400D;

        private static final double RIVER_BANK_SLOPE = 0.5D;

        private static final RiverProfile JUNGLE_RIVER =
                new RiverProfile(10.0D);

        private static final RiverProfile SAVANNA_RIVER =
                new RiverProfile(8.0D);

        private static final RiverProfile PLAINS_RIVER =
                new RiverProfile(6.0D);

        private static final RiverProfile BADLANDS_EDGE_RIVER =
                new RiverProfile(6.0D);

        private static final double BADLANDS_RIVER_DEPTH = 6.0D;

        private static final double BADLANDS_FLAT_AFTER_RIVER = 48.0D;
        private static final double BADLANDS_TERRACE_SLOPE = 1D/3;

        private static final double MAX_TERRACE_HEIGHT = 64.0D;

        private static final MapCodec<RadialRivers> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        Codec.INT
                                .fieldOf("z0")
                                .forGetter(RadialRivers::z0),
                        DensityFunction.HOLDER_HELPER_CODEC
                                .fieldOf("continents")
                                .forGetter(RadialRivers::continents),
                        Codec.INT
                                .fieldOf("x0")
                                .forGetter(RadialRivers::x0),
                        ExtraCodecs.POSITIVE_INT
                                .fieldOf("width")
                                .forGetter(RadialRivers::width),
                        ExtraCodecs.POSITIVE_INT
                                .fieldOf("biome_width")
                                .forGetter(RadialRivers::biomeWidth),
                        ExtraCodecs.POSITIVE_INT
                                .fieldOf("x_spacing")
                                .forGetter(RadialRivers::xSpacing)
                ).apply(data, RadialRivers::new));

        public static final KeyDispatchDataCodec<RadialRivers> CODEC =
                ModDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double continent = continents.compute(context);

            final int widthBlocks = width * 16;
            final int spacingBlocks = xSpacing * 16;

            final int riverStartX = x0 * 16 + 1;
            final int localX = Math.floorMod(context.blockX() - riverStartX, spacingBlocks);

            final double distanceFromRiverEdge = getDistanceFromRepeatingRiverEdge(
                    localX,
                    widthBlocks,
                    spacingBlocks
            );

            if (continent >= BADLANDS_START) {
                return getBadlandsRiverValue(distanceFromRiverEdge);
            }

            final RiverProfile profile = getNormalRiverProfile(continent);

            if (profile.maxDepth() <= 0.0D) {
                return 0.0D;
            }

            return getRiverCutFromEdge(
                    distanceFromRiverEdge,
                    profile.maxDepth()
            );
        }

        private static double getDistanceFromRepeatingRiverEdge(
                int localX,
                int widthBlocks,
                int spacingBlocks
        ) {
            if (localX >= 0 && localX < widthBlocks) {
                final int distanceFromLeftEdge = localX;
                final int distanceFromRightEdge = widthBlocks - 1 - localX;

                return Math.min(
                        distanceFromLeftEdge,
                        distanceFromRightEdge
                ) + 1.0D;
            }

            final int distanceFromRightEdgeOfThisRiver = localX - (widthBlocks - 1);
            final int distanceFromLeftEdgeOfNextRiver = spacingBlocks - localX;

            return -Math.min(
                    distanceFromRightEdgeOfThisRiver,
                    distanceFromLeftEdgeOfNextRiver
            );
        }

        private RiverProfile getNormalRiverProfile(double continents) {
            if (continents < NORMAL_RIVER_START || continents >= BADLANDS_START) {
                return RiverProfile.NONE;
            }

            final double oneBlockContinents = 0.1D/ (16 * biomeWidth);

            final double SMALL_PROFILE_RAMP =
                    4.0D * oneBlockContinents;

            final double MOUNTAIN_PROFILE_RAMP =
                    8.0D * oneBlockContinents;

            final double LOWLAND_RAMP_SHIFT = oneBlockContinents;


            final double JUNGLE_TO_SAVANNA_RAMP_END =
                    JUNGLE_END - LOWLAND_RAMP_SHIFT;

            final double JUNGLE_TO_SAVANNA_RAMP_START =
                    JUNGLE_TO_SAVANNA_RAMP_END - SMALL_PROFILE_RAMP;

            final double SAVANNA_TO_PLAINS_RAMP_END =
                    SAVANNA_END - LOWLAND_RAMP_SHIFT;

            final double SAVANNA_TO_PLAINS_RAMP_START =
                    SAVANNA_TO_PLAINS_RAMP_END - SMALL_PROFILE_RAMP;

            final double PLAINS_TO_MOUNTAIN_RAMP_START =
                    MOUNTAIN_START;

            final double PLAINS_TO_MOUNTAIN_RAMP_END =
                    PLAINS_TO_MOUNTAIN_RAMP_START + MOUNTAIN_PROFILE_RAMP;

            final double MOUNTAIN_TO_BADLANDS_RAMP_END =
                    BADLANDS_START - 2 * oneBlockContinents;

            final double MOUNTAIN_TO_BADLANDS_RAMP_START =
                    MOUNTAIN_TO_BADLANDS_RAMP_END - MOUNTAIN_PROFILE_RAMP;

            if (continents < JUNGLE_TO_SAVANNA_RAMP_START) {
                return JUNGLE_RIVER;
            }

            if (continents < JUNGLE_TO_SAVANNA_RAMP_END) {
                final double t = inverseLerp(
                        JUNGLE_TO_SAVANNA_RAMP_START,
                        JUNGLE_TO_SAVANNA_RAMP_END,
                        continents
                );

                return RiverProfile.lerp(
                        JUNGLE_RIVER,
                        SAVANNA_RIVER,
                        t
                );
            }

            if (continents < SAVANNA_TO_PLAINS_RAMP_START) {
                return SAVANNA_RIVER;
            }

            if (continents < SAVANNA_TO_PLAINS_RAMP_END) {
                final double t = inverseLerp(
                        SAVANNA_TO_PLAINS_RAMP_START,
                        SAVANNA_TO_PLAINS_RAMP_END,
                        continents
                );

                return RiverProfile.lerp(
                        SAVANNA_RIVER,
                        PLAINS_RIVER,
                        t
                );
            }

            if (continents < PLAINS_TO_MOUNTAIN_RAMP_START) {
                return PLAINS_RIVER;
            }

            if (continents < PLAINS_TO_MOUNTAIN_RAMP_END) {
                return steppedDepthRamp(
                        PLAINS_RIVER,
                        JUNGLE_RIVER,
                        PLAINS_TO_MOUNTAIN_RAMP_START,
                        oneBlockContinents,
                        continents
                );
            }

            if (continents >= MOUNTAIN_TO_BADLANDS_RAMP_START) {
                return steppedDepthRamp(
                        JUNGLE_RIVER,
                        BADLANDS_EDGE_RIVER,
                        MOUNTAIN_TO_BADLANDS_RAMP_START,
                        oneBlockContinents,
                        continents
                );
            }

            return JUNGLE_RIVER;
        }

        private double getBadlandsRiverValue(double distanceFromRiverEdge) {
            /*
             * Inside the river.
             */
            if (distanceFromRiverEdge > 0.0D) {
                return getRiverCutFromEdge(
                        distanceFromRiverEdge,
                        BADLANDS_RIVER_DEPTH
                );
            }

            /*
             * Outside the river:
             * - flat for BADLANDS_FLAT_AFTER_RIVER blocks
             * - then rises at slope 1/2
             */
            final double distanceOutsideRiver = -distanceFromRiverEdge;

            if (distanceOutsideRiver < BADLANDS_FLAT_AFTER_RIVER) {
                return 0.0D;
            }

            final double terraceDistance = distanceOutsideRiver - BADLANDS_FLAT_AFTER_RIVER;

            final double lift = Math.min(
                    terraceDistance * BADLANDS_TERRACE_SLOPE,
                    MAX_TERRACE_HEIGHT
            );

            return lift / 128.0D;
        }

        private static double getRiverCutFromEdge(
                double distanceFromRiverEdge,
                double maxDepth
        ) {
            final double depth = Math.max(
                    0.0D,
                    Math.min(
                            maxDepth,
                            distanceFromRiverEdge * RIVER_BANK_SLOPE
                    )
            );

            return -depth / 128.0D;
        }

        private static double inverseLerp(double min, double max, double value) {
            if (max == min) {
                return 0.0D;
            }

            return clamp01((value - min) / (max - min));
        }

        private static double clamp01(double value) {
            return Math.min(Math.max(value, 0.0D), 1.0D);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new RadialRivers(
                    z0,
                    continents.mapAll(visitor),
                    x0,
                    width,
                    biomeWidth,
                    xSpacing
            ));
        }

        @Override
        public double minValue() {
            return -10.0D / 128.0D;
        }

        @Override
        public double maxValue() {
            return 1.0D;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        private record RiverProfile(double maxDepth) {
            private static final RiverProfile NONE = new RiverProfile(0.0D);

            private static RiverProfile lerp(RiverProfile start, RiverProfile end, double t) {
                return new RiverProfile(
                        lerpDouble(start.maxDepth(), end.maxDepth(), t)
                );
            }

            private static double lerpDouble(double start, double end, double t) {
                return start + (end - start) * t;
            }
        }
    }

    static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> codec) {
        return KeyDispatchDataCodec.of(codec);
    }
}