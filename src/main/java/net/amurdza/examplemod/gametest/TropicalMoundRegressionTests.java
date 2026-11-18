package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalBiomeZoomState;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Regression coverage for the continuous shore replacing the former isolated mounds. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalMoundRegressionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void loweredIslandKeepsItsReefAndWidensTheSeaLevelMangroveBand(GameTestHelper helper) {
        for (long seed : new long[]{0, 12345, -7091263362684584719L}) {
            var layout = new TropicalLayout(seed);
            var island = layout.island(1, 0);
            for (double radius = 270; radius < 360; radius += .25) {
                var sample = layout.sample(island.x() + radius + .01, island.z(), island);
                helper.assertTrue(sample.biome() == 1 && (sample.height() + .5 >= 63) == (radius < 300),
                        "Dry and wet mangroves need contiguous thirty- and sixty-block banks");
            }
            for (int x = -256; x <= 256; x += 3) for (int z = -256; z <= 256; z += 3) {
                helper.assertFalse(layout.moundChunk(x >> 4, z >> 4), "No isolated mound chunks may remain");
                helper.assertTrue(layout.moundBed(x, z) == Integer.MIN_VALUE
                                && layout.surfaceHeight(x, z) == layout.sample(x, z).height(),
                        "Former mound terraces must contribute no hidden terrain height");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void shorePlantQuotasScaleWithEachMangroveBand(GameTestHelper helper) {
        for (long seed : new long[]{0, 1, -1, 12345, 98765, -7091263362684584719L}) {
            var layout = new TropicalLayout(seed);
            for (int column = 0; column < 2; column++) {
                var plan = TropicalIslandPlan.forIsland(layout, layout.island(column, 0));
                helper.assertTrue(plan.mounds().size() >= 16 && plan.shallow().size() >= 30,
                        "Continuous shore needs space for old flora/fauna plus all thirty wet crops");
                var plants = new java.util.HashMap<String, Integer>();
                for (var chunk : plan.mounds()) {
                    helper.assertTrue(layout.mangroveLandChunk(chunk.x(), chunk.z()),
                            "Every former mound reservation must now be on dry mangrove shore");
                    plants.merge(plan.moundSpecies(chunk.x(), chunk.z()), 1, Integer::sum);
                }
                for (String species : java.util.List.of("minecraft:red_mushroom", "minecraft:brown_mushroom",
                        "farmersdelight:wild_cabbages", "hearthandharvest:wild_cotton")) {
                    helper.assertTrue(plants.getOrDefault(species, 0) == 16, "Keep the area-scaled shore quota for " + species);
                }
                helper.assertTrue(plants.containsKey("minecraft:blue_orchid") && plants.containsKey("minecraft:pitcher_plant"),
                        "Both former mound flowers must remain on the shore");
                int rice = 0, cane = 0;
                for (var chunk : plan.shallow()) {
                    String crop = plan.wetCrop(chunk.x(), chunk.z());
                    if ("farmersdelight:wild_rice".equals(crop)) rice++;
                    if ("minecraft:sugar_cane".equals(crop)) cane++;
                    helper.assertTrue(TropicalIslandPlan.shallowCells(layout, chunk.x(), chunk.z()) >= 5,
                            "Each wet allocation needs actual one-deep shoreline cells");
                }
                helper.assertTrue(rice == 84 && cane == 84, "Wet crop counts must follow the restored mangrove area");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void surfaceNormalizationPreservesTheExactDryBankContourBesideItsWetShelf(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var island = layout.island(1, 0);
        var biomes = new BiomeManager((x, y, z) -> registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                BiomeManager.obfuscateSeed(level.getSeed()));
        ((TropicalBiomeZoomState) biomes).aoemod$setTropicalLayout(layout,
                registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST),
                registry.getHolderOrThrow(TropicalChannelSurface.CHERRY));
        int dry = 0, wet = 0, rainforest = 0;
        int centerX = (int) Math.round(island.x() + TropicalLayout.MANGROVE_LAND_RADIUS - 15);
        int centerZ = (int) Math.round(island.z());
        for (int cx = (centerX >> 4) - 2; cx <= (centerX >> 4) + 2; cx++) {
            ProtoChunk chunk = new ProtoChunk(new ChunkPos(cx, centerZ >> 4), UpgradeData.EMPTY, level, registry, null);
            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                    // Recreate the old submerged profile so normalization must restore actual land.
                    int originalBed = layout.surfaceHeight(x, z) < 66.5 ? 58
                            : (int) Math.floor(layout.surfaceHeight(x, z) + .5);
                    for (int y = 48; y <= Math.max(originalBed, 63); y++) chunk.setBlockState(new BlockPos(x, y, z),
                            (y <= originalBed ? Blocks.MOSS_BLOCK : Blocks.WATER).defaultBlockState(), false);
                }
            }
            chunk.setStatus(ChunkStatus.NOISE);
            TropicalChannelSurface.normalize(chunk, biomes, layout);
            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                    var sample = layout.sample(x, z);
                    int bed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
                    int expectedBed = Math.max(57, (int) Math.floor(sample.height() + .5));
                    helper.assertTrue(bed == expectedBed,
                            "Normalization must retain the exact low-bank contour instead of flattening a fuzzy biome edge");
                    var biome = biomes.getBiome(new BlockPos(x, bed + 1, z));
                    helper.assertTrue(biome.is(TropicalChannelSurface.MANGROVE) == (sample.biome() == 1),
                            "The mangrove/rainforest biome boundary must follow the physical Y63/Y64 contour");
                    if (bed >= 63) {
                        dry++;
                        if (sample.biome() == 2) rainforest++;
                        helper.assertTrue(chunk.getBlockState(new BlockPos(x, bed, z)).is(Blocks.MOSS_BLOCK)
                                        && chunk.getBlockState(new BlockPos(x, bed + 1, z)).isAir(),
                                "Normalization must preserve dry Y63 mangrove and the higher rainforest bank");
                    } else {
                        wet++;
                        helper.assertTrue(bed >= 57 && bed <= 62
                                        && chunk.getBlockState(new BlockPos(x, 63, z)).is(Blocks.WATER),
                                "Wet mangrove shelf stays at most six blocks deep with sea level Y63");
                    }
                }
            }
        }
        helper.assertTrue(dry > 100 && wet > 100 && rainforest > 0,
                "Fixture must exercise Y63 mangrove, higher rainforest, and the wet shelf");
        helper.succeed();
    }
}
