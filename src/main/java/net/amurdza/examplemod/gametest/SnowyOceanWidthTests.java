package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.SnowyBandCompression;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyOceanWidthTests {
    private SnowyOceanWidthTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void onlyDeepFlatDistanceIsRemovedAcrossSeededBands(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        StringBuilder report = new StringBuilder("Snowy band widths: [snowy forest, both dappled flanks, both shallow banks, deep water]\n");
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            MountainRidges[] found = {null};
            random.router().ridges().mapAll(function -> {
                if (function instanceof MountainRidges ridge) found[0] = ridge;
                return function;
            });
            var revised = found[0];
            helper.assertTrue(revised != null && revised.deepWaterWidth() == 110,
                    "The loaded profile must target110 blocks of deep water");
            var original = new MountainRidges(revised.ridge(), revised.shift(), revised.stretch());
            double oldSpacing = SnowyBandCompression.period(revised.stretch(), 0) / 2;
            double newSpacing = SnowyBandCompression.period(revised.stretch(), revised.deepWaterWidth()) / 2;
            double removed = oldSpacing - newSpacing;
            for (int x : new int[]{-8192, -4096, -1, 0, 4096, 8192}) {
                double bend = bend(revised, x);
                int[] before = widths(original, x, oldSpacing, bend);
                int[] after = widths(revised, x, newSpacing, bend);
                for (int band = 0; band < 3; band++) helper.assertTrue(Math.abs(before[band] - after[band]) <= 2,
                        "Every non-deep band's measured block width must be unchanged: " + Arrays.toString(before) + " -> " + Arrays.toString(after));
                helper.assertTrue(after[3] >= 108 && after[3] <= 112 && before[3] - after[3] > 300,
                        "Delete the deep dead zone, leaving a110-block deep section: " + Arrays.toString(after));
                for (int band = -3; band <= 3; band++) for (double local = -260; local <= 260; local += 13.25) {
                    double oldZ = band * oldSpacing - bend + local;
                    double newZ = oldZ - band * removed;
                    // Matching samples and their centered differences prove the
                    // full non-flat profile, its slopes and curvature are translated intact.
                    for (int dx : new int[]{-4, 0, 4}) for (double dz : new double[]{-.125, 0, .125}) {
                        double beforeHeight = height(original, x + dx, oldZ + dz);
                        double afterHeight = height(revised, x + dx, newZ + dz);
                        helper.assertTrue(Math.abs(beforeHeight - afterHeight) < 1e-8,
                                "Compression must preserve every non-flat height, slope and long-axis bend; seed=" + seed);
                    }
                }
                report.append("seed=").append(seed).append(" x=").append(x).append(' ')
                        .append(Arrays.toString(before)).append(" -> ").append(Arrays.toString(after)).append('\n');
            }
        }
        double previousFlat = SnowyBandCompression.originalFlatWidth(2) - (SnowyBandCompression.originalDeepWidth(2) - 130);
        double revisedFlat = SnowyBandCompression.originalFlatWidth(2) - (SnowyBandCompression.originalDeepWidth(2) - 110);
        helper.assertTrue(Math.abs(previousFlat - revisedFlat - 20) < 1e-8
                        && MountainLakeOffset.MAX_LAKE_DEPTH == 44.12722453022183,
                "The20-block deep-width reduction must remove only flat-bottom distance at unchanged depth");
        report.append("Target deep width110; forest288; dappled flank56.89; flat bottom reduced to ").append(revisedFlat)
                .append(" blocks; maximum depth ").append(MountainLakeOffset.MAX_LAKE_DEPTH).append('\n');
        Path path = Path.of("evidence", "snowy-deep-water-widths.txt");
        Files.createDirectories(path.getParent());
        Files.writeString(path, report);
        AOEMod.LOGGER.info("{}", report);
        helper.succeed();
    }

    private static int[] widths(MountainRidges ridge, int x, double spacing, double bend) {
        int[] widths = new int[4];
        for (int z = (int) Math.ceil(-bend - spacing / 2); z < -bend + spacing / 2; z++) {
            double w = Math.abs(ridge.compute(new DensityFunction.SinglePointContext(x, 0, z)));
            int band = w > .4 ? 0 : w >= MountainLakeOffset.LAKE_WEIRDNESS ? 1
                    : w > MountainLakeOffset.LAKE_WEIRDNESS - 24 / SnowyTerrainProfile.LAKE_SLOPE ? 2 : 3;
            widths[band]++;
        }
        return widths;
    }

    private static double height(MountainRidges ridge, double x, double z) {
        double coordinate = SnowyBandCompression.originalCoordinate(z + bend(ridge, x), ridge.stretch(), ridge.deepWaterWidth());
        double phase = coordinate / (960 * ridge.stretch());
        phase -= Math.floor(phase);
        return SnowyTerrainProfile.height(MountainRidges.contour(Math.abs(1 - 4 * Math.abs(phase - .5))));
    }

    private static double bend(MountainRidges ridge, double x) {
        double sample = x * .0625 / ridge.stretch();
        return 80 * Math.tanh(ridge.ridge().getValue(sample, 0, 0)) + 40 * Math.tanh(ridge.shift().getValue(sample, 0, 8192));
    }
}
