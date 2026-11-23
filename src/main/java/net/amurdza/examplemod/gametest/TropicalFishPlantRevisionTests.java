package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.TropicalBamboo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalFishPlantRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void lateCanopiesTrimBambooInsideTheSameTransaction(GameTestHelper helper) {
        for (var obstacle : new BlockState[]{Blocks.CHERRY_LEAVES.defaultBlockState(), Blocks.CHERRY_LOG.defaultBlockState()}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "cherry_grove", 0, true);
            helper.assertTrue(TropicalBamboo.placePatch(world, 14, 0, RandomSource.create(771)) == 10,
                    "Boundary fixture needs a bamboo patch spanning two chunks");
            var root = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                            && entry.getKey().getX() >= 16 && entry.getValue().is(Blocks.BAMBOO))
                    .map(Map.Entry::getKey).findFirst().orElseThrow();
            BlockPos collision = root.above(3);
            var before = Map.copyOf(blocks);
            var tree = new GrowthPlacementBuffer(world);
            tree.setBlock(collision, obstacle, 19);
            helper.assertTrue(blocks.equals(before), "A rejected tree attempt must not trim live bamboo");
            helper.assertTrue(tree.commit(), "A later tree canopy must be able to replace bamboo");
            helper.assertTrue(world.getBlockState(collision).is(obstacle.getBlock()), "Canopy obstacle was lost");
            helper.assertTrue(world.getBlockState(collision.below()).is(Blocks.BAMBOO)
                            && world.getBlockState(collision.below()).getValue(BambooStalkBlock.STAGE) == 1,
                    "Surviving bamboo must stop immediately below the canopy");
            for (int y = collision.getY() + 1; y < 140; y++) {
                helper.assertFalse(world.getBlockState(new BlockPos(root.getX(), y, root.getZ())).is(Blocks.BAMBOO),
                        "A neighboring crown must not leave bamboo protruding through or above it");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void actualCherryCanopiesReplaceAndTrimEarlierBamboo(GameTestHelper helper) {
        for (int seed = 1; seed <= 8; seed++) {
            Map<BlockPos, BlockState> baseline = new HashMap<>();
            var empty = TropicalTreeShapeTests.fixture(helper, baseline, "cherry_grove", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(empty, "minecraft:cherry_bees_005", seed), "Baseline cherry failed");
            BlockPos leaf = baseline.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.CHERRY_LEAVES)
                            && entry.getKey().getX() != 8 && entry.getKey().getZ() != 8)
                    .map(Map.Entry::getKey).min(java.util.Comparator.comparingLong(BlockPos::asLong)).orElseThrow();
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            for (int y = 101; y <= 140; y++) blocks.put(new BlockPos(leaf.getX(), y, leaf.getZ()), Blocks.BAMBOO.defaultBlockState());
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "cherry_grove", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(world, "minecraft:cherry_bees_005", seed),
                    "Earlier bamboo must not veto the neighboring cherry tree");
            helper.assertTrue(world.getBlockState(leaf).is(Blocks.CHERRY_LEAVES), "Earlier bamboo punched a hole in the canopy");
            for (int y = leaf.getY(); y <= 140; y++) {
                helper.assertFalse(world.getBlockState(new BlockPos(leaf.getX(), y, leaf.getZ())).is(Blocks.BAMBOO),
                        "The actual cherry feature left bamboo above its canopy");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void generatedMangrovesHaveOnlyMatureHangingPropagules(GameTestHelper helper) {
        int propagules = 0;
        for (int waterDepth : new int[]{0, 6}) for (int seed = 1; seed <= 12; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", waterDepth, true);
            if (!TropicalTreeShapeTests.place(world, "aoemod:rainforest/tree/natural_mangrove", seed)) continue;
            for (var state : blocks.values()) {
                if (!state.is(Blocks.MANGROVE_PROPAGULE) || !state.getValue(MangrovePropaguleBlock.HANGING)) continue;
                helper.assertTrue(state.getValue(MangrovePropaguleBlock.AGE) == 4, "Generated hanging propagule is immature");
                propagules++;
            }
        }
        helper.assertTrue(propagules > 0, "Generated-tree audit must inspect actual hanging propagules");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void grownMangrovesHaveOnlyMatureHangingPropagules(GameTestHelper helper) {
        var world = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos origin = new BlockPos(184, 220, 40);
        BlockPos min = origin.offset(-12, -2, -12), max = origin.offset(12, 30, 12);
        int propagules = 0;
        try {
            for (int seed = 1; seed <= 12 && propagules == 0; seed++) {
                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    world.setBlock(pos, pos.getY() < origin.getY() ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
                }
                BlockState sapling = Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
                world.setBlock(origin, sapling, 18);
                ((SaplingBlock) Blocks.MANGROVE_PROPAGULE).advanceTree(world, origin, sapling, RandomSource.create(seed));
                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    var state = world.getBlockState(pos);
                    if (!state.is(Blocks.MANGROVE_PROPAGULE) || !state.getValue(MangrovePropaguleBlock.HANGING)) continue;
                    helper.assertTrue(state.getValue(MangrovePropaguleBlock.AGE) == 4, "Sapling-grown hanging propagule is immature");
                    propagules++;
                }
            }
            helper.assertTrue(propagules > 0, "Sapling growth must produce an actual mature propagule");
        } finally {
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }
}
