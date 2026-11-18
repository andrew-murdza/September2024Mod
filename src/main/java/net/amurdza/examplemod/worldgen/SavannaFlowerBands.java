package net.amurdza.examplemod.worldgen;

import java.util.List;

/** Parallel, gently warped composite stripes. Presence and all six colours share one phase. */
public final class SavannaFlowerBands {
    public static final double COLOUR_WIDTH = 6.5;
    public static final double FLOWER_WIDTH = 6 * COLOUR_WIDTH;
    public static final double FLOWER_FREE_GAP = 120;
    public static final double PERIOD = FLOWER_WIDTH + FLOWER_FREE_GAP;
    public static final int FLOWERS_PER_256 = 12;
    public static final List<String> TULIPS = List.of("minecraft:red_tulip", "minecraft:white_tulip",
            "floral:blue_tulip", "minecraft:orange_tulip", "minecraft:pink_tulip", "floral:yellow_tulip");
    private final long seed;
    private final double normalX, normalZ, offset;

    public SavannaFlowerBands(long seed) {
        this.seed = seed;
        double angle = unit(mix(seed ^ 0x97bfa8e21465L)) * Math.PI * 2;
        normalX = Math.cos(angle);
        normalZ = Math.sin(angle);
        offset = unit(mix(seed ^ 0x1b873593L)) * PERIOD;
    }

    public double normalX() { return normalX; }
    public double normalZ() { return normalZ; }

    /** Unwrapped signed phase, increasing across a stripe rather than outwards from its middle. */
    public double phase(double x, double z) {
        double along = (-normalZ * x + normalX * z) / 128;
        long index = (long) Math.floor(along);
        double t = along - index;
        t = t * t * (3 - 2 * t);
        double a = unit(mix(seed ^ mix(index) ^ 0x4b1d3e7aL)) * 2 - 1;
        double b = unit(mix(seed ^ mix(index + 1) ^ 0x4b1d3e7aL)) * 2 - 1;
        // Warp varies only along the stripe. Its maximum slope is .422, so
        // the physical width normal to a local edge stays between 6.0 and 6.5.
        // The same global gradient bound makes the 120-block phase gap a
        // 110.56–120 block Euclidean edge-to-edge gap, including curved sections.
        return normalX * x + normalZ * z + offset + (a + t * (b - a)) * 18;
    }

    public int colour(double x, double z) {
        double phase = phase(x, z);
        phase -= Math.floor(phase / PERIOD) * PERIOD;
        return phase < FLOWER_WIDTH ? Math.min(5, (int) (phase / COLOUR_WIDTH)) : -1;
    }

    public String tulip(int x, int z) {
        int colour = colour(x, z);
        return colour < 0 ? null : TULIPS.get(colour);
    }

    public boolean hasFlower(int x, int z) {
        return colour(x, z) >= 0 && (hash(seed ^ 0x6c8e9cf570932bd5L, x, z) & 255) < FLOWERS_PER_256;
    }

    static long hash(long seed, int x, int z) {
        return mix(seed ^ mix(x * 0x632be59bd9b4e019L) ^ mix(z * 0x9e3779b97f4a7c15L));
    }

    private static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }
    private static long mix(long value) {
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
}
