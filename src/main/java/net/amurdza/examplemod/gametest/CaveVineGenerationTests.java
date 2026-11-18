package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.feature.configs.CaveVineConfig;
import net.amurdza.examplemod.worldgen.feature.features.CaveVineColumn;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CaveVineGenerationTests {
    private CaveVineGenerationTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void longVinesLeaveFourClearBlocksAboveStoneWithOneMatureTip(GameTestHelper helper) {
        BlockPos floor = clearColumn(helper);
        var level = helper.getLevel();
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(floor.above(23), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        helper.assertTrue(CaveVinePlacement.place(level, floor.above(22), 2, false), "Long vine was not placed");
        assertColumn(helper, floor.above(22), floor.above(5), false);
        helper.assertTrue(level.isEmptyBlock(floor.above(4)), "Vines must retain walking clearance");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void closestLedgeStopsVinesBeforeTheLowerFloor(GameTestHelper helper) {
        BlockPos floor = clearColumn(helper);
        var level = helper.getLevel();
        level.setBlock(floor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        level.setBlock(floor.above(12), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(floor.above(23), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        CaveVinePlacement.place(level, floor.above(22), 25, false);
        assertColumn(helper, floor.above(22), floor.above(17), false);
        helper.assertTrue(level.getBlockState(floor.above(12)).is(Blocks.OAK_LOG), "Vine overwrote its nearest obstacle");
        helper.assertTrue(level.isEmptyBlock(floor.above(16)), "Vine grew into its four-block clearance");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void underwaterVinesPreserveWaterAndMatureOnlyTheirTip(GameTestHelper helper) {
        BlockPos floor = clearColumn(helper);
        var level = helper.getLevel();
        level.setBlock(floor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        for (int y = 1; y <= 22; y++) level.setBlock(floor.above(y), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(floor.above(23), Blocks.STONE.defaultBlockState(), 2);
        CaveVinePlacement.place(level, floor.above(22), 5, true);
        assertColumn(helper, floor.above(22), floor.above(5), true);
        helper.assertTrue(level.getBlockState(floor.above(4)).is(Blocks.WATER), "Vines removed the water beneath their tip");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void chunkVinePassKeepsExistingBodiesAndExtendsNewColumns(GameTestHelper helper) {
        BlockPos floor = clearColumn(helper);
        var level = helper.getLevel();
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(floor.above(23), Blocks.STONE.defaultBlockState(), 2);
        var config = new CaveVineConfig(ConstantInt.of(2), 1, 0, 0);
        var feature = new CaveVineColumn(CaveVineConfig.CODEC);
        WorldGenLevel isolated = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    // The chunk feature must not decorate neighboring test fixtures.
                    if (method.getName().equals("getBlockState") && args[0] instanceof BlockPos pos
                            && (pos.getX() != floor.getX() || pos.getZ() != floor.getZ())) return Blocks.STONE.defaultBlockState();
                    try {
                        return method.invoke(level, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
        var context = new FeaturePlaceContext<>(Optional.empty(), isolated, level.getChunkSource().getGenerator(),
                RandomSource.create(123), floor.above(22), config);
        helper.assertTrue(feature.place(context), "Chunk hanging feature placed no vines");
        assertColumn(helper, floor.above(22), floor.above(5), false);
        feature.place(context);
        assertColumn(helper, floor.above(22), floor.above(5), false);
        helper.succeed();
    }

    private static BlockPos clearColumn(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(2, 10, 2));
        for (int y = 0; y <= 24; y++) helper.getLevel().setBlock(floor.above(y), Blocks.AIR.defaultBlockState(), 2);
        return floor;
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void existingLeavesStopVinesWithFourEmptyCells(GameTestHelper helper) {
        BlockPos floor = clearColumn(helper);
        var level = helper.getLevel();
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(floor.above(12), Blocks.OAK_LEAVES.defaultBlockState(), 18);
        level.setBlock(floor.above(23), Blocks.OAK_LEAVES.defaultBlockState(), 18);
        helper.assertTrue(CaveVinePlacement.place(level, floor.above(22), 25, false), "Leaf-bounded vine must place");
        assertColumn(helper, floor.above(22), floor.above(17), false);
        for (int dy = 13; dy <= 16; dy++) {
            helper.assertTrue(level.isEmptyBlock(floor.above(dy)), "Each of four cells above leaves must stay clear");
        }
        helper.assertTrue(level.getBlockState(floor.above(12)).is(Blocks.OAK_LEAVES), "Leaf obstacle was overwritten");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void laterCanopiesTrimEarlierVinesWithoutLeakingBufferedChanges(GameTestHelper helper) {
        for (boolean waterlogged : new boolean[]{false, true}) {
            for (var obstacle : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LEAVES, Blocks.OAK_LOG}) {
                var blocks = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
                var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
                BlockPos obstaclePos = new BlockPos(8, 108, 8);
                blocks.put(new BlockPos(8, 116, 8), Blocks.OAK_LEAVES.defaultBlockState());
                for (int y = 101; y <= 115; y++) blocks.put(new BlockPos(8, y, 8),
                        (y == 101 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                                .setValue(BlockStateProperties.BERRIES, true)
                                .setValue(BlockStateProperties.WATERLOGGED, waterlogged));
                var before = java.util.Map.copyOf(blocks);
                var buffer = new GrowthPlacementBuffer(world);
                buffer.setBlock(obstaclePos, obstacle.defaultBlockState(), 2);
                helper.assertTrue(blocks.equals(before),
                        "Canopy/vine correction must remain staged until the tree commits");
                helper.assertTrue(buffer.commit(), "Canopy and shortened vine must commit together");
                helper.assertTrue(world.getBlockState(obstaclePos).is(obstacle), "Generated canopy obstacle must remain");
                for (int y = 101; y <= 112; y++) {
                    if (y == 108) continue;
                    BlockPos cleared = new BlockPos(8, y, 8);
                    var state = world.getBlockState(cleared);
                    helper.assertTrue(state.is(waterlogged ? Blocks.WATER : Blocks.AIR),
                            "No vine may remain below the interruption or within its four-cell gap; preserve its water at "
                                    + cleared + " (waterlogged=" + waterlogged + ", obstacle=" + obstacle + "): " + state);
                }
                var tip = world.getBlockState(obstaclePos.above(5));
                helper.assertTrue(tip.is(Blocks.CAVE_VINES) && tip.getValue(BlockStateProperties.AGE_25) == 25
                                && tip.getValue(BlockStateProperties.WATERLOGGED) == waterlogged,
                        "A shortened vine must end in one mature head");
                helper.assertTrue(world.getBlockState(obstaclePos.above(6)).is(Blocks.CAVE_VINES_PLANT),
                        "The supported upper column must survive shortening");
            }
        }
        helper.succeed();
    }

    private static void assertColumn(GameTestHelper helper, BlockPos start, BlockPos tip, boolean waterlogged) {
        for (int y = start.getY(); y >= tip.getY(); y--) {
            var state = helper.getLevel().getBlockState(new BlockPos(start.getX(), y, start.getZ()));
            helper.assertTrue(state.is(y == tip.getY() ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT),
                    "Hanging column has a gap or intermediate head at Y" + y + ": " + state);
            helper.assertTrue(state.getValue(BlockStateProperties.BERRIES), "Generated vine has no berries");
            helper.assertTrue(state.getValue(BlockStateProperties.WATERLOGGED) == waterlogged, "Vine changed its water habitat");
            if (y == tip.getY()) helper.assertTrue(state.getValue(BlockStateProperties.AGE_25) == 25, "Vine tip must have age 25");
        }
    }
}
