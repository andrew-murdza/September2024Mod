package net.amurdza.examplemod.worldgen.river;

public final class MountainRiverHelper {
    /*
     * Low-side water level.
     *
     * This is 64 so the ramp from 64 -> 160 is exactly 96 blocks.
     * With a 32-block horizontal boundary, that gives slope 3.
     *
     * If you want it to meet vanilla sea level exactly instead, change this to 63,
     * but then the slope will not be exactly 3 unless the mountain level also changes.
     */
    public static final int LOW_RIVER_WATER_LEVEL = 64;

    public static final int MOUNTAIN_RIVER_WATER_LEVEL = 160;

    /*
     * These match the mountain boundary cutoffs:
     *
     * plains -> mountain ramp: 0.400 to 0.405
     * mountain plateau:        0.405 to 0.495
     * mountain -> badlands:    0.495 to 0.500
     */
    private static final double MIN_CONTINENTS = 0.400D;
    private static final double MOUNTAIN_START_CONTINENTS = 0.415D;
    private static final double MOUNTAIN_END_CONTINENTS = 0.485D;
    private static final double MAX_CONTINENTS = 0.500D;

    private MountainRiverHelper() {
    }

    public static boolean isMountainRiverContinentsValue(double continents) {
        return continents >= MIN_CONTINENTS && continents <= MAX_CONTINENTS;
    }

    public static int getMountainRiverWaterLevel(double continents) {
        if (continents <= MOUNTAIN_START_CONTINENTS) {
            double t = inverseLerp(
                    MIN_CONTINENTS,
                    MOUNTAIN_START_CONTINENTS,
                    continents
            );

            return roundedLerp(
                    LOW_RIVER_WATER_LEVEL,
                    MOUNTAIN_RIVER_WATER_LEVEL,
                    t
            );
        }

        if (continents <= MOUNTAIN_END_CONTINENTS) {
            return MOUNTAIN_RIVER_WATER_LEVEL;
        }

        double t = inverseLerp(
                MOUNTAIN_END_CONTINENTS,
                MAX_CONTINENTS,
                continents
        );

        return roundedLerp(
                MOUNTAIN_RIVER_WATER_LEVEL,
                LOW_RIVER_WATER_LEVEL,
                t
        );
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