package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

/** Exact interpolated surface elevation shared by ocean fluids, cover protection and cave treatment. */
public final class SnowyOceanFloor {
    public static final int COVER_BLOCKS = 4;
    private static final java.util.Map<RandomState, java.util.concurrent.ConcurrentHashMap<Long, Integer>> FLOORS = new java.util.WeakHashMap<>();

    private SnowyOceanFloor() { }

    public static int floor(RandomState random, int x, int z) {
        java.util.concurrent.ConcurrentHashMap<Long, Integer> cache;
        synchronized (FLOORS) { cache = FLOORS.computeIfAbsent(random, ignored -> new java.util.concurrent.ConcurrentHashMap<>()); }
        if (cache.size() > 8192) cache.clear();
        return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), ignored -> floor(random.router().ridges(), x, z));
    }

    public static int floor(DensityFunction ridges, int x, int z) {
        int bx = Math.floorDiv(x, 4) * 4, bz = Math.floorDiv(z, 4) * 4;
        double tx = (x - bx) / 4.0, tz = (z - bz) / 4.0;
        double a = height(ridges, bx, bz), b = height(ridges, bx + 4, bz);
        double c = height(ridges, bx, bz + 4), d = height(ridges, bx + 4, bz + 4);
        double height = (a + tx * (b - a)) + tz * ((c + tx * (d - c)) - (a + tx * (b - a)));
        return (int) Math.ceil(height + .5) - 1;
    }

    private static double height(DensityFunction ridges, int x, int z) {
        return SnowyTerrainProfile.height(ridges.compute(new DensityFunction.SinglePointContext(x, 0, z)));
    }

    public static boolean submerged(int floor) { return floor < MountainLakeOffset.SEA_LEVEL - 1; }

    /** Keep the low dry bank solid where an interpolated cave could open beside surface water. */
    public static int protectedFrom(int floor) {
        if (submerged(floor)) return floor - COVER_BLOCKS + 1;
        if (floor <= MountainLakeOffset.SEA_LEVEL + 2)
            return Math.min(floor - 2, MountainLakeOffset.SEA_LEVEL - COVER_BLOCKS);
        return floor - 2;
    }

    public static void restoreCoverAndWater(ChunkAccess chunk, RandomState random, BlockState rock) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int floor = floor(random, x, z);
                if (floor > MountainLakeOffset.SEA_LEVEL + 2) continue;
                for (int y = protectedFrom(floor); y <= floor; y++) {
                    pos.set(x, y, z);
                    var state = chunk.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) chunk.setBlockState(pos, rock, false);
                }
                if (!submerged(floor)) continue;
                for (int y = floor + 1; y < MountainLakeOffset.SEA_LEVEL; y++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                }
            }
        }
    }
}
