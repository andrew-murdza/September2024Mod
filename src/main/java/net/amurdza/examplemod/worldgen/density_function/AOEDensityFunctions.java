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

        private static final int OCEAN_HEIGHT = 48;
        private static final int LOW_LAND_HEIGHT = 64;

        /*
         * 0.1 continents = 640 blocks,
         * so 1 block = 0.1 / 640 = 0.00015625 continents.
         */
        private static final double ONE_BLOCK_CONTINENTS = 0.00015625D;

        private static final double OCEAN_END = 0.100D;
        private static final double LOW_LAND_START = 0.1025D;

        /*
         * Badlands/desert starts after the grove/old mountain band.
         *
         * Before this, all land is functionally lowland at y=64.
         */
        private static final double BADLANDS_START = 0.500D;
        private static final double BADLANDS_END = 1.0D;

        /*
         * First 40 blocks of desert stay at grove/lowland height.
         * After that, the allowed badlands lift increases at slope 1/2 until it
         * reaches the actual x-based terrace value from RadialRivers.
         */
        private static final int DESERT_FLAT_ENTRY_BLOCKS = 40;
        private static final double DESERT_ENTRY_SLOPE = 0.5D;

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

            final double baseOffset = getBaseOffset(c);

            return applyRivers(c, baseOffset, riverValue);
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

            /*
             * Jungle, savanna, plains, grove/old mountains, and the start of the
             * badlands all use the same base height.
             *
             * Badlands terraces are added later through positive river values.
             */
            return LOW_LAND_OFFSET;
        }

        private static double applyRivers(
                double c,
                double baseOffset,
                double riverValue
        ) {
            final double negativeRiver = Math.min(riverValue, 0.0D);
            final double positiveRiver = Math.max(riverValue, 0.0D);

            double offset = baseOffset;

            /*
             * Positive river values are badlands terrace/lift values.
             *
             * The x-based terrace value is capped near the desert entrance so the
             * first 40 blocks of desert stay at grove height, then the allowed lift
             * rises at slope 1/2 until it catches up to the actual terrace terrain.
             */
            if (positiveRiver > 0.0D && c >= BADLANDS_START && c < BADLANDS_END) {
                final double allowedEntryLift = getDesertEntryLiftLimit(c);
                final double cappedPositiveRiver = Math.min(positiveRiver, allowedEntryLift);

                offset = Math.max(
                        baseOffset,
                        LOW_LAND_OFFSET + cappedPositiveRiver
                );
            }

            if (negativeRiver >= 0.0D) {
                return offset;
            }

            double finalOffset = offset + negativeRiver;

            /*
             * Do not let rivers cut the ocean ramp below the ocean floor.
             */
            if (c >= OCEAN_END && c < LOW_LAND_START) {
                finalOffset = Math.max(finalOffset, OCEAN_OFFSET);
            }

            return finalOffset;
        }

        private static double getDesertEntryLiftLimit(double c) {
            final double flatEnd = BADLANDS_START
                    + DESERT_FLAT_ENTRY_BLOCKS * ONE_BLOCK_CONTINENTS;

            if (c <= flatEnd) {
                return 0.0D;
            }

            final int blocksPastFlat = continentsToWholeBlocks(c - flatEnd);
            return (blocksPastFlat * DESERT_ENTRY_SLOPE) / 128.0D;
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
     *
     * Current behavior:
     * - continents controls the north/south biome bands
     * - rivers repeat along x
     *
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
         * 0.1 continents = 640 blocks,
         * so 1 block = 0.1 / 640 = 0.00015625 continents.
         */
        private static final double ONE_BLOCK_CONTINENTS = 0.00015625D;

        private static final double NORMAL_RIVER_START = 0.100D;

        /*
         * Old biome-band boundaries:
         *
         * 0.100 - 0.200 : jungle
         * 0.200 - 0.300 : savanna
         * 0.300 - 0.400 : plains
         * 0.400 - 0.500 : grove/old mountains, now lowland height
         * 0.500 - 1.000 : badlands/desert
         */
        private static final double JUNGLE_END = 0.200D;
        private static final double SAVANNA_END = 0.300D;
        private static final double MOUNTAIN_START = 0.400D;
        private static final double BADLANDS_START = 0.500D;
        private static final double BADLANDS_END = 1.0D;

        /*
         * River profile ramp widths.
         */
        private static final double SMALL_PROFILE_RAMP =
                4.0D * ONE_BLOCK_CONTINENTS;

        private static final double MOUNTAIN_PROFILE_RAMP =
                8.0D * ONE_BLOCK_CONTINENTS;

        private static final double MOUNTAIN_TO_BADLANDS_PROFILE_RAMP =
                12.0D * ONE_BLOCK_CONTINENTS;
        /*
         * Ramp shifts.
         *
         * Jungle -> savanna and savanna -> plains ramps shift 5 blocks toward
         * negative continents.
         *
         * Plains -> grove ramp shifts 1 block toward positive continents.
         */
        private static final double LOWLAND_RAMP_SHIFT =
                5.0D * ONE_BLOCK_CONTINENTS;

        private static final double MOUNTAIN_RAMP_SHIFT =
                1.0D * ONE_BLOCK_CONTINENTS;

        private static final double JUNGLE_TO_SAVANNA_RAMP_END =
                JUNGLE_END + LOWLAND_RAMP_SHIFT;

        private static final double JUNGLE_TO_SAVANNA_RAMP_START =
                JUNGLE_TO_SAVANNA_RAMP_END - SMALL_PROFILE_RAMP;

        private static final double SAVANNA_TO_PLAINS_RAMP_END =
                SAVANNA_END + LOWLAND_RAMP_SHIFT;

        private static final double SAVANNA_TO_PLAINS_RAMP_START =
                SAVANNA_TO_PLAINS_RAMP_END - SMALL_PROFILE_RAMP;

        private static final double PLAINS_TO_MOUNTAIN_RAMP_START =
                MOUNTAIN_START + MOUNTAIN_RAMP_SHIFT;

        private static final double PLAINS_TO_MOUNTAIN_RAMP_END =
                PLAINS_TO_MOUNTAIN_RAMP_START + MOUNTAIN_PROFILE_RAMP;

        private static final double MOUNTAIN_TO_BADLANDS_RAMP_START =
                BADLANDS_START + ONE_BLOCK_CONTINENTS - MOUNTAIN_TO_BADLANDS_PROFILE_RAMP;

        private static final double MOUNTAIN_TO_BADLANDS_RAMP_END =
                BADLANDS_START;

        /*
         * River bank slope.
         *
         * 0.5 means:
         *   2 horizontal blocks inward from the river edge = 1 vertical block deeper
         * until maxDepth is reached.
         */
        private static final double RIVER_BANK_SLOPE = 0.5D;

        private static final double NORMAL_RIVER_RADIUS = 30.0D;

        private static final RiverProfile JUNGLE_RIVER =
                new RiverProfile(NORMAL_RIVER_RADIUS, 10.0D);

        private static final RiverProfile SAVANNA_RIVER =
                new RiverProfile(NORMAL_RIVER_RADIUS, 8.0D);

        private static final RiverProfile PLAINS_RIVER =
                new RiverProfile(NORMAL_RIVER_RADIUS, 6.0D);

        private static final RiverProfile BADLANDS_EDGE_RIVER =
                new RiverProfile(NORMAL_RIVER_RADIUS, 4.0D);

        private static final double BADLANDS_RIVER_RADIUS = 30.0D;
        private static final double BADLANDS_RIVER_DEPTH = 4.0D;

        /*
         * Desert terrace rules:
         *
         * - river itself is still radius 40
         * - then 40 blocks stay flat on each side of the river
         * - after that, terrain rises at slope 1/2 until halfway between river centers
         */
        private static final double BADLANDS_FLAT_AFTER_RIVER = 40.0D;
        private static final double BADLANDS_TERRACE_SLOPE = 0.5D;

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
            final double nearestRiverCenterX =
                    Math.round((x - 0.5D) / spacing) * spacing + 0.5D;
            final double distanceFromRiverCenter = Math.abs(x - nearestRiverCenterX);

            if (continent >= BADLANDS_START && continent <= BADLANDS_END) {
                return getBadlandsRiverValue(distanceFromRiverCenter, spacing);
            }

            final RiverProfile profile = getNormalRiverProfile(continent);

            if (profile.maxDepth() <= 0.0D || profile.radius() <= 0.0D) {
                return 0.0D;
            }

            return getRiverCut(
                    distanceFromRiverCenter,
                    profile.radius(),
                    profile.maxDepth()
            );
        }

        private static RiverProfile getNormalRiverProfile(double continents) {
            if (continents < NORMAL_RIVER_START || continents >= BADLANDS_START) {
                return RiverProfile.NONE;
            }

            /*
             * Jungle -> savanna transition.
             *
             * The whole ramp is shifted 5 blocks toward negative continents.
             */
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

            /*
             * Savanna -> plains transition.
             *
             * The whole ramp is shifted 5 blocks toward negative continents.
             */
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

            /*
             * Plains.
             */
            if (continents < PLAINS_TO_MOUNTAIN_RAMP_START) {
                return PLAINS_RIVER;
            }

            /*
             * Plains -> grove/old mountains.
             *
             * This ramp is shifted 1 block toward positive continents.
             */
            if (continents < PLAINS_TO_MOUNTAIN_RAMP_END) {
                final double t = inverseLerp(
                        PLAINS_TO_MOUNTAIN_RAMP_START,
                        PLAINS_TO_MOUNTAIN_RAMP_END,
                        continents
                );

                return RiverProfile.lerp(
                        PLAINS_RIVER,
                        JUNGLE_RIVER,
                        t
                );
            }

            /*
             * Grove/old mountains -> badlands.
             *
             * This spans the 10 blocks immediately before the start of the badlands
             * biome.
             */
            if (continents >= MOUNTAIN_TO_BADLANDS_RAMP_START) {
                final double t = inverseLerp(
                        MOUNTAIN_TO_BADLANDS_RAMP_START,
                        MOUNTAIN_TO_BADLANDS_RAMP_END,
                        continents
                );

                return RiverProfile.lerp(
                        JUNGLE_RIVER,
                        BADLANDS_EDGE_RIVER,
                        t
                );
            }

            return JUNGLE_RIVER;
        }

        private static double getBadlandsRiverValue(
                double distanceFromRiverCenter,
                double arcSpacing
        ) {
            final double halfSpacing = arcSpacing * 0.5D;
            final double d = Math.min(distanceFromRiverCenter, halfSpacing);

            /*
             * Badlands river cut.
             */
            if (d < BADLANDS_RIVER_RADIUS) {
                return getRiverCut(
                        d,
                        BADLANDS_RIVER_RADIUS,
                        BADLANDS_RIVER_DEPTH
                );
            }

            /*
             * Badlands terrain:
             *
             * - 40 blocks flat on either side of the river
             * - then rises at slope 1/2 until halfway between river centers
             */
            final double distanceFromRiverEdge = d - BADLANDS_RIVER_RADIUS;

            if (distanceFromRiverEdge < BADLANDS_FLAT_AFTER_RIVER) {
                return 0.0D;
            }

            final double terraceDistance = distanceFromRiverEdge - BADLANDS_FLAT_AFTER_RIVER;
            final double lift = terraceDistance * BADLANDS_TERRACE_SLOPE;

            return lift / 128.0D;
        }

        private static double getRiverCut(
                double distanceFromRiverCenter,
                double radius,
                double maxDepth
        ) {
            if (distanceFromRiverCenter >= radius) {
                return 0.0D;
            }

            final double distanceInwardFromEdge = radius - distanceFromRiverCenter;

            final double depth = Math.max(
                    0.0D,
                    Math.min(
                            maxDepth,
                            distanceInwardFromEdge * RIVER_BANK_SLOPE
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
             * The badlands terrace can continue rising until halfway between river
             * centers, so use a safe upper bound.
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
