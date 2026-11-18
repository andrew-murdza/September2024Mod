package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalBiomeZoomState;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalTerraceRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void firstUplandTerraceSpansTwelveBlocksAndSummitsStopAt84(GameTestHelper helper) {
        int rays = 0;
        for (long seed : new long[]{0, 1, -1, 7331, 841237, -92674112}) {
            var layout = new TropicalLayout(seed);
            for (int column : new int[]{-2, 0, 3}) {
                var island = layout.island(column, 1);
                for (int direction = 0; direction < 32; direction++) {
                    double angle = direction * Math.PI / 16;
                    int y64 = 0, y65 = 0, summit = 0;
                    for (double radius = .125; radius < 301; radius += .25) {
                        var at = layout.sample(island.x() + radius * Math.cos(angle),
                                island.z() + radius * Math.sin(angle), island);
                        int ground = (int) Math.floor(at.height() + .5);
                        helper.assertTrue(ground <= 84, "Natural ground exceeded the requested Y84 summit");
                        if (radius < TropicalLayout.FOREST_RADIUS) {
                            int formerGround = (int) Math.floor(63.5 + (TropicalLayout.FOREST_RADIUS - radius) / 12 + .5);
                            helper.assertTrue(ground == Math.min(84, formerGround),
                                    "Lowering the summit must preserve every ground level below Y84");
                        }
                        if (ground == 84) summit++;
                        if (ground == 64) y64++;
                        if (ground == 65) y65++;
                        if (ground >= 64) helper.assertTrue(at.biome() == (ground < 75 ? 2 : 3),
                                "Rainforest/cherry contour disagrees with its physical ground height " + ground);
                    }
                    helper.assertTrue(y64 == 48 && y65 == 48,
                            "Each first terrace must span twelve radial blocks, including Y64: " + y64 + "/" + y65);
                    helper.assertTrue(summit == 120,
                            "The Y84 plateau must span thirty radial blocks after absorbing the higher terraces");
                    rays++;
                }
            }
        }
        AOEMod.LOGGER.info("Tropical terrace geometry checked {} complete radial profiles", rays);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void copiedAndWorldgenBiomeManagersKeepEveryY74ColumnRainforest(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var random = level.getChunkSource().randomState();
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var layout = DimensionTerrain.tropicalLayout(random.router().temperature());
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        BiomeManager.NoiseBiomeSource noise = (x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler());
        var scoped = new BiomeManager(noise, BiomeManager.obfuscateSeed(level.getSeed()));
        ((TropicalBiomeZoomState) scoped).aoemod$setTropicalLayout(layout,
                registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST),
                registry.getHolderOrThrow(TropicalChannelSurface.CHERRY));
        var copied = scoped.withDifferentSource(noise);
        var serverCopy = level.getBiomeManager().withDifferentSource(noise);
        var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level, registry, null);
        var region = new WorldGenRegion(level, List.of(chunk), ChunkStatus.FEATURES, 0) {
            @Override
            public Holder<Biome> getNoiseBiome(int x, int y, int z) {
                return noise.getNoiseBiome(x, y, z);
            }
        };
        int rainforest = 0, cherry = 0;
        for (int column : new int[]{-1, 0, 1}) {
            var island = layout.island(column, 0);
            for (int direction = 0; direction < 32; direction++) {
                double angle = direction * Math.PI / 16;
                for (int radius = 125; radius <= 155; radius++) {
                    int x = (int) Math.round(island.x() + radius * Math.cos(angle));
                    int z = (int) Math.round(island.z() + radius * Math.sin(angle));
                    int ground = (int) Math.floor(layout.surfaceHeight(x, z) + .5);
                    var expected = ground < 75 ? TropicalChannelSurface.RAINFOREST : TropicalChannelSurface.CHERRY;
                    for (int y : new int[]{-64, 63, 74, 75, 120, 319}) {
                        var pos = new BlockPos(x, y, z);
                        helper.assertTrue(scoped.getBiome(pos).is(expected) && copied.getBiome(pos).is(expected)
                                        && serverCopy.getBiome(pos).is(expected) && region.getBiome(pos).is(expected),
                                "A scoped/copied/worldgen biome manager blurred the Y75 boundary at " + pos);
                    }
                    if (ground < 75) rainforest++; else cherry++;
                }
            }
        }
        helper.assertTrue(rainforest > 1000 && cherry > 1000, "Biome audit must straddle both sides of the new seam");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1600)
    public static void generatedTerracesMatchPhysicalHeightsAndExportViews(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        // Fresh coordinates keep prior QA chunks from hiding a generation regression.
        var island = layout.island(1469, 1109);
        int z = (int) Math.round(island.z());
        int checked = 0, summit = 0, y64 = 0, y65 = 0, y74 = 0, y75 = 0;
        List<ChunkPos> sampled = new ArrayList<>();
        for (double radius : new double[]{0, TropicalLayout.CHERRY_RADIUS, TropicalLayout.FOREST_RADIUS - 6}) {
            int centerX = (int) Math.round(island.x() + radius);
            for (int cx = (centerX >> 4) - 1; cx <= (centerX >> 4) + 1; cx++) {
                var chunkPos = new ChunkPos(cx, z >> 4);
                sampled.add(chunkPos);
                var chunk = level.getChunk(cx, z >> 4, ChunkStatus.SURFACE, true);
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int x = chunkPos.getMinBlockX() + dx, blockZ = chunkPos.getMinBlockZ() + dz;
                    int expected = (int) Math.floor(layout.surfaceHeight(x, blockZ) + .5);
                    int actual = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                    helper.assertTrue(actual == expected,
                            "Native terrain interpolation changed terrace at " + x + "," + blockZ + ": " + actual + " vs " + expected);
                    helper.assertTrue(actual <= 84, "Native summit still generated Y85/Y86 ground");
                    if (actual == 84) summit++;
                    if (actual == 64) y64++;
                    if (actual == 65) y65++;
                    if (actual == 74) y74++;
                    if (actual == 75) y75++;
                    checked++;
                }
            }
        }
        helper.assertTrue(summit > 100 && y64 > 100 && y65 > 100 && y74 > 100 && y75 > 100,
                "Native audit missed a required terrace: " + summit + "/" + y64 + "/" + y65 + "/" + y74 + "/" + y75);
        // Finish ordinary decoration so a copied QA world is ready for screenshots.
        for (var chunk : sampled) level.getChunk(chunk.x, chunk.z);
        List<Map<String, Object>> views = List.of(
                view("tropical-y64-twelve-block-terrace", level.dimension().location().toString(),
                        island.x() + 283, 77, island.z() + 20, island.x() + 259, 64, island.z()),
                view("tropical-y75-rainforest-cherry-boundary", level.dimension().location().toString(),
                        island.x() + 155, 109, island.z() + 33, island.x() + 138, 75, island.z()),
                view("tropical-y84-summit", level.dimension().location().toString(),
                        island.x() + 33, 118, island.z() + 35, island.x(), 84, island.z()));
        Path output = Path.of("tropical-terrace-views.json").toAbsolutePath();
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views));
        level.getServer().saveEverything(true, true, true);
        AOEMod.LOGGER.info("Native tropical terraces: {} columns; views {}", checked, output);
        helper.succeed();
    }

    private static Map<String, Object> view(String label, String dimension, double x, double y, double z,
                                             double targetX, double targetY, double targetZ) {
        double dx = targetX - x, dz = targetZ - z;
        return Map.of("label", label, "dimension", dimension, "x", x, "y", y, "z", z,
                "yaw", Math.toDegrees(Math.atan2(-dx, dz)),
                "pitch", -Math.toDegrees(Math.atan2(targetY - y - 1.62, Math.hypot(dx, dz))));
    }
}
