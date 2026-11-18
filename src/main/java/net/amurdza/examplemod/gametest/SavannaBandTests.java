package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaBands;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaBandTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void completeSeededRiversAndPeaksKeepConstantGrades(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var settings = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator()).generatorSettings().value();
        helper.assertTrue(SavannaBands.PERIOD == 480 && SavannaBands.WATER_WIDTH == 80
                        && SavannaBands.LAND_WIDTH == 400,
                "Each complete cycle must contain an 80-block river and 400-block dry divide");
        java.util.Set<Long> seededBends = new java.util.HashSet<>();
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(settings, level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var bands = new SavannaBands(random.router().ridges());
            seededBends.add(Double.doubleToLongBits(bands.bend(137)));
            for (int x : new int[]{-8192, -4096, -1, 0, 4096, 8192}) {
                int water = 0, dry = 0;
                double center = -bands.bend(x);
                for (int z = (int) Math.ceil(center - SavannaBands.PERIOD / 2); z < center + SavannaBands.PERIOD / 2; z++) {
                    double distance = bands.shoreDistance(x, z);
                    if (distance <= 0) water++; else dry++;
                    helper.assertTrue(Math.abs(bands.shoreDistance(x, z + SavannaBands.PERIOD) - distance) < 1e-9,
                            "Every transect repeats complete river and dry-peak bands without disconnected basin pockets");
                }
                helper.assertTrue(Math.abs(water - 80) <= 1 && Math.abs(dry - 400) <= 1,
                        "Every seeded transect must retain its full river and dry-band widths");
                for (double distance : new double[]{1, 25, 60, 100, 119, -1, -8, -10}) {
                    double z = center + SavannaBands.WATER_WIDTH / 2 + distance, e = .0001;
                    double grade = (SavannaHeight.height(bands.shoreDistance(x, z + e))
                            - SavannaHeight.height(bands.shoreDistance(x, z - e))) / (2 * e);
                    helper.assertTrue(Math.abs(grade - (distance > 0 ? 1.0 / 18 : .45)) < 1e-8,
                            "Cross-band slope must be constant with altitude on land and on the uneased water bank");
                }
                helper.assertTrue(bands.height(x, (int) Math.round(center)) == 54.5
                                && bands.height(x, (int) Math.round(center + SavannaBands.PERIOD / 2)) == 72,
                        "Every narrower river keeps its full depth and each divide ends at the revised Y72 crest");
            }
        }
        helper.assertTrue(seededBends.size() == 4, "World seed must still change the curved river paths");
        AOEMod.LOGGER.info("Savanna river bands: width80, dry400, period480, land grade1/18 below crest, water grade0.45, bed54.5, peak{}",
                SavannaBands.MAX_HEIGHT);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void narrowerRiversLowerOnlyTheY73CrestInTheLoadedGenerator(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        int changedColumns = 0, sampledColumns = 0;
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(generator.generatorSettings().value(), noises, seed);
            var bands = new SavannaBands(random.router().ridges());
            for (int x : new int[]{-4096, -1, 4096}) for (int cycle : new int[]{-2, 0, 3}) {
                double center = cycle * SavannaBands.PERIOD - bands.bend(x);
                int changedInTransect = 0;
                for (int z = (int) Math.ceil(center - 240); z < center + 240; z++) {
                    double formerHeight = SavannaHeight.height(bands.shoreDistance(x, z));
                    int formerTop = (int) Math.ceil(formerHeight + .5) - 1;
                    double loadedHeight = random.router().initialDensityWithoutJaggedness()
                            .compute(new DensityFunction.SinglePointContext(x, 0, z)) * 8 - .5;
                    int revisedTop = (int) Math.ceil(loadedHeight + .5) - 1;
                    helper.assertTrue(revisedTop == (formerTop == 73 ? 72 : formerTop),
                            "Capping the crest must change only former Y73 solid tops to Y72: seed=" + seed + ", x=" + x + ", z=" + z);
                    helper.assertTrue(loadedHeight <= 72 && loadedHeight >= 54.5,
                            "The active generator must retain the full river depth and revised maximum");
                    if (formerHeight <= 72) helper.assertTrue(Math.abs(loadedHeight - formerHeight) < 1e-9,
                            "All lower terrain slopes and underwater banks must retain their exact profile");
                    if (formerTop != revisedTop) changedInTransect++;
                    sampledColumns++;
                }
                helper.assertTrue(changedInTransect >= 3 && changedInTransect <= 5,
                        "Only the roughly four-block highest sliver in each 480-block transect may lose a solid block");
                changedColumns += changedInTransect;
                int crestZ = (int) Math.round(center + 240);
                var crest = generator.getBaseColumn(x, crestZ, level, random);
                helper.assertTrue(crest.getBlock(72).is(Blocks.STONE) && crest.getBlock(73).isAir()
                                && generator.getBaseHeight(x, crestZ, Heightmap.Types.WORLD_SURFACE_WG, level, random) == 73,
                        "Actual generated crest columns must stop at solid Y72 with air at Y73");
                var river = generator.getBaseColumn(x, (int) Math.round(center), level, random);
                helper.assertTrue(river.getBlock(54).is(Blocks.STONE) && river.getBlock(63).isAir(),
                        "The narrower river must retain its Y54 bed and Y62 water surface");
                for (int y = 55; y <= 62; y++) helper.assertTrue(river.getBlock(y).is(Blocks.WATER),
                        "The narrowed river must still contain all eight water blocks");
            }
        }
        AOEMod.LOGGER.info("Savanna crest revision: {} of {} sampled physical columns changed, all Y73 to Y72",
                changedColumns, sampledColumns);
        helper.succeed();
    }
}
