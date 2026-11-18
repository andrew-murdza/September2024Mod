package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertBands;
import net.amurdza.examplemod.worldgen.DesertCaveLayout;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertTerraceCaveTests {
    private DesertTerraceCaveTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void pairedStraightMouthsFollowNegativeTAndStayOnDrySand(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int mouths = 0, curvedSamples = 0;
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            for (int cycle : new int[]{-1, 0}) for (int stripe : new int[]{-2, -1, 0, 1}) {
                var route = terrain.desertOutlet(cycle, stripe);
                int start = route.entranceStart();
                helper.assertTrue(route.length() > start + 8 && route.length() < DesertCaveLayout.MAX_STATION,
                        "Every terrace-side route must emerge on dry terrain: seed=" + seed + ", " + cycle + "/" + stripe);
                double x = route.xAt(start), z = route.zAt(start);
                double dx = route.xAt(start + 1) - x, dz = route.zAt(start + 1) - z;
                double gx = t(terrain, x + 2, z) - t(terrain, x - 2, z);
                double gz = t(terrain, x, z + 2) - t(terrain, x, z - 2);
                helper.assertTrue((dx * gx + dz * gz) / Math.sqrt(gx * gx + gz * gz) < -.98,
                        "Entrance must follow negative T gradient, not the independent descending-tunnel bend");
                helper.assertTrue(Math.floorMod(stripe, 2) == 0 ? dz < 0 : dz > 0,
                        "Paired entrances must face opposite terrace flanks");
                double previous = t(terrain, x, z);
                for (int s = start + 1; s <= route.length(); s++) {
                    helper.assertTrue(Math.abs(route.xAt(s) - x - (s - start) * dx) < 1e-7
                                    && Math.abs(route.zAt(s) - z - (s - start) * dz) < 1e-7,
                            "No artificial curvature may remain in an entrance");
                    double current = t(terrain, route.xAt(s), route.zAt(s));
                    helper.assertTrue(current <= previous + .012, "Entrance must continue toward lower T");
                    previous = current;
                    for (int across : new int[]{-9, 0, 9}) {
                        double px = route.xAt(s) + across * dz, pz = route.zAt(s) - across * dx;
                        helper.assertFalse(DimensionTerrain.isSurfaceWater(surface(terrain, px, pz), DesertHeight.SEA_LEVEL),
                                "Entrance and its full interpolated width must not enter a lake");
                    }
                }
                double endT = t(terrain, route.xAt(route.length()), route.zAt(route.length()));
                helper.assertTrue(endT > DesertHeight.WATER_EDGE_NOISE && endT < DesertHeight.RED_SAND_START_NOISE,
                        "Each entrance must meet ordinary sand: T=" + endT + ", seed=" + seed);
                for (int s = -50; s < 48; s += 7) {
                    double expected = terrain.desertCaves().bankCoordinate(stripe) - terrain.desertCaves().bend(route.xAt(s));
                    if (Math.abs(route.zAt(s) - expected) > .5) curvedSamples++;
                    helper.assertTrue(Math.abs(t(terrain, route.xAt(s), route.zAt(s))) < .23,
                            "Descending connector must remain in the broad rock band near T=0");
                }
                mouths++;
            }
        }
        helper.assertTrue(mouths == 32 && curvedSamples > 30, "Exercise both flanks, negative coordinates and independently curved descents");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cavernsOccupyTExtremaAndLeaveSolidBanksBetweenThem(GameTestHelper helper) {
        var zero = DensityFunctions.constant(0);
        var terrain = new DimensionTerrain("desert", "density", new DesertBands(zero), zero, zero, zero, zero, zero);
        int valley = 0, peak = Mth.floor(DesertBands.PERIOD / 2), bank = Mth.floor(DesertCaveLayout.ZERO_DISTANCE);
        helper.assertTrue(terrain.compute(new DensityFunction.SinglePointContext(0, -15, valley)) < 0
                        && terrain.compute(new DensityFunction.SinglePointContext(0, -15, peak)) < 0,
                "Large caverns must exist around the minimum and maximum of the same T field");
        for (int z = bank - 12; z <= bank + 12; z++) helper.assertTrue(
                terrain.compute(new DensityFunction.SinglePointContext(0, -15, z)) > 0,
                "T=0 must retain a wide solid bank away from its authored connector");
        var nether = new DimensionTerrain("nether", "density", zero, zero, zero, zero, zero, zero);
        int seamAir = 0;
        for (int x = 180; x <= 450; x += 3) for (int z = 80; z <= 145; z += 3) {
            boolean lower = nether.compute(new DensityFunction.SinglePointContext(x, 128, z)) < 0;
            boolean upper = terrain.compute(new DensityFunction.SinglePointContext(x, -32, z)) < 0;
            helper.assertTrue(lower == upper, "Relocated Desert caves must preserve the existing Nether portal-seam openings");
            if (upper) seamAir++;
        }
        helper.assertTrue(seamAir > 40, "Exercise the real lower descending passage across the unchanged seam");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1800)
    public static void interpolatedDescentsHaveSupportedWalkingCentersAndSealedNearbyLakes(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int walking = 0, water = 0;
        for (long seed : new long[]{12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
            for (int stripe : new int[]{0, 1}) {
                var route = terrain.desertOutlet(0, stripe);
                int previous = Integer.MIN_VALUE;
                for (int station = -76; station <= route.length() + 3; station++) {
                    int x = Mth.floor(route.xAt(station)), z = Mth.floor(route.zAt(station));
                    var column = generator.getBaseColumn(x, z, level, random);
                    int nominal = Math.min(Mth.floor(route.floorAt(station) + .5), terrain.desertUncarvedSurfaceY(x, z));
                    int floor = Integer.MIN_VALUE;
                    for (int y = nominal - 2; y <= nominal + 2; y++)
                        if (column.getBlock(y).blocksMotion() && column.getBlock(y + 1).isAir() && column.getBlock(y + 2).isAir()) floor = y;
                    helper.assertTrue(floor != Integer.MIN_VALUE, "Native interpolation lost a supported walking center: seed="
                            + seed + ", stripe=" + stripe + ", station=" + station + ", at=" + x + "," + nominal + "," + z);
                    helper.assertTrue(previous == Integer.MIN_VALUE || Math.abs(floor - previous) <= 1,
                            "A descending connector must not contain an unwalkable step: " + previous + " -> " + floor + " at station=" + station);
                    if (station < route.length() - 3) helper.assertTrue(terrain.desertCaves().protectsWalkway(x, floor + 2, z),
                            "Feature decoration must reserve the actual moved tunnel center");
                    previous = floor; walking++;
                }
                int x = Mth.floor(route.xAt(route.length()));
                int center = Mth.floor((Math.floorDiv(stripe, 2) + (stripe % 2 == 0 ? 0 : 1)) * DesertBands.PERIOD - terrain.desertCaves().bend(x));
                for (int z = center - 20; z <= center + 20; z += 4) {
                    var column = generator.getBaseColumn(x, z, level, random);
                    if (!column.getBlock(53).is(Blocks.WATER)) continue;
                    for (int y = 46; y <= 48; y++) helper.assertTrue(column.getBlock(y).blocksMotion()
                                    && column.getBlock(y).getFluidState().isEmpty(), "Nearby lake bed/roof must stay sealed");
                    water++;
                }
            }
        }
        helper.assertTrue(walking > 600 && water > 20, "Inspect actual interpolated paths and lake floors across seeds");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void exportOppositeTerraceEntrancesAndCurvedDescent(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var terrain = NetherRampDecorationGuard.terrain(level.getChunkSource().randomState()).orElseThrow();
        List<Map<String, Object>> views = new ArrayList<>();
        List<Map<String, Object>> routes = new ArrayList<>();
        for (int stripe : new int[]{0, 1}) {
            var route = terrain.desertOutlet(32, stripe);
            int station = route.entranceStart();
            while (station < route.length() && terrain.desertUncarvedSurfaceY(Mth.floor(route.xAt(station)), Mth.floor(route.zAt(station)))
                    - route.floorAt(station) > 4) station++;
            double x = route.xAt(station), z = route.zAt(station), heading = route.headingAt(station);
            for (int cx = Math.floorDiv(Mth.floor(x), 16) - 1; cx <= Math.floorDiv(Mth.floor(x), 16) + 1; cx++)
                for (int cz = Math.floorDiv(Mth.floor(z), 16) - 1; cz <= Math.floorDiv(Mth.floor(z), 16) + 1; cz++) level.getChunk(cx, cz);
            double dx = Math.sin(heading), dz = Math.cos(heading);
            views.add(view(level, "desert-straight-sand-entrance-" + stripe, x + dx * 25, route.floorAt(station) + 19,
                    z + dz * 25, (float) Math.toDegrees(Math.atan2(dx, -dz)), 31));
            routes.add(Map.of("cycle", 32, "stripe", stripe, "length", route.length(), "entranceStation", station,
                    "mouthT", t(terrain, x, z), "x", x, "z", z));
        }
        var route = terrain.desertOutlet(32, 0);
        double dx = route.xAt(25) - route.xAt(45), dz = route.zAt(25) - route.zAt(45);
        views.add(view(level, "desert-curved-descent-near-t-zero", route.xAt(45), route.floorAt(45) + 3.2,
                route.zAt(45), (float) Math.toDegrees(Math.atan2(-dx, dz)), 22));
        Path output = Path.of("desert-terrace-cave-views.json").toAbsolutePath();
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views) + "\n");
        Files.writeString(output.resolveSibling("desert-terrace-cave-scenes.json"), new GsonBuilder().setPrettyPrinting().create().toJson(routes) + "\n");
        helper.succeed();
    }

    private static double t(DimensionTerrain terrain, double x, double z) { return terrain.land().compute(new DensityFunction.SinglePointContext(Mth.floor(x), 0, Mth.floor(z))); }
    private static double surface(DimensionTerrain terrain, double x, double z) { return DesertHeight.height(t(terrain, x, z),
            terrain.detail().compute(new DensityFunction.SinglePointContext(Mth.floor(x), 0, Mth.floor(z)))); }
    private static Map<String, Object> view(net.minecraft.server.level.ServerLevel level, String label, double x, double y, double z, float yaw, float pitch) {
        return Map.of("label", label, "dimension", level.dimension().location().toString(), "x", x, "y", y, "z", z, "yaw", yaw, "pitch", pitch);
    }
}
