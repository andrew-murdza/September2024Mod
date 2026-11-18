package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.feature.features.PlainsSeafloorFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsAquaticBalanceTests {
    private static final int SIDE = 96;
    private static final int SAMPLES = SIDE * SIDE;
    private static final int ROOT_Y = 101;

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void configuredPlainsSeafloorPreservesDepthSpecificAbsolutePlantRates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (int depth : new int[]{1, 2, 3}) {
            String name = depth == 1 ? "water_plants_shallow_full" : "water_plants_full";
            var feature = configured.get(new ResourceLocation("aoemod:plains/water/" + name));
            helper.assertTrue(feature != null && feature.feature() instanceof PlainsSeafloorFeature,
                    "Both configured seafloor entry points must use the depth-aware root lottery");
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, depth);
            RandomSource random = RandomSource.create(92113 + depth);
            for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
                feature.place(world, level.getChunkSource().getGenerator(), random, new BlockPos(x, ROOT_Y, z));
            }
            int[] roots = verifyRoots(helper, world, blocks, depth);
            distribution(helper, roots[0], depth < 3 ? .8 : .72, "configured short seagrass", depth);
            if (depth < 3) helper.assertTrue(roots[1] == 0, "One- and two-deep configured beds must contain no tall seagrass");
            else distribution(helper, roots[1], .02, "configured tall seagrass", depth);
            distribution(helper, roots[2], depth < 3 ? .2 : .26, "configured unchanged water", depth);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void secondaryWaterLotteryDoublesShortAndHalvesTallWithoutRedistributingFailures(GameTestHelper helper) {
        for (int depth : new int[]{1, 2, 3}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, depth);
            RandomSource random = RandomSource.create(73319 + depth);
            int shortAttempts = 0, tallAttempts = 0;
            for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
                String selected = DocumentedFloraFeature.plainsWaterSelection(random, depth == 1);
                if (selected.equals("minecraft:seagrass")) shortAttempts++;
                if (selected.equals("minecraft:tall_seagrass")) tallAttempts++;
                if (!selected.isEmpty()) DocumentedFloraFeature.placePlant(world, new BlockPos(x, ROOT_Y, z), selected, random);
            }
            distribution(helper, shortAttempts, depth == 1 ? 4.0 / 9 : 2.0 / 3, "secondary short opportunities", depth);
            distribution(helper, tallAttempts, depth == 1 ? 1.0 / 9 : 1.0 / 6, "secondary tall opportunities", depth);
            int[] roots = verifyRoots(helper, world, blocks, depth);
            helper.assertTrue(roots[0] == shortAttempts && roots[1] == (depth == 1 ? 0 : tallAttempts),
                    "Tall one-deep failures must stay unused; every valid selected native grass must place intact");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void configuredSeafloorDoesNotPlantBelowOpaqueCaveRoofs(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(0, ROOT_Y + 3, 0), Blocks.STONE.defaultBlockState());
        WorldGenLevel world = fixture(helper, blocks, 3);
        var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:plains/water/water_plants_full"));
        for (int seed = 0; seed < 20; seed++) {
            helper.assertFalse(feature.place(world, world.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(seed), new BlockPos(0, ROOT_Y, 0)), "The dense feature must enforce skylight independently");
        }
        helper.assertTrue(blocks.size() == 1, "Rejecting roofed plants must preserve their original water and bed");
        helper.succeed();
    }

    private static int[] verifyRoots(GameTestHelper helper, WorldGenLevel world, Map<BlockPos, BlockState> blocks, int depth) {
        int[] counts = new int[3];
        for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
            BlockPos root = new BlockPos(x, ROOT_Y, z);
            BlockState state = world.getBlockState(root);
            if (state.is(Blocks.SEAGRASS)) counts[0]++;
            else if (state.is(Blocks.TALL_SEAGRASS)) {
                counts[1]++;
                helper.assertTrue(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                                && world.getBlockState(root.above()).is(Blocks.TALL_SEAGRASS)
                                && world.getBlockState(root.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                        "Every tall root must retain a complete native upper half");
            } else {
                helper.assertTrue(state.is(Blocks.WATER), "An unused selection must remain ordinary water");
                counts[2]++;
            }
            for (int y = 0; y < depth; y++) helper.assertTrue(world.getFluidState(root.above(y)).is(FluidTags.WATER)
                            && world.getFluidState(root.above(y)).isSource(), "Plant placement must preserve every water source");
            helper.assertTrue(world.getBlockState(root.below()).is(Blocks.DIRT) && world.isEmptyBlock(root.above(depth)),
                    "Plant placement must preserve the bed and existing water surface");
        }
        helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getY() >= ROOT_Y && pos.getY() < ROOT_Y + depth),
                "Plants must never write outside the existing water column");
        return counts;
    }

    private static void distribution(GameTestHelper helper, int actual, double probability, String name, int depth) {
        double expected = SAMPLES * probability;
        double tolerance = 5 * Math.sqrt(SAMPLES * probability * (1 - probability)) + 2;
        helper.assertTrue(Math.abs(actual - expected) <= tolerance,
                name + " at depth " + depth + ": expected " + expected + ", observed " + actual);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, int depth) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        return AquaticPlantGenerationTests.fixture(helper, "plains", blocks, depth, level,
                Blocks.DIRT.defaultBlockState(), Blocks.WATER.defaultBlockState());
    }
}
