package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveSupports;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CavePassageRevisionTests {
    private CavePassageRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void seededDesertOutletJunctionKeepsClimbableSteps(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : new long[]{12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            for (int stripe = -1; stripe <= 1; stripe++) {
                var outlet = terrain.desertOutlet(0, stripe);
                int previous = Integer.MIN_VALUE;
                for (int distance = 0; distance <= 25; distance++) {
                    int x = (int) Math.round(outlet.xAt(distance)), z = (int) Math.round(outlet.zAt(distance));
                    var column = generator.getBaseColumn(x, z, level, random);
                    int feet = Integer.MIN_VALUE;
                    for (int y = 8; y <= 35; y++) if (column.getBlock(y - 1).isSolid()
                            && column.getBlock(y).isAir() && column.getBlock(y + 1).isAir()) { feet = y; break; }
                    helper.assertTrue(feet != Integer.MIN_VALUE && (previous == Integer.MIN_VALUE || Math.abs(feet - previous) <= 1),
                            "Seeded branch center must have climbable steps: seed=" + seed + ", stripe=" + stripe
                                    + " at " + x + "," + z + ": " + previous + " to " + feet);
                    previous = feet;
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void highDesertOutletsKeepOneContinuousHalfBlockGrade(GameTestHelper helper) {
        var zero = DensityFunctions.constant(0);
        var terrain = new DimensionTerrain("desert", "density", DensityFunctions.constant(1),
                zero, zero, zero, zero, zero);
        double surfaceHeight = DesertHeight.height(1, 0);
        int surfaceTop = (int) Math.ceil(surfaceHeight + .5) - 1;
        int previousFloor = Integer.MIN_VALUE;
        int rampColumns = 0, surfaceColumns = 0;
        var outlet = terrain.desertOutlet(0, 0);
        for (int distance = 12; distance <= outlet.length() + 40; distance++) {
            int x = (int) Math.round(outlet.xAt(distance)), z = (int) Math.round(outlet.zAt(distance));
            int rampFloor = (int) Math.floor(DimensionTerrain.desertOutletFloor(distance) + .5);
            // Once the fixed-grade ramp reaches the lowered terrain, walking
            // continues along that surface instead of on an imaginary air ramp.
            int expectedFloor = Math.min(rampFloor, surfaceTop), floor = Integer.MIN_VALUE;
            if (rampFloor < surfaceTop) rampColumns++; else surfaceColumns++;
            for (int candidate = expectedFloor - 1; candidate <= expectedFloor + 1; candidate++) {
                if (terrain.compute(new DensityFunction.SinglePointContext(x, candidate, z)) >= 0
                        && terrain.compute(new DensityFunction.SinglePointContext(x, candidate + 1, z)) < 0
                        && terrain.compute(new DensityFunction.SinglePointContext(x, candidate + 2, z)) < 0) floor = candidate;
            }
            helper.assertTrue(floor != Integer.MIN_VALUE,
                    "The curved center must retain its intended supported grade, without end-cap scooping, at distance " + distance);
            helper.assertTrue(previousFloor == Integer.MIN_VALUE || Math.abs(floor - previousFloor) <= 1,
                    "High surface elevation must lengthen the outlet instead of increasing its grade");
            previousFloor = floor;
        }
        helper.assertTrue(rampColumns > 100 && surfaceColumns > 20,
                "Exercise the long fixed-grade ascent and its continuous transition onto the current surface");
        helper.assertTrue(DimensionTerrain.desertOutletFloor(outlet.length() - 24) >= surfaceHeight + 2
                        && DimensionTerrain.desertOutletFloor(outlet.length() - 26) < surfaceHeight + 2,
                "The outlet must retain a full 24-block clear exit run before ending");
        helper.assertTrue(terrain.compute(new DensityFunction.SinglePointContext(308, 35, 72)) > 0,
                "The opposite desert branch must remain solid");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void surfaceAscentsTurnGraduallyWithoutMovingTheirLowerLandings(GameTestHelper helper) {
        var zero = DensityFunctions.constant(0);
        var terrain = new DimensionTerrain("desert", "density", DensityFunctions.constant(1),
                zero, zero, zero, zero, zero);
        java.util.Set<String> mouths = new java.util.HashSet<>();
        java.util.Set<Integer> headings = new java.util.HashSet<>();
        for (int stripe = -12; stripe <= 12; stripe++) {
            var outlet = terrain.desertOutlet(0, stripe);
            for (int d = -5; d <= 12; d++) helper.assertTrue(outlet.xAt(d) == outlet.x() && outlet.zAt(d) == outlet.z() + d,
                    "Lower junctions and their level landing must keep their original coordinates");
            mouths.add(Math.round((outlet.xAt(outlet.length()) - outlet.x()) / 8) + ":"
                    + Math.round((outlet.zAt(outlet.length()) - outlet.z()) / 8));
            headings.add((int) Math.floor((outlet.headingAt(outlet.length()) + Math.PI) * 4 / Math.PI));
            double turn = 0, minimum = Double.POSITIVE_INFINITY, maximum = 0;
            for (int d = 14; d < 639; d++) {
                double delta = outlet.headingAt(d + 1) - outlet.headingAt(d);
                delta = Math.atan2(Math.sin(delta), Math.cos(delta));
                helper.assertTrue(delta * Math.signum(outlet.bendA()) > 0,
                        "Every successive ascent turn must accumulate in the same handedness");
                turn += Math.abs(delta);
                minimum = Math.min(minimum, Math.abs(delta)); maximum = Math.max(maximum, Math.abs(delta));
                helper.assertTrue(Math.abs(outlet.xAt(d) - outlet.x()) <= DimensionTerrain.DESERT_OUTLET_MAX_BEND
                                && Math.abs(outlet.zAt(d) - outlet.z()) <= DimensionTerrain.DESERT_OUTLET_MAX_BEND
                                && Math.abs(delta) < .08,
                        "The spiral must stay within its density search bounds and turn gradually");
            }
            helper.assertTrue(turn > Math.PI * 2 && maximum > minimum * 1.3,
                    "An ascent must accumulate a spiral with changing angular velocity, not alternate side to side");
            for (int y = 13; y <= 21; y++) helper.assertTrue(outlet.density((int) Math.round(outlet.x()), y,
                            (int) Math.round(outlet.z() - 4)) < 0,
                    "The original flat landing must retain its entire nine-block air clearance");
        }
        helper.assertTrue(mouths.size() > 15 && headings.size() >= 6,
                "Neighboring cave mouths need widely varied positions and compass headings");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void curvedMouthsFinishOnlyAfterAContinuousDryExit(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int exits = 0;
        for (long seed : new long[]{12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            for (int stripe = -8; stripe <= 8; stripe++) {
                var outlet = terrain.desertOutlet(0, stripe);
                if (outlet.length() == 640) continue;
                exits++;
                for (int distance = outlet.length() - 24; distance <= outlet.length(); distance += 2) {
                    double heading = outlet.headingAt(distance);
                    for (int across = -9; across <= 9; across += 3) {
                        int x = (int) Math.floor(outlet.xAt(distance) + across * Math.cos(heading));
                        int z = (int) Math.floor(outlet.zAt(distance) - across * Math.sin(heading));
                        var point = new DensityFunction.SinglePointContext(x, 0, z);
                        double height = DesertHeight.height(terrain.land().compute(point), terrain.detail().compute(point));
                        helper.assertTrue(!DimensionTerrain.isSurfaceWater(height, DesertHeight.SEA_LEVEL)
                                        && DimensionTerrain.desertOutletFloor(distance) >= height + 2,
                                "A mouth must not stop inside a returning dry bank or lake after one low center column");
                    }
                }
            }
        }
        helper.assertTrue(exits >= 20, "Exercise complete exits across two independently seeded terrain fields");
        var zero = DensityFunctions.constant(0);
        var dryCollar = new DimensionTerrain("desert", "lake_guard", DensityFunctions.constant(-.39),
                zero, zero, zero, zero, zero);
        helper.assertTrue(!DimensionTerrain.isSurfaceWater(DesertHeight.height(-.39, 0), DesertHeight.SEA_LEVEL)
                        && dryCollar.compute(new DensityFunction.SinglePointContext(0, 50, 0)) < 0,
                "Shallow dry shore columns must not receive the lake guard's solid cutoff wall");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void seededPillarsMoveAsideForTheFullBridge(GameTestHelper helper) {
        for (long seed : new long[]{1, 12345, -8152788754161584091L}) {
            var supports = new CaveSupports(seed, DensityFunctions.constant(0));
            int nearbySolid = 0;
            for (int z = -360; z <= 360; z += 4) {
                for (int x = -16; x <= 16; x += 2) helper.assertTrue(supports.density(x, z, 0, 0) < 0,
                        "A shifted seeded pillar must clear the transverse bridge at " + x + "," + z);
                for (int x = -100; x <= 100; x += 4) if (supports.density(x, z, 0, 0) > 0) nearbySolid++;
            }
            helper.assertTrue(nearbySolid > 100, "Offsetting bridge obstructions must retain nearby random pillars");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedBasaltLandingsReachTheGalleryAndBridge(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : new long[]{12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            int x = 0, center = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2);
            for (int iteration = 0; iteration < 8; iteration++) {
                var point = new DensityFunction.SinglePointContext(x, 0, center);
                x = (int) Math.round(-terrain.rampBasin().compute(point) * 32);
                center = (int) Math.round(DimensionTerrain.STAIR_BAND_SPACING / 2 - terrain.rampDetail().compute(point) * 18);
            }
            int previousFeet = Integer.MIN_VALUE;
            for (int z = center; z <= center + 112; z++) {
                for (int iteration = 0; iteration < 6; iteration++) x = (int) Math.round(-terrain.rampBasin()
                        .compute(new DensityFunction.SinglePointContext(x, 0, z)) * 32);
                var column = generator.getBaseColumn(x, z, level, random);
                int feet = Integer.MIN_VALUE;
                for (int y = 12; y <= 24; y++) {
                    if (column.getBlock(y - 1).isSolid() && column.getBlock(y).isAir() && column.getBlock(y + 1).isAir()) {
                        feet = y;
                        break;
                    }
                }
                helper.assertTrue(feet != Integer.MIN_VALUE,
                        "The bottom landing must connect to the basalt bridge at " + x + "," + z + " seed=" + seed);
                helper.assertTrue(previousFeet == Integer.MIN_VALUE || Math.abs(feet - previousFeet) <= 1,
                        "The basalt connection must have climbable adjacent steps at " + x + "," + z);
                previousFeet = feet;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedDesertMouthsStayDryAndLakesKeepProtectedBlocks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), 12345);
        var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
        int dryOpenings = 0, protectedLakeBlocks = 0;
        for (int cycle = -6; cycle <= 6 && (dryOpenings < 12 || protectedLakeBlocks < 12); cycle++) {
            for (int stripe = -8; stripe <= 8 && (dryOpenings < 12 || protectedLakeBlocks < 12); stripe++) {
                double rootX = cycle * 616.0 + 308, rootZ = (stripe + .5) * DimensionTerrain.STAIR_BAND_SPACING;
                for (int iteration = 0; iteration < 8; iteration++) {
                    var point = new DensityFunction.SinglePointContext((int) Math.floor(rootX), 0, (int) Math.floor(rootZ));
                    rootX = cycle * 616.0 + 308 - terrain.rampBasin().compute(point) * 32;
                    rootZ = (stripe + .5) * DimensionTerrain.STAIR_BAND_SPACING - terrain.rampDetail().compute(point) * 18;
                }
                var outlet = terrain.desertOutlet(cycle, stripe);
                for (int distance = 76; distance <= 96; distance += 4) {
                    int x = (int) Math.round(outlet.xAt(distance));
                    int z = (int) Math.round(outlet.zAt(distance));
                    var point = new DensityFunction.SinglePointContext(x, 0, z);
                    double height = DesertHeight.height(terrain.land().compute(point), terrain.detail().compute(point));
                    boolean lake = DimensionTerrain.isSurfaceWater(height, DesertHeight.SEA_LEVEL);
                    if (lake ? protectedLakeBlocks >= 12 : dryOpenings >= 12) continue;
                    var column = generator.getBaseColumn(x, z, level, random);
                    for (int y = 50; y <= 53; y++) {
                        var state = column.getBlock(y);
                        if (lake && y <= height + .5) {
                            helper.assertTrue(state.isSolid(), "Interpolation carved a protected lake block at " + x + "," + y + "," + z);
                            protectedLakeBlocks++;
                        } else if (!lake) {
                            helper.assertFalse(state.is(Blocks.WATER), "A dry desert entrance flooded at " + x + "," + y + "," + z);
                            if (state.isAir()) dryOpenings++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(dryOpenings > 0 && protectedLakeBlocks > 0,
                "The actual seeded generator must exercise dry mouths and protected lake blocks: " + dryOpenings + "/" + protectedLakeBlocks);
        helper.succeed();
    }
}
