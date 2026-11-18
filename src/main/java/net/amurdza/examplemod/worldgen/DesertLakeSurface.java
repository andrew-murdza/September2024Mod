package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Extend lake source water across connected, exposed sand beds after density interpolation. */
public final class DesertLakeSurface {
    private enum Surface { BANK, WATER, GAP }
    private static final int LINE = DesertHeight.SEA_LEVEL - 1;

    private DesertLakeSurface() { }

    public static void fillWater(ChunkAccess chunk, RandomState random, NoiseBasedChunkGenerator generator) {
        var surfaces = new HashMap<Long, Surface>();
        var connected = new HashMap<Long, Boolean>();
        var pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int bed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
                if (surface(bed, chunk.getBlockState(pos.set(x, LINE, z))) != Surface.GAP) continue;
                if (!reachesWater(x, z, chunk, random, generator, surfaces, connected)) continue;
                for (int y = bed + 1; y <= LINE; y++) {
                    if (chunk.getBlockState(pos.set(x, y, z)).isAir())
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                }
            }
        }
    }

    private static boolean reachesWater(int x, int z, ChunkAccess chunk, RandomState random,
                                        NoiseBasedChunkGenerator generator, Map<Long, Surface> surfaces,
                                        Map<Long, Boolean> connected) {
        long start = ChunkPos.asLong(x, z);
        var known = connected.get(start);
        if (known != null) return known;
        var pending = new ArrayDeque<Long>();
        var visited = new HashSet<Long>();
        pending.add(start);
        visited.add(start);
        boolean reaches = false;
        while (!pending.isEmpty() && !reaches) {
            long key = pending.remove();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nx = ChunkPos.getX(key) + direction.getStepX(), nz = ChunkPos.getZ(key) + direction.getStepZ();
                long next = ChunkPos.asLong(nx, nz);
                if (visited.contains(next)) continue;
                var prior = connected.get(next);
                if (prior != null) {
                    if (prior) { reaches = true; break; }
                    continue;
                }
                var terrain = surfaces.computeIfAbsent(next, ignored -> {
                    if (chunk.getPos().x == nx >> 4 && chunk.getPos().z == nz >> 4) {
                        return surface(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, nx & 15, nz & 15),
                                chunk.getBlockState(new BlockPos(nx, LINE, nz)));
                    }
                    // Read immutable terrain across a chunk edge without loading any neighbors.
                    var column = generator.getBaseColumn(nx, nz, chunk, random);
                    int bed = chunk.getMaxBuildHeight() - 1;
                    while (bed >= DesertHeight.BOTTOM - 1 && !column.getBlock(bed).blocksMotion()) bed--;
                    return surface(bed, column.getBlock(LINE));
                });
                if (terrain == Surface.WATER) { reaches = true; break; }
                if (terrain == Surface.GAP) { visited.add(next); pending.add(next); }
            }
        }
        for (long key : visited) connected.put(key, reaches);
        return reaches;
    }

    private static Surface surface(int bed, BlockState atWaterline) {
        if (bed < DesertHeight.BOTTOM - 1 || bed >= LINE) return Surface.BANK;
        if (atWaterline.getFluidState().is(FluidTags.WATER)) return Surface.WATER;
        return atWaterline.isAir() ? Surface.GAP : Surface.BANK;
    }
}
