package net.amurdza.examplemod.worldgen;

/** Deletes distance only from the level deep-ocean floor; every sloping contour is translated intact. */
public final class SnowyBandCompression {
    private static final double DEEP_CONTOUR = inverse(MountainLakeOffset.LAKE_WEIRDNESS
            - DocumentedFauna.DEEP_WATER_THRESHOLD / SnowyTerrainProfile.LAKE_SLOPE);
    private static final double FLAT_CONTOUR = inverse(MountainLakeOffset.LAKE_WEIRDNESS
            - MountainLakeOffset.MAX_LAKE_DEPTH / SnowyTerrainProfile.LAKE_SLOPE);

    public static final double EXTRA_PLATEAU_WIDTH = 20;

    private SnowyBandCompression() { }

    private static final double PLATEAU_START = plateauContour();

    private static double plateauContour() {
        double low = .7, high = 1;
        for (int i = 0; i < 48; i++) {
            double mid = (low + high) / 2;
            if (MountainRidges.contour(mid) < .85) low = mid; else high = mid;
        }
        return (low + high) / 2;
    }

    public static double plateauWidth(double stretch) {
        return 480 * stretch * (1 - PLATEAU_START) + EXTRA_PLATEAU_WIDTH;
    }

    public static double originalDeepWidth(double stretch) { return 480 * stretch * DEEP_CONTOUR; }
    public static double originalFlatWidth(double stretch) { return 480 * stretch * FLAT_CONTOUR; }
    public static double flatWidthAtDepth(double stretch, double depth) {
        return 480 * stretch * inverse(MountainLakeOffset.LAKE_WEIRDNESS - depth / SnowyTerrainProfile.LAKE_SLOPE);
    }
    public static double minimumDeepWidth(double stretch) { return originalDeepWidth(stretch) - originalFlatWidth(stretch); }

    public static double period(double stretch, double targetDeepWidth) {
        return 960 * stretch - 2 * removed(stretch, targetDeepWidth) + 2 * EXTRA_PLATEAU_WIDTH;
    }

    public static double originalCoordinate(double coordinate, double stretch, double targetDeepWidth) {
        double removed = removed(stretch, targetDeepWidth);
        double oldSpacing = 480 * stretch, spacing = oldSpacing - removed + EXTRA_PLATEAU_WIDTH;
        double peak = Math.floor((coordinate + spacing / 2) / spacing);
        double local = coordinate - peak * spacing;
        double oldFlatHalf = originalFlatWidth(stretch) / 2;
        double edge = oldSpacing / 2 - oldFlatHalf;
        double distance = Math.max(0, Math.abs(local) - EXTRA_PLATEAU_WIDTH / 2);
        if (distance > edge) distance = edge + (distance - edge) * oldFlatHalf / (oldFlatHalf - removed / 2);
        return peak * oldSpacing + Math.copySign(distance, local);
    }

    private static double removed(double stretch, double target) {
        if (target <= 0 || target >= originalDeepWidth(stretch)) return 0;
        if (target <= minimumDeepWidth(stretch))
            throw new IllegalArgumentException("Deep-water width must leave the existing deep ramps intact: " + target);
        return originalDeepWidth(stretch) - target;
    }

    private static double inverse(double weirdness) {
        double lo = 0, hi = .7;
        for (int i = 0; i < 60; i++) {
            double middle = (lo + hi) / 2;
            if (MountainRidges.contour(middle) < weirdness) lo = middle; else hi = middle;
        }
        return (lo + hi) / 2;
    }
}
