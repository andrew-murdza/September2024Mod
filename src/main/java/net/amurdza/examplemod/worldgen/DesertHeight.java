package net.amurdza.examplemod.worldgen;

/** Existing high desert ridges, with a broad, shallow groundwater basin in their low center. */
public final class DesertHeight {
    public static final int SEA_LEVEL = 54;
    public static final int FIRST_TERRACOTTA_Y = 63;
    public static final double BOTTOM = 49;
    public static final double ORIGINAL_WATER_EDGE_HEIGHT = 62.5;
    private static final double SHALLOW_START = Math.pow(1.0 / 145, 2.0 / 3) - 0.06;
    private static final double LOWLAND_GRADE = 50;
    // 2,359,296 columns over four seeded noise fields put half the previous
    // water coverage at -0.419948 (TerrainNoiseRevisionAudit).
    public static final double WATER_EDGE_NOISE = -0.42;
    public static final double RED_SAND_START_NOISE = WATER_EDGE_NOISE
            + 0.975 * (Math.pow(8.0 / 145, 2.0 / 3) - 0.06 - (SHALLOW_START - 14.5 / 50));
    public static final double TERRACOTTA_START_NOISE = RED_SAND_START_NOISE
            + 1.25 * (Math.pow(15.0 / 145, 2.0 / 3) - Math.pow(8.0 / 145, 2.0 / 3));
    public static final double STEEP_START_NOISE = Math.pow(3.0 / 145, 2.0 / 3) - 0.06;
    public static final double REMOVED_SAND_RISE = 2;
    public static final double LAST_SAND_NOISE = RED_SAND_START_NOISE - REMOVED_SAND_RISE * 3 / LOWLAND_GRADE;
    public static final double RED_SAND_START_HEIGHT = SEA_LEVEL - 1.5 - REMOVED_SAND_RISE
            + (RED_SAND_START_NOISE - WATER_EDGE_NOISE) * LOWLAND_GRADE / 3;
    public static final double TERRACOTTA_START_HEIGHT = RED_SAND_START_HEIGHT
            + upperContour(STEEP_START_NOISE + TERRACOTTA_START_NOISE - RED_SAND_START_NOISE) - 75;
    public static final double TERRACOTTA_GRADE_SCALE = .4;

    private DesertHeight() { }

    public static double height(double land, double detail) {
        double original = materialHeight(land, detail);
        if (land <= TERRACOTTA_START_NOISE) return original;
        // Horizontal terracotta expansion divides the grade by its stretch;
        // compensate here so the final broad slope is exactly 0.4 of before.
        double broadRise = materialHeight(land, 0) - TERRACOTTA_START_HEIGHT;
        return original + broadRise * (TERRACOTTA_GRADE_SCALE * DesertBandLayout.TERRACOTTA_STRETCH - 1);
    }

    /** Reference contour used to calculate the flattened physical grade above the red-sand band. */
    public static double materialHeight(double land, double detail) {
        double shore = SEA_LEVEL - 1.5;
        if (land <= WATER_EDGE_NOISE) return Math.max(BOTTOM, shore + (land - WATER_EDGE_NOISE) * LOWLAND_GRADE);
        if (land <= RED_SAND_START_NOISE) return shore + (Math.min(land, LAST_SAND_NOISE) - WATER_EDGE_NOISE) * LOWLAND_GRADE / 3;
        double rise = land - RED_SAND_START_NOISE;
        double base = RED_SAND_START_HEIGHT + upperContour(STEEP_START_NOISE + rise) - 75;
        return base + detail * 5 * Math.min(1, rise / 0.08);
    }

    /** The upper desert contour, without its former broad, flat lowland section. */
    public static double upperContour(double land) {
        return 72 + Math.pow(Math.max(0, land + 0.06), 1.5) * 145;
    }

    /** Uncapped original contour, shared with Savanna's compressed dry relief. */
    public static double contour(double land, double detail) {
        double ridge = 72 + Math.pow(Math.max(0, land + 0.06), 1.5) * 145;
        // Continue the same large-scale contour toward its center instead of
        // adding separate lake/river noise or small detail-noise islands.
        // Lowering only the floor cap lets this same ramp continue four blocks
        // farther down. Sea level 60 fills Y56..59 above the solid Y55 bed.
        if (ridge <= 73) return 73 + (land - SHALLOW_START) * LOWLAND_GRADE;
        // Fade the old small ripples out at the shallow shelf. Above this short
        // transition the former sand slopes, red sand and terracotta are exact.
        double t = Math.max(0, Math.min(1, ridge - 73));
        return ridge + detail * 5 * t * t * (3 - 2 * t);
    }
}
