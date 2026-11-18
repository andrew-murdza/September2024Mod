package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntFunction;

/** Classify face-connected vertical air runs before surface materials, ores and structures are placed. */
public final class SnowyCavePockets {
    public static final int MIN_COLUMNS = 400;
    private static final Map<RandomState, Map<Long, List<Run>>> WORLDS = new WeakHashMap<>();
    private static final int[][] NEIGHBORS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private record Run(int x, int z, int bottom, int top, boolean open) { }
    private interface Columns { List<Run> at(int x, int z); }
    private interface Fill { void block(int x, int y, int z); }

    private SnowyCavePockets() { }

    /** A bounded cache contains original geometry only, so another chunk's cleanup cannot shrink a large cave. */
    public static void remove(NoiseBasedChunkGenerator generator, ChunkAccess chunk, RandomState random, BlockState rock) {
        int min = chunk.getMinBuildHeight(), max = chunk.getMaxBuildHeight();
        var origin = chunk.getPos();
        Map<Long, List<Run>> shared;
        synchronized (WORLDS) {
            shared = WORLDS.computeIfAbsent(random, ignored -> new LinkedHashMap<>(1024, .75f, true));
        }
        Map<Long, List<Run>> local = new HashMap<>();
        var pos = new BlockPos.MutableBlockPos();
        for (int z = origin.getMinBlockZ(); z <= origin.getMaxBlockZ(); z++) {
            for (int x = origin.getMinBlockX(); x <= origin.getMaxBlockX(); x++) {
                int bx = x, bz = z;
                local.put(ChunkPos.asLong(x, z), runs(x, z, min, max, y -> chunk.getBlockState(pos.set(bx, y, bz))));
            }
        }
        synchronized (shared) {
            shared.putAll(local);
            var iterator = shared.keySet().iterator();
            while (shared.size() > 32768) { iterator.next(); iterator.remove(); }
        }
        var supports = SnowyCaveColumns.supports(random);
        Columns columns = (x, z) -> local.computeIfAbsent(ChunkPos.asLong(x, z), key -> {
            synchronized (shared) {
                var existing = shared.get(key);
                if (existing != null) return existing;
            }
            // Noise generation has no completed neighbors guarantee. Sample their
            // unmodified base columns without loading chunks or waiting on other tasks.
            var column = generator.getBaseColumn(x, z, chunk, random);
            var states = new BlockState[max - min];
            for (int y = min; y < max; y++) states[y - min] = column.getBlock(y);
            int floor = SnowyOceanFloor.floor(random, x, z);
            if (floor <= MountainLakeOffset.SEA_LEVEL + 2) {
                for (int y = SnowyOceanFloor.protectedFrom(floor); y <= floor; y++)
                    if (states[y - min].isAir() || !states[y - min].getFluidState().isEmpty()) states[y - min] = rock;
                for (int y = floor + 1; y < MountainLakeOffset.SEA_LEVEL; y++)
                    if (states[y - min].isAir()) states[y - min] = Blocks.WATER.defaultBlockState();
            }
            var finished = SnowyCaveColumns.finishColumn(states, min, floor, supports.clearance(x, z), rock);
            var result = runs(x, z, min, max, y -> finished[y - min]);
            synchronized (shared) { shared.put(key, result); }
            return result;
        });
        remove(origin.getMinBlockX(), origin.getMinBlockZ(), columns,
                (x, y, z) -> chunk.setBlockState(pos.set(x, y, z), rock, false));
    }

    /** Identical classifier for a finite, already-materialized world or test fixture. */
    public static int remove(WorldGenLevel level, int minX, int minZ) {
        Map<Long, List<Run>> cache = new HashMap<>();
        var pos = new BlockPos.MutableBlockPos();
        Columns columns = (x, z) -> cache.computeIfAbsent(ChunkPos.asLong(x, z), key -> {
            if (level.getChunk(x >> 4, z >> 4, ChunkStatus.NOISE, false) == null) return null;
            return runs(x, z, level.getMinBuildHeight(), level.getMaxBuildHeight(), y -> level.getBlockState(pos.set(x, y, z)));
        });
        return remove(minX, minZ, columns, (x, y, z) -> level.setBlock(pos.set(x, y, z), Blocks.STONE.defaultBlockState(), 2));
    }

    private static List<Run> runs(int x, int z, int min, int max, IntFunction<BlockState> blocks) {
        var result = new ArrayList<Run>();
        for (int y = min; y < max; y++) {
            var state = blocks.apply(y);
            if (!state.isAir()) continue;
            int bottom = y;
            boolean authored = state.is(Blocks.CAVE_AIR);
            while (y + 1 < max && (state = blocks.apply(y + 1)).isAir()) {
                authored |= state.is(Blocks.CAVE_AIR);
                y++;
            }
            result.add(new Run(x, z, bottom, y, authored || bottom == min || y == max - 1));
        }
        return result;
    }

    private static int remove(int minX, int minZ, Columns columns, Fill fill) {
        Set<Run> inspected = new HashSet<>();
        int filled = 0;
        for (int z = minZ; z < minZ + 16; z++) for (int x = minX; x < minX + 16; x++) {
            var owned = columns.at(x, z);
            if (owned == null) continue;
            for (var start : owned) {
                if (start.open || inspected.contains(start)) continue;
                var pending = new ArrayDeque<Run>();
                var seen = new HashSet<Run>();
                var footprint = new HashSet<Long>();
                seen.add(start);
                pending.add(start);
                boolean small = true;
                while (!pending.isEmpty() && small) {
                    Run current = pending.remove();
                    footprint.add(ChunkPos.asLong(current.x, current.z));
                    if (current.open || inspected.contains(current) || footprint.size() >= MIN_COLUMNS) {
                        small = false;
                        break;
                    }
                    for (int[] delta : NEIGHBORS) {
                        var neighbors = columns.at(current.x + delta[0], current.z + delta[1]);
                        if (neighbors == null) { small = false; break; }
                        for (Run neighbor : neighbors) {
                            if (neighbor.bottom <= current.top && neighbor.top >= current.bottom && seen.add(neighbor))
                                pending.add(neighbor);
                        }
                    }
                }
                inspected.addAll(seen);
                if (!small) continue;
                for (Run run : seen) {
                    if (run.x < minX || run.x >= minX + 16 || run.z < minZ || run.z >= minZ + 16) continue;
                    for (int y = run.bottom; y <= run.top; y++) { fill.block(run.x, y, run.z); filled++; }
                }
            }
        }
        return filled;
    }
}
