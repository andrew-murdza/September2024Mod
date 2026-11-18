package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.amurdza.examplemod.worldgen.structure.LegacySavannaOrcVillageV5;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaRoundThreeTests {
    private static final int[] ENDS = {4, 7, 12, 20};
    private SavannaRoundThreeTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void groundPlanStopsAtFirstEightLaneTerrainIntersection(GameTestHelper helper) {
        int floor = 128;
        IntBinaryOperator terrain = unevenTerrain(floor);
        var plan = SavannaOrcVillage.planGround(floor, terrain);
        for (int direction = 0; direction < 4; direction++) {
            helper.assertTrue(plan.length(direction) == ENDS[direction],
                    "A high lane must not end the ramp before its lowest lane meets terrain");
            for (int step = 0; step <= ENDS[direction]; step++) {
                int highest = floor - step;
                for (int across = -4; across <= 3 && step > 0; across++) highest = Math.max(highest,
                        terrain.applyAsInt(SavannaOrcVillage.rampX(direction, step, across), SavannaOrcVillage.rampZ(direction, step, across)));
                for (int across = -4; across <= 3; across++) {
                    int expected = step == ENDS[direction] ? terrain.applyAsInt(SavannaOrcVillage.rampX(direction, step, across),
                            SavannaOrcVillage.rampZ(direction, step, across)) : highest;
                    helper.assertTrue(plan.top(direction, step, across) == expected,
                            "Every earlier tread is eight wide; each final lane returns to its own natural height");
                }
            }
            helper.assertTrue(terrain.applyAsInt(SavannaOrcVillage.rampX(direction, ENDS[direction] + 1, 0),
                    SavannaOrcVillage.rampZ(direction, ENDS[direction] + 1, 0)) < floor - 40,
                    "The fixture must dip again after the first intersection, without restarting the path");
        }
        var deep = SavannaOrcVillage.planGround(floor, (x, z) -> floor - 80);
        var immediate = SavannaOrcVillage.planGround(floor, (x, z) -> floor - 1);
        for (int direction = 0; direction < 4; direction++) {
            helper.assertTrue(deep.length(direction) == SavannaOrcVillage.MARGIN && deep.top(direction, 20, 0) == floor - 20,
                    "A ramp without a terrain intersection must remain bounded and keep its descending tread");
            helper.assertTrue(immediate.length(direction) == 1 && immediate.top(direction, 1, 0) == floor - 1,
                    "An immediate all-lane intersection must stop after its first row");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void circularMoundHasACompleteSingleBlockOutsideLedge(GameTestHelper helper) {
        for (int x = -20; x < 145; x++) for (int z = -20; z < 145; z++) {
            long dx = x - 62L, dz = z - 62L;
            helper.assertTrue(LegacySavannaOrcVillageV5.contains(x, z) == (dx * dx + dz * dz <= 3600),
                    "The saved version-five stockade must follow the radius-sixty circle");
            boolean flat = false;
            for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                long nx = dx + ox, nz = dz + oz;
                flat |= nx * nx + nz * nz <= 3600;
            }
            helper.assertTrue(LegacySavannaOrcVillageV5.onFlatMound(x, z) == flat,
                    "The flat ledge must include exactly the complete one-block ring, including every diagonal: " + x + "," + z);
        }
        for (int x = -SavannaOrcVillage.MARGIN; x < SavannaOrcVillage.SIZE + SavannaOrcVillage.MARGIN; x++)
            for (int z = -SavannaOrcVillage.MARGIN; z < SavannaOrcVillage.SIZE + SavannaOrcVillage.MARGIN; z++) {
                int dx = Math.abs(x - SavannaOrcVillage.CENTER), dz = Math.abs(z - SavannaOrcVillage.CENTER);
                helper.assertTrue(SavannaOrcVillage.contains(x, z) == (Math.max(dx, dz) <= SavannaOrcVillage.RADIUS),
                        "A fresh village must use its complete square footprint");
                helper.assertTrue(SavannaOrcVillage.onFlatMound(x, z) == (Math.max(dx, dz) <= SavannaOrcVillage.RADIUS + 1),
                        "The square mound must keep exactly one complete outer ledge including corners");
            }
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var placed = new HashMap<BlockPos, BlockState>();
        BoundingBox[] clip = {null};
        var world = capture(helper, level, placed, clip);
        BlockPos origin = new BlockPos(20480, 18, 20480);
        var plan = SavannaOrcVillage.planGround(20, (x, z) -> 60);
        for (int[] local : new int[][]{{97,1}, {107,1}, {1,97}, {1,107}, {95,191}, {191,95}, {1,1}, {191,191}}) {
            BlockPos pos = origin.offset(local[0], 2, local[1]);
            clip[0] = new BoundingBox(pos.getX(), level.getMinBuildHeight(), pos.getZ(), pos.getX(), level.getMaxBuildHeight() - 1, pos.getZ());
            SavannaOrcVillage.placeGround(world, clip[0], origin, plan);
            helper.assertTrue(state(placed, pos).is(Blocks.GRASS_BLOCK) || state(placed, pos).is(Blocks.DIRT_PATH),
                    "The bush ledge must stay at mound height even when original terrain was higher");
            helper.assertTrue(SavannaOrcVillage.hasMoundColumn(local[0], local[1], plan),
                    "Vegetation replay must recognize every flat ledge column");
        }
        helper.succeed();
    }

    static void assertEightLanePlacement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos origin = new BlockPos(20483, 126, 20483);
        var terrain = unevenTerrain(128);
        var plan = SavannaOrcVillage.planGround(128, terrain);
        var placed = new HashMap<BlockPos, BlockState>();
        BoundingBox[] clip = {null};
        var world = capture(helper, level, placed, clip);
        var clips = new ArrayList<BoundingBox>();
        for (int direction = 0; direction < 4; direction++) {
            var first = origin.offset(SavannaOrcVillage.rampX(direction, 0, -4), 0, SavannaOrcVillage.rampZ(direction, 0, -4));
            var last = origin.offset(SavannaOrcVillage.rampX(direction, 20, 4), 0, SavannaOrcVillage.rampZ(direction, 20, 4));
            var bounds = BoundingBox.fromCorners(first, last);
            for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                clips.add(new BoundingBox(Math.max(cx * 16, bounds.minX()), level.getMinBuildHeight(), Math.max(cz * 16, bounds.minZ()),
                        Math.min(cx * 16 + 15, bounds.maxX()), level.getMaxBuildHeight() - 1, Math.min(cz * 16 + 15, bounds.maxZ())));
            }
        }
        Collections.reverse(clips);
        for (var part : clips) { clip[0] = part; SavannaOrcVillage.placeGround(world, part, origin, plan); }
        for (int direction = 0; direction < 4; direction++) for (int step = 0; step <= 20; step++) for (int across = -4; across <= 4; across++) {
            int x = SavannaOrcVillage.rampX(direction, step, across), z = SavannaOrcVillage.rampZ(direction, step, across);
            if (across >= -4 && across <= 3 && step <= ENDS[direction]) {
                BlockPos pos = new BlockPos(origin.getX() + x, plan.top(direction, step, across), origin.getZ() + z);
                helper.assertTrue(state(placed, pos).is(Blocks.DIRT_PATH) && state(placed, pos.above()).isAir(),
                        "Every active ramp row needs all eight path lanes with clear headroom, including natural endpoint heights");
            } else {
                int wx = origin.getX() + x, wz = origin.getZ() + z;
                helper.assertTrue(placed.entrySet().stream().noneMatch(entry -> entry.getKey().getX() == wx && entry.getKey().getZ() == wz
                                && entry.getValue().is(Blocks.DIRT_PATH)),
                        "No dirt path may extend beyond the first complete terrain intersection or outside its eight lanes");
            }
        }
        var before = Map.copyOf(placed);
        Collections.reverse(clips);
        var recomputed = SavannaOrcVillage.planGround(128, terrain);
        for (var part : clips) { clip[0] = part; SavannaOrcVillage.placeGround(world, part, origin, recomputed); }
        helper.assertTrue(before.equals(placed), "Recomputed ground plans and reverse chunk order must produce identical ramp blocks");
    }

    private static IntBinaryOperator unevenTerrain(int floor) {
        return (x, z) -> {
            int dx = x - SavannaOrcVillage.CENTER, dz = z - SavannaOrcVillage.CENTER;
            int direction = dx >= -4 && dx <= 3 ? (dz < 0 ? 0 : 1) : (dx < 0 ? 2 : 3);
            int step = Math.max(Math.abs(dx), Math.abs(dz)) - (SavannaOrcVillage.RADIUS + 1);
            int across = direction < 2 ? dx : dz;
            return step > ENDS[direction] ? floor - 80 : floor - ENDS[direction] + Math.floorMod(across + 4, 4);
        };
    }

    private static WorldGenLevel capture(GameTestHelper helper, ServerLevel level, Map<BlockPos, BlockState> placed, BoundingBox[] clip) {
        return (WorldGenLevel) Proxy.newProxyInstance(SavannaRoundThreeTests.class.getClassLoader(), new Class[]{WorldGenLevel.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getBlockState": return state(placed, (BlockPos) args[0]);
                case "getFluidState": return state(placed, (BlockPos) args[0]).getFluidState();
                case "setBlock": {
                    BlockPos pos = ((BlockPos) args[0]).immutable();
                    helper.assertTrue(clip[0].isInside(pos), "Ground placement must stay inside its supplied chunk clip");
                    placed.put(pos, (BlockState) args[1]); return true;
                }
            }
            try { return method.invoke(level, args); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    private static BlockState state(Map<BlockPos, BlockState> states, BlockPos pos) {
        return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }
}
