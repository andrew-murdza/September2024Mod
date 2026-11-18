package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.registry.RetainedDragonFruit;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/** One 2x2 group of five-plant patches per 240-square desert tile. */
public final class DragonFruitPatches {
    public static final int TILE_SIZE = 240;
    public static final int CENTER_SIZE = 120;
    public static final int PLANTS_PER_PATCH = 5;
    private static final Map<RandomState, Map<Long, CompletableFuture<List<Patch>>>> PLANS = new WeakHashMap<>();

    private DragonFruitPatches() { }

    public static List<Patch> plan(WorldGenLevel level, int tileX, int tileZ) {
        var source = level.getLevel().getChunkSource();
        RandomState state = source.randomState();
        long key = ChunkPos.asLong(tileX, tileZ);
        CompletableFuture<List<Patch>> future;
        boolean compute;
        synchronized (PLANS) {
            Map<Long, CompletableFuture<List<Patch>>> cache = PLANS.computeIfAbsent(state, ignored -> new LinkedHashMap<>(64, .75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, CompletableFuture<List<Patch>>> eldest) {
                    return size() > 256 && eldest.getValue().isDone();
                }
            });
            future = cache.get(key);
            compute = future == null;
            if (compute) { future = new CompletableFuture<>(); cache.put(key, future); }
        }
        if (compute) {
            try {
                var generator = source.getGenerator();
                CropTerrainHeights heights = generator instanceof NoiseBasedChunkGenerator noise
                        ? new CropTerrainHeights(level, noise, state) : null;
                future.complete(plan(level.getSeed(), tileX, tileZ, (x, z) -> {
                    int y = heights == null ? generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state)
                            : heights.height(x, z);
                    // The authored surface rule uses sand through top Y80 and red sand through Y86.
                    // Sampling density avoids loading neighboring chunks during decoration.
                    return y >= generator.getSeaLevel() && y <= 87;
                }));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                synchronized (PLANS) { PLANS.get(state).remove(key, future); }
            }
        }
        return future.join();
    }

    public static List<Patch> plan(long seed, int tileX, int tileZ, BiPredicate<Integer, Integer> suitable) {
        long tileSeed = hash(seed ^ 0xd6a60fL, tileX, tileZ);
        List<ChunkPos> clusters = new ArrayList<>();
        int minChunkX = tileX * 15, minChunkZ = tileZ * 15;
        for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) clusters.add(new ChunkPos(minChunkX + x, minChunkZ + z));
        clusters.sort(Comparator.comparingLong(p -> hash(tileSeed, p.x, p.z)));
        Map<Long, Boolean> columns = new HashMap<>();
        BiPredicate<Integer, Integer> cached = (x, z) -> columns.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> suitable.test(x, z));
        // Exhaust every complete central square before allowing a fallback elsewhere in the tile.
        for (boolean central : new boolean[]{true, false}) {
            Map<Long, Patch> sites = new HashMap<>();
            for (ChunkPos cluster : clusters) {
                List<Patch> chosen = new ArrayList<>();
                for (int dx = 0; dx < 2 && chosen.size() == dx * 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        int cx = cluster.x + dx, cz = cluster.z + dz;
                        long key = ChunkPos.asLong(cx, cz);
                        if (!sites.containsKey(key)) sites.put(key, findSite(tileSeed, cx, cz, tileX, tileZ, central, cached));
                        Patch site = sites.get(key);
                        if (site == null) break;
                        chosen.add(site);
                    }
                }
                if (chosen.size() == 4) return List.copyOf(chosen);
            }
        }
        return List.of();
    }

    private static Patch findSite(long seed, int chunkX, int chunkZ, int tileX, int tileZ,
                                  boolean central, BiPredicate<Integer, Integer> suitable) {
        for (Patch patch : centers(seed, chunkX, chunkZ)) {
            if (central && !centralFootprint(patch, tileX, tileZ)) continue;
            int count = 0;
            for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
                if (suitable.test(patch.x() + dx, patch.z() + dz) && ++count == PLANTS_PER_PATCH) return patch;
            }
        }
        return null;
    }

    public static boolean centralFootprint(Patch patch, int tileX, int tileZ) {
        int x = tileX * TILE_SIZE, z = tileZ * TILE_SIZE;
        return patch.x() - 2 >= x + 60 && patch.x() + 3 < x + 180
                && patch.z() - 2 >= z + 60 && patch.z() + 3 < z + 180;
    }

    private static List<Patch> centers(long seed, int chunkX, int chunkZ) {
        List<Patch> result = new ArrayList<>();
        for (int x = 2; x <= 12; x++) for (int z = 2; z <= 12; z++) result.add(new Patch(chunkX * 16 + x, chunkZ * 16 + z));
        result.sort(Comparator.comparingLong(p -> hash(seed, p.x(), p.z())));
        return result;
    }

    public static int placeChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT)) return 0;
        int tileX = Math.floorDiv(chunkX, 15), tileZ = Math.floorDiv(chunkZ, 15);
        for (Patch patch : plan(level, tileX, tileZ)) {
            if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) return place(level, patch);
        }
        return 0;
    }

    public static int place(WorldGenLevel level, Patch patch) {
        int chunkX = Math.floorDiv(patch.x(), 16), chunkZ = Math.floorDiv(patch.z(), 16);
        Block plant = RetainedDragonFruit.WILD.get();
        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
            BlockPos top = surface(level, x, z);
            if (level.getBlockState(top).is(plant) || level.getBlockState(top.below()).is(plant)) return 0;
        }
        List<Patch> sites = centers(hash(level.getSeed(), chunkX, chunkZ), chunkX, chunkZ);
        sites.remove(patch);
        sites.add(0, patch);
        int tileX = Math.floorDiv(chunkX, 15), tileZ = Math.floorDiv(chunkZ, 15);
        sites.sort(Comparator.comparing((Patch p) -> !centralFootprint(p, tileX, tileZ)));
        for (Patch site : sites) if (placeAt(level, site)) return PLANTS_PER_PATCH;
        return 0;
    }

    private static boolean placeAt(WorldGenLevel level, Patch site) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) candidates.add(surface(level, site.x() + dx, site.z() + dz));
        RandomSource random = RandomSource.create(hash(level.getSeed(), site.x(), site.z()));
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos old = candidates.get(i); candidates.set(i, candidates.get(j)); candidates.set(j, old);
        }
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        int planted = 0;
        var plant = RetainedDragonFruit.WILD.get().defaultBlockState();
        for (BlockPos pos : candidates) {
            var soil = level.getBlockState(pos.below());
            var current = level.getBlockState(pos);
            boolean replaceable = current.isAir() || current.is(Blocks.DEAD_BUSH) || current.is(Blocks.GRASS) || current.is(Blocks.TALL_GRASS)
                    || current.is(net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get())
                    || current.is(net.amurdza.examplemod.registry.ModBlocks.DESERT_TALL_GRASS.get());
            if (!replaceable || !(soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND)) || !plant.canSurvive(level, pos)) continue;
            var upper = level.getBlockState(pos.above());
            if (current.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && current.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    && upper.is(current.getBlock()) && upper.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                buffer.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            buffer.setBlock(pos, plant, Block.UPDATE_CLIENTS);
            if (++planted == PLANTS_PER_PATCH) return buffer.commit();
        }
        return false;
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static long hash(long seed, int x, int z) {
        return TropicalLayout.mix(seed ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L));
    }

    public record Patch(int x, int z) { }
}
