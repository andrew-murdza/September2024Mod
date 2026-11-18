package net.amurdza.examplemod.worldgen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/**
 * Noise warps explicit shore coordinates; distance, rather than noise amplitude,
 * determines height. Each lake uses its own rotated land metric whose singular
 * values are 1/20 and 1/18. The minimum of these distance fields retains those
 * Euclidean gradient bounds wherever the closest shore/metric is unique.
 * Every dry point has a descending path to a shoreline: independent height
 * noise or elevated valley fields would introduce dry local minima again.
 */
public final class SavannaLakeBasins {
    public static final int REGION_SIZE = 384;
    public static final double LAKE_DIAMETER = 160;
    public static final double MAX_BOUNDING_RADIUS = 96;
    public static final double CENTER_JITTER_FRACTION = .24;
    private static final int VERTICES = 96;
    private static final int CACHE_LIMIT = 65536;
    private final DoubleBinaryOperator noise;
    private final Map<Long, Lake> lakes = new ConcurrentHashMap<>();
    private final Map<Long, Double> columns = new ConcurrentHashMap<>();
    private final double cosine, sine;

    public SavannaLakeBasins(DoubleBinaryOperator noise) {
        this.noise = noise;
        double angle = Math.PI * noise.applyAsDouble(173, -419);
        cosine = Math.cos(angle);
        sine = Math.sin(angle);
    }

    public double height(int x, int z) {
        if (columns.size() > CACHE_LIMIT) columns.clear();
        return columns.computeIfAbsent(key(x, z), ignored -> height((double) x, z));
    }

    public double height(double x, double z) {
        double u = cosine * x + sine * z, v = -sine * x + cosine * z;
        int rx = (int) Math.floor(u / REGION_SIZE), rz = (int) Math.floor(v / REGION_SIZE);
        double rise = Double.POSITIVE_INFINITY;
        // Two rings suffice even for the slowest metric and maximum site jitter.
        // Bound each candidate before evaluating its shoreline segments.
        for (int ring = 0; ring <= 2; ring++) {
            for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                Lake lake = lake(rx + dx, rz + dz);
                double radial = Math.hypot(u - lake.x, v - lake.z);
                if (radial <= lake.boundingRadius) {
                    double shore = lake.distance(u, v);
                    if (shore <= 0) return SavannaHeight.height(shore);
                }
                if ((radial - lake.boundingRadius) * SavannaHeight.MIN_LAND_SLOPE >= rise) continue;
                rise = Math.min(rise, lake.landRise(u, v));
            }
        }
        // The cell's own center is at most sqrt(2) * .74 * 384 blocks away.
        // Every outline encloses a radius of at least .25 * 160 blocks, so
        // even the steepest metric stays below Y82.5 without clipping hilltops.
        return SavannaHeight.WATER_EDGE_HEIGHT + rise;
    }

    /** Physical Euclidean shore distance, independent of the dry-land metric. */
    public double shoreDistance(double x, double z) {
        double u = cosine * x + sine * z, v = -sine * x + cosine * z;
        int rx = (int) Math.floor(u / REGION_SIZE), rz = (int) Math.floor(v / REGION_SIZE);
        double nearest = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Lake lake = lake(rx + dx, rz + dz);
            if (Math.hypot(u - lake.x, v - lake.z) - lake.boundingRadius < nearest) {
                nearest = Math.min(nearest, lake.distance(u, v));
            }
        }
        return nearest;
    }

    public Lake lake(int regionX, int regionZ) {
        if (lakes.size() > 4096) lakes.clear();
        return lakes.computeIfAbsent(key(regionX, regionZ), ignored -> {
            double x = (regionX + .5) * REGION_SIZE, z = (regionZ + .5) * REGION_SIZE;
            double[] xs = new double[VERTICES], zs = new double[VERTICES];
            double orientation = Math.PI * sample(x, z, 2);
            double c = Math.cos(orientation), s = Math.sin(orientation);
            double phase2 = Math.PI * sample(x, z, 3), phase3 = Math.PI * sample(x, z, 4);
            double phase5 = Math.PI * sample(x, z, 5), aspect = .75 + .1 * sample(x, z, 6);
            for (int i = 0; i < VERTICES; i++) {
                double theta = 2 * Math.PI * i / VERTICES;
                // A monotone angular warp and positive radial warp make an
                // irregular simple shoreline, with coves and unequal headlands.
                double angle = theta + .12 * Math.sin(2 * theta + phase2);
                double radius = 1 + .035 * Math.sin(2 * theta + phase2)
                        + .055 * Math.sin(3 * theta + phase3) + .01 * Math.sin(5 * theta + phase5);
                double px = radius * Math.cos(angle), pz = aspect * radius * Math.sin(angle);
                xs[i] = c * px - s * pz;
                zs[i] = s * px + c * pz;
            }
            double diameter = 0;
            for (int i = 0; i < VERTICES; i++) for (int j = i + 1; j < VERTICES; j++) {
                diameter = Math.max(diameter, Math.hypot(xs[i] - xs[j], zs[i] - zs[j]));
            }
            for (int i = 0; i < VERTICES; i++) { xs[i] *= LAKE_DIAMETER / diameter; zs[i] *= LAKE_DIAMETER / diameter; }
            return new Lake(x + CENTER_JITTER_FRACTION * REGION_SIZE * sample(x, z, 0),
                    z + CENTER_JITTER_FRACTION * REGION_SIZE * sample(x, z, 1), xs, zs,
                    Math.PI * sample(x, z, 7));
        });
    }

    private double sample(double x, double z, int channel) {
        return Math.tanh(4 * noise.applyAsDouble(Math.rint(x) + channel * 7919, Math.rint(z) - channel * 6151));
    }

    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xffffffffL; }

    /** Convert lake-local coordinates for previews and boundary tests. */
    public double worldX(double x, double z) { return cosine * x - sine * z; }
    public double worldZ(double x, double z) { return sine * x + cosine * z; }

    public static final class Lake {
        private final double x, z, boundingRadius, area, metricCosine, metricSine;
        private final double[] xs, zs, metricX, metricZ;

        private Lake(double x, double z, double[] xs, double[] zs, double metricAngle) {
            this.x = x; this.z = z; this.xs = xs; this.zs = zs;
            metricCosine = Math.cos(metricAngle); metricSine = Math.sin(metricAngle);
            metricX = new double[xs.length]; metricZ = new double[xs.length];
            double radius = 0, doubleArea = 0;
            for (int i = 0; i < xs.length; i++) {
                int next = (i + 1) % xs.length;
                radius = Math.max(radius, Math.hypot(xs[i], zs[i]));
                doubleArea += xs[i] * zs[next] - xs[next] * zs[i];
                metricX[i] = (metricCosine * xs[i] + metricSine * zs[i]) * SavannaHeight.MAX_LAND_SLOPE;
                metricZ[i] = (-metricSine * xs[i] + metricCosine * zs[i]) * SavannaHeight.MIN_LAND_SLOPE;
            }
            boundingRadius = radius; area = Math.abs(doubleArea) / 2;
        }

        public double x() { return x; }
        public double z() { return z; }
        public double boundingRadius() { return boundingRadius; }
        public double area() { return area; }
        public int vertices() { return xs.length; }
        public double vertexX(int i) { return x + xs[i]; }
        public double vertexZ(int i) { return z + zs[i]; }

        public double distance(double px, double pz) {
            px -= x; pz -= z;
            boolean inside = false;
            for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
                if ((zs[i] > pz) != (zs[j] > pz)
                        && px < (xs[j] - xs[i]) * (pz - zs[i]) / (zs[j] - zs[i]) + xs[i]) inside = !inside;
            }
            return (inside ? -1 : 1) * segmentDistance(px, pz, xs, zs);
        }

        public double landRise(double px, double pz) {
            px -= x; pz -= z;
            double u = (metricCosine * px + metricSine * pz) * SavannaHeight.MAX_LAND_SLOPE;
            double v = (-metricSine * px + metricCosine * pz) * SavannaHeight.MIN_LAND_SLOPE;
            return segmentDistance(u, v, metricX, metricZ);
        }

        private static double segmentDistance(double px, double pz, double[] xs, double[] zs) {
            double nearest = Double.POSITIVE_INFINITY;
            for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
                double dx = xs[i] - xs[j], dz = zs[i] - zs[j];
                double t = Math.max(0, Math.min(1, ((px - xs[j]) * dx + (pz - zs[j]) * dz) / (dx * dx + dz * dz)));
                double ex = px - xs[j] - t * dx, ez = pz - zs[j] - t * dz;
                nearest = Math.min(nearest, ex * ex + ez * ez);
            }
            return Math.sqrt(nearest);
        }
    }
}
