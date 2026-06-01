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

import java.util.List;


public final class AOEDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AOEMod.MOD_ID);

    public static final RegistryObject<Codec<? extends DensityFunction>> MODULUS_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("modulus", Modulus.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> DIVIDE_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("divide", Divide.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> COORDINATE_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("coordinate", Coordinate.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> LINEAR_SPLINE_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("linear_spline", LinearSpline.CODEC::codec);

    public static final RegistryObject<Codec<? extends DensityFunction>> RADIAL_RIVERS_DENSITY_FUNCTION_TYPE =
            DENSITY_FUNCTION_TYPES.register("radial_rivers", RadialRivers.CODEC::codec);

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }

    public static final Long2DoubleOpenHashMap heightmap = new Long2DoubleOpenHashMap();
    public static DensityFunction temperature;

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


    /**
     * aoemod:radial_rivers
     * Current test behavior:
     * - continents controls the north/south biome-height bands
     * - rivers repeat along x
     * The JSON type is still called radial_rivers so existing JSONs do not need a codec rename,
     * but this version intentionally uses linear x-based rivers instead of angular rivers.
     */
    protected record RadialRivers(
            DensityFunction x0,
            DensityFunction z0,
            DensityFunction continents,
            DensityFunction period,
            DensityFunction arcSpacing
    ) implements DensityFunction {

        // 0.1 continents = 640 blocks, so 2 blocks = 0.1 * 2 / 640.
        private static final double PROFILE_RAMP = 0.0003125D;

        private static final double NORMAL_RIVER_START = 0.1D;
        private static final double NORMAL_RIVER_END = 0.5D;

        private static final double BADLANDS_RIVER_CUT_START = 0.5D;
        private static final double BADLANDS_END = 1.0D;

        private static final double RIVER_BANK_SLOPE = 0.5D;

        private static final double BADLANDS_RIVER_RADIUS = 40.0D;
        private static final double BADLANDS_RIVER_DEPTH = 4.0D;

        /*
         * Relative to base badlands height y=64:
         * y=64 = lift 0
         * y=68 = lift 4
         *
         * After the lower-slope area, the terrace keeps rising at slope 1/4
         * instead of trying to reach a fixed target height.
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
             * - slope is 1 vertical block per 2 horizontal blocks
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
             * - no immediate 1-block drop at the shoreline
             * - river descends at slope 1/2 until it reaches max depth
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
             * Badlands terrace shaping outside the river:
             * first lower-slope area, then a constant 1/4 slope.
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
             * All normal rivers are radius 40, so full width is exactly 80
             * because the river center is shifted by 0.5 in compute(...).
             */
            if (continents <= 0.2D) {
                return new RiverProfile(40.0D, 10.0D);
            }

            if (continents < 0.2D + PROFILE_RAMP) {
                final double t = inverseLerp(0.2D, 0.2D + PROFILE_RAMP, continents);
                return RiverProfile.lerp(
                        new RiverProfile(40.0D, 10.0D),
                        new RiverProfile(40.0D, 8.0D),
                        t
                );
            }

            if (continents <= 0.3D) {
                return new RiverProfile(40.0D, 8.0D);
            }

            if (continents < 0.3D + PROFILE_RAMP) {
                final double t = inverseLerp(0.3D, 0.3D + PROFILE_RAMP, continents);
                return RiverProfile.lerp(
                        new RiverProfile(40.0D, 8.0D),
                        new RiverProfile(40.0D, 6.0D),
                        t
                );
            }

            if (continents <= 0.4D) {
                return new RiverProfile(40.0D, 6.0D);
            }

            if (continents < 0.4D + PROFILE_RAMP) {
                final double t = inverseLerp(0.4D, 0.4D + PROFILE_RAMP, continents);
                return RiverProfile.lerp(
                        new RiverProfile(40.0D, 6.0D),
                        new RiverProfile(40.0D, 10.0D),
                        t
                );
            }

            return new RiverProfile(40.0D, 10.0D);
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
             * The badlands terrace can now continue rising at slope 1/4 instead of targeting
             * a fixed maximum height, so use a safe conservative upper bound.
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
                        start.radius() + (end.radius() - start.radius()) * t,
                        start.maxDepth() + (end.maxDepth() - start.maxDepth()) * t
                );
            }
        }
    }

    static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> codec) {
        return KeyDispatchDataCodec.of(codec);
    }
}
