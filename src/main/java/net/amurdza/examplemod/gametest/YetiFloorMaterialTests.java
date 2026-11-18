package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class YetiFloorMaterialTests {
    private YetiFloorMaterialTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void snowFloorKeepsBlueIceOnlyInsideWallsAndCeiling(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos center = new BlockPos(8, 100, 8);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 96 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        BoundingBox[] active = {null};
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(YetiFloorMaterialTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "getBlockEntity": return null;
                        case "setBlock": {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            helper.assertTrue(active[0].isInside(pos), "Yeti material writes remain chunk-clipped: " + pos);
                            blocks.put(pos, (BlockState) args[1]);
                            return true;
                        }
                        case "addFreshEntityWithPassengers": return null;
                        case "addFreshEntity": return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        var serial = StructurePieceSerializationContext.fromLevel(level);
        var piece = new BossLairPiece("yeti", center, 67);
        piece = new BossLairPiece(serial, piece.createTag(serial));
        var bounds = piece.getBoundingBox();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                var chunk = new ChunkPos(cx, cz);
                var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                active[0] = clip;
                piece.postProcess(world, level.structureManager(), level.getChunkSource().getGenerator(),
                        RandomSource.create(67), clip, chunk, center);
                piece.finishYetiInterior(world, clip);
            }
        }
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            helper.assertTrue(state.apply(center.offset(x, 0, z)).is(Blocks.SNOW_BLOCK),
                    "Every chamber floor block must be snow: " + x + "," + z);
            helper.assertTrue(state.apply(center.offset(x, 17, z)).isAir()
                            && state.apply(center.offset(x, 18, z)).is(Blocks.BLUE_ICE)
                            && state.apply(center.offset(x, 24, z)).is(Blocks.SNOW_BLOCK),
                    "The tall clear chamber must retain an inner blue-ice ceiling beneath its snow roof");
            for (int y = 18; y <= 23; y++) helper.assertTrue(state.apply(center.offset(x, y, z)).is(Blocks.BLUE_ICE),
                    "The entire chamber roof must contain exactly six blue-ice layers under one snow layer");
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int side : new int[]{-20, 20}) for (int y = 1; y <= 17; y++) {
                BlockPos inner = center.relative(direction, 25).relative(direction.getClockWise(), side).above(y);
                helper.assertTrue(state.apply(inner).is(Blocks.BLUE_ICE)
                                && state.apply(inner.relative(direction)).is(Blocks.BLUE_ICE),
                        "Unexposed wall thickness must be blue ice, not buried snow: " + inner);
            }
            for (int distance = 25; distance <= 44; distance++) for (int side = -8; side <= 8; side++) {
                BlockPos floor = center.relative(direction, distance).relative(direction.getClockWise(), side);
                helper.assertTrue(state.apply(floor).is(Blocks.SNOW_BLOCK) && state.apply(floor.above()).isAir(),
                        "All four flat entrances must continue the snow floor outdoors: " + floor);
            }
            for (int side : new int[]{-9, 9}) for (int y = 1; y <= 17; y++) {
                BlockPos rim = center.relative(direction, 25).relative(direction.getClockWise(), side).above(y);
                helper.assertTrue(state.apply(rim).is(Blocks.SNOW_BLOCK),
                        "Snow must continue to conceal the ice at each entrance rim: " + rim);
            }
            for (int side = -8; side <= 8; side++) {
                BlockPos gate = center.relative(direction, 25).relative(direction.getClockWise(), side);
                int height = Math.abs(side) == 6 ? 15 : Math.min(16, 40 - 4 * Math.abs(side));
                helper.assertTrue(state.apply(gate.above(height)).isAir()
                                && state.apply(gate.above(height + 1)).is(Blocks.SNOW_BLOCK),
                        "The reference gate needs its two upper shoulder blocks and snow lintel below the ice ceiling");
            }
            for (int side = -5; side <= 5; side++) {
                BlockPos lintel = center.relative(direction, 25).relative(direction.getClockWise(), side).above(17);
                helper.assertTrue(state.apply(lintel).is(Blocks.SNOW_BLOCK)
                                && state.apply(lintel.relative(direction.getOpposite())).isAir(),
                        "A full snow layer must be visible above the gate from inside the raised chamber");
            }
            helper.assertTrue(state.apply(center.relative(direction, 36).relative(direction.getClockWise(), 20).above(8)).is(Blocks.SNOW_BLOCK),
                    "The hill's exposed outer slope remains snow");
        }
        helper.succeed();
    }
}
