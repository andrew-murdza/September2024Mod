package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.tree_decorators.TropicalShelfMushroomDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyWoodlandVegetationTests {
    private SnowyWoodlandVegetationTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void tropicalTreesActuallyGenerateSupportedShelfMushrooms(GameTestHelper helper) {
        var mushroom = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:shelf_mushroom"));
        helper.assertTrue(mushroom != null && mushroom != Blocks.AIR, "The backported shelf mushroom must be available");
        for (String species : new String[]{"fancy_oak", "dark_oak", "mangrove"}) {
            int generated = 0;
            int mushrooms = 0;
            for (long seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                var level = TropicalTreeShapeTests.fixture(helper, blocks,
                        species.equals("mangrove") ? "mangrove_swamp" : "rainforest", 0, true);
                if (!TropicalTreeShapeTests.place(level, "aoemod:rainforest/tree/natural_" + species, seed)) continue;
                generated++;
                for (var entry : blocks.entrySet()) {
                    if (!entry.getValue().is(mushroom)) continue;
                    mushrooms++;
                    helper.assertTrue(entry.getValue().canSurvive(level, entry.getKey()),
                            "Shelf mushrooms must attach to the actual " + species + " tree at " + entry.getKey());
                }
            }
            helper.assertTrue(generated > 0 && (species.equals("fancy_oak") ? mushrooms > 0 : mushrooms == 0),
                    "Only fancy oaks retain shelf fungi; mangroves and dark oaks exclude them; species="
                            + species + ", trees=" + generated + ", fungi=" + mushrooms);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalShelfFungiExcludeMangrovesWithoutChangingTreeRandomness(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var level = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 0, true);
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        for (int y = 108; y <= 112; y++) {
            BlockPos log = new BlockPos(8, y, 8);
            logs.add(log);
            blocks.put(log, Blocks.MANGROVE_LOG.defaultBlockState());
            if (y == 108) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos leaf = log.relative(direction);
                leaves.add(leaf);
                blocks.put(leaf, Blocks.MANGROVE_LEAVES.defaultBlockState());
            }
        }
        RandomSource treeRandom = RandomSource.create(50881);
        RandomSource control = RandomSource.create(50881);
        new TropicalShelfMushroomDecorator(1.0F).place(new TreeDecorator.Context(level,
                (pos, state) -> blocks.put(pos.immutable(), state), treeRandom, logs, leaves, Set.of()));
        var mushroom = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:shelf_mushroom"));
        var fungi = blocks.entrySet().stream().filter(entry -> entry.getValue().is(mushroom)).toList();
        helper.assertTrue(fungi.isEmpty(), "Mangrove trunks must remain free of shelf mushrooms");
        helper.assertTrue(leaves.stream().allMatch(pos -> level.getBlockState(pos).is(Blocks.MANGROVE_LEAVES)),
                "Shelf fungi must retain the full mangrove crown");
        for (int i = 0; i < 16; i++) helper.assertTrue(treeRandom.nextLong() == control.nextLong(),
                "Adding shelf fungi must preserve subsequent tree selection and shape randomness");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void snowyTreeSelectionProducesMostlyRegularSpruceAndPodzolGiants(GameTestHelper helper) {
        int regular = 0;
        int giant = 0;
        for (long seed = 0; seed < 128; seed++) {
            Map<BlockPos, BlockState> blocks = grassFloor();
            var level = TropicalTreeShapeTests.fixture(helper, blocks, "snowy_forest", 0, false);
            if (!TropicalTreeShapeTests.place(level, "aoemod:snowy/trees", seed * 341873128712L + 132897987541L)) continue;
            if (blocks.values().stream().noneMatch(state -> state.is(Blocks.SPRUCE_LOG))) continue;
            boolean twoByTwo = level.getBlockState(new BlockPos(9, 101, 8)).is(Blocks.SPRUCE_LOG)
                    && level.getBlockState(new BlockPos(8, 101, 9)).is(Blocks.SPRUCE_LOG)
                    && level.getBlockState(new BlockPos(9, 101, 9)).is(Blocks.SPRUCE_LOG);
            if (twoByTwo) {
                giant++;
                helper.assertTrue(blocks.values().stream().anyMatch(state -> state.is(Blocks.PODZOL)),
                        "The selected giant spruce must produce its podzol floor");
            } else {
                regular++;
            }
            helper.assertTrue(blocks.values().stream().noneMatch(state -> {
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id != null && id.getPath().contains("poplar");
            }), "Snowy taiga tree selection must never produce poplar trees");
        }
        helper.assertTrue(giant > 0 && regular > giant * 4,
                "The real selector must produce occasional giants and predominantly regular spruce: " + regular + "/" + giant);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void dappledTreesKeepShelfFungiWithoutForestDebris(GameTestHelper helper) {
        int generated = 0;
        int mushrooms = 0;
        var shelf = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:shelf_mushroom"));
        for (long seed = 0; seed < 32; seed++) {
            Map<BlockPos, BlockState> blocks = grassFloor();
            var level = TropicalTreeShapeTests.fixture(helper, blocks, "dappled_forest", 0, false);
            if (!TropicalTreeShapeTests.place(level, "aoemod:dappled/trees", seed)) continue;
            generated++;
            for (var state : blocks.values()) {
                if (state.is(shelf)) mushrooms++;
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                helper.assertTrue(!state.is(Blocks.COARSE_DIRT)
                                && (id == null || !id.getPath().equals("leaf_litter")),
                        "Dappled tree placement must not create coarse dirt or leaf litter");
            }
        }
        helper.assertTrue(generated > 0 && mushrooms > 0,
                "The dappled tree selector must retain live poplar shelf-fungus decoration");
        helper.succeed();
    }

    private static Map<BlockPos, BlockState> grassFloor() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(-16, 100, -16, 32, 100, 32)) {
            blocks.put(pos.immutable(), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        return blocks;
    }
}
