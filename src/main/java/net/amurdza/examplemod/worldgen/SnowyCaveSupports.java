package net.amurdza.examplemod.worldgen;

/** Slender circular supports, with independently jittered centers instead of a visible lattice. */
public final class SnowyCaveSupports {
    public static final int CELL_SIZE = 40;
    public static final double FLARE = 2.5;
    private final long seed;

    public SnowyCaveSupports(long seed) { this.seed = seed; }

    /** Signed distance outside the nearest circular core. */
    public double clearance(int x, int z) {
        int cx = Math.floorDiv(x, CELL_SIZE), cz = Math.floorDiv(z, CELL_SIZE);
        double nearest = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = TropicalLayout.mix(seed ^ (cx + dx) * 341873128712L ^ (cz + dz) * 132897987541L);
            double px = (cx + dx + .15 + unit(hash) * .7) * CELL_SIZE;
            double pz = (cz + dz + .15 + unit(Long.rotateLeft(hash, 21)) * .7) * CELL_SIZE;
            double radius = 2 + unit(Long.rotateLeft(hash, 42));
            nearest = Math.min(nearest, Math.hypot(x + .5 - px, z + .5 - pz) - radius);
        }
        return nearest;
    }

    public static double flare(int y, int floorAir, int ceilingAir) {
        double distance = Math.min(y - floorAir, ceilingAir - y);
        double nearEnd = Math.max(0, 1 - distance / 5);
        return FLARE * nearEnd * nearEnd;
    }

    private static double unit(long value) { return (value >>> 40) / (double) (1 << 24); }
}
