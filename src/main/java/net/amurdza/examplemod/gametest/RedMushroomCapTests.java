package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.AtomicTreeGrowth;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.RedMushroomCapSpace;
import net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedMushroomCapTests {
    private RedMushroomCapTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void realRedMushroomsClearOnlyLeavesEnclosedByTheirCaps(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) for (boolean buffered : new boolean[]{false, true}) {
            for (long seed : new long[]{1, 7, 42, 91}) {
                Map<BlockPos, BlockState> reference = new HashMap<>();
                var control = TropicalTreeShapeTests.fixture(helper, reference, "rainforest", 0, tropical);
                helper.assertTrue(TropicalTreeShapeTests.place(control, "minecraft:huge_red_mushroom", seed),
                        "The red mushroom reference must generate");
                int top = capTop(reference);
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                for (int y = top - 3; y < top; y++) {
                    for (int x = 7; x <= 9; x++) for (int z = 7; z <= 9; z++) {
                        if (x != 8 || z != 8) blocks.put(new BlockPos(x, y, z), Blocks.JUNGLE_LEAVES.defaultBlockState());
                    }
                }
                var preserved = Map.of(new BlockPos(11, top - 1, 8), Blocks.OAK_LEAVES.defaultBlockState(),
                        new BlockPos(10, top - 1, 10), Blocks.JUNGLE_LEAVES.defaultBlockState(),
                        new BlockPos(9, top - 4, 8), Blocks.AZALEA_LEAVES.defaultBlockState(),
                        new BlockPos(9, top + 1, 8), Blocks.OAK_LEAVES.defaultBlockState());
                blocks.putAll(preserved);
                var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, tropical);
                WorldGenLevel placement = buffered ? new GrowthPlacementBuffer(world) : world;
                Map<BlockPos, BlockState> before = Map.copyOf(blocks);
                helper.assertTrue(TropicalTreeShapeTests.place(placement, "minecraft:huge_red_mushroom", seed),
                        "Overlapping leaves must not prevent complete red mushroom growth");
                if (placement instanceof GrowthPlacementBuffer buffer) {
                    helper.assertTrue(blocks.equals(before), "Natural generation must keep cap cleanup staged until commit");
                    helper.assertTrue(buffer.commit(), "The complete mushroom and enclosed-leaf cleanup must commit together");
                }
                for (int y = top - 3; y < top; y++) {
                    for (int x = 7; x <= 9; x++) for (int z = 7; z <= 9; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        helper.assertTrue(x == 8 && z == 8 ? world.getBlockState(pos).is(Blocks.MUSHROOM_STEM)
                                        : world.getBlockState(pos).isAir(),
                                "The red cap interior must contain only its stem and air: " + pos);
                    }
                }
                reference.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                        "Clearing enclosed leaves must preserve the entire native cap and stem: " + pos));
                preserved.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                        "Leaves outside, above, or below the cap must remain: " + pos));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void laterFoliageCannotRefillRedCapsButCanGrowBelowAndOutside(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
        helper.assertTrue(TropicalTreeShapeTests.place(world, "minecraft:huge_red_mushroom", 7),
                "The actual mushroom must generate before the later foliage");
        int top = capTop(blocks);
        var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:rainforest/tree/natural_fancy_oak"));
        var config = (TreeConfiguration) feature.config();
        var setter = new FoliagePlacer.FoliageSetter() {
            @Override
            public void set(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); }
            @Override
            public boolean isSet(BlockPos pos) { return world.getBlockState(pos).is(Blocks.OAK_LEAVES); }
        };
        for (int y = top - 3; y < top; y++) {
            BlockPos inside = new BlockPos(9, y, 8);
            helper.assertTrue(RedMushroomCapSpace.isInsideRedCap(world, inside), "All three hollow cap layers are protected");
            helper.assertFalse(LeafProbe.place(world, setter, config, inside),
                    "Native foliage must skip enclosed air inside an existing red cap");
            helper.assertTrue(world.getBlockState(inside).isAir(), "Native foliage must not refill the hollow cap");
            helper.assertFalse(TreeGrowthCollisionRules.isAllowed(world, inside, Blocks.OAK_LEAVES.defaultBlockState()),
                    "Template-based foliage must also keep the enclosed cap air");
            helper.assertFalse(TreeGrowthCollisionRules.isAllowed(world, inside, Blocks.JUNGLE_LOG.defaultBlockState()),
                    "Later branches must not enter the hollow cap even without touching a cap block");
        }
        for (BlockPos outside : new BlockPos[]{new BlockPos(9, top - 4, 8), new BlockPos(11, top - 1, 8)}) {
            helper.assertFalse(RedMushroomCapSpace.isInsideRedCap(world, outside),
                    "The exclusion must stop at the cap walls and bottom edge");
            helper.assertTrue(LeafProbe.place(world, setter, config, outside),
                    "Foliage must still grow underneath and alongside the cap");
        }
        BlockPos roof = new BlockPos(9, top, 8);
        helper.assertFalse(LeafProbe.place(world, setter, config, roof), "Later foliage must preserve the cap itself");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void logsInIntendedCapsRejectWholeMushroomsInsideNaturalBuffers(GameTestHelper helper) {
        for (String feature : new String[]{"minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom"}) {
            for (long seed : new long[]{1, 7, 42, 91}) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                // An off-center existing trunk crosses every possible cap height,
                // including the red cap's hollow corner. Height retries cannot escape it.
                for (int y = 101; y <= 120; y++) {
                    blocks.put(new BlockPos(9, y, 9), Blocks.JUNGLE_LOG.defaultBlockState());
                    blocks.put(new BlockPos(7, y, 8), Blocks.JUNGLE_LEAVES.defaultBlockState());
                }
                Map<BlockPos, BlockState> before = Map.copyOf(blocks);
                var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
                var outer = new GrowthPlacementBuffer(world);
                helper.assertFalse(TropicalTreeShapeTests.place(outer, feature, seed),
                        "A mushroom must fail when an existing log crosses its intended cap volume: " + feature);
                helper.assertTrue(outer.commit() && blocks.equals(before),
                        "A failed mushroom must leave no partial cap, stem or leaf clearing in its outer transaction");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nestedTreeAttemptsCannotRefillCapsOrLeakRejectedBlocks(GameTestHelper helper) {
        for (BlockState intrusion : new BlockState[]{Blocks.JUNGLE_LOG.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState()}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(world, "minecraft:huge_red_mushroom", 7),
                    "The complete mushroom must generate before a neighboring tree");
            Map<BlockPos, BlockState> mushroom = Map.copyOf(blocks);
            BlockPos inside = new BlockPos(9, capTop(blocks) - 1, 9);
            helper.assertTrue(RedMushroomCapSpace.isInsideRedCap(world, inside), "The fixture must target hollow cap air");
            var outer = new GrowthPlacementBuffer(world);
            BlockPos earlier = new BlockPos(30, 110, 30), branch = earlier.above(4);
            outer.setBlock(earlier, Blocks.OAK_LEAVES.defaultBlockState(), 2);
            boolean placed = AtomicTreeGrowth.place(new FeaturePlaceContext<>(Optional.empty(), outer,
                    world.getLevel().getChunkSource().getGenerator(), RandomSource.create(37), new BlockPos(8, 101, 8),
                    NoneFeatureConfiguration.INSTANCE), staged -> {
                staged.level().setBlock(branch, Blocks.JUNGLE_LOG.defaultBlockState(), 2);
                staged.level().setBlock(inside, intrusion, 2);
                return true;
            });
            helper.assertFalse(placed, "An outer natural-generation buffer must not bypass cap collision checks");
            helper.assertTrue(outer.getBlockState(inside).isAir() && outer.getBlockState(branch).isAir(),
                    "Every rejected nested attempt must discard both its intruding block and unrelated branch");
            helper.assertTrue(outer.commit() && world.getBlockState(earlier).is(Blocks.OAK_LEAVES),
                    "Rejecting the nested tree must retain the caller's earlier staged work");
            mushroom.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                    "Rejected trees must preserve every existing mushroom block: " + pos));
            helper.assertTrue(world.getBlockState(inside).isAir(), "The final mushroom interior must remain free of logs and leaves");
        }
        helper.succeed();
    }

    private static int capTop(Map<BlockPos, BlockState> blocks) {
        return blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.RED_MUSHROOM_BLOCK))
                .mapToInt(entry -> entry.getKey().getY()).max().orElseThrow();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void laterTreeFoliageRejectsAWholeCrownThatWouldWrapAnExistingStem(GameTestHelper helper) {
        BlockPos stem = new BlockPos(16, 107, 8), origin = new BlockPos(12, 101, 8);
        for (boolean crossesStem : new boolean[]{true, false}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            blocks.put(stem, Blocks.MUSHROOM_STEM.defaultBlockState());
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
            var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("aoemod:rainforest/tree/natural_fancy_oak"));
            var config = (TreeConfiguration) feature.config();
            var outer = new GrowthPlacementBuffer(world);
            BlockPos earlier = new BlockPos(33, 115, 33);
            outer.setBlock(earlier, Blocks.OAK_LEAVES.defaultBlockState(), 2);
            boolean placed = AtomicTreeGrowth.place(new FeaturePlaceContext<>(Optional.empty(), outer,
                    world.getLevel().getChunkSource().getGenerator(), RandomSource.create(71), origin,
                    NoneFeatureConfiguration.INSTANCE), staged -> {
                staged.level().setBlock(origin, Blocks.OAK_LOG.defaultBlockState(), 2);
                var setter = new FoliagePlacer.FoliageSetter() {
                    @Override
                    public void set(BlockPos pos, BlockState state) { staged.level().setBlock(pos, state, 2); }
                    @Override
                    public boolean isSet(BlockPos pos) { return staged.level().getBlockState(pos).is(Blocks.OAK_LEAVES); }
                };
                helper.assertTrue(LeafProbe.place(staged.level(), setter, config, stem.west()),
                        "Foliage beside an existing stem must remain allowed");
                if (crossesStem) {
                    helper.assertFalse(LeafProbe.place(staged.level(), setter, config, stem),
                            "The native leaf routine must preserve the existing stem");
                }
                // Vanilla foliage ignores a false leaf result and reports tree
                // success. The transaction must still reject that omitted cell.
                return true;
            });
            helper.assertTrue(placed != crossesStem,
                    "Only a crown whose intended foliage crosses the stem must be rejected");
            helper.assertTrue(outer.commit(), "The enclosing natural-generation transaction must remain usable");
            helper.assertTrue(world.getBlockState(stem).is(Blocks.MUSHROOM_STEM)
                            && world.getBlockState(earlier).is(Blocks.OAK_LEAVES),
                    "The earlier mushroom and unrelated caller placements must survive either order");
            helper.assertTrue(crossesStem
                            ? world.getBlockState(origin).isAir() && world.getBlockState(stem.west()).isAir()
                            : world.getBlockState(origin).is(Blocks.OAK_LOG) && world.getBlockState(stem.west()).is(Blocks.OAK_LEAVES),
                    "Rejected crowns must leave no partial trunk or surrounding leaves");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void stemObstaclesCancelWholeMushroomsButCapsCanReplaceLeaves(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) {
            for (String feature : new String[]{"minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom"}) {
                for (var obstacle : new net.minecraft.world.level.block.Block[]{Blocks.JUNGLE_LEAVES, Blocks.STONE, Blocks.OAK_LOG}) {
                    Map<BlockPos, BlockState> blocks = new HashMap<>();
                    blocks.put(new BlockPos(8, 102, 8), obstacle.defaultBlockState());
                    var before = Map.copyOf(blocks);
                    var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, tropical);
                    var buffer = new GrowthPlacementBuffer(world);
                    helper.assertFalse(TropicalTreeShapeTests.place(buffer, feature, 7),
                            "A stem crossing leaves or a solid obstacle must reject the entire mushroom: " + feature);
                    helper.assertTrue(buffer.commit() && blocks.equals(before),
                            "Rejected mushrooms must leave no cap, partial stem, or cleared foliage");
                }
                Map<BlockPos, BlockState> reference = new HashMap<>();
                var control = TropicalTreeShapeTests.fixture(helper, reference, "rainforest", 0, tropical);
                helper.assertTrue(TropicalTreeShapeTests.place(control, feature, 7), "Unobstructed reference must grow");
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                reference.forEach((pos, state) -> {
                    if (state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)) {
                        blocks.put(pos, Blocks.JUNGLE_LEAVES.defaultBlockState());
                    }
                });
                var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, tropical);
                helper.assertTrue(TropicalTreeShapeTests.place(world, feature, 7),
                        "Leaves confined to the actual red/brown cap must remain replaceable");
                reference.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                        "Permitted cap overlap must preserve the complete mushroom shape"));
            }
        }
        helper.succeed();
    }

    private static final class LeafProbe extends BlobFoliagePlacer {
        private LeafProbe() { super(ConstantInt.of(2), ConstantInt.of(0), 3); }

        private static boolean place(LevelSimulatedReader level, FoliageSetter setter, TreeConfiguration config, BlockPos pos) {
            return tryPlaceLeaf(level, setter, RandomSource.create(1), config, pos);
        }
    }
}
