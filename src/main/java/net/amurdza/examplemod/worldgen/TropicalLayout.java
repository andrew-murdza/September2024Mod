package net.amurdza.examplemod.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeded, circular tropical islands separated by connected reef channels.
 * All band widths and slopes are measured along an island radius.
 * This class deliberately has no Minecraft dependency, so the production geometry
 * can also be sampled by the standalone layout audit.
 */
public final class TropicalLayout {
    // Keep the former 200-block reef corridor between neighboring islands.
    public static final double COLUMN_SPACING = 920;
    public static final double ROW_SPACING = COLUMN_SPACING * Math.sqrt(3) / 2;
    public static final int CENTER_JITTER = 12;
    public static final double FOREST_RADIUS = 270;
    // Y75 begins eleven complete twelve-block terraces inland from the Y64 bank.
    public static final double CHERRY_RADIUS = FOREST_RADIUS - 11 * 12;
    public static final double MANGROVE_LAND_WIDTH = 30;
    public static final double MANGROVE_LAND_RADIUS = FOREST_RADIUS + MANGROVE_LAND_WIDTH;
    public static final double ISLAND_RADIUS = MANGROVE_LAND_RADIUS + 60;
    public static final double CHERRY_TREE_SETBACK = 10;
    public static final double RAINFOREST_TREE_SETBACK = 4;
    private static final int[] MOUND_ROW_RADII = {1, 2, 3, 3, 3, 2, 1};
    private final long seed;
    private final double offsetX, offsetZ;

    public TropicalLayout(long seed) {
        this.seed = seed;
        offsetX = signed(mix(seed)) * COLUMN_SPACING / 2;
        offsetZ = signed(mix(seed ^ 0x51ed2705L)) * ROW_SPACING / 2;
    }
    public long seed() { return seed; }

    public record Island(double x, double z, long hash, int column, int row) {
        public Island(double x, double z, long hash) { this(x, z, hash, 0, 0); }
    }

    public record Sample(double height, int biome, double distance, double cherryEdge,
                         double forestEdge, double mangroveEdge, Island island) { }
    public Island island(int column, int row) {
        long hash = mix(seed ^ mix(column * 0x632be59bd9b4e019L) ^ mix(row * 0x9e3779b97f4a7c15L));
        return new Island(offsetX + column * COLUMN_SPACING + ((row & 1) == 0 ? 0 : COLUMN_SPACING / 2)
                + signed(hash) * CENTER_JITTER,
                offsetZ + row * ROW_SPACING + signed(mix(hash)) * CENTER_JITTER, hash, column, row);
    }

    public Island nearest(double x, double z) {
        int row = (int) Math.floor((z - offsetZ) / ROW_SPACING);
        double best = Double.POSITIVE_INFINITY;
        Island result = null;
        for (int rz = row - 1; rz <= row + 1; rz++) {
            int column = (int) Math.floor((x - offsetX - ((rz & 1) == 0 ? 0 : COLUMN_SPACING / 2)) / COLUMN_SPACING);
            for (int cx = column - 1; cx <= column + 1; cx++) {
                Island candidate = island(cx, rz);
                double dx = x - candidate.x, dz = z - candidate.z;
                double distance = dx * dx + dz * dz;
                if (distance < best) { best = distance; result = candidate; }
            }
        }
        return result;
    }

    /** Enumerate nearby lattice cells without generating terrain or loading chunks. */
    public List<Island> nearbyIslands(double x, double z, int radius) {
        List<Island> result = new ArrayList<>();
        int row = (int) Math.round((z - offsetZ) / ROW_SPACING);
        for (int rz = row - radius; rz <= row + radius; rz++) {
            int column = (int) Math.round((x - offsetX - ((rz & 1) == 0 ? 0 : COLUMN_SPACING / 2)) / COLUMN_SPACING);
            for (int cx = column - radius; cx <= column + radius; cx++) {
                result.add(island(cx, rz));
            }
        }
        return result;
    }

    public Sample sample(double x, double z) { return sample(x, z, nearest(x, z)); }

    public Sample sample(double x, double z, Island island) {
        double dx = x - island.x, dz = z - island.z;
        double radius = Math.sqrt(dx * dx + dz * dz);
        double height;
        if (radius < FOREST_RADIUS) {
            // Rounding to block heights starts Y64 at the bank, then Y65 twelve
            // blocks inland. Flatten the former Y85/Y86 summit into the Y84 terrace.
            height = Math.min(84.1, 63.5 + (FOREST_RADIUS - radius) / 12.0);
        } else if (radius < MANGROVE_LAND_RADIUS) {
            height = 63.1;
        } else if (radius < ISLAND_RADIUS) {
            // Restore the shore planting terrace and the established deep shelf.
            double reefApproach = Math.min(58.1, 57.1 + (ISLAND_RADIUS - radius) / 2);
            height = Math.max(reefApproach, 62.1 - Math.max(0, shorelineDistance(dx, dz, MANGROVE_LAND_RADIUS) - 6));
        } else {
            height = Math.max(55.1, 57.1 - (radius - ISLAND_RADIUS) / 2);
        }
        int biome = radius < FOREST_RADIUS ? (Math.floor(height + .5) >= 75 ? 3 : 2)
                : radius < ISLAND_RADIUS ? 1 : 0;
        return new Sample(height, biome, radius, CHERRY_RADIUS, FOREST_RADIUS, ISLAND_RADIUS, island);
    }

    /** A chunk is eligible when its complete top and shoreline clearance are wet. */
    public boolean moundEligibleChunk(int chunkX, int chunkZ) {
        Sample center = sample(chunkX * 16 + 8, chunkZ * 16 + 8);
        int baseBed = (int) Math.floor(center.height + .5);
        return center.biome == 1 && baseBed >= 58 && baseBed <= 62
                && center.distance >= center.forestEdge + 12
                && center.distance <= center.mangroveEdge - 12;
    }

    /** Kept for callers that audit the retired mound terrain. */
    public boolean moundChunk(int chunkX, int chunkZ) {
        return false;
    }

    public boolean mangroveLandChunk(int chunkX, int chunkZ) {
        Sample center = sample(chunkX * 16 + 8, chunkZ * 16 + 8);
        return center.biome() == 1 && center.distance() >= FOREST_RADIUS + 6
                && center.distance() <= MANGROVE_LAND_RADIUS - 5;
    }

    public static boolean moundCheckerboardChunk(int chunkX, int chunkZ) {
        return ((chunkX + chunkZ) & 1) == 0;
    }

    public static boolean mangroveTreeChunk(int chunkX, int chunkZ) {
        return !moundCheckerboardChunk(chunkX, chunkZ);
    }

    public static boolean moundTopOffset(int x, int z) {
        return Math.abs(z) <= 3 && Math.abs(x) <= MOUND_ROW_RADII[z + 3];
    }

    public boolean moundTop(int x, int z) {
        int chunkX = Math.floorDiv(x, 16), chunkZ = Math.floorDiv(z, 16);
        return moundTopOffset(x - (chunkX * 16 + 8), z - (chunkZ * 16 + 8))
                && moundChunk(chunkX, chunkZ);
    }

    /** Exact pre-decoration surface; the former isolated mound terraces are removed. */
    public double surfaceHeight(int x, int z) {
        return sample(x, z).height();
    }

    /** No isolated mound contributes any terrain height. */
    public int moundBed(int x, int z) {
        return Integer.MIN_VALUE;
    }

    private static double shorelineDistance(double x, double z, double radius) {
        double a = Math.abs(x), b = Math.abs(z);
        if (Math.abs(a - b) >= radius) return Math.max(a, b) - radius;
        return (a + b - Math.sqrt(2 * radius * radius - (a - b) * (a - b))) / 2;
    }

    public static long mix(long value) {
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }

    private static double signed(long value) { return (value >>> 11) * 0x1.0p-53 * 2 - 1; }
}
