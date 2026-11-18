package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.amurdza.examplemod.worldgen.SavannaLakeBasins;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaLakeSizeTests {
    private SavannaLakeSizeTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyIrregularLakeHasFullDepthAndContinuousShorelines(GameTestHelper helper) {
        var terrain = new SavannaLakeBasins((x, z) -> Math.sin(x * .009) * Math.cos(z * .013));
        for (int rx = -2; rx <= 2; rx++) for (int rz = -2; rz <= 2; rz++) {
            var lake = terrain.lake(rx, rz);
            double diameter = 0, shortRadius = Double.POSITIVE_INFINITY;
            for (int i = 0; i < lake.vertices(); i++) {
                shortRadius = Math.min(shortRadius, Math.hypot(lake.vertexX(i) - lake.x(), lake.vertexZ(i) - lake.z()));
                for (int j = 0; j < i; j++) diameter = Math.max(diameter,
                        Math.hypot(lake.vertexX(i) - lake.vertexX(j), lake.vertexZ(i) - lake.vertexZ(j)));
                double x = terrain.worldX(lake.vertexX(i), lake.vertexZ(i));
                double z = terrain.worldZ(lake.vertexX(i), lake.vertexZ(i));
                helper.assertTrue(Math.abs(terrain.height(x, z) - SavannaHeight.WATER_EDGE_HEIGHT) < 1e-8,
                        "The distance field and physical shoreline must meet without a height seam");
            }
            helper.assertTrue(Math.abs(diameter - 160) < 1e-8, "The halved terrain cycle must give each lake a 160-block maximum diameter");
            helper.assertTrue(lake.boundingRadius() < SavannaLakeBasins.MAX_BOUNDING_RADIUS
                            && shortRadius / lake.boundingRadius() < .96,
                    "Lake outlines must be bounded and visibly noncircular");
            helper.assertTrue(terrain.height(terrain.worldX(lake.x(), lake.z()), terrain.worldZ(lake.x(), lake.z())) == 54.5,
                    "Every lake basin must reach the eight-block-deep floor, leaving water at Y55..62");
            helper.assertTrue(lake.distance(lake.x(), lake.z()) < -SavannaHeight.BANK_WIDTH,
                    "Every lake must have room for its complete descending bank before reaching the center");
        }
        double maximumShift = SavannaLakeBasins.REGION_SIZE * SavannaLakeBasins.CENTER_JITTER_FRACTION;
        double maximumAdjacentCenterDistance = Math.hypot(SavannaLakeBasins.REGION_SIZE + 2 * maximumShift, 2 * maximumShift);
        helper.assertTrue(maximumAdjacentCenterDistance < 600,
                "Every lake has a neighboring lake within half the previous terrain spacing");
        helper.assertTrue(SavannaLakeBasins.REGION_SIZE - 2 * maximumShift > 2 * SavannaLakeBasins.MAX_BOUNDING_RADIUS,
                "Even the largest allowed irregular footprints must remain disjoint");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void narrowLakeRetainsFullDepthWithRestoredWaterSlope(GameTestHelper helper) {
        double[] samples = {0, 0, 0, 1.5888 / Math.PI, -.1958 / Math.PI, 2.2004 / Math.PI, -1, 0};
        var terrain = new SavannaLakeBasins((x, z) -> {
            int channel = (int) Math.round((x - 192) / 7919);
            if (channel < 0 || channel >= samples.length
                    || x != 192 + channel * 7919 || z != 192 - channel * 6151) return 0;
            double sample = samples[channel];
            return sample == -1 ? -10 : Math.log((1 + sample) / (1 - sample)) / 8;
        });
        var lake = terrain.lake(0, 0);
        double radius = -lake.distance(lake.x(), lake.z());
        helper.assertTrue(radius < 45.1 && radius > SavannaHeight.BANK_WIDTH,
                "A narrow allowed outline must have room for the restored bank and its eight-block floor easing");
        helper.assertTrue(terrain.height(terrain.worldX(lake.x(), lake.z()), terrain.worldZ(lake.x(), lake.z())) == 54.5,
                "Even a narrow lake must retain eight water blocks instead of losing its full-depth floor");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void restoredHillReliefRetainsUncappedWatersheds(GameTestHelper helper) {
        var terrain = new SavannaLakeBasins((x, z) -> 0);
        double center = SavannaLakeBasins.REGION_SIZE / 2.0;
        double distance = SavannaLakeBasins.REGION_SIZE * Math.sqrt(2);
        double maximum = 0;
        int rising = 0, falling = 0, flatDry = 0;
        double previous = terrain.height(center, center);
        for (int step = 1; step <= (int) distance; step++) {
            double height = terrain.height(center + step / Math.sqrt(2), center + step / Math.sqrt(2));
            maximum = Math.max(maximum, height);
            if (height > 61.5 && previous > 61.5) {
                if (height > previous) rising++;
                else if (height < previous) falling++;
                else flatDry++;
            }
            previous = height;
        }
        helper.assertTrue(maximum > 68.5 && rising > 50 && falling > 50 && flatDry == 0,
                "A watershed must rise and descend between lakes without broad height caps");
        double regionalMaximum = 0;
        for (int x = -1308; x <= 1308; x += 13) for (int z = -1308; z <= 1308; z += 17) {
            regionalMaximum = Math.max(regionalMaximum, terrain.height(x, z));
        }
        helper.assertTrue(regionalMaximum > 68.5 && regionalMaximum < SavannaHeight.MAX_HEIGHT,
                "Doubled land slopes must increase hill relief while staying below Y82.5 without clipping");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void spatialDistanceSearchRemainsContinuousAcrossRegionBoundaries(GameTestHelper helper) {
        java.util.function.DoubleBinaryOperator noise = (x, z) -> Math.sin(x * .013 + Math.cos(z * .007)) * .6;
        var terrain = new SavannaLakeBasins(noise);
        for (int rx = -3; rx <= 3; rx++) for (int rz = -3; rz <= 3; rz++) {
            for (double offset : new double[]{-.0001, .0001, 137.25}) {
                double x = rx * SavannaLakeBasins.REGION_SIZE + offset;
                double z = rz * SavannaLakeBasins.REGION_SIZE + .314159;
                int lakeX = (int) Math.floor(x / SavannaLakeBasins.REGION_SIZE), lakeZ = (int) Math.floor(z / SavannaLakeBasins.REGION_SIZE);
                double expected = Double.POSITIVE_INFINITY, shore = Double.POSITIVE_INFINITY;
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                    var lake = terrain.lake(lakeX + dx, lakeZ + dz);
                    shore = Math.min(shore, lake.distance(x, z));
                    expected = Math.min(expected, SavannaHeight.WATER_EDGE_HEIGHT + lake.landRise(x, z));
                }
                if (shore <= 0) expected = SavannaHeight.height(shore);
                double worldX = terrain.worldX(x, z), worldZ = terrain.worldZ(x, z);
                double actual = terrain.height(worldX, worldZ);
                helper.assertTrue(Math.abs(actual - expected) < 1e-8,
                        "The bounded lake search must agree with a much larger weighted search across region boundaries");
                helper.assertTrue(Math.abs(terrain.shoreDistance(worldX, worldZ) - shore) < 1e-8,
                        "Physical shore distance must agree with the exhaustive search across region boundaries");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void allSampledDryLandHasADownhillDirectionTowardWater(GameTestHelper helper) {
        var terrain = new SavannaLakeBasins((x, z) -> Math.sin(x * .013 + Math.cos(z * .007)) * .6);
        int dry = 0, wet = 0, deep = 0, consistentGrade = 0, gradeSamples = 0;
        for (int x = -2304; x <= 2304; x += 37) for (int z = -2304; z <= 2304; z += 41) {
            double height = terrain.height(x, z);
            helper.assertTrue(height >= 54.5 && height < SavannaHeight.MAX_HEIGHT,
                    "Savanna terrain exceeded its lake-bed or hill-height bounds");
            if (height <= SavannaHeight.WATER_EDGE_HEIGHT) {
                wet++;
                if (height == 54.5) deep++;
            } else {
                dry++;
                double lowerNeighbor = Math.min(Math.min(terrain.height(x - .25, z), terrain.height(x + .25, z)),
                        Math.min(terrain.height(x, z - .25), terrain.height(x, z + .25)));
                helper.assertTrue(lowerNeighbor < height - 1e-6,
                        "Dry terrain must always have a downhill direction instead of an elevated valley floor at " + x + "," + z);
                double epsilon = .01;
                if (terrain.shoreDistance(x, z) > epsilon * 2) {
                    gradeSamples++;
                    double dx = (terrain.height(x + epsilon, z) - terrain.height(x - epsilon, z)) / (2 * epsilon);
                    double dz = (terrain.height(x, z + epsilon) - terrain.height(x, z - epsilon)) / (2 * epsilon);
                    double grade = Math.hypot(dx, dz);
                    helper.assertTrue(grade <= 1.0 / 18 + 1e-5,
                            "The physical dry slope must stay within its doubled maximum Euclidean grade");
                    if (grade >= 1.0 / 20 - 1e-5) consistentGrade++;
                }
            }
        }
        helper.assertTrue(dry > 0 && wet > 0 && deep > 0,
                "The sampled landscape must contain gentle hills, lakes and full-depth lake beds");
        helper.assertTrue(gradeSamples > dry * .99 && consistentGrade > gradeSamples * .99,
                "Away from the few exact watershed joins, dry slopes must retain the doubled 1/20..1/18 grade");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void spatialTerrainIgnoresQueryOrderAndThreads(GameTestHelper helper) throws Exception {
        java.util.function.DoubleBinaryOperator field = (x, z) -> Math.sin(x * .013 + Math.cos(z * .007)) * .6;
        int[][] samples = {{-1221, -532}, {3240, 2948}, {-4, -1}, {2090, 2074}, {26462, -27344}, {10000, 10000}};
        var forward = new SavannaLakeBasins(field);
        var backward = new SavannaLakeBasins(field);
        double[] expected = new double[samples.length];
        for (int i = 0; i < samples.length; i++) expected[i] = forward.height(samples[i][0], samples[i][1]);
        for (int i = samples.length - 1; i >= 0; i--) helper.assertTrue(
                backward.height(samples[i][0], samples[i][1]) == expected[i], "Query order changed spatial terrain");
        var concurrent = new SavannaLakeBasins(field);
        var executor = Executors.newFixedThreadPool(4);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int repeat = 0; repeat < 8; repeat++) for (int i = 0; i < samples.length; i++) {
                int index = i;
                futures.add(executor.submit(() -> concurrent.height(samples[index][0], samples[index][1]) == expected[index]));
            }
            for (var future : futures) helper.assertTrue(future.get(10, TimeUnit.SECONDS),
                    "Concurrent generation must agree on every spatial height");
        } finally {
            executor.shutdownNow();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void columnCacheEvictionDoesNotChangeTerrain(GameTestHelper helper) {
        var terrain = new SavannaLakeBasins((x, z) -> Math.sin(x * .009) * Math.cos(z * .013));
        double before = terrain.height(-17, 29);
        for (int i = 0; i < 66000; i++) terrain.height(i % 1000, i / 1000);
        helper.assertTrue(terrain.height(-17, 29) == before, "Memoization changed terrain after eviction");
        helper.succeed();
    }
}
