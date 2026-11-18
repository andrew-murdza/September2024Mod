package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalBiomeZoomState;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
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
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalShoreBiomeTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshlyDecoratedShoreKeepsItsBiomeHeightsAndMangroveUnderstory(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var island = layout.island(1205, 907);
        int centerX = (int) Math.round(island.x() + TropicalLayout.FOREST_RADIUS + 13);
        int centerZ = (int) Math.round(island.z());
        int low = 0, high = 0, grass = 0;
        for (int cx = (centerX >> 4) - 1; cx <= (centerX >> 4) + 1; cx++) {
            for (int cz = (centerZ >> 4) - 1; cz <= (centerZ >> 4) + 1; cz++) {
                var chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int x = cx * 16 + dx, z = cz * 16 + dz;
                    int bed = 90;
                    while (bed > 48 && !chunk.getBlockState(new BlockPos(x, bed, z)).is(Blocks.MOSS_BLOCK)
                            && !chunk.getBlockState(new BlockPos(x, bed, z)).is(Blocks.MUD)) bed--;
                    helper.assertTrue(bed > 48, "Generated shore lost its natural substrate");
                    var biome = level.getBiome(new BlockPos(x, bed + 1, z));
                    if (biome.is(TropicalChannelSurface.MANGROVE) || biome.is(TropicalChannelSurface.CORAL)) {
                        helper.assertTrue(bed <= 65, "Decorated mangrove exceeded Y65 at " + x + "," + z);
                        low++;
                        var above = chunk.getBlockState(new BlockPos(x, bed + 1, z));
                        if (biome.is(TropicalChannelSurface.MANGROVE) && bed >= 63
                                && (above.is(Blocks.GRASS) || above.is(Blocks.FERN))) grass++;
                        helper.assertFalse(above.is(Blocks.LARGE_FERN),
                                "Mangrove retained a large fern at " + x + "," + (bed + 1) + "," + z);
                    } else {
                        helper.assertTrue(bed >= 64, "Decorated upland fell below Y64 at " + x + "," + z);
                        high++;
                    }
                }
            }
        }
        helper.assertTrue(low > 100 && high > 100 && grass > 20,
                "Natural shore audit lacked lowland, upland or restored grass: " + low + "/" + high + "/" + grass);
        java.util.List<java.util.Map<String, Object>> views = new java.util.ArrayList<>();
        for (int view = 0; view < 2; view++) {
            double x = centerX + 9 + view * 7, z = centerZ + 17 - view * 30;
            double targetX = centerX - 5, targetZ = centerZ;
            double dx = targetX - x, dz = targetZ - z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(64.7 - 70.62, Math.hypot(dx, dz)));
            views.add(java.util.Map.of("label", "tropical_restored_shore_" + view,
                    "dimension", level.dimension().location().toString(), "x", x + .5, "y", 69,
                    "z", z + .5, "yaw", yaw, "pitch", pitch));
        }
        var output = java.nio.file.Path.of("build/reports/tropical-shore-views.json").toAbsolutePath();
        java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.writeString(output, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
        level.getServer().saveEverything(true, true, true);
        AOEMod.LOGGER.info("Natural tropical shore: {} lowland / {} upland columns, {} grass/fern roots; views {}",
                low, high, grass, output);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void allFourBiomesUseTheY63ColumnThroughWorldgenSourceChanges(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var layout = DimensionTerrain.tropicalLayout(random.router().temperature());
        var source = generator.getBiomeSource();
        var manager = level.getBiomeManager().withDifferentSource((x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler()));
        var unscoped = new BiomeManager((x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler()),
                BiomeManager.obfuscateSeed(level.getSeed()));
        int checked = 0;
        var habitats = new java.util.HashSet<>();
        for (int column = 0; column <= 1; column++) {
            var island = layout.island(column, 0);
            for (int radius = 0; radius <= 400; radius++) {
                BlockPos pos = new BlockPos((int) Math.round(island.x() + radius), 63, (int) Math.round(island.z()));
                var atSea = unscoped.getBiome(pos);
                if (atSea.is(TropicalChannelSurface.MANGROVE) || atSea.is(TropicalChannelSurface.RAINFOREST)
                        || atSea.is(TropicalChannelSurface.CHERRY)) {
                    var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
                    atSea = registry.getHolderOrThrow(switch (layout.sample(pos.getX(), pos.getZ()).biome()) {
                        case 3 -> TropicalChannelSurface.CHERRY;
                        case 2 -> TropicalChannelSurface.RAINFOREST;
                        default -> TropicalChannelSurface.MANGROVE;
                    });
                }
                habitats.add(atSea.unwrapKey().orElseThrow());
                for (int y : new int[]{-64, 0, 62, 63, 64, 75, 140, 319}) {
                    helper.assertTrue(manager.getBiome(pos.atY(y)).equals(atSea),
                            "Tropical column changed from its original Y63 biome at " + pos.atY(y));
                    checked++;
                }
            }
        }
        helper.assertTrue(habitats.size() == 4, "Column regression must exercise all three active tropical biomes");
        AOEMod.LOGGER.info("Tropical Y63 biome audit: {} lookups across {} habitats", checked, habitats.size());
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void physicalShoreMatchesTheHeightSplitDespiteQuartBiomeZoom(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int checked = 0, mangrove = 0, rainforest = 0, coral = 0;
        for (long seed : new long[]{0, 12345, -7091263362684584719L}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var layout = DimensionTerrain.tropicalLayout(random.router().temperature());
            var biomes = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()),
                    BiomeManager.obfuscateSeed(seed));
            ((TropicalBiomeZoomState) biomes).aoemod$setTropicalLayout(layout,
                    registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                    registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST),
                    registry.getHolderOrThrow(TropicalChannelSurface.CHERRY));
            var island = layout.island(1, 0);
            for (double angle : new double[]{0, Math.PI / 4}) {
                for (double radius : new double[]{TropicalLayout.FOREST_RADIUS + 9, TropicalLayout.ISLAND_RADIUS}) {
                    int centerX = (int) Math.round(island.x() + radius * Math.cos(angle));
                    int centerZ = (int) Math.round(island.z() + radius * Math.sin(angle));
                    for (int cx = (centerX >> 4) - 1; cx <= (centerX >> 4) + 1; cx++) {
                        for (int cz = (centerZ >> 4) - 1; cz <= (centerZ >> 4) + 1; cz++) {
                            var chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, level, registry, null);
                            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                                int x = cx * 16 + dx, z = cz * 16 + dz;
                                int bed = (int) Math.floor(layout.sample(x, z).height() + .5);
                                for (int y = 48; y <= Math.max(63, bed); y++) {
                                    chunk.setBlockState(new BlockPos(x, y, z),
                                            (y <= bed ? Blocks.MOSS_BLOCK : Blocks.WATER).defaultBlockState(), false);
                                }
                            }
                            chunk.setStatus(ChunkStatus.NOISE);
                            TropicalChannelSurface.normalize(chunk, biomes, layout);
                            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                                var pos = new BlockPos(cx * 16 + dx, 63, cz * 16 + dz);
                                int bed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                                var sample = layout.sample(pos.getX(), pos.getZ());
                                if (sample.height() >= 62.5 && sample.height() < 66.5) {
                                    helper.assertTrue(bed == (int) Math.floor(sample.height() + .5),
                                            "Biome zoom reshaped the original smooth land contour at " + pos);
                                }
                                var biome = biomes.getBiome(pos.atY(bed + 1));
                                if (biome.is(TropicalChannelSurface.MANGROVE)) {
                                    helper.assertTrue(bed <= 65, "Mangrove escaped above Y65 at " + pos + ": " + bed);
                                    mangrove++;
                                } else if (biome.is(TropicalChannelSurface.CORAL)) {
                                    helper.assertTrue(bed < 63, "Coral reef lost its existing submerged habitat at " + pos);
                                    coral++;
                                } else {
                                    helper.assertTrue(bed >= 64, "Rainforest/cherry escaped below Y64 at " + pos + ": " + bed);
                                    rainforest++;
                                }
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(mangrove > 1000 && rainforest > 1000 && coral > 1000, "Height audit missed a shoreline habitat");
        AOEMod.LOGGER.info("Tropical physical height split: {} columns, {} mangrove / {} forest / {} reef", checked, mangrove, rainforest, coral);
        helper.succeed();
    }
}
