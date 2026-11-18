package net.amurdza.examplemod.worldgen;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/** Keep water only when its already-carved connected component reaches a surface body. */
public final class PlainsSurfaceWater {
    private static final Map<ServerLevel, Object> WORLD_LOCKS = new WeakHashMap<>();

    private PlainsSurfaceWater() { }

    public static int drainDisconnected(WorldGenLevel level, int minX, int minZ) {
        Object lock;
        synchronized (WORLD_LOCKS) {
            lock = WORLD_LOCKS.computeIfAbsent(level.getLevel(), ignored -> new Object());
        }
        // Keep proven paths across chunk seams until neighboring chunks decorate.
        // Serializing this pass also prevents a neighbor from draining a path while it is examined.
        synchronized (lock) {
            Map<Long, Boolean> readableChunks = new HashMap<>();
            Map<Long, Integer> roofs = new HashMap<>();
            int seaLevel = level.getLevel().getChunkSource().getGenerator().getSeaLevel();
            int ownerX = Math.floorDiv(minX, 16), ownerZ = Math.floorDiv(minZ, 16);
            Predicate<BlockPos> water = pos -> {
                if (pos.getY() <= 0 || pos.getY() >= seaLevel) return false;
                int cx = Math.floorDiv(pos.getX(), 16), cz = Math.floorDiv(pos.getZ(), 16);
                boolean readable = readableChunks.computeIfAbsent(ChunkPos.asLong(cx, cz), ignored -> {
                    // A neighboring CARVERS chunk can still have its apparent
                    // connection sealed by structures or decoration. Only the
                    // current chunk may use its in-progress terrain snapshot.
                    ChunkStatus required = cx == ownerX && cz == ownerZ ? ChunkStatus.CARVERS : ChunkStatus.FEATURES;
                    return level.hasChunk(cx, cz) && level.getChunk(cx, cz, required, false) != null;
                });
                return readable && level.getFluidState(pos).is(FluidTags.WATER);
            };
            Predicate<BlockPos> sky = pos -> pos.getY() > roofs.computeIfAbsent(
                    ChunkPos.asLong(pos.getX(), pos.getZ()), ignored -> roofHeight(level, pos.getX(), pos.getZ()));
            LongSet handled = new LongOpenHashSet();
            int drained = 0;
            for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
                for (int y = 1; y < seaLevel; y++) {
                    BlockPos start = new BlockPos(x, y, z);
                    if (handled.contains(start.asLong()) || !water.test(start) || sky.test(start)
                            || knownConnection(level, start)) continue;
                    Search result = search(start, water, pos -> sky.test(pos) || knownConnection(level, pos));
                    handled.addAll(result.visited());
                    if (result.connected()) {
                        for (long packed : result.visited()) {
                            BlockPos pos = BlockPos.of(packed);
                            PlainsWaterProof.mark(level.getChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16)), pos);
                        }
                        continue;
                    }
                    for (long packed : result.visited()) {
                        BlockPos pos = BlockPos.of(packed);
                        if (pos.getX() >= minX && pos.getX() < minX + 16 && pos.getZ() >= minZ && pos.getZ() < minZ + 16) {
                            BlockState original = level.getBlockState(pos);
                            BlockState dry = drainedState(original);
                            if (dry != original && level.setBlock(pos, dry, Block.UPDATE_CLIENTS)) drained++;
                        }
                    }
                }
            }
            return drained;
        }
    }

    private static BlockState drainedState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        // Structural blocks must survive removal of their water. Preserve any
        // unfamiliar solid fluid-bearing block that has no standard dry state.
        return state.is(Blocks.WATER) || !state.blocksMotion() && !state.hasBlockEntity()
                ? Blocks.AIR.defaultBlockState() : state;
    }

    private static boolean knownConnection(WorldGenLevel level, BlockPos pos) {
        return PlainsWaterProof.contains(level.getChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16)), pos);
    }

    private static int roofHeight(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y > level.getMinBuildHeight(); y--) {
            pos.setY(y);
            var state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE)
                    || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.SNOW)) continue;
            if (state.getLightBlock(level, pos) >= 15) return y;
        }
        return level.getMinBuildHeight();
    }

    /** Finite graph search, also used by tests with carved synthetic cave layouts. */
    public static boolean reachesSurface(BlockPos start, Predicate<BlockPos> water, Predicate<BlockPos> surface) {
        return search(start, water, surface).connected();
    }

    private static Search search(BlockPos start, Predicate<BlockPos> water, Predicate<BlockPos> surface) {
        LongSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        if (!water.test(start)) return new Search(false, visited);
        visited.add(start.asLong());
        queue.enqueue(start.asLong());
        while (!queue.isEmpty()) {
            BlockPos pos = BlockPos.of(queue.dequeueLong());
            if (surface.test(pos)) return new Search(true, visited);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next.asLong()) && water.test(next)) {
                    visited.add(next.asLong());
                    queue.enqueue(next.asLong());
                }
            }
        }
        return new Search(false, visited);
    }

    private record Search(boolean connected, LongSet visited) { }
}
