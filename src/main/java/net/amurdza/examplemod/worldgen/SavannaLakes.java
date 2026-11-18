package net.amurdza.examplemod.worldgen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/** Sparse lowland lakes with smooth asymmetric shores and unchanged hills between them. */
public final class SavannaLakes {
    public static final int CELL_SIZE = 640;
    public static final int REGION_SIZE = 240;
    public static final double MINIMUM_CENTER_DISTANCE = REGION_SIZE;
    public static final double WATER_EDGE_HEIGHT = 62.5;
    public static final double BOTTOM_HEIGHT = 56;
    public static final double UNDERWATER_SLOPE = .4;
    public static final double DRY_SLOPE = .08;
    private static final double DRY_BLEND_PER_HEIGHT = 20;
    private static final double MAX_DRY_REACH = (81 - WATER_EDGE_HEIGHT) * DRY_BLEND_PER_HEIGHT;
    private final long seed;
    private final DoubleBinaryOperator dryHeight;
    private final int offsetX, offsetZ;
    private final ConcurrentHashMap<Long, Region> regions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Lake> lakes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Double> columns = new ConcurrentHashMap<>();

    public SavannaLakes(long seed, DoubleBinaryOperator dryHeight) {
        this.seed = seed;
        this.dryHeight = dryHeight;
        offsetX = (int) Long.remainderUnsigned(mix(seed), CELL_SIZE / 16) * 16;
        offsetZ = (int) Long.remainderUnsigned(mix(seed ^ 0x51ed2705L), CELL_SIZE / 16) * 16;
    }

    public record Region(int column, int row, int cellMinX, int cellMinZ,
                         int minX, int minZ, int centerX, int centerZ) {
        public boolean containsBlock(int x, int z) {
            return x >= minX && x < minX + REGION_SIZE && z >= minZ && z < minZ + REGION_SIZE;
        }
        public boolean containsChunk(int chunkX, int chunkZ) {
            return containsBlock(chunkX * 16, chunkZ * 16) && containsBlock(chunkX * 16 + 15, chunkZ * 16 + 15);
        }
    }

    /** A 40-by-40-chunk cell owns one 15-by-15-chunk lake region; all other chunks are background. */
    public Region regionFor(int column, int row) {
        long key = key(column, row);
        Region cached = regions.get(key);
        if (cached != null) return cached;
        int cellMinX = offsetX + column * CELL_SIZE + stagger(row);
        int cellMinZ = offsetZ + row * CELL_SIZE;
        int centerX = 0, centerZ = 0;
        double lowest = Double.POSITIVE_INFINITY;
        long candidate = hash(column, row);
        // Choose a broad existing lowland, rather than carving every lake into a hill.
        // The candidate region remains wholly inside its cell, including negative coordinates.
        for (int i = 0; i < 9; i++) {
            candidate = mix(candidate + 0x9e3779b97f4a7c15L);
            int x = cellMinX + 120 + (int) Long.remainderUnsigned(candidate, 26) * 16;
            int z = cellMinZ + 120 + (int) Long.remainderUnsigned(mix(candidate), 26) * 16;
            double height = dryHeight.applyAsDouble(x, z) * 2;
            height += dryHeight.applyAsDouble(x - 48, z) + dryHeight.applyAsDouble(x + 48, z)
                    + dryHeight.applyAsDouble(x, z - 48) + dryHeight.applyAsDouble(x, z + 48);
            if (height < lowest) { lowest = height; centerX = x; centerZ = z; }
        }
        Region result = new Region(column, row, cellMinX, cellMinZ, centerX - 120, centerZ - 120, centerX, centerZ);
        if (regions.size() > 8192) regions.clear();
        regions.put(key, result);
        return result;
    }

    /** Returns the containing cell's region even when the queried block is background land. */
    public Region regionForBlock(int x, int z) {
        int row = Math.floorDiv(z - offsetZ, CELL_SIZE);
        int column = Math.floorDiv(x - offsetX - stagger(row), CELL_SIZE);
        return regionFor(column, row);
    }

    public Region lakeRegionAtChunk(int chunkX, int chunkZ) {
        Region region = regionForBlock(chunkX * 16 + 8, chunkZ * 16 + 8);
        return region.containsChunk(chunkX, chunkZ) ? region : null;
    }

    private static int stagger(int row) { return (row & 1) == 0 ? 0 : 320; }
    private long hash(int column, int row) {
        return mix(seed ^ mix(column * 0x632be59bd9b4e019L) ^ mix(row * 0x9e3779b97f4a7c15L));
    }

    /** A low-frequency closed outline. Distances are Euclidean, so elongation never steepens banks. */
    public static final class Lake {
        private static final int POINTS = 192;
        private final double x, z, radius, stretch, rotation, bound;
        private final long hash;
        private final double[] px = new double[POINTS], pz = new double[POINTS];
        private final double[] minX = new double[POINTS / 8], maxX = new double[POINTS / 8];
        private final double[] minZ = new double[POINTS / 8], maxZ = new double[POINTS / 8];
        private final double cos, sin;

        private Lake(double x, double z, double area, double stretch, double rotation, long hash) {
            this.x = x; this.z = z; this.radius = Math.sqrt(area / Math.PI);
            this.stretch = stretch; this.rotation = rotation; this.hash = hash;
            cos = Math.cos(rotation); sin = Math.sin(rotation);
            double phase2 = signed(mix(hash ^ 0x510e527fade682d1L)) * Math.PI;
            double phase3 = signed(mix(hash ^ 0x9b05688c2b3e6c1fL)) * Math.PI;
            for (int i = 0; i < POINTS; i++) {
                double angle = i * (2 * Math.PI / POINTS);
                double r = 1 + .10 * Math.cos(angle) + .10 * Math.sin(2 * angle + phase2)
                        + .06 * Math.cos(3 * angle + phase3);
                px[i] = Math.cos(angle) * r * stretch;
                pz[i] = Math.sin(angle) * r / stretch;
            }
            double twiceArea = 0;
            for (int i = 0; i < POINTS; i++) {
                int next = (i + 1) % POINTS;
                twiceArea += px[i] * pz[next] - pz[i] * px[next];
            }
            double scale = Math.sqrt(2 * area / twiceArea), enclosing = 0;
            for (int i = 0; i < POINTS; i++) {
                px[i] *= scale; pz[i] *= scale;
                enclosing = Math.max(enclosing, Math.hypot(px[i], pz[i]));
            }
            bound = enclosing;
            for (int group = 0; group < POINTS / 8; group++) {
                minX[group] = minZ[group] = Double.POSITIVE_INFINITY;
                maxX[group] = maxZ[group] = Double.NEGATIVE_INFINITY;
                for (int j = 0; j <= 8; j++) {
                    int i = (group * 8 + j) % POINTS;
                    minX[group] = Math.min(minX[group], px[i]); maxX[group] = Math.max(maxX[group], px[i]);
                    minZ[group] = Math.min(minZ[group], pz[i]); maxZ[group] = Math.max(maxZ[group], pz[i]);
                }
            }
        }
        public double x() { return x; }
        public double z() { return z; }
        public double radius() { return radius; }
        public double stretch() { return stretch; }
        public double rotation() { return rotation; }
        public long hash() { return hash; }
        public boolean active() { return true; }
        public double bound() { return bound; }

        /** Negative inside; exact distance to a finely sampled, precomputed smooth outline. */
        public double shoreDistance(double worldX, double worldZ) {
            double dx = worldX - x, dz = worldZ - z;
            double qx = cos * dx + sin * dz, qz = -sin * dx + cos * dz;
            double angle = Math.atan2(qz * stretch, qx / stretch);
            if (angle < 0) angle += 2 * Math.PI;
            int radial = Math.min(POINTS - 1, (int) (angle * POINTS / (2 * Math.PI)));
            int next = (radial + 1) % POINTS;
            boolean inside = (px[next] - px[radial]) * (qz - pz[radial])
                    - (pz[next] - pz[radial]) * (qx - px[radial]) >= 0;
            double nearest = segmentDistanceSquared(radial, qx, qz);
            for (int group = 0; group < POINTS / 8; group++) {
                double gx = Math.max(Math.max(minX[group] - qx, qx - maxX[group]), 0);
                double gz = Math.max(Math.max(minZ[group] - qz, qz - maxZ[group]), 0);
                if (gx * gx + gz * gz >= nearest) continue;
                for (int j = 0; j < 8; j++) nearest = Math.min(nearest, segmentDistanceSquared(group * 8 + j, qx, qz));
            }
            return (inside ? -1 : 1) * Math.sqrt(nearest);
        }
        private double segmentDistanceSquared(int index, double x, double z) {
            int next = (index + 1) % POINTS;
            double dx = px[next] - px[index], dz = pz[next] - pz[index];
            double t = Math.max(0, Math.min(1, ((x - px[index]) * dx + (z - pz[index]) * dz) / (dx * dx + dz * dz)));
            double ex = x - px[index] - t * dx, ez = z - pz[index] - t * dz;
            return ex * ex + ez * ez;
        }
    }

    /** Restore the original gentle land derivative of fifteen, with a smooth upper bound of 81. */
    public static double dryHeight(double land, double detail) {
        return 72 + 8.5 * land / Math.hypot(land, 8.5 / 15)
                + .5 * detail / Math.hypot(detail, 1.0 / 3);
    }

    public Lake lake(int cellX, int cellZ) {
        long key = key(cellX, cellZ);
        Lake result = lakes.get(key);
        if (result != null) return result;
        Region region = regionFor(cellX, cellZ);
        long hash = hash(cellX, cellZ);
        double area = 10400 + signed(mix(hash ^ 0x6a09e667f3bcc909L)) * 1000;
        double stretch = 1.325 + signed(mix(hash ^ 0xbb67ae8584caa73bL)) * .075;
        result = new Lake(region.centerX(), region.centerZ(), area, stretch,
                signed(mix(hash ^ 0x3c6ef372fe94f82bL)) * Math.PI, hash);
        if (lakes.size() > 8192) lakes.clear();
        lakes.put(key, result);
        return result;
    }

    public double surfaceHeight(int x, int z) {
        long key = key(x, z);
        Double cached = columns.get(key);
        if (cached != null) return cached;
        double height = height(x, z, dryHeight.applyAsDouble(x, z));
        if (columns.size() > 16384) columns.clear();
        columns.put(key, height);
        return height;
    }

    public double height(double x, double z, double landHeight) {
        int row = (int) Math.floor((z - offsetZ) / CELL_SIZE);
        double result = landHeight;
        for (int rz = row - 1; rz <= row + 1; rz++) {
            int column = (int) Math.floor((x - offsetX - stagger(rz)) / CELL_SIZE);
            for (int cx = column - 1; cx <= column + 1; cx++) {
                Lake lake = lake(cx, rz);
                double outer = lake.bound() + Math.min(MAX_DRY_REACH, (landHeight - WATER_EDGE_HEIGHT) * DRY_BLEND_PER_HEIGHT);
                if (Math.abs(x - lake.x()) > outer || Math.abs(z - lake.z()) > outer
                        || Math.hypot(x - lake.x(), z - lake.z()) > outer) continue;
                double distance = lake.shoreDistance(x, z);
                double shaped = profile(distance, landHeight);
                // Fade the smoothing width to zero at either approach's outer
                // boundary, retaining exact untouched land and avoiding joining ridges.
                double overlap = Math.max(0, landHeight - result) * Math.max(0, landHeight - shaped);
                double width = .4 * overlap / (overlap + .25);
                if (width == 0) result = Math.min(result, shaped);
                else {
                    double blend = Math.max(width - Math.abs(result - shaped), 0) / width;
                    result = Math.min(result, shaped) - blend * blend * width / 4;
                }
            }
        }
        return result;
    }

    private static double profile(double distance, double landHeight) {
        // Preserve the 1:2.5 submerged bank, easing only its shallow uppermost
        // block into the .08 dry-shore grade. The rounded bottom has no hard flat cap.
        final double knee = -18.56875;
        if (distance < knee) return BOTTOM_HEIGHT + .1 * Math.exp((distance - knee) / 8);
        if (distance < knee + 2) {
            double t = distance - knee;
            return BOTTOM_HEIGHT + .1 + .0125 * t + (.4 - .0125) * t * t / 4;
        }
        if (distance < -4) return 63.14 + UNDERWATER_SLOPE * distance;
        if (distance < 0) return WATER_EDGE_HEIGHT + DRY_SLOPE * distance - .04 * distance * distance;
        double relief = landHeight - WATER_EDGE_HEIGHT;
        double t = distance / (DRY_BLEND_PER_HEIGHT * relief);
        if (t >= 1) return landHeight;
        // Hermite blend inherits the actual hills' height and asymmetry. Its
        // shoreline derivative is .08, joining the unmodified land with C1 continuity.
        double blend = t * (1.6 + t * (-.2 - .4 * t));
        return WATER_EDGE_HEIGHT + relief * blend;
    }

    public static double interpolatedHeight(int x, int z, DoubleBinaryOperator rawHeight) {
        int x0 = Math.floorDiv(x, 4) * 4, z0 = Math.floorDiv(z, 4) * 4;
        double tx = (x - x0) / 4.0, tz = (z - z0) / 4.0;
        double north = lerp(tx, rawHeight.applyAsDouble(x0, z0), rawHeight.applyAsDouble(x0 + 4, z0));
        double south = lerp(tx, rawHeight.applyAsDouble(x0, z0 + 4), rawHeight.applyAsDouble(x0 + 4, z0 + 4));
        return lerp(tz, north, south);
    }

    public static int waterDepth(double height) { return Math.max(0, 64 - (int) Math.ceil(height + .5)); }
    private static double lerp(double t, double first, double second) { return first + t * (second - first); }
    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xffffffffL; }
    private static double signed(long hash) { return (hash >>> 11) * 0x1.0p-53 * 2 - 1; }
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
