package net.amurdza.examplemod.util;

public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Packs RGB values (0–255) into a single int (0xRRGGBB).
     */
    public static int color(int r, int g, int b) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}