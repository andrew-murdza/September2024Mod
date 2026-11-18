package net.amurdza.examplemod.worldgen;

/** Gentle lake watersheds with a common shoreline and full-depth basin floor. */
public final class SavannaHeight {
    public static final int SEA_LEVEL = 63;
    // Solid tops at Y61 and below are wet; Y62 is the first dry block elevation.
    public static final double WATER_EDGE_HEIGHT = SEA_LEVEL - 1.5;
    public static final int MAX_WATER_DEPTH = 8;
    public static final double BOTTOM = SEA_LEVEL - MAX_WATER_DEPTH - .5;
    public static final double MAX_HEIGHT = 82.5;
    public static final int MIN_LAND_BLOCK = 62;
    public static final double NOISE_SCALE = 4.7;
    public static final double MIN_LAND_SLOPE = 1.0 / 20;
    public static final double MAX_LAND_SLOPE = 1.0 / 18;
    public static final double BANK_SLOPE = .45;
    public static final double BANK_FLOOR_EASE_WIDTH = 8;
    public static final double BANK_WIDTH = (WATER_EDGE_HEIGHT - BOTTOM) / BANK_SLOPE + BANK_FLOOR_EASE_WIDTH / 2;

    private SavannaHeight() { }

    /** Negative distance is submerged; every basin ends at an eight-block-deep Y54 bed. */
    public static double height(double shoreDistance) {
        if (shoreDistance <= 0) {
            double inward = -shoreDistance;
            if (inward >= BANK_WIDTH) return BOTTOM;
            double straightWidth = BANK_WIDTH - BANK_FLOOR_EASE_WIDTH;
            double easeDistance = Math.max(0, inward - straightWidth);
            // Keep the complete bank at or below the configured grade. Ease only the
            // final eight horizontal blocks into the flat bottom; a normalized
            // bowl would steepen the middle again as the maximum depth changes.
            double depth = BANK_SLOPE * (inward - easeDistance * easeDistance / (2 * BANK_FLOOR_EASE_WIDTH));
            return Math.max(BOTTOM, WATER_EDGE_HEIGHT - depth);
        }
        return WATER_EDGE_HEIGHT + shoreDistance * MAX_LAND_SLOPE;
    }
}
