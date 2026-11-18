package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

/** Shared dry-biome-aware reservation plans for the Snowy dimension's 120-square crop tiles. */
public final class SnowyCropClusters {
    private static final Map<RandomState, Map<Tile, CompletableFuture<List<CropTiles.Patch>>>> PLANS = new WeakHashMap<>();

    private SnowyCropClusters() { }

    public static List<CropTiles.Patch> plan(WorldGenLevel level, int tileX, int tileZ, String biome, List<String> palette, int target) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return List.of();
        var source = level.getLevel().getChunkSource();
        var state = source.randomState();
        Tile tile = new Tile(level.getSeed(), tileX, tileZ, biome);
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
                var heights = generator instanceof NoiseBasedChunkGenerator noise
                        ? new CropTerrainHeights(level, noise, state) : null;
                // Match BiomeManager's actual block-level Voronoi selection, not
                // just the coarser noise-biome sample used to fill a chunk.
                var biomes = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, state.sampler()),
                        BiomeManager.obfuscateSeed(level.getSeed()));
                var biomeKey = ResourceKey.create(Registries.BIOME, AOEMod.makeID(biome));
                future.complete(SpacedCropPatches.plan(tile.seed() ^ biome.hashCode(), tileX, tileZ, palette, target, (x, z) -> {
                    int height = heights == null ? generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state)
                            : heights.height(x, z);
                    return height >= generator.getSeaLevel() && biomes.getBiome(new BlockPos(x, height, z)).is(biomeKey);
                }));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                synchronized (PLANS) { PLANS.get(state).remove(tile, future); }
            } finally {
                // Never sample densities or wait on another tile while holding
                // the shared lock. In-flight entries are retained until done.
                synchronized (PLANS) {
                    var cache = PLANS.get(state);
                    var entries = cache.entrySet().iterator();
                    while (cache.size() > 256 && entries.hasNext()) if (entries.next().getValue().isDone()) entries.remove();
                }
            }
        }
        return future.join();
    }

    private record Tile(long seed, int x, int z, String biome) { }
}
