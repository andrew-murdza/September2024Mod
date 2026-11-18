package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Random;

/** Checks production geometry through the seeded router and Minecraft's real Voronoi biome manager. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalTerrainRegressionTests {
    private static final long[] SEEDS = {12345L, 98765L, -8152788754161584091L,
            0L, 1L, -1L, 8829123411L, -7091263362684584719L};

    private TropicalTerrainRegressionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void tropicalRiversHaveBroadWaterAndGentleBanks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            RandomState random = random(level, generator, seed);
            DensityFunction heights = random.router().initialDensityWithoutJaggedness();
            TropicalLayout layout = DimensionTerrain.tropicalLayout(random.router().temperature());
            double maximumStep = 0, maximumForestRise = 0, maximumHeight = 0;
            for (int z = -1024; z <= 1024; z += 64) for (int x = -1024; x <= 1024; x += 8) {
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                double height = heights.compute(point);
                helper.assertTrue(Math.abs(height - layout.sample(x, z).height()) < .000001,
                        "Height router and surface layout use different seeded geometry");
                maximumHeight = Math.max(maximumHeight, height);
                maximumStep = Math.max(maximumStep, Math.abs(height - heights.compute(
                        new DensityFunction.SinglePointContext(x + 1, 0, z + 1))));
                double north = heights.compute(new DensityFunction.SinglePointContext(x, 0, z + 16));
                if (layout.sample(x, z).biome() == 2 && layout.sample(x, z + 16).biome() == 2) {
                    maximumForestRise = Math.max(maximumForestRise, Math.abs(height - north));
                }
            }
            helper.assertTrue(maximumStep < 2.01, "Steep tropical diagonal step " + maximumStep + " for seed " + seed);
            helper.assertTrue(maximumForestRise < 3, "Rainforest rises too steeply: " + maximumForestRise + " rise /16");
            helper.assertTrue(maximumHeight > 83.5 && maximumHeight < 84.5,
                    "The hill must reach Y84 without any Y85 or Y86 terrain");
            var island = layout.island(0, 0);
            var neighbor = layout.island(1, 0);
            int centerX = (int) Math.round((island.x() + neighbor.x()) / 2);
            int centerZ = (int) Math.round((island.z() + neighbor.z()) / 2);
            for (int dx = -24; dx <= 24; dx++) {
                var column = generator.getBaseColumn(centerX + dx, centerZ, level, random);
                helper.assertTrue(column.getBlock(63).is(Blocks.WATER), "Generated channel lacks 49 consecutive water columns");
            }
            AOEMod.LOGGER.info("Tropical settled terrain seed {}: max diagonal step {}, max rainforest rise/16 {}, maximum hill {}",
                    seed, maximumStep, maximumForestRise, maximumHeight);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void tropicalChannelsConnectCoralCentersAndMangroveShoulders(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : SEEDS) {
            RandomState random = random(level, generator, seed);
            TropicalLayout layout = DimensionTerrain.tropicalLayout(random.router().temperature());
            BiomeManager actual = manager(generator, random, seed);
            int minimumReef = Integer.MAX_VALUE;
            for (int row = -2; row <= 2; row++) for (int col = -2; col <= 2; col++) {
                var a = layout.island(col, row);
                for (var b : new TropicalLayout.Island[]{layout.island(col + 1, row), layout.island(col, row + 1)}) {
                    double dx = b.x() - a.x(), dz = b.z() - a.z(), distance = Math.sqrt(dx * dx + dz * dz);
                    int running = 0, longest = 0;
                    for (int offset = 0; offset <= (int) distance; offset++) {
                        BlockPos pos = new BlockPos((int) Math.round(a.x() + dx / distance * offset), 63,
                                (int) Math.round(a.z() + dz / distance * offset));
                        running = actual.getBiome(pos).is(TropicalChannelSurface.CORAL) ? running + 1 : 0;
                        longest = Math.max(longest, running);
                    }
                    minimumReef = Math.min(minimumReef, longest);
                    helper.assertTrue(longest >= 150, "Real Voronoi reef channel narrower than its preserved 200-block corridor: " + longest);
                    helper.assertTrue(longest <= 250, "Real Voronoi reef channel wider than the preserved corridor plus island jitter: " + longest);
                }
            }
            int shallow = 0, deep = 0;
            for (int z = -1024; z <= 1024; z += 16) for (int x = -1024; x <= 1024; x += 16) {
                var sample = layout.sample(x, z);
                int depth = DimensionTerrain.tropicalWaterDepth(sample.height());
                if (sample.biome() == 1) {
                    // The final fractional block of the land ramp becomes one
                    // deep when the surface matches actual Voronoi biomes.
                    helper.assertTrue(depth >= 0 && depth <= 6, "Mangrove source shelf outside zero-to-six-deep range at "
                            + x + "," + z + ": " + depth);
                    helper.assertTrue(depth != 0 || sample.distance() < TropicalLayout.MANGROVE_LAND_RADIUS + 1,
                            "Dry source mangrove terrain extended beyond the new land strip");
                    shallow++;
                } else if (sample.biome() == 0) {
                    helper.assertTrue(depth >= 6 && depth <= 8, "Reef source bed must be six-to-eight deep");
                    deep++;
                }
            }
            helper.assertTrue(shallow > 1000 && deep > 1000, "Missing wet mangrove/reef habitats");
            AOEMod.LOGGER.info("Tropical Euclidean channel seed {}: minimum actual coral center width {}, {} shallow / {} deep samples",
                    seed, minimumReef, shallow, deep);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void tropicalBandsHaveTheRequestedWidthsAndOneInTwelveGrade(GameTestHelper helper) {
        for (long seed : SEEDS) {
            TropicalLayout layout = new TropicalLayout(seed);
            var island = layout.island(0, 0);
            int[] widths = new int[4];
            double previous = layout.sample(island.x(), island.z(), island).height();
            for (int r = 0; r < 360; r++) {
                var sample = layout.sample(island.x() + r + .5, island.z(), island);
                int band = sample.biome() == 1 ? (sample.height() + .5 >= 63 ? 1 : 0) : sample.biome();
                widths[band]++;
                if (r > 0 && r < TropicalLayout.FOREST_RADIUS) {
                    double fall = previous - sample.height();
                    helper.assertTrue(fall >= 0 && fall <= 1.0 / 12 + .000001,
                            "The crown or slope exceeded the one-in-twelve grade at radius " + r);
                    if (previous < 84.1) helper.assertTrue(Math.abs(fall - 1.0 / 12) < .000001,
                            "Island grade changed below the flattened crown at radius " + r);
                }
                if (r >= 270 && r < 300) helper.assertTrue(Math.round(sample.height()) == 63,
                        "All dry mangrove ground must be Y63");
                if (r >= 315 && r < 358) helper.assertTrue(Math.round(sample.height()) == 58,
                        "The water mangrove must retain its five-deep shelf");
                previous = sample.height();
            }
            helper.assertTrue(java.util.Arrays.equals(widths, new int[]{60, 30, 132, 138}),
                    "Incorrect wet mangrove, dry mangrove, rainforest, cherry radial widths");
        }
        helper.succeed();
    }

    private static BiomeManager manager(NoiseBasedChunkGenerator generator, RandomState random, long seed) {
        return new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()),
                BiomeManager.obfuscateSeed(seed));
    }

    private static RandomState random(ServerLevel level, NoiseBasedChunkGenerator generator, long seed) {
        return RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
    }
}
