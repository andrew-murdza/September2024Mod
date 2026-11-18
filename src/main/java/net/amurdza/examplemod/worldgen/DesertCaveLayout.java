package net.amurdza.examplemod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The two terrace flanks share T with the surface; only the underground descent bends. */
public final class DesertCaveLayout {
    public static final int ENTRANCE_START = 72;
    public static final int FIRST_STATION = -84;
    public static final int MAX_STATION = 168;
    public static final double CAVERN_THRESHOLD = .25;
    public static final double ZERO_DISTANCE = DesertBandLayout.resizedDistance(DesertBandLayout.originalDistance(0));
    private final DimensionTerrain terrain;
    private final DesertBands bands;
    private final Map<Long, Route> routes = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> cutColumns = new ConcurrentHashMap<>();

    public DesertCaveLayout(DimensionTerrain terrain) {
        this.terrain = terrain;
        DesertBands[] found = {null};
        terrain.land().mapAll(function -> {
            if (function instanceof DesertBands value) found[0] = value;
            return function;
        });
        bands = found[0];
    }

    public double bend(double x) {
        if (bands == null) return 0; // Detached constant-field fixtures have no seeded terrace displacement.
        int lower = Mth.floor(x);
        return Mth.lerp(x - lower, bands.bend(lower), bands.bend(lower + 1));
    }

    public double bankCoordinate(int stripe) {
        int terrace = Math.floorDiv(stripe, 2);
        return terrace * DesertBands.PERIOD + (Math.floorMod(stripe, 2) == 0
                ? ZERO_DISTANCE : DesertBands.PERIOD - ZERO_DISTANCE);
    }

    public double bankZ(double x, int stripe) {
        double z = bankCoordinate(stripe) - bend(x);
        // Keep the descending tunnel's independent seeded curvature rather
        // than making it a perfectly straight contour-following slot.
        return z - 18 * Math.tanh(terrain.rampDetail().compute(point(x, z)));
    }

    public Route route(int cycle, int stripe) {
        if (routes.size() > 2048) routes.clear();
        long key = ((long) cycle << 32) ^ (stripe & 0xffffffffL);
        return routes.computeIfAbsent(key, ignored -> create(cycle, stripe));
    }

    private Route create(int cycle, int stripe) {
        double endX = cycle * 616.0 + 308;
        for (int i = 0; i < 4; i++) endX = cycle * 616.0 + 308
                - 32 * Math.tanh(terrain.rampBasin().compute(point(endX, bankZ(endX, stripe))));
        double[] xs = new double[MAX_STATION - FIRST_STATION + 1];
        double[] zs = new double[xs.length];
        for (int station = FIRST_STATION; station <= ENTRANCE_START; station++) {
            double x = endX + station - ENTRANCE_START;
            xs[station - FIRST_STATION] = x;
            double center = bankCoordinate(stripe) - bend(x);
            double bendWeight = Mth.clamp((ENTRANCE_START - station) / 24.0, 0, 1);
            zs[station - FIRST_STATION] = center + (bankZ(x, stripe) - center) * bendWeight;
        }
        double x = xs[ENTRANCE_START - FIRST_STATION], z = zs[ENTRANCE_START - FIRST_STATION];
        double derivative = (bend(x + 2) - bend(x - 2)) / 4;
        double side = Math.floorMod(stripe, 2) == 0 ? -1 : 1;
        double norm = Math.sqrt(1 + derivative * derivative);
        double dx = side * derivative / norm, dz = side / norm;
        for (int station = ENTRANCE_START + 1; station <= MAX_STATION; station++) {
            // A single straight ray follows the local negative gradient of T.
            // There is no independent curl and no return toward the terrace.
            xs[station - FIRST_STATION] = x + (station - ENTRANCE_START) * dx;
            zs[station - FIRST_STATION] = z + (station - ENTRANCE_START) * dz;
        }
        int sandDistance = 1;
        while (sandDistance < MAX_STATION - ENTRANCE_START && terrain.land().compute(point(
                x + sandDistance * dx, z + sandDistance * dz)) > -.24) sandDistance++;
        // Meet the middle of the dry sand band, not the lake or terracotta.
        // Steeper horizontal T gradients need a slightly higher upper landing;
        // spread that difference over sixty underground steps, never a cliff.
        double entranceFloor = Mth.clamp(surface(x + sandDistance * dx, z + sandDistance * dz)
                + .5 - sandDistance * .5, 36, Math.min(48, surface(x, z) - 10.5));
        int end = MAX_STATION;
        for (int station = ENTRANCE_START + 1; station <= MAX_STATION; station++) {
            boolean clear = true;
            for (int across = -10; across <= 10; across += 2) {
                double px = xs[station - FIRST_STATION] + across * dz;
                double pz = zs[station - FIRST_STATION] - across * dx;
                double height = surface(px, pz);
                if (DimensionTerrain.isSurfaceWater(height, DesertHeight.SEA_LEVEL)
                        || floor(station, entranceFloor) < height + 1) { clear = false; break; }
            }
            if (clear) { end = station; break; }
        }
        return new Route(xs, zs, end, dx, dz, entranceFloor);
    }

    private double surface(double x, double z) {
        var point = point(x, z);
        return DesertHeight.height(terrain.land().compute(point), terrain.detail().compute(point));
    }

    private static DensityFunction.SinglePointContext point(double x, double z) {
        return new DensityFunction.SinglePointContext(Mth.floor(x), 0, Mth.floor(z));
    }

    public double density(int x, int y, int z) {
        if (y < -32 || y > 104) return 1;
        int firstCycle = Mth.floor((x - 432) / 616.0), lastCycle = Mth.floor((x + 64) / 616.0);
        int terrace = Mth.floor((z + bend(x)) / DesertBands.PERIOD);
        double value = 1;
        for (int cycle = firstCycle; cycle <= lastCycle; cycle++)
            for (int stripe = 2 * terrace - 1; stripe <= 2 * terrace + 2; stripe++) {
                Route route = route(cycle, stripe);
                value = Math.min(value, route.density(x, y, z));
                if (y < 0) value = Math.min(value, galleryMouth(route, x, y, z));
            }
        return value;
    }

    public boolean protectsWalkway(int x, int y, int z) {
        if (y < -29 || y > 80) return false;
        int firstCycle = Mth.floor((x - 432) / 616.0), lastCycle = Mth.floor((x + 64) / 616.0);
        int terrace = Mth.floor((z + bend(x)) / DesertBands.PERIOD);
        for (int cycle = firstCycle; cycle <= lastCycle; cycle++)
            for (int stripe = 2 * terrace - 1; stripe <= 2 * terrace + 2; stripe++)
                if (route(cycle, stripe).protectsWalkway(x, y, z)) return true;
        return false;
    }

    private double galleryMouth(Route route, int x, int y, int z) {
        double along = Math.abs(x - route.xAt(-76));
        if (along > 12 || Math.abs(z - route.zAt(-76)) > 84) return 1;
        double across = Math.abs(z - route.zAt(-76));
        double blend = Mth.clamp((across - 12) / 44, 0, 1);
        double floor = -26 + blend * DimensionTerrain.galleryRelief(terrain.rampDetail().compute(point(x, z)));
        return Math.max(Math.max(floor + .5 - y, y - floor - 9.5), Math.max(along - 7.5, across - 80)) / 4;
    }

    /** Stone belongs only to columns whose original surface was actually removed. */
    public boolean surfaceWasCut(int x, int z) {
        if (cutColumns.size() > 65536) cutColumns.clear();
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        return cutColumns.computeIfAbsent(key, ignored -> {
            int top = terrain.desertUncarvedSurfaceY(x, z);
            int cx = Math.floorDiv(x, 4) * 4, cy = Math.floorDiv(top, 4) * 4, cz = Math.floorDiv(z, 4) * 4;
            double[] values = new double[8];
            for (int iy = 0; iy < 2; iy++) for (int ix = 0; ix < 2; ix++) for (int iz = 0; iz < 2; iz++) {
                int px = cx + ix * 4, py = cy + iy * 4, pz = cz + iz * 4;
                values[iy * 4 + ix * 2 + iz] = Math.min((surface(px, pz) + .5 - py) / 8, density(px, py, pz));
            }
            double lower = Mth.lerp2((x - cx) / 4.0, (z - cz) / 4.0, values[0], values[2], values[1], values[3]);
            double upper = Mth.lerp2((x - cx) / 4.0, (z - cz) / 4.0, values[4], values[6], values[5], values[7]);
            return Mth.lerp((top - cy) / 4.0, lower, upper) <= 0;
        });
    }

    public static double floor(double station) {
        return floor(station, 42);
    }

    private static double floor(double station, double entranceFloor) {
        // The lateral cavern doorway ends before the first rising tread;
        // otherwise its level floor would scoop away that tread's support.
        if (station <= -60) return -26;
        if (station < 0) return Mth.lerp((station + 60) / 60, -26, 12);
        if (station <= 12) return 12;
        if (station <= ENTRANCE_START) return Mth.lerp((station - 12) / 60, 12, entranceFloor);
        return entranceFloor + (station - ENTRANCE_START) * .5;
    }

    public static final class Route {
        private final double[] xs, zs;
        private final int length;
        private final double dx, dz, minX, maxX, minZ, maxZ, entranceFloor;
        private Route(double[] xs, double[] zs, int length, double dx, double dz, double entranceFloor) {
            this.xs = xs; this.zs = zs; this.length = length; this.dx = dx; this.dz = dz;
            this.entranceFloor = entranceFloor;
            double ax = Double.POSITIVE_INFINITY, az = ax, bx = -ax, bz = -ax;
            for (int station = FIRST_STATION; station <= length; station++) {
                ax = Math.min(ax, xAt(station)); bx = Math.max(bx, xAt(station));
                az = Math.min(az, zAt(station)); bz = Math.max(bz, zAt(station));
            }
            minX = ax - 12; maxX = bx + 12; minZ = az - 12; maxZ = bz + 12;
        }
        public int length() { return length; }
        public double floorAt(double station) { return floor(station, entranceFloor); }
        public double xAt(double station) { return interpolate(xs, station); }
        public double zAt(double station) { return interpolate(zs, station); }
        public double directionX() { return dx; }
        public double directionZ() { return dz; }
        public double headingAt(double station) {
            return Math.atan2(xAt(station + .5) - xAt(station - .5), zAt(station + .5) - zAt(station - .5));
        }
        private static double interpolate(double[] values, double station) {
            double index = Mth.clamp(station - FIRST_STATION, 0, values.length - 1);
            int low = (int) index;
            return low == values.length - 1 ? values[low] : Mth.lerp(index - low, values[low], values[low + 1]);
        }
        public double density(int x, int y, int z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ || y < -32 || y > floorAt(length) + 14) return 1;
            double closest = Double.POSITIVE_INFINITY, station = 0;
            for (int s = FIRST_STATION; s < length; s++) {
                if (y < floorAt(s) - 5 || y > floorAt(s + 1) + 15) continue;
                double ax = xAt(s), az = zAt(s), dx = xAt(s + 1) - ax, dz = zAt(s + 1) - az;
                double t = Mth.clamp(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz), 0, 1);
                double ox = x - ax - t * dx, oz = z - az - t * dz, squared = ox * ox + oz * oz;
                if (squared < closest) { closest = squared; station = s + t; }
            }
            if (!Double.isFinite(closest)) return 1;
            double floor = floorAt(station);
            return Math.max(Math.max(floor + .5 - y, y - floor - 9.5), Math.sqrt(closest) - DimensionTerrain.FALLING_CAVE_HALF_WIDTH) / 4;
        }
        public boolean protectsWalkway(int x, int y, int z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) return false;
            for (int station = FIRST_STATION; station <= length; station++) {
                if (y < floorAt(station) - 2 || y > floorAt(station) + 10) continue;
                double dx = x - xAt(station), dz = z - zAt(station);
                if (dx * dx + dz * dz <= 6.25) return true;
            }
            return false;
        }
    }
}
