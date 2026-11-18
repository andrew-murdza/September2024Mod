package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTerrainHeights;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CropTerrainHeightTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void batchedCropHeightsExactlyMatchVanillaQueriesAcrossSeedsAndSignedCells(GameTestHelper helper) {
        for (var dimension : List.of(GameplayDimensions.PLAINS, GameplayDimensions.SAVANNA, GameplayDimensions.SNOWY)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
            for (long seed : new long[]{12345L, 98765L}) {
                var random = RandomState.create(generator.generatorSettings().value(), noises, seed);
                var heights = new CropTerrainHeights(level, generator, random);
                Set<ChunkPos> cells = new LinkedHashSet<>();
                // Two adjacent signed seams always participate, regardless of
                // which additional terrain cells supply the wet/dry cases.
                cells.add(new ChunkPos(-1, 0));
                cells.add(new ChunkPos(0, -1));
                boolean wet = false, dry = false;
                for (int i = 0; i < 64 && !(wet && dry); i++) {
                    int cellX = (i % 8 - 4) * 43 + 1;
                    int cellZ = (i / 8 - 4) * 61 - 1;
                    boolean isDry = heights.height(cellX * 8 + 3, cellZ * 8 + 3) >= generator.getSeaLevel();
                    if (isDry && !dry || !isDry && !wet) cells.add(new ChunkPos(cellX, cellZ));
                    dry |= isDry;
                    wet |= !isDry;
                }
                helper.assertTrue(wet && dry, "Height comparison must exercise wet and dry terrain for " + dimension.location() + ", seed=" + seed);
                int comparisons = 0;
                for (ChunkPos cell : cells) {
                    int[][] expected = new int[8][8];
                    for (int dx = 0; dx < 8; dx++) for (int dz = 0; dz < 8; dz++) {
                        int x = cell.x * 8 + dx, z = cell.z * 8 + dz;
                        expected[dx][dz] = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                        int actual = heights.height(x, z);
                        helper.assertTrue(actual == expected[dx][dz], "Batched height differs from vanilla at " + x + "," + z
                                + " in " + dimension.location() + ", seed=" + seed + ": expected=" + expected[dx][dz] + ", actual=" + actual);
                        comparisons++;
                    }
                    for (int dx = 7; dx >= 0; dx--) for (int dz = 7; dz >= 0; dz--) {
                        helper.assertTrue(heights.height(cell.x * 8 + dx, cell.z * 8 + dz) == expected[dx][dz],
                                "Reverse cached queries changed a column height");
                    }
                }
                AOEMod.LOGGER.info("Exact crop height audit {} seed {}: {} columns across {} signed/wet/dry cells",
                        dimension.location(), seed, comparisons, cells.size());
            }
        }
        helper.succeed();
    }
}
