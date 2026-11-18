package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.CaveSupports;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CaveNaturalnessTests {
    private CaveNaturalnessTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void galleryReliefHasRoundedPeaksAndIndependentBoundedCeilings(GameTestHelper helper) {
        double previous = DimensionTerrain.galleryRelief(-2);
        for (int step = 1; step <= 400; step++) {
            double detail = -2 + step / 100.0;
            double floor = DimensionTerrain.galleryRelief(detail);
            helper.assertTrue(floor > previous && floor > -2 && floor < 2,
                    "Gallery relief must remain bounded and smoothly rise beyond the old flat +/-0.5 clamp");
            helper.assertTrue(Math.abs(floor + DimensionTerrain.galleryRelief(-detail)) < 1e-9,
                    "Natural relief must preserve equally bounded rises and depressions");
            for (double basin : new double[]{-2, -.5, 0, .5, 2}) {
                double ceiling = DimensionTerrain.galleryCeilingRelief(detail, basin);
                helper.assertTrue(ceiling > -2 && ceiling < 2,
                        "Independent ceiling relief must remain inside its existing two-block band");
            }
            previous = floor;
        }
        helper.assertTrue(DimensionTerrain.galleryRelief(0) == 0,
                "The central floor height must remain unchanged");
        helper.assertTrue(DimensionTerrain.galleryCeilingRelief(.25, -.5)
                        < DimensionTerrain.galleryCeilingRelief(.25, .5),
                "Ceilings must respond to their second noise field instead of copying the floor exactly");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void desertAndNetherPillarsHaveBoundedSeededIrregularOutlines(GameTestHelper helper) throws Exception {
        int checked = 0;
        for (long seed : new long[]{1, 12345, -8152788754161584091L}) {
            CaveSupports supports = new CaveSupports(seed, DensityFunctions.constant(0));
            CaveSupports repeated = new CaveSupports(seed, DensityFunctions.constant(0));
            int seedChecked = 0;
            for (int cellX = -4; cellX <= 4 && seedChecked < 8; cellX++) {
                for (int cellZ = -4; cellZ <= 4 && seedChecked < 8; cellZ++) {
                    Object support = pillar(supports, cellX, cellZ);
                    double x = value(support, "x"), z = value(support, "z"), radius = value(support, "radius");
                    if (!isolated(supports, support, x, z)) continue;
                    double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
                    for (int step = 0; step < 48; step++) {
                        double angle = step * Math.PI / 24;
                        int px = (int) Math.round(x + Math.cos(angle) * 8), pz = (int) Math.round(z + Math.sin(angle) * 8);
                        double actual = supports.density(px, pz, 0, 0);
                        double offset = actual - (radius - Math.hypot(px - x, pz - z));
                        helper.assertTrue(Math.abs(offset) <= 1.4 + 1e-9,
                                "Irregularity must stay inside the bridge-clearance allowance");
                        helper.assertTrue(actual == repeated.density(px, pz, 0, 0),
                                "The same seed must retain identical pillar contours across the dimension seam");
                        minimum = Math.min(minimum, offset);
                        maximum = Math.max(maximum, offset);
                    }
                    helper.assertTrue(maximum - minimum > 1,
                            "A support must have an irregular outline instead of a perfect circular cylinder");
                    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                        helper.assertTrue(supports.density((int) Math.round(x) + dx, (int) Math.round(z) + dz, 0, -1) > 0,
                                "Even recessed contours must preserve a broad solid core through every cave layer");
                    }
                    seedChecked++;
                }
            }
            helper.assertTrue(seedChecked == 8, "The outline audit must inspect eight isolated pillars for each seed");
            checked += seedChecked;
        }
        helper.assertTrue(checked == 24, "The audit must cover all three seeds");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowySupportsKeepTheirOriginalCircularShape(GameTestHelper helper) throws Exception {
        for (long seed : new long[]{1, 12345, -8152788754161584091L}) {
            CaveSupports supports = new CaveSupports(seed);
            for (int cellX = -1; cellX <= 1; cellX++) for (int cellZ = -1; cellZ <= 1; cellZ++) {
                Object support = pillar(supports, cellX, cellZ);
                double x = value(support, "x"), z = value(support, "z"), radius = value(support, "radius");
                for (int step = 0; step < 32; step++) {
                    double angle = step * Math.PI / 16;
                    int px = (int) Math.round(x + Math.cos(angle) * 8), pz = (int) Math.round(z + Math.sin(angle) * 8);
                    helper.assertTrue(Math.abs(supports.density(px, pz, 0, 0)
                                    - (radius - Math.hypot(px - x, pz - z))) < 1e-9,
                            "The Desert/Nether contour change must not reshape Snowy's no-ramp supports");
                }
            }
        }
        helper.succeed();
    }

    private static boolean isolated(CaveSupports supports, Object target, double x, double z) throws Exception {
        int centerX = Math.floorDiv((int) Math.floor(x), 72), centerZ = Math.floorDiv((int) Math.floor(z), 72);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) {
            Object neighbor = pillar(supports, centerX + dx, centerZ + dz);
            if (neighbor != target && Math.hypot(value(neighbor, "x") - x, value(neighbor, "z") - z) < 30) return false;
        }
        return true;
    }

    private static Object pillar(CaveSupports supports, int x, int z) throws Exception {
        var method = CaveSupports.class.getDeclaredMethod("pillar", int.class, int.class);
        method.setAccessible(true);
        return method.invoke(supports, x, z);
    }

    private static double value(Object support, String name) throws Exception {
        Field field = support.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(support);
    }
}
