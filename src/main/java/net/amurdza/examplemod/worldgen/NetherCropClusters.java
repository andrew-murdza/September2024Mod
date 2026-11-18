package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/** One five-plant nether-wart patch per suitable 120-square tile. Soul beets are grown from seeds. */
public final class NetherCropClusters {
    public static final List<String> PALETTE = List.of("minecraft:nether_wart");
    public static final int PLANTS_PER_PATCH = 5;
    private static final Map<RandomState, Map<Tile, CompletableFuture<List<CropTiles.Patch>>>> PLANS = new WeakHashMap<>();

    private NetherCropClusters() { }

    public static List<CropTiles.Patch> plan(long seed, int tileX, int tileZ, BiPredicate<Integer, Integer> suitable) {
        return SpacedCropPatches.plan(seed ^ 0x50b37aL, tileX, tileZ, PALETTE,
                PLANTS_PER_PATCH, suitable);
    }

    public static List<CropTiles.Patch> plan(WorldGenLevel level, int tileX, int tileZ) {
        var source = level.getLevel().getChunkSource();
        RandomState state = source.randomState();
        Tile tile = new Tile(level.getSeed(), tileX, tileZ);
        CompletableFuture<List<CropTiles.Patch>> future;
        boolean compute;
        synchronized (PLANS) {
            var cache = PLANS.computeIfAbsent(state, ignored -> new LinkedHashMap<>(64, .75f, true));
            future = cache.get(tile);
            compute = future == null;
            if (compute) {
                future = new CompletableFuture<>();
                cache.put(tile, future);
            }
        }
        if (compute) {
            try {
                var generator = source.getGenerator();
                DimensionTerrain terrain = NetherRampDecorationGuard.terrain(state).orElse(null);
                Map<Long, Boolean> columns = new HashMap<>();
                future.complete(plan(tile.seed(), tileX, tileZ, (x, z) -> columns.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> {
                    if (terrain != null && DimensionTerrain.netherRampDistance(z,
                            terrain.detail().compute(new DensityFunction.SinglePointContext(x, 0, z))) < 8) return false;
                    // Base columns include Minecraft's actual noise-cell interpolation.
                    // Sampling them neither loads neighboring chunks nor depends on decoration order.
                    var column = generator.getBaseColumn(x, z, level, state);
                    for (int y = 124; y >= 96; y--) {
                        if (column.getBlock(y).isAir() && column.getBlock(y + 1).isAir()
                                && !column.getBlock(y - 1).isAir() && column.getBlock(y - 1).getFluidState().isEmpty()) return true;
                    }
                    return false;
                })));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                synchronized (PLANS) { PLANS.get(state).remove(tile, future); }
            } finally {
                synchronized (PLANS) {
                    var cache = PLANS.get(state);
                    var entries = cache.entrySet().iterator();
                    while (cache.size() > 256 && entries.hasNext()) if (entries.next().getValue().isDone()) entries.remove();
                }
            }
        }
        return future.join();
    }

    public static boolean placeChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        boolean placed = false;
        for (int tx = Math.floorDiv(chunkX * 16, SpacedCropPatches.TILE_SIZE);
             tx <= Math.floorDiv(chunkX * 16 + 15, SpacedCropPatches.TILE_SIZE); tx++) {
            for (int tz = Math.floorDiv(chunkZ * 16, SpacedCropPatches.TILE_SIZE);
                 tz <= Math.floorDiv(chunkZ * 16 + 15, SpacedCropPatches.TILE_SIZE); tz++) {
                for (CropTiles.Patch patch : plan(level, tx, tz)) {
                    if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) {
                        placed |= placePatch(level, patch) == PLANTS_PER_PATCH;
                    }
                }
            }
        }
        return placed;
    }

    public static int placePatch(WorldGenLevel level, CropTiles.Patch patch) {
        int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(patch.species()));
        if (block == null || block == Blocks.AIR) return 0;
        List<BlockPos> floors = new ArrayList<>();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = 124; y >= 96; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).is(block)) return 0;
                var ground = level.getBlockState(pos.below());
                if ((ground.is(Blocks.SOUL_SOIL) || ground.is(Blocks.SOUL_SAND))
                        && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                        && GameplayDimensions.biomeIs(level, pos, "soul_sand_valley")
                        && NetherRampDecorationGuard.allows(level, pos, block.defaultBlockState())) floors.add(pos);
            }
        }
        // Try the reserved six-by-six footprint first, then relocate within its own chunk only.
        List<BlockPos> centres = new ArrayList<>();
        centres.add(new BlockPos(patch.x(), 0, patch.z()));
        for (int x = minX + 2; x <= minX + 12; x++) for (int z = minZ + 2; z <= minZ + 12; z++) {
            if (x != patch.x() || z != patch.z()) centres.add(new BlockPos(x, 0, z));
        }
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ patch.x() * 341873128712L
                ^ patch.z() * 132897987541L));
        for (BlockPos centre : centres) {
            List<BlockPos> candidates = new ArrayList<>();
            for (BlockPos pos : floors) {
                if (pos.getX() >= centre.getX() - 2 && pos.getX() <= centre.getX() + 3
                        && pos.getZ() >= centre.getZ() - 2 && pos.getZ() <= centre.getZ() + 3) candidates.add(pos);
            }
            if (candidates.size() < PLANTS_PER_PATCH) continue;
            for (int i = candidates.size() - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                BlockPos value = candidates.get(i);
                candidates.set(i, candidates.get(other));
                candidates.set(other, value);
            }
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            int planted = 0;
            for (BlockPos pos : candidates) {
                if (DocumentedFloraFeature.placePlant(buffer, pos, patch.species(), random)
                        && ++planted == PLANTS_PER_PATCH) break;
            }
            if (planted == PLANTS_PER_PATCH && buffer.commit()) return planted;
        }
        return 0;
    }

    private record Tile(long seed, int x, int z) { }
}
