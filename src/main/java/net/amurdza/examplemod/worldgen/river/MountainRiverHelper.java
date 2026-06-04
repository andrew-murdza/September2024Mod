package net.amurdza.examplemod.worldgen.river;

public final class MountainRiverHelper {
    public static final int LOW_RIVER_WATER_LEVEL = 64;
    public static final int MOUNTAIN_RIVER_WATER_LEVEL = 160;


    /*
     * 0.1 continents = 640 blocks
     * so 1 block = 0.1 / 640 = 0.00015625 continents.
     */
    private static final double ONE_BLOCK_CONTINENTS = 0.00015625D;

    /*
     * Terrain cutoffs from offset.json / SteppedMountainOffset:
     *
     * plains -> mountain ramp: 0.400 to 0.415
     * mountain plateau:        0.415 to 0.485
     * mountain -> badlands:    0.485 to 0.500
     */
    private static final double TERRAIN_MOUNTAIN_RISE_START_CONTINENTS = 0.400D;
    private static final double TERRAIN_MOUNTAIN_PLATEAU_START_CONTINENTS = 0.415D;
    private static final double TERRAIN_MOUNTAIN_PLATEAU_END_CONTINENTS = 0.485D;
    private static final double TERRAIN_MOUNTAIN_FALL_END_CONTINENTS = 0.500D;

    /*
     * River mountain edges.
     *
     * These now match the terrain cutoffs exactly.
     *
     * The river floor is handled by SteppedMountainOffset.
     * The water level is lowered by 1 block during the transition areas so the
     * river keeps the same effective depth while the drop starts earlier.
     */
    public static final double RIVER_MOUNTAIN_RISE_START_CONTINENTS =
            TERRAIN_MOUNTAIN_RISE_START_CONTINENTS + ONE_BLOCK_CONTINENTS;

    public static final double RIVER_MOUNTAIN_PLATEAU_START_CONTINENTS =
            TERRAIN_MOUNTAIN_PLATEAU_START_CONTINENTS + ONE_BLOCK_CONTINENTS;

    public static final double RIVER_MOUNTAIN_FALL_START_CONTINENTS =
            TERRAIN_MOUNTAIN_PLATEAU_END_CONTINENTS - ONE_BLOCK_CONTINENTS;

    public static final double RIVER_MOUNTAIN_FALL_END_CONTINENTS =
            TERRAIN_MOUNTAIN_FALL_END_CONTINENTS - ONE_BLOCK_CONTINENTS;

    private MountainRiverHelper() {
    }

    public static boolean isMountainRiverContinentsValue(double continents) {
        return continents >= TERRAIN_MOUNTAIN_RISE_START_CONTINENTS
                && continents <= TERRAIN_MOUNTAIN_FALL_END_CONTINENTS;
    }

    public static int getMountainRiverWaterLevel(double continents) {
        if (continents < RIVER_MOUNTAIN_RISE_START_CONTINENTS) {
            return LOW_RIVER_WATER_LEVEL;
        }

        /*
         * Plains -> mountain rise.
         *
         * This now starts exactly when the surrounding terrain rise starts.
         */
        if (continents < RIVER_MOUNTAIN_PLATEAU_START_CONTINENTS) {
            double t = inverseLerp(
                    RIVER_MOUNTAIN_RISE_START_CONTINENTS,
                    RIVER_MOUNTAIN_PLATEAU_START_CONTINENTS,
                    continents
            );

            return roundedLerp(
                    LOW_RIVER_WATER_LEVEL,
                    MOUNTAIN_RIVER_WATER_LEVEL,
                    t
            );
        }

        /*
         * Mountain plateau.
         *
         * This now starts exactly when the terrain plateau starts
         * and ends exactly when the terrain plateau ends.
         */
        if (continents < RIVER_MOUNTAIN_FALL_START_CONTINENTS) {
            return MOUNTAIN_RIVER_WATER_LEVEL;
        }

        /*
         * Mountain -> badlands fall.
         *
         * This now starts exactly when the surrounding terrain fall starts.
         */
        if (continents < RIVER_MOUNTAIN_FALL_END_CONTINENTS) {
            double t = inverseLerp(
                    RIVER_MOUNTAIN_FALL_START_CONTINENTS,
                    RIVER_MOUNTAIN_FALL_END_CONTINENTS,
                    continents
            );

            return roundedLerp(
                    MOUNTAIN_RIVER_WATER_LEVEL,
                    LOW_RIVER_WATER_LEVEL,
                    t
            );
        }

        return LOW_RIVER_WATER_LEVEL;
    }

    private static int roundedLerp(int start, int end, double t) {
        return (int)Math.round(start + (end - start) * clamp01(t));
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
}