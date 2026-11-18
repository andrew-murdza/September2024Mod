package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalVineAndRootBoundaryTests {
    private TropicalVineAndRootBoundaryTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void sparseCaveVinesUseTheSameDecisionAcrossTropicalIslands(GameTestHelper helper) {
        var world = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (int x = 0; x < 256; x++) {
            BlockPos start = new BlockPos(x, 120, 0);
            boolean kept = CaveVinePlacement.keepTropicalColumn(world.getSeed(), x, 0);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var fixture = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
            helper.assertTrue(CaveVinePlacement.place(fixture, start, 25, false) == kept,
                    "All rainforest generation routes must apply the same sparse placement rule");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void randomGrowthRetainsFourEmptyCellsAboveLeavesAndStone(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos obstaclePos = helper.absolutePos(new BlockPos(2, 10, 2));
        BlockPos tipPos = obstaclePos.above(5);
        for (var obstacle : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LEAVES, Blocks.STONE}) {
            for (int y = 0; y <= 12; y++) level.setBlock(obstaclePos.above(y), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(obstaclePos, obstacle.defaultBlockState(), 18);
            level.setBlock(tipPos.above(), Blocks.OAK_LEAVES.defaultBlockState(), 18);
            BlockState tip = Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25, 0)
                    .setValue(BlockStateProperties.BERRIES, true);
            level.setBlock(tipPos, tip, 18);
            RandomSource random = RandomSource.create(32771);
            for (int tick = 0; tick < 128; tick++) Blocks.CAVE_VINES.randomTick(tip, level, tipPos, random);
            helper.assertTrue(level.isEmptyBlock(tipPos.below()),
                    "Random growth cannot consume one of the four clear cells above " + obstacle);
            helper.assertTrue(level.getBlockState(obstaclePos).is(obstacle), "The obstacle must remain intact");

            // Moving the obstacle down one cell permits exactly the next step.
            level.setBlock(obstaclePos, Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(obstaclePos.below(), obstacle.defaultBlockState(), 18);
            random = RandomSource.create(32771);
            for (int tick = 0; tick < 128 && level.isEmptyBlock(tipPos.below()); tick++) {
                Blocks.CAVE_VINES.randomTick(tip, level, tipPos, random);
            }
            helper.assertTrue(level.getBlockState(tipPos.below()).is(Blocks.CAVE_VINES),
                    "Growth remains available when the proposed tip leaves four clear cells");
            for (int y = -1; y <= 12; y++) level.setBlock(obstaclePos.above(y), Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void giantMushroomsKeepTheLogClearanceAroundBothMangroveRootTypes(GameTestHelper helper) {
        for (String feature : new String[]{"minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom"}) {
            Map<BlockPos, BlockState> clearBlocks = new HashMap<>();
            var clear = TropicalTreeShapeTests.fixture(helper, clearBlocks, "mangrove_swamp", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(clear, feature, 7), "Control mushroom must generate");
            for (var obstacle : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LOG, Blocks.MANGROVE_ROOTS,
                    Blocks.MUDDY_MANGROVE_ROOTS}) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                BlockPos root = new BlockPos(9, 101, 8);
                blocks.put(root, obstacle.defaultBlockState());
                var occupied = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 0, true);
                helper.assertFalse(TropicalTreeShapeTests.place(occupied, feature, 7),
                        "A giant mushroom cannot grow against an existing " + obstacle);
                helper.assertTrue(blocks.size() == 1 && blocks.get(root).is(obstacle),
                        "Rejected mushrooms must preserve the roots and leave no partial cap or stem");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void randomGrowthCannotReplaceWaterloggedLeaves(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos tipPos = helper.absolutePos(new BlockPos(2, 20, 2));
        BlockState tip = Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25, 0)
                .setValue(BlockStateProperties.BERRIES, true);
        for (int dy = -7; dy <= 1; dy++) level.setBlock(tipPos.above(dy), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(tipPos.above(), Blocks.OAK_LEAVES.defaultBlockState(), 18);
        level.setBlock(tipPos, tip, 18);
        level.setBlock(tipPos.below(), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true), 18);
        RandomSource random = RandomSource.create(32771);
        for (int tick = 0; tick < 128; tick++) Blocks.CAVE_VINES.randomTick(tip, level, tipPos, random);
        helper.assertTrue(level.getBlockState(tipPos.below()).is(Blocks.OAK_LEAVES)
                        && level.getBlockState(tipPos.below()).getValue(BlockStateProperties.WATERLOGGED),
                "Water inside leaves must not make the leafy block replaceable by growing cave vines");
        helper.succeed();
    }
}
