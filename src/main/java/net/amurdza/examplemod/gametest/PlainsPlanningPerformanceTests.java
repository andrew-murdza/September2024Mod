package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTerrainHeights;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsPlanningPerformanceTests {
    // The player's current save, also used by PlainsCrashRegressionTests.
    private static final long SAVE_SEED = 6896527134938031266L;

    private PlainsPlanningPerformanceTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void submergedTilesMeasureFirstEntryPlanningWithoutLoadingChunks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), SAVE_SEED);
        int seaLevel = generator.getSeaLevel();
        List<Tile> candidates = new ArrayList<>();
        Set<Long> inspected = new HashSet<>();
        long searchStart = System.nanoTime();
        int queries = 0;
        // Broad deterministic samples find wet terrain without generating chunks
        // or warming the batched height caches that are measured below.
        for (int attempt = 0; attempt < 256 && candidates.size() < 3; attempt++) {
            long hash = TropicalLayout.mix(SAVE_SEED ^ attempt * 0x632be59bd9b4e019L);
            int tileX = Math.floorMod(hash, 97) - 48;
            int tileZ = Math.floorMod(Long.rotateLeft(hash, 29), 97) - 48;
            if (!inspected.add(ChunkPos.asLong(tileX, tileZ))) continue;
            int minX = tileX * CropTiles.TILE_SIZE, minZ = tileZ * CropTiles.TILE_SIZE;
            queries++;
            if (generator.getBaseHeight(minX + 144, minZ + 144,
                    Heightmap.Types.OCEAN_FLOOR_WG, level, random) >= seaLevel) continue;
            int wet = 1;
            for (int dx : new int[]{24, 144, 264}) for (int dz : new int[]{24, 144, 264}) {
                if (dx == 144 && dz == 144) continue;
                queries++;
                if (generator.getBaseHeight(minX + dx, minZ + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, level, random) < seaLevel) wet++;
            }
            if (wet >= 8) candidates.add(new Tile(tileX, tileZ, wet));
        }
        AOEMod.LOGGER.info("Plains planning precheck seed {}: {} candidate tiles, {} sparse columns, {} ms",
                SAVE_SEED, candidates.size(), queries, (System.nanoTime() - searchStart) / 1_000_000);
        helper.assertFalse(candidates.isEmpty(), "The saved seed must supply at least one mostly submerged diagnostic tile");
        for (Tile tile : candidates) {
            // This is the same pure planning path as CropTiles.plan(WorldGenLevel),
            // with a fresh height sampler and no shared completed tile plan.
            var heights = new CropTerrainHeights(level, generator, random);
            Set<Long> sampledCells = new HashSet<>();
            int[] queriedColumns = {0};
            long started = System.nanoTime();
            List<CropTiles.Patch> patches = CropTiles.plan(SAVE_SEED, tile.x, tile.z, CropTiles.PLAINS_SPECIES, (x, z) -> {
                queriedColumns[0]++;
                sampledCells.add(ChunkPos.asLong(Math.floorDiv(x, CropTiles.CELL_SIZE), Math.floorDiv(z, CropTiles.CELL_SIZE)));
                return heights.height(x, z) >= seaLevel;
            });
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            AOEMod.LOGGER.info("Plains first-entry crop plan seed {} tile {},{} ({} of 9 wet precheck columns): "
                            + "{} ms, {} queried columns, {} eight-block cells, {} crop patches",
                    SAVE_SEED, tile.x, tile.z, tile.wet, elapsed, queriedColumns[0], sampledCells.size(), patches.size());
            // Timing is diagnostic, never a machine-dependent pass/fail threshold.
            helper.assertTrue(patches.isEmpty() || patches.size() == 9, "A wet tile must omit or complete its nine-patch cluster");
            for (CropTiles.Patch patch : patches) {
                helper.assertTrue(Math.floorDiv(patch.x() - 2, CropTiles.TILE_SIZE) == tile.x
                                && Math.floorDiv(patch.x() + 3, CropTiles.TILE_SIZE) == tile.x
                                && Math.floorDiv(patch.z() - 2, CropTiles.TILE_SIZE) == tile.z
                                && Math.floorDiv(patch.z() + 3, CropTiles.TILE_SIZE) == tile.z,
                        "Measured plan must keep complete crop footprints inside their owner tile");
                int dry = 0;
                for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
                    if (heights.height(patch.x() + dx, patch.z() + dz) >= seaLevel) dry++;
                }
                helper.assertTrue(dry >= CropTiles.plantsPerPatch(patch.species()),
                        "Measured plan cannot reserve an underwater crop footprint: " + patch);
            }
        }
        helper.succeed();
    }

    private record Tile(int x, int z, int wet) { }
}
