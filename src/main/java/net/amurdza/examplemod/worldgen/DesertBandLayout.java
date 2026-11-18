package net.amurdza.examplemod.worldgen;

/** Resizes only lake and terracotta distances along the existing bent desert contours. */
public final class DesertBandLayout {
    public static final double ORIGINAL_PERIOD = 768;
    public static final double ORIGINAL_LAKE_HALF = originalDistance(DesertHeight.WATER_EDGE_NOISE);
    public static final double ORIGINAL_TERRACOTTA_START = originalDistance(DesertHeight.TERRACOTTA_START_NOISE);
    public static final double ORIGINAL_TERRACOTTA_WIDTH = ORIGINAL_PERIOD - 2 * ORIGINAL_TERRACOTTA_START;
    // Below this contour the physical bed has reached its flat minimum. Halve
    // just that part of the existing half-scale lake, preserving both banks.
    public static final double ORIGINAL_FLAT_HALF = originalDistance(DesertHeight.WATER_EDGE_NOISE
            + (DesertHeight.BOTTOM - (DesertHeight.SEA_LEVEL - 1.5)) / 50);
    public static final double FLAT_HALF = ORIGINAL_FLAT_HALF / 4;
    public static final double LAKE_HALF = ORIGINAL_LAKE_HALF / 2 - FLAT_HALF;
    public static final double ORIGINAL_RED_START = originalDistance(DesertHeight.RED_SAND_START_NOISE);
    public static final double SAND_END = originalDistance(DesertHeight.LAST_SAND_NOISE);
    public static final double REMOVED_PER_FLANK = ORIGINAL_RED_START - SAND_END;
    public static final double TERRACOTTA_WIDTH = 288 * .6 + 2 * REMOVED_PER_FLANK;
    public static final double TERRACOTTA_START = LAKE_HALF + ORIGINAL_TERRACOTTA_START - ORIGINAL_LAKE_HALF - REMOVED_PER_FLANK;
    public static final double PERIOD = 2 * TERRACOTTA_START + TERRACOTTA_WIDTH;
    public static final double TERRACOTTA_STRETCH = TERRACOTTA_WIDTH / ORIGINAL_TERRACOTTA_WIDTH;

    private DesertBandLayout() { }

    public static double originalDistance(double contour) {
        return ORIGINAL_PERIOD / (2 * Math.PI) * Math.acos((.02 - contour) / .60);
    }

    /** Distance from the nearest valley center in the resized profile. */
    public static double resizedDistance(double original) {
        if (original <= ORIGINAL_FLAT_HALF) return original / 4;
        if (original <= ORIGINAL_LAKE_HALF) return original / 2 - FLAT_HALF;
        if (original <= ORIGINAL_TERRACOTTA_START) return LAKE_HALF + original - ORIGINAL_LAKE_HALF
                - Math.min(REMOVED_PER_FLANK, Math.max(0, original - SAND_END));
        return TERRACOTTA_START + (original - ORIGINAL_TERRACOTTA_START) * TERRACOTTA_STRETCH;
    }

    public static double originalCoordinate(double coordinate) {
        double cycle = Math.floor((coordinate + PERIOD / 2) / PERIOD);
        double local = coordinate - cycle * PERIOD;
        double distance = Math.abs(local), original;
        if (distance <= FLAT_HALF) original = distance * 4;
        else if (distance <= LAKE_HALF) original = (distance + FLAT_HALF) * 2;
        else if (distance <= TERRACOTTA_START) {
            original = ORIGINAL_LAKE_HALF + distance - LAKE_HALF;
            if (original >= SAND_END) original += REMOVED_PER_FLANK;
        }
        else original = ORIGINAL_TERRACOTTA_START + (distance - TERRACOTTA_START) / TERRACOTTA_STRETCH;
        return cycle * ORIGINAL_PERIOD + Math.copySign(original, local);
    }
}
