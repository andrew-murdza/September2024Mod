package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AOEDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AOEMod.MOD_ID);

    static {
        DENSITY_FUNCTION_TYPES.register("modulus", Modulus.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("divide", Divide.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("coordinate", Coordinate.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("linear_spline", LinearSpline.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("radial_rivers", RadialRivers.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("stepped_mountain_offset", SteppedMountainOffset.CODEC::codec);
        DENSITY_FUNCTION_TYPES.register("x_bands", XBands.CODEC::codec);
    }

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }

    /**
     * aoemod:river_distance_bands
     * Returns horizontal distance from the nearest river water edge.
     * With arc_spacing = 320 and river radius = 40:
     *   0   = at/inside the river water area
     *   120 = halfway between two rivers
     * This is intended for things like flower color bands parallel to rivers.
     */
    protected record XBands (
            DensityFunction x0,
            DensityFunction arcSpacing
    ) implements DensityFunction {
        /*
         * Match your current river cutoff.
         * Normal rivers run until the mountain plateau ends.
         * Badlands river/terrace logic takes over after this.
         */
        private static final MapCodec<XBands> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("x0").forGetter(XBands::x0),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("arc_spacing").forGetter(XBands::arcSpacing)
                ).apply(data, XBands::new));

        public static final KeyDispatchDataCodec<XBands> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double cx = x0.compute(context);
            final double spacing = arcSpacing.compute(context);
            final double x = context.blockX() - cx;
            return x - spacing * Math.floor(x / spacing);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new XBands(
                    x0.mapAll(visitor),
                    arcSpacing.mapAll(visitor)
            ));
        }

        @Override
        public double minValue() {
            return 0.0D;
        }

        @Override
        public double maxValue() {
            /*
             * Safe upper bound.
             * With arc_spacing = 320 and radius = 40, the actual max is 120.
             */
            return arcSpacing.maxValue();
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * aoemod:coordinate
     * Returns one block coordinate minus an origin:
     * x -> blockX - origin
     * y -> blockY - origin
     * z -> blockZ - origin
     */
    protected record Coordinate(String axis, DensityFunction origin) implements DensityFunction {

        private static final MapCodec<Coordinate> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        Codec.STRING.fieldOf("axis").forGetter(Coordinate::axis),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("origin").forGetter(Coordinate::origin)
                ).apply(data, Coordinate::new));

        public static final KeyDispatchDataCodec<Coordinate> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double o = origin.compute(context);

            return switch (axis) {
                case "x" -> context.blockX() - o;
                case "y" -> context.blockY() - o;
                case "z" -> context.blockZ() - o;
                default -> 0.0D;
            };
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Coordinate(axis, origin.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return -6.0e7;
        }

        @Override
        public double maxValue() {
            return 6.0e7;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /**
     * aoemod:linear_spline
     * A simple piecewise-linear density function.
     * JSON format:
     * {
     *   "type": "aoemod:linear_spline",
     *   "coordinate": "aoemod:continents",
     *   "points": [
     *     { "location": 0.4, "value": -0.5 },
     *     { "location": 0.405, "value": 0.25 }
     *   ]
     * }
     * Behavior:
     * - below the first point: returns first value
     * - between points: true linear interpolation
     * - above the last point: returns last value
     */
    protected record LinearSpline(
            DensityFunction coordinate,
            List<Point> points
    ) implements DensityFunction {

        private static final Codec<List<Point>> POINTS_CODEC =
                Point.CODEC.listOf().comapFlatMap(
                        points -> {
                            if (points.size() < 2) {
                                return com.mojang.serialization.DataResult.error(
                                        () -> "aoemod:linear_spline requires at least 2 points"
                                );
                            }

                            for (int i = 1; i < points.size(); i++) {
                                if (points.get(i).location() <= points.get(i - 1).location()) {
                                    return com.mojang.serialization.DataResult.error(
                                            () -> "aoemod:linear_spline points must be strictly increasing by location"
                                    );
                                }
                            }

                            return com.mojang.serialization.DataResult.success(points);
                        },
                        points -> points
                );

        private static final MapCodec<LinearSpline> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("coordinate").forGetter(LinearSpline::coordinate),
                        POINTS_CODEC.fieldOf("points").forGetter(LinearSpline::points)
                ).apply(data, LinearSpline::new));

        public static final KeyDispatchDataCodec<LinearSpline> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double x = coordinate.compute(context);

            final Point first = points.get(0);
            if (x <= first.location()) {
                return first.value();
            }

            final Point last = points.get(points.size() - 1);
            if (x >= last.location()) {
                return last.value();
            }

            for (int i = 1; i < points.size(); i++) {
                final Point left = points.get(i - 1);
                final Point right = points.get(i);

                if (x <= right.location()) {
                    final double t = (x - left.location()) / (right.location() - left.location());
                    return left.value() + (right.value() - left.value()) * t;
                }
            }

            return last.value();
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new LinearSpline(
                    coordinate.mapAll(visitor),
                    points
            ));
        }

        @Override
        public double minValue() {
            double min = Double.POSITIVE_INFINITY;

            for (Point point : points) {
                min = Math.min(min, point.value());
            }

            return min;
        }

        @Override
        public double maxValue() {
            double max = Double.NEGATIVE_INFINITY;

            for (Point point : points) {
                max = Math.max(max, point.value());
            }

            return max;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        protected record Point(double location, double value) {

            private static final Codec<Point> CODEC =
                    RecordCodecBuilder.create((data) -> data.group(
                            Codec.DOUBLE.fieldOf("location").forGetter(Point::location),
                            Codec.DOUBLE.fieldOf("value").forGetter(Point::value)
                    ).apply(data, Point::new));
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


    protected record SteppedMountainOffset(
            DensityFunction continents,
            DensityFunction rivers
    ) implements DensityFunction {

        private static final double OCEAN_OFFSET = -0.625D;
        private static final double LOW_LAND_OFFSET = -0.5D;
        private static final double MOUNTAIN_OFFSET = 0.25D;

        private static final int OCEAN_HEIGHT = 48;
        private static final int LOW_LAND_HEIGHT = 64;
        private static final int MOUNTAIN_HEIGHT = 160;

        /*
         * 0.1 continents = 640 blocks,
         * so 1 block = 0.1 / 640 = 0.00015625 continents.
         */
        private static final double ONE_BLOCK_CONTINENTS = 0.00015625D;

        private static final double OCEAN_END = 0.100D;
        private static final double LOW_LAND_START = 0.1025D;

        private static final double MOUNTAIN_RISE_START = 0.400D;
        private static final double MOUNTAIN_PLATEAU_START = 0.415D;
        private static final double MOUNTAIN_PLATEAU_END = 0.485D;
        private static final double MOUNTAIN_FALL_END = 0.500D;

        /*
         * River terrain timing now matches normal terrain timing exactly.
         *
         * The extra one-block lowering is handled separately in applyRivers.
         */
        private static final double RIVER_MOUNTAIN_RISE_START =
                MOUNTAIN_RISE_START + ONE_BLOCK_CONTINENTS;

        private static final double RIVER_MOUNTAIN_PLATEAU_START =
                MOUNTAIN_PLATEAU_START + ONE_BLOCK_CONTINENTS;

        private static final double RIVER_MOUNTAIN_PLATEAU_END =
                MOUNTAIN_PLATEAU_END - ONE_BLOCK_CONTINENTS;

        private static final double RIVER_MOUNTAIN_FALL_END =
                MOUNTAIN_FALL_END - ONE_BLOCK_CONTINENTS;
        /*
         * Mountain river depth is 10 blocks, so full river cut is -10 / 128.
         */
        private static final double FULL_MOUNTAIN_RIVER_CUT = -10.0D / 128.0D;

        /*
         * Allow a tiny tolerance so floating point rounding does not stop the center
         * of the river from being treated as full-depth.
         */
        private static final double FULL_CUT_EPSILON = 0.000001D;

        /*
         * Lowland river depths.
         *
         * Plains rivers are 6 blocks deep.
         * Badlands/desert rivers are 4 blocks deep.
         */
        private static final int PLAINS_RIVER_DEPTH = 6;
        private static final int DESERT_RIVER_DEPTH = 4;

        private static final int PLAINS_LAND_HEIGHT = LOW_LAND_HEIGHT;
        private static final int DESERT_LAND_HEIGHT = LOW_LAND_HEIGHT;

        private static final double ONE_BLOCK_OFFSET = 1.0D / 128.0D;

        /*
         * Only apply the extra one-block lowering where the river cut is at least
         * 1 block deep. This keeps the shallow land/bank blocks beside the river
         * from dropping.
         *
         * If the bank still lowers too much, change this to -2.0D / 128.0D.
         */
        private static final double MIN_EXTRA_DROP_RIVER_CUT = -1.0D / 128.0D;

        private static final double PLAINS_RIVER_FLOOR_OFFSET =
                heightToOffset(PLAINS_LAND_HEIGHT - PLAINS_RIVER_DEPTH);

        private static final double DESERT_RIVER_FLOOR_OFFSET =
                heightToOffset(DESERT_LAND_HEIGHT - DESERT_RIVER_DEPTH);

        private static final double POSITIVE_RIVER_START = MOUNTAIN_PLATEAU_END;
        private static final double POSITIVE_RIVER_END = 1.0D;

        private static final double EPSILON = 0.0000001D;

        private static final MapCodec<SteppedMountainOffset> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC
                                .fieldOf("continents")
                                .forGetter(SteppedMountainOffset::continents),
                        DensityFunction.HOLDER_HELPER_CODEC
                                .fieldOf("rivers")
                                .forGetter(SteppedMountainOffset::rivers)
                ).apply(data, SteppedMountainOffset::new));

        public static final KeyDispatchDataCodec<SteppedMountainOffset> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double c = continents.compute(context);
            final double riverValue = rivers.compute(context);

            final double normalBaseOffset = getBaseOffset(c);
            final double riverBaseOffset = getRiverBaseOffset(c);

            return applyRivers(c, normalBaseOffset, riverBaseOffset, riverValue);
        }

        private static double getBaseOffset(double c) {
            if (c < OCEAN_END) {
                return OCEAN_OFFSET;
            }

            if (c < LOW_LAND_START) {
                int blocksIntoRamp = continentsToWholeBlocks(c - OCEAN_END);

                return heightToOffset(clampInt(
                        OCEAN_HEIGHT + blocksIntoRamp,
                        OCEAN_HEIGHT,
                        LOW_LAND_HEIGHT
                ));
            }

            return getMountainOffset(
                    c,
                    MOUNTAIN_RISE_START,
                    MOUNTAIN_PLATEAU_START,
                    MOUNTAIN_PLATEAU_END,
                    MOUNTAIN_FALL_END
            );
        }

        private static double getRiverBaseOffset(double c) {
            if (c < OCEAN_END) {
                return OCEAN_OFFSET;
            }

            if (c < LOW_LAND_START) {
                int blocksIntoRamp = continentsToWholeBlocks(c - OCEAN_END);

                return heightToOffset(clampInt(
                        OCEAN_HEIGHT + blocksIntoRamp,
                        OCEAN_HEIGHT,
                        LOW_LAND_HEIGHT
                ));
            }

            return getMountainOffset(
                    c,
                    RIVER_MOUNTAIN_RISE_START,
                    RIVER_MOUNTAIN_PLATEAU_START,
                    RIVER_MOUNTAIN_PLATEAU_END,
                    RIVER_MOUNTAIN_FALL_END
            );
        }

        private static double getMountainOffset(
                double c,
                double mountainRiseStart,
                double mountainPlateauStart,
                double mountainPlateauEnd,
                double mountainFallEnd
        ) {
            if (c < mountainRiseStart) {
                return LOW_LAND_OFFSET;
            }

            if (c < mountainPlateauStart) {
                int blocksIntoRamp = continentsToWholeBlocks(c - mountainRiseStart);

                return heightToOffset(clampInt(
                        LOW_LAND_HEIGHT + blocksIntoRamp,
                        LOW_LAND_HEIGHT,
                        MOUNTAIN_HEIGHT
                ));
            }

            if (c < mountainPlateauEnd) {
                return MOUNTAIN_OFFSET;
            }

            if (c < mountainFallEnd) {
                int blocksIntoRamp = continentsToWholeBlocks(c - mountainPlateauEnd);

                return heightToOffset(clampInt(
                        MOUNTAIN_HEIGHT - blocksIntoRamp,
                        LOW_LAND_HEIGHT,
                        MOUNTAIN_HEIGHT
                ));
            }

            return LOW_LAND_OFFSET;
        }

        private static double applyRivers(
                double c,
                double normalBaseOffset,
                double riverBaseOffset,
                double riverValue
        ) {
            final double negativeRiver = Math.min(riverValue, 0.0D);
            final double positiveRiver = Math.max(riverValue, 0.0D);

            /*
             * Positive river values are badlands terrace/lift values.
             * These should not use shifted river terrain.
             */
            double offsetWithPositiveRiver = normalBaseOffset;

            if (positiveRiver > 0.0D && c >= POSITIVE_RIVER_START && c < POSITIVE_RIVER_END) {
                offsetWithPositiveRiver = Math.max(
                        normalBaseOffset,
                        LOW_LAND_OFFSET + positiveRiver
                );
            }

            /*
             * No river cut.
             */
            if (negativeRiver >= 0.0D) {
                return offsetWithPositiveRiver;
            }

            final boolean isMountainRegion =
                    c >= MOUNTAIN_RISE_START
                            && c <= MOUNTAIN_FALL_END;

            final boolean isPlainsSideMountainTransition =
                    c >= MOUNTAIN_RISE_START
                            && c < RIVER_MOUNTAIN_PLATEAU_START;

            final boolean isDesertSideMountainTransition =
                    c >= MOUNTAIN_PLATEAU_END
                            && c < MOUNTAIN_FALL_END;

            /*
             * These are the zones where the extra 1-block lowering is allowed.
             *
             * They are intentionally shifted one block toward the mountain center:
             *
             * - plains side starts at MOUNTAIN_RISE_START + 1 block
             * - desert side starts at MOUNTAIN_PLATEAU_END - 1 block
             */
            final boolean isPlainsSideExtraDropZone =
                    c >= RIVER_MOUNTAIN_RISE_START
                            && c < RIVER_MOUNTAIN_PLATEAU_START;

            final boolean isDesertSideExtraDropZone =
                    c >= RIVER_MOUNTAIN_PLATEAU_END
                            && c < MOUNTAIN_FALL_END;

            /*
             * Only use the river base in the extra-drop zones where the river is at
             * least 1 block deep. This prevents the two shallow land/bank blocks
             * beside the river from dropping.
             *
             * On the mountain plateau outside the edge transitions, only the full
             * 10-block-deep river center uses the river base.
             */
            final boolean shouldUseMountainRiverBase =
                    (isPlainsSideExtraDropZone
                            && negativeRiver <= MIN_EXTRA_DROP_RIVER_CUT + FULL_CUT_EPSILON)
                            || (isDesertSideExtraDropZone
                            && negativeRiver <= MIN_EXTRA_DROP_RIVER_CUT + FULL_CUT_EPSILON)
                            || (!isPlainsSideMountainTransition
                            && !isDesertSideMountainTransition
                            && isMountainRegion
                            && negativeRiver <= FULL_MOUNTAIN_RIVER_CUT + FULL_CUT_EPSILON);

            final double baseForRiverCut = shouldUseMountainRiverBase
                    ? riverBaseOffset
                    : offsetWithPositiveRiver;

            double finalOffset = baseForRiverCut + negativeRiver;

            if (c >= OCEAN_END && c < LOW_LAND_START) {
                finalOffset = Math.max(finalOffset, OCEAN_OFFSET);
            }

            return finalOffset;
        }

        private static int continentsToWholeBlocks(double continentsDistance) {
            return (int) Math.floor((continentsDistance / ONE_BLOCK_CONTINENTS) + EPSILON);
        }

        private static double heightToOffset(int height) {
            return ((double) height / 128.0D) - 1.0D;
        }

        private static int clampInt(int value, int min, int max) {
            return Math.min(Math.max(value, min), max);
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new SteppedMountainOffset(
                    continents.mapAll(visitor),
                    rivers.mapAll(visitor)
            ));
        }

        @Override
        public double minValue() {
            return OCEAN_OFFSET - (10.0D / 128.0D);
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
     * aoemod:radial_rivers
     * Current behavior:
     * - continents controls the north/south biome-height bands
     * - rivers repeat along x
     * The JSON type is still called radial_rivers so existing JSONs do not need
     * a codec rename, but this version intentionally uses linear x-based rivers
     * instead of angular rivers.
     */
    protected record RadialRivers(
            DensityFunction x0,
            DensityFunction z0,
            DensityFunction continents,
            DensityFunction period,
            DensityFunction arcSpacing
    ) implements DensityFunction {

        /*
         * 0.1 continents = 640 blocks, so 2 blocks = 0.1 * 2 / 640.
         *
         * This is used for the jungle/savanna/plains river-depth transitions.
         */
        private static final double PROFILE_RAMP = 0.0003125D;

        private static final double NORMAL_RIVER_START = 0.1D;

        /*
         * Mountain boundary cutoffs:
         *
         * terrain plains -> mountain ramp: 0.400 to 0.415
         * terrain mountain plateau:        0.415 to 0.485
         * terrain mountain -> badlands:    0.485 to 0.500
         */
        private static final double MOUNTAIN_RAMP_START = 0.400D;
        private static final double MOUNTAIN_PLATEAU_START = 0.415D;
        private static final double MOUNTAIN_PLATEAU_END = 0.485D;
        private static final double ONE_BLOCK_CONTINENTS = 0.00015625D;


        /*
         * River mountain profile now matches terrain timing exactly.
         * There is no one-block delay here.
         */
        private static final double RIVER_MOUNTAIN_RAMP_START =
                MOUNTAIN_RAMP_START;

        private static final double RIVER_MOUNTAIN_PLATEAU_START =
                MOUNTAIN_PLATEAU_START - ONE_BLOCK_CONTINENTS;

        private static final double NORMAL_RIVER_END =
                MOUNTAIN_PLATEAU_END;

        private static final double BADLANDS_RIVER_CUT_START =
                MOUNTAIN_PLATEAU_END;

        private static final double BADLANDS_END = 1.0D;

        /*
         * River bank slope.
         *
         * 0.5 means:
         *   2 horizontal blocks inward from the river edge = 1 vertical block deeper
         * until maxDepth is reached.
         */
        private static final double RIVER_BANK_SLOPE = 0.5D;

        private static final double NORMAL_RIVER_RADIUS = 40.0D;

        private static final double BADLANDS_RIVER_RADIUS = 40.0D;
        private static final double BADLANDS_RIVER_DEPTH = 4.0D;

        /*
         * Relative to base badlands height y=64:
         * y=64 = lift 0
         * y=68 = lift 4
         *
         * After the lower-slope area, the terrace keeps rising at slope 1/4.
         */
        private static final double BADLANDS_LOW_LIFT = 0.0D;
        private static final double BADLANDS_MID_LIFT = 4.0D;
        private static final double BADLANDS_TERRACE_SLOPE_AFTER_LOW_AREA = 0.25D;

        private static final MapCodec<RadialRivers> DATA_CODEC =
                RecordCodecBuilder.mapCodec((data) -> data.group(
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("x0").forGetter(RadialRivers::x0),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("z0").forGetter(RadialRivers::z0),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(RadialRivers::continents),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("period").forGetter(RadialRivers::period),
                        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("arc_spacing").forGetter(RadialRivers::arcSpacing)
                ).apply(data, RadialRivers::new));

        public static final KeyDispatchDataCodec<RadialRivers> CODEC =
                AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            final double continent = continents.compute(context);

            final double cx = x0.compute(context);
            final double spacing = Math.max(1.0D, Math.abs(arcSpacing.compute(context)));

            /*
             * Linear river layout:
             * - continents varies by z
             * - rivers repeat by x
             *
             * River centers are shifted by 0.5 so a radius of 40 gives exactly 80
             * integer block columns instead of 81.
             */
            final double x = context.blockX() - cx;
            final double nearestRiverCenterX = Math.round((x - 0.5D) / spacing) * spacing + 0.5D;
            final double distanceFromRiverCenter = Math.abs(x - nearestRiverCenterX);

            if (continent >= BADLANDS_RIVER_CUT_START && continent <= BADLANDS_END) {
                return getBadlandsRiverValue(distanceFromRiverCenter, spacing);
            }

            final RiverProfile profile = getNormalRiverProfile(continent);

            if (profile.maxDepth() <= 0.0D || profile.radius() <= 0.0D) {
                return 0.0D;
            }

            if (distanceFromRiverCenter >= profile.radius()) {
                return 0.0D;
            }

            /*
             * Normal river behavior:
             * - all normal rivers are exactly 80 blocks wide because radius = 40
             * - depth increases from the edge toward the center
             * - slope is controlled by RIVER_BANK_SLOPE
             * - depth stops increasing once it reaches maxDepth
             */
            final double distanceInwardFromEdge = profile.radius() - distanceFromRiverCenter;

            final double depth = Math.max(
                    0.0D,
                    Math.min(
                            profile.maxDepth(),
                            distanceInwardFromEdge * RIVER_BANK_SLOPE
                    )
            );

            return -depth / 128.0D;
        }

        private static double getBadlandsRiverValue(
                double distanceFromRiverCenter,
                double arcSpacing
        ) {
            final double halfSpacing = arcSpacing * 0.5D;
            final double d = Math.min(distanceFromRiverCenter, halfSpacing);

            /*
             * Badlands river:
             * - exactly 80 blocks wide because radius = 40
             * - river descends until it reaches max depth
             */
            if (d < BADLANDS_RIVER_RADIUS) {
                final double distanceInwardFromEdge = BADLANDS_RIVER_RADIUS - d;

                final double depth = Math.max(
                        0.0D,
                        Math.min(
                                BADLANDS_RIVER_DEPTH,
                                distanceInwardFromEdge * RIVER_BANK_SLOPE
                        )
                );

                return -depth / 128.0D;
            }

            /*
             * Badlands terrace shaping outside the river.
             */
            final double distanceFromRiverEdge = d - BADLANDS_RIVER_RADIUS;

            final double firstSlopeWidth = arcSpacing * 0.2D;

            if (distanceFromRiverEdge <= firstSlopeWidth) {
                final double t = distanceFromRiverEdge / firstSlopeWidth;
                return lerp(BADLANDS_LOW_LIFT, BADLANDS_MID_LIFT, t) / 128.0D;
            }

            final double afterFirstSlope = distanceFromRiverEdge - firstSlopeWidth;
            final double lift = BADLANDS_MID_LIFT
                    + afterFirstSlope * BADLANDS_TERRACE_SLOPE_AFTER_LOW_AREA;

            return lift / 128.0D;
        }

        private static RiverProfile getNormalRiverProfile(double continents) {
            if (continents < NORMAL_RIVER_START || continents >= NORMAL_RIVER_END) {
                return RiverProfile.NONE;
            }

            /*
             * Jungle rivers.
             */
            if (continents <= 0.2D) {
                return new RiverProfile(NORMAL_RIVER_RADIUS, 10.0D);
            }

            if (continents < 0.2D + PROFILE_RAMP) {
                final double t = inverseLerp(0.2D, 0.2D + PROFILE_RAMP, continents);

                return RiverProfile.lerp(
                        new RiverProfile(NORMAL_RIVER_RADIUS, 10.0D),
                        new RiverProfile(NORMAL_RIVER_RADIUS, 8.0D),
                        t
                );
            }

            /*
             * Savanna rivers.
             */
            if (continents <= 0.3D) {
                return new RiverProfile(NORMAL_RIVER_RADIUS, 8.0D);
            }

            if (continents < 0.3D + PROFILE_RAMP) {
                final double t = inverseLerp(0.3D, 0.3D + PROFILE_RAMP, continents);

                return RiverProfile.lerp(
                        new RiverProfile(NORMAL_RIVER_RADIUS, 8.0D),
                        new RiverProfile(NORMAL_RIVER_RADIUS, 6.0D),
                        t
                );
            }

            /*
             * Plains rivers.
             *
             * This continues until the terrain mountain ramp starts.
             * There is no one-block delay.
             */
            if (continents < RIVER_MOUNTAIN_RAMP_START) {
                return new RiverProfile(NORMAL_RIVER_RADIUS, 6.0D);
            }

            /*
             * Plains -> mountain river depth transition.
             *
             * This matches the terrain mountain ramp exactly.
             * The river changes from 6 blocks deep to 10 blocks deep over the same
             * continents interval where the terrain changes from plains height to
             * mountain plateau height.
             */
            if (continents < RIVER_MOUNTAIN_PLATEAU_START) {
                final double t = inverseLerp(
                        RIVER_MOUNTAIN_RAMP_START,
                        RIVER_MOUNTAIN_PLATEAU_START,
                        continents
                );

                return RiverProfile.lerp(
                        new RiverProfile(NORMAL_RIVER_RADIUS, 6.0D),
                        new RiverProfile(NORMAL_RIVER_RADIUS, 10.0D),
                        t
                );
            }

            /*
             * Mountain plateau rivers.
             *
             * These continue until the exact end of the mountain plateau.
             * Badlands river/terrace logic takes over at MOUNTAIN_PLATEAU_END.
             */
            return new RiverProfile(NORMAL_RIVER_RADIUS, 10.0D);
        }

        private static double lerp(double start, double end, double t) {
            return start + (end - start) * t;
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
                    x0.mapAll(visitor),
                    z0.mapAll(visitor),
                    continents.mapAll(visitor),
                    period.mapAll(visitor),
                    arcSpacing.mapAll(visitor)
            ));
        }

        @Override
        public double minValue() {
            return -10.0D / 128.0D;
        }

        @Override
        public double maxValue() {
            /*
             * The badlands terrace can continue rising at slope 1/4 instead of
             * targeting a fixed maximum height, so use a safe upper bound.
             */
            return 1.0D;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        private record RiverProfile(double radius, double maxDepth) {
            private static final RiverProfile NONE = new RiverProfile(0.0D, 0.0D);

            private static RiverProfile lerp(RiverProfile start, RiverProfile end, double t) {
                return new RiverProfile(
                        lerpDouble(start.radius(), end.radius(), t),
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
