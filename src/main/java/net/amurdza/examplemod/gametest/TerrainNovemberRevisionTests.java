package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.SavannaHeight;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerrainNovemberRevisionTests {
    private TerrainNovemberRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void landAndWaterProfilesHaveRequestedGrades(GameTestHelper helper) {
        double epsilon = 0.000001;
        for (double distance : new double[]{1, 32, 128, 384}) {
            double slope = (SavannaHeight.height(distance + epsilon) - SavannaHeight.height(distance - epsilon)) / (2 * epsilon);
            helper.assertTrue(Math.abs(slope - 1.0 / 18) < .000001,
                    "Savanna's dry grade must double its previous slope within the approved shorter cycles");
        }
        for (double distance : new double[]{-1, -8, -10}) {
            double slope = (SavannaHeight.height(distance + epsilon) - SavannaHeight.height(distance - epsilon)) / (2 * epsilon);
            helper.assertTrue(Math.abs(slope - .45) < .000001,
                    "The submerged bank must retain its current 0.45 grade before the bottom easing");
        }
        helper.assertTrue(SavannaHeight.height(0) == 61.5 && SavannaHeight.height(-34) == 54.5
                        && SavannaHeight.height(-128) == 54.5,
                "Savanna lakes must level at the eight-block depth after a gently descending bank");
        double sandy = (DesertHeight.height(-.3 + epsilon, 0) - DesertHeight.height(-.3 - epsilon, 0)) / (2 * epsilon);
        helper.assertTrue(Math.abs(sandy - 50.0 / 3) < .000001, "Sandy grade must be one third");
        helper.assertTrue(DesertHeight.SEA_LEVEL - 1 - DesertHeight.BOTTOM == 4, "Desert maximum water depth must remain four");
        double previousRedWidth = Math.pow(15.0 / 145, 2.0 / 3) - Math.pow(8.0 / 145, 2.0 / 3);
        helper.assertTrue(Math.abs(DesertHeight.TERRACOTTA_START_NOISE - DesertHeight.RED_SAND_START_NOISE
                - previousRedWidth * 1.25) < 1e-9, "Red-sand contour band must be half its previous width");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void narrowedSnowyForestPreservesShoreAndNativeDappledGrass(GameTestHelper helper) {
        double shore = MountainLakeOffset.LAKE_WEIRDNESS;
        double compression = .3 / (.4 - shore);
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(shore) - 62.5) < 1e-9,
                "Lake and forest must share a shoreline height");
        for (double distance : new double[]{.001, .02, .06, .079, .085, .09}) {
            double rise = distance < .08 ? 10 * distance + 30 * distance * distance / .16 : 40 * distance - 1.2;
            helper.assertTrue(Math.abs(SnowyTerrainProfile.height(shore + distance / compression) - 62.5 - rise) < 1e-9,
                    "The narrower forest must retain the authored shoreline contour");
        }
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(SnowyTerrainProfile.FOREST_GENTLE_START) - 65) < 1e-9,
                "The forest grade transition must still occur at Y65");
        helper.assertTrue(SnowyTerrainProfile.height(.9) == SnowyTerrainProfile.height(.99), "Taiga summit must be a level plateau");
        for (double boundary : new double[]{shore, shore + .08 / compression, SnowyTerrainProfile.FOREST_GENTLE_START, .4, .85}) {
            helper.assertTrue(Math.abs(SnowyTerrainProfile.height(boundary - 1e-8)
                            - SnowyTerrainProfile.height(boundary + 1e-8)) < .0001,
                    "Terrain height must remain continuous at weirdness " + boundary);
        }
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        helper.assertFalse(generator.generatorSettings().value().isAquifersEnabled(), "Snowy underground pockets must have no aquifer barriers");
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (String id : new String[]{"snowy_forest", "dappled_forest", "mountain_lake", "mushroom_caves"}) {
            var biome = biomes.get(new ResourceLocation("aoemod", id));
            if (biome != null) helper.assertFalse(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).iterator().hasNext(),
                    "Independent carvers must not erase snowy pillars or create disconnected mouths: " + id);
        }
        var forest = biomes.get(new ResourceLocation("aoemod:dappled_forest"));
        var lake = biomes.get(new ResourceLocation("aoemod:mountain_lake"));
        var originalDappled = biomes.get(new ResourceLocation("minecraft:dappled_forest"));
        helper.assertTrue(originalDappled != null && forest.getGrassColor(0, 0) == originalDappled.getGrassColor(0, 0)
                        && lake.getGrassColor(0, 0) == originalDappled.getGrassColor(0, 0),
                "Lake and dappled grass must both match the native dappled palette");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyLakeKeepsOneDescendingGradeDespiteChangingNoiseGradientAndCaverns(GameTestHelper helper) {
        var ridges = new QuadraticRidge();
        var profile = new SnowyTerrainProfile(ridges, DensityFunctions.constant(-128));
        var surface = new SnowyTerrainProfile(ridges, DensityFunctions.constant(128), true);
        double previous = SnowyTerrainProfile.height(MountainLakeOffset.LAKE_WEIRDNESS);
        for (int x = 1; x <= 200; x++) {
            var point = new DensityFunction.SinglePointContext(x, 0, 0);
            double weirdness = ridges.compute(point);
            double floor = surface.compute(point) * 8 - .5;
            helper.assertTrue(floor < previous && Math.abs(floor - (62.5 - 500 * (MountainLakeOffset.LAKE_WEIRDNESS - weirdness))) < 1e-9,
                    "Progress toward lower weirdness must deepen the lake without trenches or reversals at x=" + x);
            for (int y = (int) Math.floor(floor) - 2; y <= (int) Math.ceil(floor); y++) {
                var column = new DensityFunction.SinglePointContext(x, y, 0);
                helper.assertTrue(profile.compute(column) == surface.compute(column),
                        "Even fully open cavern noise must not change the four-block ocean bed at " + x + "," + y);
            }
            helper.assertTrue(profile.compute(new DensityFunction.SinglePointContext(x, -32, 0)) < 0,
                    "The bed protection must still permit underground cheese caves");
            helper.assertTrue(SnowyTerrainProfile.height(-weirdness) == SnowyTerrainProfile.height(weirdness),
                    "Both sides of the river must use the same depth progression");
            previous = floor;
        }
        helper.assertTrue(Math.abs(previous - (22.5)) < 1e-9,
                "The steeper submerged grade must descend forty blocks over this transect");
        helper.succeed();
    }

    private record QuadraticRidge() implements DensityFunction.SimpleFunction {
        @Override public double compute(FunctionContext context) {
            double t = context.blockX() / 200.0;
            return MountainLakeOffset.LAKE_WEIRDNESS - .08 * t * t;
        }
        @Override public double minValue() { return -Double.MAX_VALUE; }
        @Override public double maxValue() { return MountainLakeOffset.LAKE_WEIRDNESS; }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return DensityFunctions.constant(0).codec(); }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void basaltRiverHasBottomAndBridgeAndDesertLakesKeepLowerOutlets(GameTestHelper helper) {
        double center = DimensionTerrain.STAIR_BAND_SPACING / 2;
        double floor = DimensionTerrain.basaltFloor(154, center, 0, 0);
        helper.assertTrue(floor == 2 && DimensionTerrain.BASALT_LAVA_LEVEL - 1 - floor == 8,
                "Basalt river must have three solid layers including bedrock and eight lava blocks");
        helper.assertTrue(DimensionTerrain.basaltFloor(0, center, 0, 0) > DimensionTerrain.BASALT_LAVA_LEVEL,
                "The lowest transverse landing must continue over the river as a dry bridge");
        helper.assertTrue(DimensionTerrain.basaltFloor(154, center - 40, 0, 0) > DimensionTerrain.BASALT_LAVA_LEVEL,
                "River must have land on both sides");
        var zero = DensityFunctions.constant(0);
        var wet = new DimensionTerrain("desert", "density", DensityFunctions.constant(-1), zero, zero, zero, zero, zero);
        var dry = new DimensionTerrain("desert", "density", DensityFunctions.constant(0), zero, zero, zero, zero, zero);
        var topRamp = new DensityFunction.SinglePointContext(308, 16, 111);
        helper.assertTrue(wet.compute(topRamp) < 0 && dry.compute(topRamp) < 0,
                "Lake columns must retain the underground part of the upper ramp");
        helper.assertTrue(wet.compute(new DensityFunction.SinglePointContext(308, 45, 171)) < 0,
                "A lake column must retain the outlet below its protective roof");
        for (int y = 46; y <= 53; y++) helper.assertTrue(Math.abs(wet.compute(new DensityFunction.SinglePointContext(308, y, 181))
                        - (DesertHeight.BOTTOM + .5 - y) / 8) < 1e-9,
                "A lake column must preserve its original density from Y46 upward");
        helper.succeed();
    }
}
