package net.amurdza.examplemod.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

/** Sparse, open lowland channels. Distances are Euclidean blocks, not noise thresholds. */
public final class SavannaRivers {
    public static final int CELL_WIDTH = 1024;
    public static final int CELL_LENGTH = 1536;
    public static final double MAX_ROUTE_HEIGHT = 80;
    private static final double WATER_EDGE_HEIGHT = 62.5;
    // Even diagonal neighbours differ by less than one before block rounding.
    private static final double BANK_SLOPE = .5;
    private final long seed;
    private final DoubleBinaryOperator landHeight;
    private final double cosine, sine, offsetX, offsetZ;
    private final Map<Long, River> routes = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, River> eldest) {
            return size() > 256;
        }
    };

    public SavannaRivers(long seed, DoubleBinaryOperator landHeight) {
        this.seed = seed;
        this.landHeight = landHeight;
        double angle = unit(mix(seed)) * Math.PI;
        cosine = Math.cos(angle);
        sine = Math.sin(angle);
        offsetX = unit(mix(seed ^ 0x63dab21L)) * CELL_WIDTH;
        offsetZ = unit(mix(seed ^ 0x4ba6721L)) * CELL_LENGTH;
    }

    public record Point(double x, double z) { }
    public record River(Point[] points, double halfWidth, boolean active) { }

    public double height(double x, double z, double originalHeight) {
        double localX = x * cosine + z * sine - offsetX;
        double localZ = -x * sine + z * cosine - offsetZ;
        River river = river((int) Math.floor(localX / CELL_WIDTH), (int) Math.floor(localZ / CELL_LENGTH));
        if (!river.active) return originalHeight;
        double distance = distance(x, z, river);
        // The dry margin between cells is much wider than the entire bank, even
        // at maximum terrain height. No channel crosses a cell edge or loops.
        double channel = Math.max(54, WATER_EDGE_HEIGHT + BANK_SLOPE * (distance - river.halfWidth));
        return Math.min(originalHeight, channel);
    }

    public synchronized River river(int column, int row) {
        long key = ((long) column << 32) ^ (row & 0xffffffffL);
        return routes.computeIfAbsent(key, ignored -> createRiver(column, row));
    }

    private River createRiver(int column, int row) {
        long hash = mix(seed ^ mix(column * 0x632be59bd9b4e019L) ^ mix(row * 0x9e3779b97f4a7c15L));
        double width = 14 + 7 * unit(hash);
        double center = column * (double) CELL_WIDTH + CELL_WIDTH / 2.0 + 128 * (2 * unit(mix(hash)) - 1);
        double amplitude = 32 + 24 * unit(mix(hash ^ 0x15ab4L));
        double phase = unit(mix(hash ^ 0x378ceL)) * Math.PI * 2;
        Point[] points = new Point[9];
        for (int i = 0; i < points.length; i++) {
            double u = center + amplitude * Math.sin(phase + i * Math.PI / 4);
            double v = row * (double) CELL_LENGTH + 256 + i * 128;
            points[i] = world(u, v);
        }
        boolean active = unit(mix(hash ^ 0x6c19dL)) < .8;
        // Reject the whole route through high terrain instead of narrowing the
        // water or cutting a steep gorge. Test both banks as well as the bed.
        for (int i = 1; active && i < points.length; i++) {
            Point a = points[i - 1], b = points[i];
            double dx = b.x - a.x, dz = b.z - a.z, length = Math.hypot(dx, dz);
            for (int step = 0; active && step <= 4; step++) {
                double t = step / 4.0;
                for (int side = -1; side <= 1; side++) {
                    double x = a.x + t * dx - side * 48 * dz / length;
                    double z = a.z + t * dz + side * 48 * dx / length;
                    if (landHeight.applyAsDouble(x, z) > MAX_ROUTE_HEIGHT) { active = false; break; }
                }
            }
        }
        return new River(points, width, active);
    }

    private Point world(double x, double z) {
        x += offsetX;
        z += offsetZ;
        return new Point(x * cosine - z * sine, x * sine + z * cosine);
    }

    public static double distance(double x, double z, River river) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < river.points.length; i++) {
            Point a = river.points[i - 1], b = river.points[i];
            double dx = b.x - a.x, dz = b.z - a.z;
            double t = Math.max(0, Math.min(1, ((x - a.x) * dx + (z - a.z) * dz) / (dx * dx + dz * dz)));
            best = Math.min(best, Math.hypot(x - a.x - t * dx, z - a.z - t * dz));
        }
        return best;
    }

    private static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
