package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertBands;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerrainFollowupTests {
    private TerrainFollowupTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void desertRemappingRetainsPreviouslyDoubledSeededBends(GameTestHelper helper) {
        var warp = new TestWarp();
        var bands = new DesertBands(warp);
        for (int x = -2000; x <= 2000; x += 79) for (int z = -2000; z <= 2000; z += 71) {
            double formerBend = 192 * Math.tanh(warp.compute(new DensityFunction.SinglePointContext(x * 2, 0, 0)));
            double remapped = net.amurdza.examplemod.worldgen.DesertBandLayout.originalCoordinate(z + formerBend / 2);
            double formerAtDoubleCoordinates = .02 - .60 * Math.cos(2 * Math.PI * remapped / 768);
            helper.assertTrue(Math.abs(bands.compute(new DensityFunction.SinglePointContext(x, 0, z))
                            - formerAtDoubleCoordinates) < 1e-12,
                    "The new band distances must retain the exact previous XZ-scaled bend field");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void seededMountainsKeepSlowerCurvatureAndNarrowForestWithSteeperFoothills(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        double oldWidth = 0, newWidth = 0, oldSlope = 0, newSlope = 0;
        for (double z = 0; z < 480; z += .125) {
            double w = contour(z / 480);
            if (w >= .25 && w <= .4) oldWidth += .125;
            if (w >= MountainLakeOffset.LAKE_WEIRDNESS && w <= .4) newWidth += .125;
            if (w < .4) continue;
            double next = contour((z + .125) / 480);
            oldSlope = Math.max(oldSlope, (oldHeight(next) - oldHeight(w)) / .125);
            newSlope = Math.max(newSlope, (SnowyTerrainProfile.height(next) - SnowyTerrainProfile.height(w)) / .125);
        }
        helper.assertTrue(Math.abs(newWidth / oldWidth - .5) < .003,
                "The forest must be half as wide in blocks, accounting for the nonlinear ridge contour");
        helper.assertTrue(newSlope > .85 && newSlope < 1.1,
                "The fortress foothills must rise steeply while remaining a traversable mountainside: " + oldSlope + " -> " + newSlope);
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(.28 + .001) - SnowyTerrainProfile.height(.28) - .5) < 1e-9,
                "Water-region grade must quadruple from the interim125 to500 blocks per weirdness unit");
        StringBuilder report = new StringBuilder("Snowy terrain revision\n")
                .append("Dappled width in blocks: ").append(oldWidth).append(" -> ").append(newWidth).append('\n')
                .append("Maximum transverse mountain slope: ").append(oldSlope).append(" -> ").append(newSlope).append('\n');
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            MountainRidges[] found = {null};
            random.router().ridges().mapAll(function -> {
                if (function instanceof MountainRidges ridge) found[0] = ridge;
                return function;
            });
            helper.assertTrue(found[0] != null, "Audit the actual seeded mountain field");
            var ridge = found[0];
            double oldCurvature = 0, newCurvature = 0;
            for (int x = -16384; x <= 16384; x += 8) {
                double before = bend(ridge, x - 8, true) - 2 * bend(ridge, x, true) + bend(ridge, x + 8, true);
                double after = bend(ridge, x - 8, false) - 2 * bend(ridge, x, false) + bend(ridge, x + 8, false);
                oldCurvature += before * before;
                newCurvature += after * after;
            }
            double ratio = Math.sqrt(newCurvature / oldCurvature);
            helper.assertTrue(ratio < .15,
                    "Seeded ridgelines must curve more gradually across X: seed=" + seed + " RMS curvature ratio=" + ratio);
            report.append("Seed ").append(seed).append(" bend RMS curvature ratio: ").append(ratio).append('\n');
            if (seed == 12345) picture(ridge, oldWidth, newWidth, oldSlope, newSlope);
        }
        Path evidence = Path.of("evidence", "terrain-followup-audit.txt");
        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, report.toString());
        AOEMod.LOGGER.info("{}", report);
        helper.succeed();
    }

    private static double bend(MountainRidges ridge, double x, boolean previous) {
        double sampleX = x * (previous ? .25 : .0625) / ridge.stretch();
        double scale = previous ? 1 : .5;
        return scale * (160 * Math.tanh(ridge.ridge().getValue(sampleX, 0, 0))
                + 80 * Math.tanh(ridge.shift().getValue(sampleX, 0, 8192)));
    }

    private static double contour(double t) {
        if (t <= .7) return hermite(t / .7, 0, .4, .55 * .7, .7 * .7);
        return hermite((t - .7) / .3, .4, .9, .7 * .3, 0);
    }

    private static double hermite(double t, double a, double b, double da, double db) {
        return (2 * t * t * t - 3 * t * t + 1) * a + (t * t * t - 2 * t * t + t) * da
                + (-2 * t * t * t + 3 * t * t) * b + (t * t * t - t * t) * db;
    }

    private static double oldHeight(double w) {
        w = Math.abs(w);
        if (w < .25) {
            double depth = Math.min(50, (.25 - w) * 250);
            if (depth > 46) { double t = (depth - 46) / 4; depth = 46 + 4 * (t + t * t - t * t * t); }
            return 62.5 - depth;
        }
        if (w <= .4) {
            double distance = (w - .25) * 2;
            if (distance < .08) return 62.5 + 10 * distance + 30 * distance * distance / .16;
            double gentleStart = .25 + 3.7 / 80;
            if (w <= gentleStart) return 62.5 + 40 * distance - 1.2;
            return 65 + 40 * (w - gentleStart);
        }
        double top = 69.15 + 217.5 * Math.pow(.45, 1.5);
        if (w >= .85) return top;
        if (w <= .7) return 69.15 + 217.5 * Math.pow(w - .4, 1.5);
        return hermite((w - .7) / .15, 69.15 + 217.5 * Math.pow(.3, 1.5), top,
                326.25 * Math.sqrt(.3) * .15, 0);
    }

    private static void picture(MountainRidges ridge, double oldWidth, double newWidth, double oldSlope, double newSlope) throws Exception {
        var image = new BufferedImage(1200, 1040, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xf7f5ef)); g.fillRect(0, 0, 1200, 1040);
            g.setColor(new Color(0x25303b)); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.drawString("Snowy terrain: slower bends and steeper foothills", 30, 42);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            g.drawString("Seed 12345 | Density surface preview; colors show water, dappled forest and snowy mountain elevation", 30, 72);
            for (int panel = 0; panel < 2; panel++) {
                boolean previous = panel == 0;
                int left = 30 + panel * 590;
                g.setColor(new Color(0x25303b)); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
                g.drawString(previous ? "Previous" : "Updated", left, 112);
                for (int px = 0; px < 550; px++) {
                    double x = (px / 549.0 - .5) * 4096;
                    double bend = bend(ridge, x, previous);
                    for (int pz = 0; pz < 410; pz++) {
                        double z = (pz / 409.0 - .5) * 1920;
                        double phase = (z + bend) / (960 * ridge.stretch());
                        phase -= Math.floor(phase);
                        double w = contour(Math.abs(1 - 4 * Math.abs(phase - .5)));
                        double y = previous ? oldHeight(w) : SnowyTerrainProfile.height(w);
                        Color color = w < (previous ? .25 : MountainLakeOffset.LAKE_WEIRDNESS)
                                ? new Color(0x3c7fa4) : w < .4 ? new Color(0xc79a51)
                                : Color.getHSBColor(.58f, .08f, (float) Math.max(.55, Math.min(1, .63 + (y - 69) / 180)));
                        g.setColor(color); g.fillRect(left + px, 128 + pz, 1, 1);
                    }
                }
            }
            g.setColor(new Color(0x25303b)); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            g.drawString("Each map: X -2048..2048; Z -960..960 blocks", 30, 568);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
            g.drawString("Unbent cross section from lake center to summit (horizontal distance in blocks)", 30, 615);
            int left = 80, top = 655, width = 1040, height = 260;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            for (int y = 20; y <= 140; y += 20) {
                int py = top + height - y * height / 150;
                g.setColor(new Color(0xd6d8da)); g.drawLine(left, py, left + width, py);
                g.setColor(new Color(0x25303b)); g.drawString(Integer.toString(y), left - 35, py + 5);
            }
            for (int z = 0; z <= 480; z += 80) {
                int px = left + z * width / 480;
                g.setColor(new Color(0xd6d8da)); g.drawLine(px, top, px, top + height);
                g.setColor(new Color(0x25303b)); g.drawString(Integer.toString(z), px - 12, top + height + 24);
            }
            g.setStroke(new BasicStroke(3));
            for (int series = 0; series < 2; series++) {
                g.setColor(series == 0 ? new Color(0xb6543f) : new Color(0x236b7b));
                for (int z = 1; z <= 480; z++) {
                    double a = contour((z - 1) / 480.0), b = contour(z / 480.0);
                    double ya = series == 0 ? oldHeight(a) : SnowyTerrainProfile.height(a);
                    double yb = series == 0 ? oldHeight(b) : SnowyTerrainProfile.height(b);
                    g.drawLine(left + (z - 1) * width / 480, top + height - (int) (ya * height / 150),
                            left + z * width / 480, top + height - (int) (yb * height / 150));
                }
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            g.setColor(new Color(0xb6543f)); g.drawString("Previous", 80, 975);
            g.setColor(new Color(0x236b7b)); g.drawString("Updated", 190, 975);
            g.setColor(new Color(0x25303b));
            g.drawString(String.format("Forest width %.1f -> %.1f blocks | Peak mountain grade %.2f -> %.2f", oldWidth, newWidth, oldSlope, newSlope), 330, 975);
            g.drawString("Illustrates generated density geometry; this is not an in-game rendering.", 30, 1015);
            Path path = Path.of("evidence", "snowy-terrain-followup.png");
            Files.createDirectories(path.getParent());
            ImageIO.write(image, "png", path.toFile());
        } finally { g.dispose(); }
    }

    private record TestWarp() implements DensityFunction.SimpleFunction {
        @Override public double compute(FunctionContext context) { return Math.sin(context.blockX() / 317.0) * .6; }
        @Override public double minValue() { return -.6; }
        @Override public double maxValue() { return .6; }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return DensityFunctions.constant(0).codec(); }
    }
}
