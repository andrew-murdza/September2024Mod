package net.amurdza.examplemod.worldgen.density_function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.List;
import java.util.Optional;

/**
 * Sealed interface for per-continentalness-range terrain descriptors.
 *
 * Each implementation computes a surface height (the y of the first AIR block)
 * given a FunctionContext and the sampled continentalness value.
 *
 * Three subtypes:
 *   SteppedRange – standard quantised noise terrain (most biomes)
 *   RampRange    – linear transition between two y-values (beach, mountain ramps)
 *   MesaRange    – basin + plateau logic (badlands)
 *
 * Replace "yourmod" with your actual mod ID throughout.
 */
public sealed interface TerrainRange
        permits TerrainRange.SteppedRange, TerrainRange.RampRange, TerrainRange.MesaRange {

    /**
     * Dispatch codec: JSON objects must contain a "range_type" string field
     * set to "stepped", "ramp", or "mesa".
     */
    Codec<TerrainRange> CODEC = Codec.STRING.dispatch(
            "range_type",
            TerrainRange::rangeType,
            s -> switch (s) {
                case "stepped" -> SteppedRange.MAP_CODEC.codec();
                case "ramp"    -> RampRange.MAP_CODEC.codec();
                case "mesa"    -> MesaRange.MAP_CODEC.codec();
                default        -> throw new IllegalArgumentException("Unknown range_type: " + s);
            }
    );

    // -------------------------------------------------------------------------
    // Interface contract
    // -------------------------------------------------------------------------

    /** The type discriminator string used in JSON. */
    String rangeType();

    double minContinentalness();
    double maxContinentalness();

    /**
     * Returns the y-coordinate of the first AIR block at this position.
     * The top solid block is therefore (result - 1).
     *
     * @param ctx           density function evaluation context
     * @param continentalness already-sampled continentalness value for this column
     */
    double computeSurface(DensityFunction.FunctionContext ctx, double continentalness);

    /** Called during DensityFunction.mapAll() to propagate the visitor to child functions. */
    TerrainRange mapAll(DensityFunction.Visitor visitor);

    // =========================================================================
    // STEPPED RANGE
    // =========================================================================

    /**
     * Standard biome terrain: noise remapped to [minY, maxY] then floor-quantised
     * to produce discrete stepped terrain.
     *
     * Land vs water is chosen by comparing continentalness to landWaterSplit.
     * Values >= landWaterSplit use landTerrain; values < landWaterSplit use waterTerrain.
     *
     * The two noise functions should differ only in xz_scale – that is the sole
     * control for "how far apart height transitions occur" on land vs underwater.
     *
     * Water surface profile
     * ---------------------
     * If waterStepProfile is non-empty it defines the horizontal band width (in noise
     * units) for each successive y-level, starting at (waterThreshold - 1) and stepping
     * downward.  E.g., [2, 2, 2, 2, 3, 3, 4] places:
     *   y = waterThreshold-1  … 2 units wide
     *   y = waterThreshold-2  … 2 units wide
     *   ...
     *   y = waterThreshold-7  … 4 units wide
     *   y = minY              … flat bottom (catches remaining noise)
     *
     * If waterStepProfile is empty a simple uniform stepped remap is used instead
     * (like warm ocean: min step width controlled purely by xz_scale).
     */
    record SteppedRange(
            double minContinentalness,
            double maxContinentalness,
            int minY,
            int maxY,
            Optional<Integer> waterThreshold,   // absent = no water in this zone
            double landWaterSplit,               // continentalness dividing land/water noise selection
            DensityFunction landTerrain,
            DensityFunction waterTerrain,
            List<Integer> waterStepProfile,      // per-level band widths; empty = uniform stepped
            int riverMinWidth                    // informational; -1 = N/A
    ) implements TerrainRange {

        public static final MapCodec<SteppedRange> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("min_continentalness").forGetter(SteppedRange::minContinentalness),
                Codec.DOUBLE.fieldOf("max_continentalness").forGetter(SteppedRange::maxContinentalness),
                Codec.INT.fieldOf("min_y").forGetter(SteppedRange::minY),
                Codec.INT.fieldOf("max_y").forGetter(SteppedRange::maxY),
                Codec.INT.optionalFieldOf("water_threshold").forGetter(SteppedRange::waterThreshold),
                Codec.DOUBLE.optionalFieldOf("land_water_split", 0.0).forGetter(SteppedRange::landWaterSplit),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("land_terrain").forGetter(SteppedRange::landTerrain),
                DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("water_terrain", DensityFunctions.zero())
                        .forGetter(SteppedRange::waterTerrain),
                Codec.INT.listOf().optionalFieldOf("water_step_profile", List.of())
                        .forGetter(SteppedRange::waterStepProfile),
                Codec.INT.optionalFieldOf("river_min_width", -1).forGetter(SteppedRange::riverMinWidth)
        ).apply(i, SteppedRange::new));

        @Override public String rangeType() { return "stepped"; }

        @Override
        public double computeSurface(DensityFunction.FunctionContext ctx, double continentalness) {
            boolean hasWater = waterThreshold.isPresent();
            if (hasWater && continentalness < landWaterSplit) {
                return computeWater(ctx);
            }
            return computeLand(ctx);
        }

        private double computeLand(DensityFunction.FunctionContext ctx) {
            double t = clamp01((landTerrain.compute(ctx) + 1.0) / 2.0);
            // Floor-quantise: each integer y-level is one "step".
            // The step width is determined by the xz_scale of landTerrain's noise.
            double surface = Math.floor(minY + t * (maxY - minY));
            return clampD(surface, minY, maxY) + 1.0; // +1: surface = first air block
        }

        private double computeWater(DensityFunction.FunctionContext ctx) {
            double t = clamp01((waterTerrain.compute(ctx) + 1.0) / 2.0);
            int wt = waterThreshold.get();

            if (waterStepProfile.isEmpty()) {
                // Uniform stepped water (warm ocean style).
                // Step width controlled by waterTerrain xz_scale.
                double surface = Math.floor(minY + t * ((wt - 1) - minY));
                return clampD(surface, minY, wt - 1) + 1.0;
            }

            return profiledSurface(t, wt) + 1.0;
        }

        /**
         * Maps noise [0,1] through the step profile to a surface y.
         * Profile entries cover (waterThreshold-1) downward; anything beyond
         * the last entry falls to minY.
         */
        private double profiledSurface(double t, int wt) {
            int total = waterStepProfile.stream().mapToInt(Integer::intValue).sum();
            double target = t * total;
            int cumulative = 0;
            for (int i = 0; i < waterStepProfile.size(); i++) {
                cumulative += waterStepProfile.get(i);
                if (target <= cumulative) {
                    return Math.max(minY, (wt - 1) - i);
                }
            }
            return minY; // flat bottom
        }

        @Override
        public TerrainRange mapAll(DensityFunction.Visitor v) {
            return new SteppedRange(
                    minContinentalness, maxContinentalness, minY, maxY,
                    waterThreshold, landWaterSplit,
                    landTerrain.mapAll(v), waterTerrain.mapAll(v),
                    waterStepProfile, riverMinWidth
            );
        }
    }

    // =========================================================================
    // RAMP RANGE
    // =========================================================================

    /**
     * Transition zone: surface height is driven primarily by the continentalness
     * position within the range (linear interpolation from startY to endY), then
     * floor-quantised to stepWidth-block increments.
     *
     * An optional low-amplitude noise can be added to break up perfectly uniform
     * steps; pass DensityFunctions.zero() (or omit "noise" in JSON) for a clean ramp.
     *
     * Usage:
     *   Beach ramp  – startY = jungle level (~63), endY = ocean level (~44), stepWidth = 2
     *   Plains→Grove – startY = plains level (~66), endY = grove level (~169), stepWidth = 1
     *                  (stepWidth 1 gives per-block steps ≈ "2-3 blocks height gain per block"
     *                   when the ramp is steep enough)
     */
    record RampRange(
            double minContinentalness,
            double maxContinentalness,
            int startY,       // surface y at the minContinentalness boundary
            int endY,         // surface y at the maxContinentalness boundary
            int stepWidth,    // quantisation step in blocks; 1 = per-block steps
            DensityFunction noise  // small wobble noise; use zero() for pure ramp
    ) implements TerrainRange {

        public static final MapCodec<RampRange> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("min_continentalness").forGetter(RampRange::minContinentalness),
                Codec.DOUBLE.fieldOf("max_continentalness").forGetter(RampRange::maxContinentalness),
                Codec.INT.fieldOf("start_y").forGetter(RampRange::startY),
                Codec.INT.fieldOf("end_y").forGetter(RampRange::endY),
                Codec.INT.optionalFieldOf("step_width", 1).forGetter(RampRange::stepWidth),
                DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("noise", DensityFunctions.zero())
                        .forGetter(RampRange::noise)
        ).apply(i, RampRange::new));

        @Override public String rangeType() { return "ramp"; }

        @Override
        public double computeSurface(DensityFunction.FunctionContext ctx, double continentalness) {
            double t = clamp01(
                    (continentalness - minContinentalness) / (maxContinentalness - minContinentalness)
            );
            double raw = startY + t * (endY - startY);

            // Add noise wobble capped to ±(stepWidth/2) so it cannot skip a step
            raw += noise.compute(ctx) * (stepWidth * 0.5);

            // Quantise and clamp
            double lo = Math.min(startY, endY);
            double hi = Math.max(startY, endY);
            double quantised = Math.floor(raw / stepWidth) * stepWidth;
            return clampD(quantised, lo, hi) + 1.0;
        }

        @Override
        public TerrainRange mapAll(DensityFunction.Visitor v) {
            return new RampRange(
                    minContinentalness, maxContinentalness, startY, endY, stepWidth, noise.mapAll(v)
            );
        }
    }

    // =========================================================================
    // MESA RANGE  (Badlands)
    // =========================================================================

    /**
     * Two-mode terrain for badlands:
     *
     *   Basin mode  – mesaTerrain noise <= 0: surface drawn from basinTerrain,
     *                 remapped to [minY, basinMaxY].  Flat basin floors emerge
     *                 naturally from the low xz_scale of basinTerrain.
     *
     *   Mesa mode   – mesaTerrain noise > 0: surface drawn from mesaTerrain,
     *                 remapped to [mesaMinY, maxY] with a power-curve flattening
     *                 near maxY to produce the characteristic flat mesa tops.
     *
     * Water works identically to SteppedRange (profile or uniform).
     */
    record MesaRange(
            double minContinentalness,
            double maxContinentalness,
            int minY,               // absolute floor (deepest water point)
            int maxY,               // mesa top
            Optional<Integer> waterThreshold,
            double landWaterSplit,
            int basinMaxY,          // basin surface stays at or below this y
            int mesaMinY,           // mesa surface starts at or above this y
            DensityFunction basinTerrain,
            DensityFunction mesaTerrain,
            DensityFunction waterTerrain,
            List<Integer> waterStepProfile,
            int riverMinWidth
    ) implements TerrainRange {

        public static final MapCodec<MesaRange> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("min_continentalness").forGetter(MesaRange::minContinentalness),
                Codec.DOUBLE.fieldOf("max_continentalness").forGetter(MesaRange::maxContinentalness),
                Codec.INT.fieldOf("min_y").forGetter(MesaRange::minY),
                Codec.INT.fieldOf("max_y").forGetter(MesaRange::maxY),
                Codec.INT.optionalFieldOf("water_threshold").forGetter(MesaRange::waterThreshold),
                Codec.DOUBLE.optionalFieldOf("land_water_split", 0.0).forGetter(MesaRange::landWaterSplit),
                Codec.INT.fieldOf("basin_max_y").forGetter(MesaRange::basinMaxY),
                Codec.INT.fieldOf("mesa_min_y").forGetter(MesaRange::mesaMinY),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("basin_terrain").forGetter(MesaRange::basinTerrain),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("mesa_terrain").forGetter(MesaRange::mesaTerrain),
                DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("water_terrain", DensityFunctions.zero())
                        .forGetter(MesaRange::waterTerrain),
                Codec.INT.listOf().optionalFieldOf("water_step_profile", List.of())
                        .forGetter(MesaRange::waterStepProfile),
                Codec.INT.optionalFieldOf("river_min_width", -1).forGetter(MesaRange::riverMinWidth)
        ).apply(i, MesaRange::new));

        @Override public String rangeType() { return "mesa"; }

        @Override
        public double computeSurface(DensityFunction.FunctionContext ctx, double continentalness) {
            if (waterThreshold.isPresent() && continentalness < landWaterSplit) {
                return computeWater(ctx);
            }
            return computeLand(ctx);
        }

        private double computeLand(DensityFunction.FunctionContext ctx) {
            double mesaRaw = mesaTerrain.compute(ctx); // [-1, 1]

            if (mesaRaw > 0.0) {
                // Mesa mode: remap (0,1] to [mesaMinY, maxY] with flattened top.
                // Power curve < 1 squishes values toward maxY, broadening the plateau.
                double t = clamp01(mesaRaw); // (0, 1]
                double flatT = 1.0 - Math.pow(1.0 - t, 2.0); // flattens near top
                double surface = mesaMinY + flatT * (maxY - mesaMinY);
                return Math.floor(surface) + 1.0;
            } else {
                // Basin mode: remap [-1, 0] to [minY, basinMaxY].
                double basinRaw = basinTerrain.compute(ctx);
                double t = clamp01((basinRaw + 1.0) / 2.0);
                double surface = minY + t * (basinMaxY - minY);
                return Math.floor(surface) + 1.0;
            }
        }

        private double computeWater(DensityFunction.FunctionContext ctx) {
            double t = clamp01((waterTerrain.compute(ctx) + 1.0) / 2.0);
            int wt = waterThreshold.get();

            if (waterStepProfile.isEmpty()) {
                double surface = Math.floor(minY + t * ((wt - 1) - minY));
                return clampD(surface, minY, wt - 1) + 1.0;
            }

            int total = waterStepProfile.stream().mapToInt(Integer::intValue).sum();
            double target = t * total;
            int cumulative = 0;
            for (int i = 0; i < waterStepProfile.size(); i++) {
                cumulative += waterStepProfile.get(i);
                if (target <= cumulative) {
                    return Math.max(minY, (wt - 1) - i) + 1.0;
                }
            }
            return minY + 1.0;
        }

        @Override
        public TerrainRange mapAll(DensityFunction.Visitor v) {
            return new MesaRange(
                    minContinentalness, maxContinentalness, minY, maxY,
                    waterThreshold, landWaterSplit, basinMaxY, mesaMinY,
                    basinTerrain.mapAll(v), mesaTerrain.mapAll(v), waterTerrain.mapAll(v),
                    waterStepProfile, riverMinWidth
            );
        }
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    static double clampD(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
