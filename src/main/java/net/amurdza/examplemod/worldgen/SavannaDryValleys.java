package net.amurdza.examplemod.worldgen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/**
 * Elevated, winding valley lines interrupt the lake watersheds. Noise determines
 * their coordinates, bends and local metric, never the sampled column height.
 * Distance to a line has unit gradient away from its medial axes; the metric's
 * singular values therefore bound the resulting Euclidean slope by 1/24..1/16.
 */
public final class SavannaDryValleys {
    public static final int REGION_SIZE = 654;
    public static final double BASE_HEIGHT = 63.25;
    private static final int SEGMENTS = 48;
    private final DoubleBinaryOperator noise;
    private final Map<Long, Route[]> regions = new ConcurrentHashMap<>();

    public SavannaDryValleys(DoubleBinaryOperator noise) {
        this.noise = noise;
    }

    /** Lower an existing dry surface only where a nearby elevated valley wins. */
    public double height(double x, double z, double existingHeight) {
        if (existingHeight <= BASE_HEIGHT) return existingHeight;
        int rx = (int) Math.floor(x / REGION_SIZE), rz = (int) Math.floor(z / REGION_SIZE);
        double rise = existingHeight - BASE_HEIGHT;
        // Endpoint jitter is at most .2 cells and Bezier controls bend at most
        // .24 cells. An omitted anchor's route is therefore at least 1.06 cells
        // (693 blocks) away. Even the conservative lake-height upper bound can
        // only let valleys affect points within 569 blocks. This is a complete
        // search, including at region boundaries; boxes reject distant routes.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (Route route : routes(rx + dx, rz + dz)) {
                if (route.lowerBound(x, z) < rise) rise = Math.min(rise, route.rise(x, z));
            }
        }
        return Math.min(existingHeight, BASE_HEIGHT + rise);
    }

    public Route[] routes(int regionX, int regionZ) {
        if (regions.size() > 4096) regions.clear();
        return regions.computeIfAbsent((long) regionX << 32 ^ regionZ & 0xffffffffL, ignored -> {
            double x = (regionX + .5) * REGION_SIZE, z = (regionZ + .5) * REGION_SIZE;
            Route east = route(regionX, regionZ, regionX + 1, regionZ, 13);
            // Continuous winding trunks with occasional branches, rather than
            // another lake-centered field or a regular square drainage mesh.
            return sample(x, z, 15) > 0
                    ? new Route[]{east, route(regionX, regionZ, regionX, regionZ + 1, 16)}
                    : new Route[]{east};
        });
    }

    private Route route(int ax, int az, int bx, int bz, int channel) {
        double[] a = node(ax, az), b = node(bx, bz);
        double dx = b[0] - a[0], dz = b[1] - a[1], length = Math.hypot(dx, dz);
        double bendA = .24 * REGION_SIZE * sample(a[0], a[1], channel);
        double bendB = .24 * REGION_SIZE * sample(b[0], b[1], channel + 1);
        double c1x = a[0] + dx / 3 - dz / length * bendA;
        double c1z = a[1] + dz / 3 + dx / length * bendA;
        double c2x = b[0] - dx / 3 - dz / length * bendB;
        double c2z = b[1] - dz / 3 + dx / length * bendB;
        double[] xs = new double[SEGMENTS + 1], zs = new double[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = (double) i / SEGMENTS, s = 1 - t;
            xs[i] = s * s * s * a[0] + 3 * s * s * t * c1x + 3 * s * t * t * c2x + t * t * t * b[0];
            zs[i] = s * s * s * a[1] + 3 * s * s * t * c1z + 3 * s * t * t * c2z + t * t * t * b[1];
        }
        return new Route(xs, zs, Math.PI * sample(a[0], a[1], channel + 4));
    }

    private double[] node(int rx, int rz) {
        double x = (rx + .5) * REGION_SIZE, z = (rz + .5) * REGION_SIZE;
        return new double[]{x + .2 * REGION_SIZE * sample(x, z, 11), z + .2 * REGION_SIZE * sample(x, z, 12)};
    }

    private double sample(double x, double z, int channel) {
        return Math.tanh(2 * noise.applyAsDouble(Math.rint(x) + channel * 7919, Math.rint(z) - channel * 6151));
    }

    public static final class Route {
        private final double[] xs, zs, metricX, metricZ;
        private final double minX, maxX, minZ, maxZ, originX, originZ, cosine, sine;

        private Route(double[] xs, double[] zs, double angle) {
            this.xs = xs; this.zs = zs;
            originX = xs[0]; originZ = zs[0];
            cosine = Math.cos(angle); sine = Math.sin(angle);
            metricX = new double[xs.length]; metricZ = new double[xs.length];
            double loX = xs[0], hiX = xs[0], loZ = zs[0], hiZ = zs[0];
            for (int i = 0; i < xs.length; i++) {
                loX = Math.min(loX, xs[i]); hiX = Math.max(hiX, xs[i]);
                loZ = Math.min(loZ, zs[i]); hiZ = Math.max(hiZ, zs[i]);
                double x = xs[i] - originX, z = zs[i] - originZ;
                metricX[i] = (cosine * x + sine * z) * SavannaHeight.MAX_LAND_SLOPE;
                metricZ[i] = (-sine * x + cosine * z) * SavannaHeight.MIN_LAND_SLOPE;
            }
            minX = loX; maxX = hiX; minZ = loZ; maxZ = hiZ;
        }

        public int vertices() { return xs.length; }
        public double vertexX(int index) { return xs[index]; }
        public double vertexZ(int index) { return zs[index]; }

        private double lowerBound(double x, double z) {
            return Math.hypot(Math.max(0, Math.max(minX - x, x - maxX)),
                    Math.max(0, Math.max(minZ - z, z - maxZ))) * SavannaHeight.MIN_LAND_SLOPE;
        }

        public double rise(double x, double z) {
            x -= originX; z -= originZ;
            double px = (cosine * x + sine * z) * SavannaHeight.MAX_LAND_SLOPE;
            double pz = (-sine * x + cosine * z) * SavannaHeight.MIN_LAND_SLOPE;
            double nearest = Double.POSITIVE_INFINITY;
            for (int i = 1; i < metricX.length; i++) {
                double dx = metricX[i] - metricX[i - 1], dz = metricZ[i] - metricZ[i - 1];
                double t = Math.max(0, Math.min(1, ((px - metricX[i - 1]) * dx + (pz - metricZ[i - 1]) * dz) / (dx * dx + dz * dz)));
                double ex = px - metricX[i - 1] - t * dx, ez = pz - metricZ[i - 1] - t * dz;
                nearest = Math.min(nearest, ex * ex + ez * ez);
            }
            return Math.sqrt(nearest);
        }
    }
}
