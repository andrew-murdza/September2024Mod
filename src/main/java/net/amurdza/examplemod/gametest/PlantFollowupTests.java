package net.amurdza.examplemod.gametest;

import com.soytutta.mynethersdelight.common.registry.MNDBlocks;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.PowderCaneShape;
import net.amurdza.examplemod.worldgen.TreeLogConnections;
import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantFollowupTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void thinCaneModelsFollowLeafStateWithoutSideBulbs(GameTestHelper helper) throws Exception {
        try (var stream = PlantFollowupTests.class.getResourceAsStream(
                "/assets/mynethersdelight/blockstates/powdery_cane.json")) {
            helper.assertTrue(stream != null, "The thin-cane blockstate override must be packaged");
            var parts = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("multipart");
            for (var state : MNDBlocks.POWDERY_CANE.get().getStateDefinition().getPossibleStates()) {
                Set<String> selected = new HashSet<>();
                String leaves = state.getValue(BlockStateProperties.BAMBOO_LEAVES).getSerializedName();
                for (var element : parts) {
                    var part = element.getAsJsonObject();
                    if (part.has("when")) {
                        var condition = part.getAsJsonObject("when");
                        helper.assertTrue(condition.size() == 1 && condition.has("leaves"),
                                "A lower stalk cannot add age/lit-controlled side bulbs independently of its leaves");
                        if (!condition.get("leaves").getAsString().equals(leaves)) continue;
                    }
                    selected.add(part.getAsJsonObject("apply").get("model").getAsString());
                }
                Set<String> expected = new HashSet<>(Set.of("mynethersdelight:block/cane_on"));
                if (leaves.equals("small")) expected.add("mynethersdelight:block/cane_leaves");
                if (leaves.equals("large")) expected.add("mynethersdelight:block/cannon_small_leaves");
                helper.assertTrue(selected.equals(expected), "Thin-cane geometry must follow only its leaf state: " + state);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fancyBranchConnectionsUseMinimalUpperRoutes(GameTestHelper helper) {
        BlockPos start = new BlockPos(8, 100, 8);
        helper.assertTrue(TreeLogConnections.connectors(Set.of(start, start.offset(1, 1, 0)), pos -> true)
                        .equals(java.util.List.of(start.above())),
                "A sloping branch must use its upper corner without adding a hanging lower duplicate");
        for (int dx : new int[]{-1, 0, 1}) for (int dy : new int[]{-1, 0, 1}) for (int dz : new int[]{-1, 0, 1}) {
            BlockPos end = start.offset(dx, dy, dz);
            Set<BlockPos> nativeLogs = new HashSet<>(); nativeLogs.add(start); nativeLogs.add(end);
            var additions = TreeLogConnections.connectors(nativeLogs, pos -> true);
            int expected = Math.max(0, Math.abs(dx) + Math.abs(dy) + Math.abs(dz) - 1);
            helper.assertTrue(additions.size() == expected, "Only the minimum face-connecting logs may be added: " + additions);
            helper.assertTrue(additions.stream().allMatch(p -> p.getY() == Math.max(start.getY(), end.getY())),
                    "Available upper routes must avoid lower hanging logs");
            Set<BlockPos> logs = new HashSet<>(additions), visited = new HashSet<>();
            logs.add(start); logs.add(end);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                if (!logs.contains(pos) || !visited.add(pos)) continue;
                for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
            }
            helper.assertTrue(visited.equals(logs), "Every diagonal must become face-connected");
            helper.assertTrue(TreeLogConnections.connectors(logs, pos -> true).isEmpty(),
                    "An already connected whole-tree graph must never gain another set of filler logs");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void tropicalIslandsKeepAreaScaledCropAndTorchflowerQuotas(GameTestHelper helper) {
        for (long seed : new long[]{0, 50881, -49835}) {
            TropicalLayout layout = new TropicalLayout(seed);
            for (int cell : new int[]{-1, 0, 1}) {
                var plan = TropicalIslandPlan.forIsland(layout, layout.island(cell, -cell));
                Map<String, Integer> forest = new HashMap<>(), cherry = new HashMap<>();
                for (var chunk : plan.rainforest()) forest.merge(plan.rainforestSpecies(chunk.x(), chunk.z()), 1, Integer::sum);
                for (var chunk : plan.cherry()) {
                    String crop = plan.cherryCrop(chunk.x(), chunk.z());
                    if (crop != null) {
                        cherry.merge(crop, 1, Integer::sum);
                        helper.assertTrue(plan.cherryFlower(chunk.x(), chunk.z()) == null, "Crop chunks must replace flower chunks");
                    }
                }
                helper.assertTrue(cherry.equals(Map.of("farmersdelight:wild_carrots", 11, "legumedelight:wild_beans", 11)), "Cherry crop counts must scale with grove area: " + cherry);
                for (String species : new String[]{"minecraft:melon", "farmersdelight:wild_tomatoes", "pineapple_delight:pineapple_wild_crop"}) {
                    helper.assertTrue(forest.getOrDefault(species, 0) == 16, "Rainforest crop counts must scale with annular area: " + forest);
                }
                for (int i = 132; i < 148; i++) {
                    var chunk = plan.rainforest().get(i);
                    helper.assertTrue("minecraft:torchflower".equals(plan.rainforestSpecies(chunk.x(), chunk.z())),
                            "Each freed rainforest crop chunk must contain torchflowers");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void nativeGroundCoverLootContainsItsRegionalSeeds(GameTestHelper helper) {
        Item kernels = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hearthandharvest:corn_kernels"));
        Set<Item> savannaSeeds = Set.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, ModItems.POTATO_SEEDS.get(), kernels);
        for (var dimension : java.util.List.of(GameplayDimensions.PLAINS, GameplayDimensions.SAVANNA)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            Set<Item> expected = dimension.equals(GameplayDimensions.SAVANNA) ? savannaSeeds
                    : Set.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, ModItems.POTATO_SEEDS.get());
            for (Block grass : new Block[]{Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN}) {
                Set<Item> observed = new HashSet<>();
                for (int sample = 0; sample < 6000; sample++) {
                    var drops = Block.getDrops(grass.defaultBlockState(), level, new BlockPos(0, 300, 0), null, null, ItemStack.EMPTY);
                    helper.assertTrue(drops.size() <= 1 && drops.stream().allMatch(stack -> expected.contains(stack.getItem())),
                            "Native grass loot escaped its regional seed palette: " + drops);
                    drops.forEach(stack -> observed.add(stack.getItem()));
                }
                helper.assertTrue(observed.equals(expected), "Native block loot must reach every seed, not only wheat: " + observed);
                var sheared = Block.getDrops(grass.defaultBlockState(), level, new BlockPos(0, 300, 0), null, null, new ItemStack(Items.SHEARS));
                helper.assertTrue(sheared.stream().noneMatch(stack -> expected.contains(stack.getItem())), "Shears must not roll seeds");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void powderCaneGeneratesTreesAndPlacesPepperSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(2, 20, 2));
        for (boolean thick : new boolean[]{false, true}) {
            for (int i = 0; i < 16; i++) level.setBlock(root.above(i), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(root.below(), Blocks.BASALT.defaultBlockState(), 18);
            helper.assertTrue(PowderCaneShape.placeTree(level, root, thick, RandomSource.create(3311)),
                    "Natural powder cane must generate a complete tree on basalt");
            int height = 0;
            while (PowderCaneShape.isPlant(level.getBlockState(root.above(height)))) height++;
            helper.assertTrue(height >= 7 && height <= 12, "Powder cane worldgen cannot leave a single starting block");
            var cap = level.getBlockState(root.above(height - 1));
            helper.assertTrue(cap.is(MNDBlocks.BULLET_PEPPER.get()) && cap.getValue(BlockStateProperties.AGE_3) == 3
                            && cap.getValue(BlockStateProperties.LIT),
                    "Tree needs one mature lit pepper cap, without the overlapping age-zero cap model");
            for (int i = 0; i < height - 1; i++) {
                var state = level.getBlockState(root.above(i));
                helper.assertTrue(!state.getValue(BlockStateProperties.LIT)
                                && state.getValue(BlockStateProperties.BAMBOO_LEAVES) == PowderCaneShape.leaves(height - 1 - i),
                        "Lower stalk must be unlit and retain the requested leaf sequence: " + state);
                helper.assertTrue(thick ? state.getValue(BlockStateProperties.AGE_1) == 1
                                : state.getValue(BlockStateProperties.AGE_3) == 3,
                        "Natural stems must select their mature unlit models: " + state);
            }
            if (!thick) {
                level.setBlock(root, level.getBlockState(root).setValue(BlockStateProperties.AGE_3, 0)
                        .setValue(BlockStateProperties.LIT, true), 18);
                PowderCaneShape.normalize(level, root);
                helper.assertTrue(!level.getBlockState(root).getValue(BlockStateProperties.LIT)
                                && level.getBlockState(root).getValue(BlockStateProperties.AGE_3) == 3,
                        "Normalizing a lower young/lit stem must make it mature and unlit");
            }
        }
        for (int i = 0; i < 16; i++) level.setBlock(root.above(i), Blocks.AIR.defaultBlockState(), 18);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("mynethersdelight:powder_cannon"))));
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(root.below()), Direction.UP, root.below(), false)));
        helper.assertTrue(MNDBlocks.POWDERY_CANNON.get().getStateForPlacement(context).is(MNDBlocks.BULLET_PEPPER.get()),
                "Planting a cane on the ground must select bullet pepper, not the chubby sapling");
        helper.succeed();
    }
}
