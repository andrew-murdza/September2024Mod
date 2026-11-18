package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.amurdza.examplemod.worldgen.SavannaBands;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Checks flooded valley floors, gentle relief and the actual seeded generator. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaTerrainRegressionTests {
    private SavannaTerrainRegressionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void lakeBanksAlwaysReachTheFullDepth(GameTestHelper helper) {
        double previous = SavannaHeight.BOTTOM;
        for (int i = -1200; i <= 1200; i++) {
            double distance = i / 10.0;
            double height = SavannaHeight.height(distance);
            helper.assertTrue(height >= previous && height >= 54.5, "Savanna banks must rise continuously from the eight-block lake bed");
            if (distance <= 0) helper.assertTrue(height - previous <= .045 + 1e-9,
                    "No submerged bank segment may exceed the increased 0.45 grade");
            helper.assertTrue(DimensionTerrain.isSurfaceWater(height, 63) == (distance <= 0),
                    "Surface water must follow the physical shoreline");
            previous = height;
        }
        helper.assertTrue(SavannaHeight.height(0) == 61.5, "Lake shore must retain the existing water level");
        double bankEnd = -SavannaHeight.BANK_WIDTH;
        double easingStart = bankEnd + SavannaHeight.BANK_FLOOR_EASE_WIDTH;
        helper.assertTrue(SavannaHeight.height(bankEnd) == 54.5 && SavannaHeight.height(-120) == 54.5,
                "The bank must meet a flat floor with exactly eight water blocks above its solid bed");
        double epsilon = .00001;
        for (double distance : new double[]{1, 32, 128, 384}) {
            double grade = (SavannaHeight.height(distance + epsilon) - SavannaHeight.height(distance - epsilon)) / (2 * epsilon);
            helper.assertTrue(Math.abs(grade - 1.0 / 18) < 1e-8,
                    "The dry-land grade must be twice its previous 1/36 slope");
        }
        helper.assertTrue(Math.abs((SavannaHeight.height(0) - SavannaHeight.height(-epsilon)) / epsilon - .45) < 1e-6,
                "The underwater shore grade must be 50% steeper at 0.45");
        helper.assertTrue(Math.abs((SavannaHeight.height(bankEnd + epsilon) - SavannaHeight.height(bankEnd)) / epsilon) < 1e-6,
                "The descending bank must meet the shallow lake floor smoothly");
        helper.assertTrue(Math.abs((SavannaHeight.height(easingStart + epsilon) - SavannaHeight.height(easingStart - epsilon))
                / (2 * epsilon) - .45) < 1e-6, "The bottom easing must join the main bank without a slope discontinuity");
        helper.succeed();
    }
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void seededSavannaFloodsEveryBasinAndKeepsSolidColumns(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = generator.generatorSettings().value();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        for (String name : new String[]{"land", "detail"}) {
            var parameters = noises.getOrThrow(ResourceKey.create(Registries.NOISE,
                    new ResourceLocation(AOEMod.MOD_ID, "terrain/savanna/" + name))).value();
            helper.assertTrue(parameters.firstOctave() == (name.equals("land") ? -11 : -8),
                    "Savanna " + name + " must start two octaves lower");
            helper.assertTrue(parameters.amplitudes().size() == 3
                            && parameters.amplitudes().getDouble(0) == 1
                            && parameters.amplitudes().getDouble(1) == .5
                            && parameters.amplitudes().getDouble(2) == .25,
                    "Savanna " + name + " must retain its three octave amplitudes");
        }
        helper.assertTrue(settings.seaLevel() == 63, "Savanna water must end at Y62");
        helper.assertFalse(settings.isAquifersEnabled() || settings.oreVeinsEnabled(),
                "Savanna must retain cave-free and restricted-ore rules");
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:savanna"));
        helper.assertFalse(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).iterator().hasNext(),
                "Savanna must not add cave carvers");
        long previousSignature = 0;
        for (long seed : new long[]{0L, 12345L, 98765L, -8152788754161584091L}) {
            var random = RandomState.create(settings, noises, seed);
            var bands = DimensionTerrain.savannaBands(random.router().ridges());
            int water = 0, bottom = 0, dry = 0, highestLand = 61;
            long signature = 1;
            for (int gx = -48; gx <= 48; gx++) for (int gz = -48; gz <= 48; gz++) {
                int x = (gx * 73 + 7) * 4, z = (gz * 79 - 11) * 4;
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                double height = random.router().initialDensityWithoutJaggedness().compute(point) * 8 - .5;
                helper.assertTrue(Math.abs(height - bands.height(x, z)) < 1e-9, "Router must use the continuous river/peak bands");
                helper.assertTrue(height >= 54.5 && height <= SavannaBands.MAX_HEIGHT,
                        "Savanna must stay between its eight-block lake bed and uncapped hilltops");
                if (height <= 61.5) water++;
                else {
                    dry++;
                    highestLand = Math.max(highestLand, (int) Math.ceil(height + .5) - 1);
                }
                if (height == 54.5) {
                    bottom++;
                }
                helper.assertTrue(random.router().continents().compute(point) == (height <= 61.5 ? 1 : 0),
                        "Savanna lake labels must follow the actual surface-water contour");
                signature = signature * 31 + Double.doubleToLongBits(height);
            }
            helper.assertTrue(water > 0 && bottom > 0 && dry > 0, "Seed must generate dry land, banks and deep water");
            helper.assertTrue(highestLand == 72,
                    "The dry divides must retain their lower gentle slope and end at the revised Y72 crest");
            helper.assertTrue(water > (water + dry) * .15 && water < (water + dry) * .185,
                    "The 80-block rivers in 480-block periods must retain predominantly dry land");
            if (previousSignature != 0) helper.assertTrue(signature != previousSignature, "Savanna ignored a seed change");
            previousSignature = signature;
            for (int rx = -2; rx <= 2; rx++) for (int rz = -2; rz <= 2; rz++) {
                int x = rx * 512;
                int z = (int) Math.round(rz * net.amurdza.examplemod.worldgen.SavannaBands.PERIOD - bands.bend(x));
                var deepColumn = generator.getBaseColumn(x, z, level, random);
                helper.assertTrue(deepColumn.getBlock(54).is(Blocks.STONE), "Every valley must have a solid Y54 lake bed");
                for (int y = 55; y <= 62; y++) helper.assertTrue(deepColumn.getBlock(y).is(Blocks.WATER),
                        "Every valley must reach exactly eight water blocks Y55..62");
                helper.assertTrue(deepColumn.getBlock(63).isAir(), "Savanna water must stop below Y63");
                helper.assertTrue(random.router().continents().compute(new DensityFunction.SinglePointContext(x, 0, z)) == 1,
                        "Every valley floor must be labeled as lake habitat");
            }
            for (int gx = -8; gx <= 8; gx++) for (int gz = -8; gz <= 8; gz++) {
                int x = gx * 349 + 19, z = gz * 373 - 37;
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                // Savanna's active graph has no minecraft:interpolated marker:
                // NoiseChunk caches its direct per-block density, unlike Snowy.
                double profile = bands.height(x, z);
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                helper.assertTrue(height == (int) Math.ceil(profile + .5),
                        "Generated column changed the direct contour height at " + x + "," + z + " seed=" + seed
                                + ": expected=" + (int) Math.ceil(profile + .5) + ", actual=" + height);
                var column = generator.getBaseColumn(x, z, level, random);
                for (int y = 1; y < height; y++) helper.assertTrue(column.getBlock(y).is(Blocks.STONE),
                        "Savanna generated an underground void at " + x + "," + y + "," + z);
            }
            AOEMod.LOGGER.info("Flooded Savanna seed {}: water {}, flat-bottom {}, dry {}, highest land {}",
                    seed, water, bottom, dry, highestLand);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedSavannaKeepsSolidMudBelowItsDirtSurface(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        for (int[] coordinate : new int[][]{{112461, -123572}, {-115281, 132283}}) {
            var chunk = level.getChunk(coordinate[0], coordinate[1], ChunkStatus.CARVERS, true);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                for (int y = 1; y <= 54; y++) helper.assertTrue(chunk.getBlockState(new BlockPos(
                        coordinate[0] * 16 + x, y, coordinate[1] * 16 + z)).is(Blocks.MUD),
                        "Savanna's solid underground mud must survive the height-field conversion");
            }
        }
        helper.succeed();
    }
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshAdjacentSavannaChunksMatchTheContinuousContour(GameTestHelper h) throws Exception {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var random = level.getChunkSource().randomState();
        var bands = DimensionTerrain.savannaBands(random.router().ridges());
        for (int[] origin : new int[][]{{121872, -122794}, {-121841, 122777}}) {
            // Adjacent surface-stage chunks exercise NoiseChunk wrappers and the real surface rules.
            // They are remote from authored structure/test fixtures and contain no feature edits.
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                var chunk = level.getChunk(origin[0] + dx, origin[1] + dz, ChunkStatus.SURFACE, true);
                for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
                    int x = (origin[0] + dx) * 16 + lx, z = (origin[1] + dz) * 16 + lz;
                    int top = (int) Math.ceil(bands.height(x, z) + .5) - 1;
                    var pos = new BlockPos(x, top, z);
                    h.assertTrue(chunk.getBlockState(pos).isSolid() && !chunk.getBlockState(pos.above()).isSolid(),
                            "Fresh chunk contour changed at " + pos + ": top=" + chunk.getBlockState(pos)
                                    + ", above=" + chunk.getBlockState(pos.above()));
                }
            }
        }
        // An optional read-only comparison profile for an existing world's saved terrain.
        String seed = System.getProperty("aoemod.savannaAudit.seed");
        if (seed != null) {
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var saved = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), Long.parseLong(seed));
            var profile = new SavannaBands(saved.router().ridges());
            var rows = new java.util.ArrayList<int[]>();
            for (int x = -2432; x <= -1920; x += 4) for (int z = -960; z <= -448; z += 4)
                rows.add(new int[]{x, z, (int) Math.ceil(profile.height(x, z) + .5) - 1});
            java.nio.file.Files.writeString(java.nio.file.Path.of("savanna-sourceworld-profile.json"), new com.google.gson.Gson().toJson(rows));
        }
        h.succeed();
    }

}
