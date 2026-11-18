package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertBandLayout;
import net.amurdza.examplemod.worldgen.DesertBands;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertBandRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void exactBandDistancesPreserveSandAndRedProfiles(GameTestHelper helper) {
        double oldLake = 2 * DesertBandLayout.ORIGINAL_LAKE_HALF;
        double oldRed = DesertBandLayout.originalDistance(DesertHeight.RED_SAND_START_NOISE);
        double newRed = DesertBandLayout.resizedDistance(oldRed);
        helper.assertTrue(Math.abs(DesertBandLayout.resizedDistance(DesertBandLayout.ORIGINAL_FLAT_HALF) / (DesertBandLayout.ORIGINAL_FLAT_HALF / 2) - .5) < 1e-12,
                "The flat lakebed must be exactly half its previous nominal block width");
        helper.assertTrue(Math.abs((newRed - DesertBandLayout.LAKE_HALF)
                        - (oldRed - DesertBandLayout.ORIGINAL_LAKE_HALF - DesertBandLayout.REMOVED_PER_FLANK)) < 1e-12,
                "Each ordinary sand band must lose exactly the width of the two removed terraces");
        helper.assertTrue(Math.abs((DesertBandLayout.TERRACOTTA_START - newRed)
                        - (DesertBandLayout.ORIGINAL_TERRACOTTA_START - oldRed)) < 1e-12,
                "Each red-sand band must retain its exact previous width");
        helper.assertTrue(Math.abs(MountainRidges.contour(.7) - .4) < 1e-12,
                "Snowy's existing forest edge stays at contour .7");
        double snowyForest = 480 * 2 * (1 - .7);
        helper.assertTrue(Math.abs((DesertBandLayout.TERRACOTTA_WIDTH - 2 * DesertBandLayout.REMOVED_PER_FLANK) / snowyForest - .6) < 1e-12,
                "Terracotta receives exactly the width removed from both sand flanks");
        for (int cycle = -8; cycle <= 8; cycle++) for (int side : new int[]{-1, 1}) {
            for (double oldDistance = DesertBandLayout.ORIGINAL_LAKE_HALF + .01;
                 oldDistance < DesertBandLayout.ORIGINAL_TERRACOTTA_START; oldDistance += .5) {
                if (oldDistance >= DesertBandLayout.SAND_END && oldDistance < oldRed) continue;
                double revised = cycle * DesertBandLayout.PERIOD + side * DesertBandLayout.resizedDistance(oldDistance);
                double expected = cycle * DesertBandLayout.ORIGINAL_PERIOD + side * oldDistance;
                helper.assertTrue(Math.abs(DesertBandLayout.originalCoordinate(revised) - expected) < 1e-9,
                        "Sand and red-sand distances must be translated without stretching on either side of every cycle");
                double land = originalContour(oldDistance);
                for (double detail : new double[]{-.8, 0, .8}) helper.assertTrue(
                        Math.abs(DesertHeight.height(land, detail) - previousHeight(land, detail)) < 1e-10,
                        "The local sand/red height profile and detail contribution must remain unchanged");
            }
        }
        AOEMod.LOGGER.info("Desert nominal widths: lake={} -> {}; sandEach={}; redEach={}; terracotta={} -> {}; period={} -> {}",
                oldLake, 2 * DesertBandLayout.LAKE_HALF, oldRed - DesertBandLayout.ORIGINAL_LAKE_HALF,
                DesertBandLayout.ORIGINAL_TERRACOTTA_START - oldRed, DesertBandLayout.ORIGINAL_TERRACOTTA_WIDTH,
                DesertBandLayout.TERRACOTTA_WIDTH, DesertBandLayout.ORIGINAL_PERIOD, DesertBandLayout.PERIOD);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void seededBandsKeepTheirExactBendsAndMeasuredRegionWidths(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var settings = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator()).generatorSettings().value();
        int transects = 0;
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(settings, level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            DesertBands[] found = {null};
            random.router().temperature().mapAll(f -> { if (f instanceof DesertBands bands) found[0] = bands; return f; });
            helper.assertTrue(found[0] != null, "Inspect the active seeded desert band field");
            var bands = found[0];
            for (int x : new int[]{-8192, -4096, -1, 0, 4096, 8192}) {
                double bend = bands.bend(x);
                double previousBend = 96 * Math.tanh(bands.warp().compute(new DensityFunction.SinglePointContext(x * 2, 0, 0)));
                helper.assertTrue(bend == previousBend, "Every seeded desert bend must remain bit-for-bit unchanged");
                int[] before = widths(random.router().temperature(), x, bend, false);
                int[] after = widths(random.router().temperature(), x, bend, true);
                helper.assertTrue(Math.abs(after[0] - 2 * DesertBandLayout.LAKE_HALF) <= 1.5
                                && Math.abs(after[1] - 2 * (DesertBandLayout.SAND_END - DesertBandLayout.ORIGINAL_LAKE_HALF)) <= 2 && Math.abs(after[2] - before[2]) <= 2
                                && Math.abs(after[3] - DesertBandLayout.TERRACOTTA_WIDTH) <= 1,
                        "Loaded integer columns must realize the nominal widths within block rounding: seed=" + seed + ", x=" + x
                                + ", before=" + java.util.Arrays.toString(before) + ", after=" + java.util.Arrays.toString(after));
                // All contour lines are z=constant-bend(x). Changing their
                // constant leaves their first and second X differences exact.
                for (double contour : new double[]{DesertHeight.WATER_EDGE_NOISE, DesertHeight.RED_SAND_START_NOISE,
                        DesertHeight.TERRACOTTA_START_NOISE, .3, .5}) {
                    double oldPosition = DesertBandLayout.originalDistance(contour);
                    double newPosition = DesertBandLayout.resizedDistance(oldPosition);
                    double oldCurvature = (oldPosition - bands.bend(x - 8)) - 2 * (oldPosition - bend)
                            + oldPosition - bands.bend(x + 8);
                    double newCurvature = (newPosition - bands.bend(x - 8)) - 2 * (newPosition - bend)
                            + newPosition - bands.bend(x + 8);
                    helper.assertTrue(Math.abs(oldCurvature - newCurvature) < 1e-10,
                            "Lake, sand, red and terracotta contour bends must retain identical longitudinal curvature");
                }
                transects++;
            }
        }
        AOEMod.LOGGER.info("Desert band remap audited {} seeded transects with unchanged bend curvature", transects);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void terracottaGradeIsFortyPercentWithContinuousBoundariesAndUnchangedDetail(GameTestHelper helper) {
        double epsilon = .001, minimumRatio = 1, maximumRatio = 0;
        for (double land = -.58; land <= .62; land += .001) for (double detail : new double[]{-.8, 0, .8})
            helper.assertTrue(Math.abs(DesertHeight.materialHeight(land, detail) - previousHeight(land, detail)) < 1e-10,
                    "Color classification must keep the independent previous profile throughout every band");
        for (double oldDistance = DesertBandLayout.ORIGINAL_TERRACOTTA_START + 1;
             oldDistance < DesertBandLayout.ORIGINAL_PERIOD / 2 - 1; oldDistance += 1) {
            double revised = DesertBandLayout.resizedDistance(oldDistance);
            double oldGrade = (previousHeight(originalContour(oldDistance + epsilon), 0)
                    - previousHeight(originalContour(oldDistance - epsilon), 0)) / (2 * epsilon);
            double newGrade = (DesertHeight.height(originalContour(DesertBandLayout.originalCoordinate(revised + epsilon)), 0)
                    - DesertHeight.height(originalContour(DesertBandLayout.originalCoordinate(revised - epsilon)), 0)) / (2 * epsilon);
            double ratio = newGrade / oldGrade;
            minimumRatio = Math.min(minimumRatio, ratio); maximumRatio = Math.max(maximumRatio, ratio);
            helper.assertTrue(Math.abs(ratio - .4) < 1e-7,
                    "The broad transverse grade, excluding unchanged detail noise, must be0.4 of its previous value: " + ratio);
            double land = originalContour(oldDistance);
            for (double detail : new double[]{-.8, .8}) helper.assertTrue(Math.abs(
                    DesertHeight.height(land, detail) - DesertHeight.height(land, 0)
                            - (previousHeight(land, detail) - previousHeight(land, 0))) < 1e-10,
                    "The independent detail-noise amplitude must remain unchanged in terracotta");
        }
        for (double boundary : new double[]{DesertHeight.WATER_EDGE_NOISE, DesertHeight.RED_SAND_START_NOISE,
                DesertHeight.TERRACOTTA_START_NOISE}) for (double detail : new double[]{-.8, 0, .8}) {
            helper.assertTrue(Math.abs(DesertHeight.height(boundary - 1e-9, detail)
                            - DesertHeight.height(boundary + 1e-9, detail)) < 1e-5,
                    "Every material boundary must retain continuous height");
            helper.assertTrue(Math.abs(DesertHeight.height(boundary, detail) - previousHeight(boundary, detail)) < 1e-10,
                    "Every existing boundary height must remain exact");
        }
        AOEMod.LOGGER.info("Desert broad terracotta grade ratio={}..{}; detail noise unchanged", minimumRatio, maximumRatio);
        helper.succeed();
    }

    private static int[] widths(DensityFunction active, int x, double bend, boolean revised) {
        double period = revised ? DesertBandLayout.PERIOD : DesertBandLayout.ORIGINAL_PERIOD;
        int[] widths = new int[4];
        for (int z = (int) Math.ceil(-bend - period / 2); z < -bend + period / 2; z++) {
            double land = revised ? active.compute(new DensityFunction.SinglePointContext(x, 0, z)) : originalContour(z + bend);
            int band = land <= DesertHeight.WATER_EDGE_NOISE ? 0 : land < DesertHeight.RED_SAND_START_NOISE ? 1
                    : land < DesertHeight.TERRACOTTA_START_NOISE ? 2 : 3;
            widths[band]++;
        }
        return widths;
    }

    private static double originalContour(double distance) {
        return .02 - .60 * Math.cos(2 * Math.PI * distance / DesertBandLayout.ORIGINAL_PERIOD);
    }

    private static double previousHeight(double land, double detail) {
        double shore = DesertHeight.SEA_LEVEL - 1.5;
        if (land <= DesertHeight.WATER_EDGE_NOISE) return Math.max(DesertHeight.BOTTOM, shore + (land - DesertHeight.WATER_EDGE_NOISE) * 50);
        if (land <= DesertHeight.RED_SAND_START_NOISE) return shore + (Math.min(land, DesertHeight.LAST_SAND_NOISE) - DesertHeight.WATER_EDGE_NOISE) * 50 / 3;
        double rise = land - DesertHeight.RED_SAND_START_NOISE;
        return DesertHeight.RED_SAND_START_HEIGHT + DesertHeight.upperContour(DesertHeight.STEEP_START_NOISE + rise) - 75
                + detail * 5 * Math.min(1, rise / .08);
    }
}
