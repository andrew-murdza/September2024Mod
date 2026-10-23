package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionTerrainTests {
    private DimensionTerrainTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void fixedBandsAndDimensionBounds(GameTestHelper helper) {
        String[] names = {"tropical", "savanna", "plains", "snowy", "desert", "nether"};
        int[] minimum = {0, 0, -64, -96, -32, 0};
        int[] maximum = {256, 256, 320, 320, 320, 128};
        int[] seas = {64, 64, 64, 96, 64, 12};
        for (int i = 0; i < names.length; i++) {
            ServerLevel level = level(helper, names[i]);
            helper.assertTrue(level.getMinBuildHeight() == minimum[i] && level.getMaxBuildHeight() == maximum[i],
                    "Wrong bounds for " + names[i]);
            helper.assertTrue(level.getChunkSource().getGenerator().getSeaLevel() == seas[i], "Wrong sea level for " + names[i]);
        }
        ServerLevel nether = level(helper, "nether");
        String[] bands = {"basalt_deltas", "crimson_forest", "warped_forest", "soul_sand_valley"};
        for (int band = 0; band < 4; band++) {
            assertBiome(helper, nether, band * 32, bands[band]);
            assertBiome(helper, nether, band * 32 + 28, bands[band]);
        }
        assertBiome(helper, level(helper, "snowy"), -36, "snowy_forest");
        assertBiome(helper, level(helper, "snowy"), -32, "mushroom_caves");
        assertBiome(helper, level(helper, "snowy"), -4, "mushroom_caves");
        assertBiome(helper, level(helper, "snowy"), 0, "snowy_forest");
        assertBiome(helper, level(helper, "desert"), -4, "deep_dark");
        assertBiome(helper, level(helper, "desert"), 0, "desert");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void terrainChangesWithSeedAndProtectsLakeFloor(GameTestHelper helper) {
        for (String name : new String[]{"tropical", "savanna", "plains", "snowy", "desert", "nether"}) {
            ServerLevel level = level(helper, name);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
            RandomState first = RandomState.create(generator.generatorSettings().value(), noises, 12345L);
            RandomState second = RandomState.create(generator.generatorSettings().value(), noises, 98765L);
            boolean changed = false;
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                var point = new DensityFunction.SinglePointContext(x * 311, name.equals("nether") ? 38 : 0, z * 397);
                double a = first.router().finalDensity().compute(point);
                double b = second.router().finalDensity().compute(point);
                changed |= Math.abs(a - b) > 0.001;
                if (name.equals("snowy")) {
                    double height = first.router().initialDensityWithoutJaggedness().compute(point);
                    helper.assertTrue(height >= 16, "A lake would breach the required rock barrier");
                    for (int y = 0; y < 16; y++) {
                        helper.assertTrue(first.router().finalDensity().compute(
                                new DensityFunction.SinglePointContext(point.blockX(), y, point.blockZ())) > 0,
                                "Snow's lake/cave separation must stay solid");
                    }
                }
            }
            helper.assertTrue(changed, name + " terrain ignored the world seed");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void dryMiningGalleriesAndNoTropicalCaves(GameTestHelper helper) {
        for (String name : new String[]{"tropical", "savanna", "plains", "snowy", "desert"}) {
            ServerLevel level = level(helper, name);
            var generator = level.getChunkSource().getGenerator();
            var random = level.getChunkSource().randomState();
            boolean foundGallery = false;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                var column = generator.getBaseColumn(x * 127, z * 163, level, random);
                int low = name.equals("tropical") || name.equals("savanna") ? 1 : level.getMinBuildHeight() + 6;
                int top = name.equals("snowy") || name.equals("desert") ? -1 : name.equals("plains") ? 40 : 20;
                for (int y = low; y <= top; y++) {
                    var state = column.getBlock(y);
                    helper.assertFalse(state.is(Blocks.LAVA) || state.is(Blocks.WATER), name + " underground flooded at " + y);
                    if (name.equals("tropical") || name.equals("savanna")) {
                        helper.assertFalse(state.isAir(), name + " generated an underground cave");
                    } else foundGallery |= state.isAir();
                }
            }
            if (name.equals("snowy") || name.equals("desert")) helper.assertTrue(foundGallery, name + " galleries missing");
        }
        helper.succeed();
    }

    private static void assertBiome(GameTestHelper helper, ServerLevel level, int y, String expected) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var biome = source.getNoiseBiome(0, Math.floorDiv(y, 4), 0, level.getChunkSource().randomState().sampler());
        helper.assertTrue(biome.is(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", expected))),
                "Expected " + expected + " at Y" + y);
    }

    private static ServerLevel level(GameTestHelper helper, String name) {
        var key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("aoemod", name + "_dimension"));
        ServerLevel level = helper.getLevel().getServer().getLevel(key);
        helper.assertTrue(level != null, "Dimension missing: " + name);
        return level;
    }
}
