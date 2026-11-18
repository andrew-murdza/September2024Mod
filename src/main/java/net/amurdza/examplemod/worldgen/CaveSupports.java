package net.amurdza.examplemod.worldgen;

import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Isolated floor-to-ceiling pillars. Continuous mining banks are supplied by the staircase geometry. */
public final class CaveSupports {
    private static final int CELL_SIZE = 72;
    private static final double MAX_SHAPE_VARIATION = 1.4;
    private final long seed;
    private final DensityFunction rampBasin;
    private final Map<Long, Pillar> pillars = new ConcurrentHashMap<>();

    public CaveSupports(long seed) {
        this(seed, null);
    }

    public CaveSupports(long seed, DensityFunction rampBasin) {
        this.seed = seed;
        this.rampBasin = rampBasin;
    }

    public double density(int x, int z, double land, double detail) {
        int cellX = Math.floorDiv(x, CELL_SIZE);
        int cellZ = Math.floorDiv(z, CELL_SIZE);
        double pillar = -CELL_SIZE;
        int reach = rampBasin == null ? 1 : 2;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Pillar support = pillar(cellX + dx, cellZ + dz);
                double radius = support.radius + detail * 2;
                double offsetX = x - support.x;
                double offsetZ = z - support.z;
                double density = radius - Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
                if (rampBasin != null && density + MAX_SHAPE_VARIATION > pillar) {
                    // Shared Desert/Nether pillars keep a continuous solid core,
                    // but their outlines no longer repeat perfect cylinders.
                    // Snowy's separate support field retains its existing shape.
                    double angle = Math.atan2(offsetZ, offsetX);
                    density += .9 * Math.sin(angle * 3 + support.phase)
                            + .5 * Math.sin(angle * 5 + support.secondaryPhase);
                }
                pillar = Math.max(pillar, density);
            }
        }
        return pillar;
    }

    private Pillar pillar(int cellX, int cellZ) {
        if (pillars.size() > 8192) pillars.clear();
        long key = ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
        return pillars.computeIfAbsent(key, ignored -> {
            long hash = TropicalLayout.mix(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L);
            double x = (cellX + .2 + unit(hash) * .6) * CELL_SIZE;
            double z = (cellZ + .2 + unit(Long.rotateLeft(hash, 21)) * .6) * CELL_SIZE;
            double radius = 7 + unit(Long.rotateLeft(hash, 42)) * 4;
            double phase = unit(Long.rotateLeft(hash, 7)) * Math.PI * 2;
            double secondaryPhase = unit(Long.rotateLeft(hash, 37)) * Math.PI * 2;
            if (rampBasin != null && !clearsBridge(x, z, radius)) {
                // Keep every seeded pillar. Move only obstructing centers to
                // the closest clear start, identically at every cave altitude.
                int preferred = (hash & 1) == 0 ? 1 : -1;
                for (int distance = 2; distance <= 80; distance += 2) {
                    for (int direction : new int[]{preferred, -preferred}) {
                        double shifted = x + direction * distance;
                        if (clearsBridge(shifted, z, radius)) return new Pillar(shifted, z, radius, phase, secondaryPhase);
                    }
                }
            }
            return new Pillar(x, z, radius, phase, secondaryPhase);
        });
    }

    private boolean clearsBridge(double x, double z, double radius) {
        // Cover the pillar's radius, noise relief and density interpolation.
        double margin = radius + MAX_SHAPE_VARIATION + 7;
        for (double dx : new double[]{-margin, 0, margin}) for (double dz : new double[]{-margin, 0, margin}) {
            var point = new DensityFunction.SinglePointContext((int) Math.floor(x + dx), 0, (int) Math.floor(z + dz));
            if (DimensionTerrain.netherRampFloor(point.blockX(), rampBasin.compute(point)) <= 26) return false;
        }
        return true;
    }

    private record Pillar(double x, double z, double radius, double phase, double secondaryPhase) { }

    private static double unit(long value) {
        return (value >>> 40) / (double) (1 << 24);
    }
}
