package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.BoneMealConfig;
import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.WildCropBalance;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildCropBalanceTests {
    private static final Map<String, String> SEEDS = Map.of(
            "farmersdelight:wild_carrots", "aoemod:carrot_seeds",
            "farmersdelight:wild_cabbages", "farmersdelight:cabbage_seeds",
            "farmersdelight:wild_tomatoes", "farmersdelight:tomato_seeds",
            "farmersdelight:wild_rice", "aoemod:rice_seeds",
            "legumedelight:wild_beans", "aoemod:bean_seeds",
            "hearthandharvest:wild_cotton", "hearthandharvest:cotton_seeds",
            "pineapple_delight:pineapple_wild_crop", "pineapple_delight:pineapple_crop");
    private static final BlockPos ROOT = new BlockPos(8, 101, 8);

    @GameTest(template = "plant_gameplay_empty")
    public static void loadedWildCropTablesYieldNinePlantingItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var original = level.getBlockState(pos);
        var originalUpper = level.getBlockState(pos.above());
        var fortune = new ItemStack(Items.DIAMOND_HOE);
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        try {
            for (var mapping : SEEDS.entrySet()) {
                BlockState state = block(mapping.getKey());
                level.getChunkAt(pos).setBlockState(pos, state, false);
                boolean tall = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
                level.getChunkAt(pos.above()).setBlockState(pos.above(), tall
                        ? state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                        : Blocks.AIR.defaultBlockState(), false);
                Item seed = ForgeRegistries.ITEMS.getValue(new ResourceLocation(mapping.getValue()));
                if (mapping.getKey().equals("legumedelight:wild_beans")) {
                    helper.assertTrue(seed instanceof net.minecraft.world.item.BlockItem beanSeeds
                                    && beanSeeds.getBlock() == block("legumedelight:budding_beans").getBlock()
                                    && net.amurdza.examplemod.util.RetainedContent.isKeptItem(new ItemStack(seed)),
                            "Bean seeds must plant the budding crop and remain available in the item catalog");
                }
                for (ItemStack tool : List.of(ItemStack.EMPTY, fortune)) {
                    for (long roll = 1; roll <= 16; roll++) {
                        List<ItemStack> drops = drops(level, pos, state, tool, roll);
                        int count = drops.stream().filter(stack -> stack.is(seed)).mapToInt(ItemStack::getCount).sum();
                        helper.assertTrue(count == 9, mapping.getKey() + " must yield exactly nine planting items, got " + count);
                        if (mapping.getValue().startsWith("aoemod:")) {
                            helper.assertTrue(drops.stream().noneMatch(ItemStack::isEdible),
                                    mapping.getKey() + " must require cultivation before producing food");
                        }
                    }
                }
                if (tall) {
                    var upper = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                    helper.assertTrue(drops(level, pos.above(), upper, ItemStack.EMPTY, 1).stream()
                                    .filter(stack -> stack.is(seed)).mapToInt(ItemStack::getCount).sum() == 9,
                            "Harvesting the upper rice half must give the same nine rice seeds");
                }
                if (mapping.getValue().startsWith("aoemod:")) {
                    List<ItemStack> sheared = drops(level, pos, state, new ItemStack(Items.SHEARS), 1);
                    helper.assertTrue(sheared.size() == 1 && sheared.get(0).is(state.getBlock().asItem())
                                    && sheared.get(0).getCount() == 1,
                            "Shears must preserve exactly one wild plant for " + mapping.getKey());
                }
            }
        } finally {
            level.getChunkAt(pos).setBlockState(pos, original, false);
            level.getChunkAt(pos.above()).setBlockState(pos.above(), originalUpper, false);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyWildCropUsesOnePercentAndAddsAtMostOneRoot(GameTestHelper helper) {
        for (String species : WildCropBalance.CROPS) {
            helper.assertTrue(SEEDS.containsKey(species), "Balanced wild crop must have a planting-item loot check: " + species);
            boolean rice = species.equals("farmersdelight:wild_rice");
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            String biome = switch (species) {
                case "farmersdelight:wild_carrots", "legumedelight:wild_beans" -> "cherry_grove";
                case "farmersdelight:wild_rice", "farmersdelight:wild_cabbages", "hearthandharvest:wild_cotton" -> "mangrove_swamp";
                default -> "rainforest";
            };
            var world = AquaticPlantGenerationTests.fixture(helper, biome, blocks, rice ? 1 : 0);
            for (float roll : new float[]{0, .0099F, .01F, .99F}) {
                blocks.clear();
                helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, species, RandomSource.create(19)),
                        "Spread fixture must support the real wild source " + species);
                var before = Map.copyOf(blocks);
                // Rice also exercises selecting the upper half of a two-block source.
                BlockPos clicked = rice && roll == 0 ? ROOT.above() : ROOT;
                boolean spread = WildCropBalance.spread(world, clicked, new LegacyRandomSource(19) {
                    @Override public float nextFloat() { return roll; }
                });
                boolean expected = roll < .01F;
                long roots = blocks.values().stream().filter(state -> WildCropBalance.isWildCrop(state)
                        && (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)).count();
                helper.assertTrue(spread == expected && roots == (expected ? 2 : 1),
                        species + " must use one common one-percent roll and create only one new root: " + roll);
                before.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                        "Spreading cannot replace its original plant or either original rice half"));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wildCarrotBonemealRemainsFiveTimesLessEfficientThanCultivation(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var world = AquaticPlantGenerationTests.fixture(helper, "cherry_grove", blocks, 0);
        var source = block("farmersdelight:wild_carrots");
        RandomSource random = RandomSource.create(8012514);
        int successes = 0;
        for (int use = 0; use < 10000; use++) {
            blocks.clear();
            blocks.put(ROOT, source);
            if (WildCropBalance.spread(world, ROOT, random)) successes++;
            helper.assertTrue(blocks.size() <= 2, "One bone meal must never generate a multi-crop patch");
        }
        helper.assertTrue(successes >= 65 && successes <= 140,
                "Actual spread rolls must remain near one percent, got " + successes + "/10000");
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var ripe = Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
        double cultivatedYield = BlockConfig.blockDropEntries(plains, ROOT, ripe).stream()
                .filter(entry -> entry.item() == Items.CARROT).mapToDouble(entry -> BlockConfig.blockDropAmount(ripe, entry)).sum();
        // Seven growth stages at a minimum of two stages per normal application
        // take at most four bone meal; reserve one harvested carrot to replant.
        double cultivatedMinimum = (cultivatedYield - 1) / 4;
        double wildMaximum = WildCropBalance.SPREAD_CHANCE * WildCropBalance.SEED_YIELD;
        helper.assertTrue(BoneMealConfig.DIMENSION_TO_BONEMEAL_MULTIPLIERS.get(GameplayDimensions.PLAINS).plant() == 1,
                "The conservative cultivated-carrot comparison assumes one full growth attempt in Plains");
        helper.assertTrue(cultivatedMinimum > 0 && wildMaximum <= cultivatedMinimum / 5,
                "Nine-drop wild carrots must still be at least five times less efficient than cultivated carrots");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void failedWildSpreadStillConsumesExactlyOneMealThroughBothInputs(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(4, 3, 4)).atY(220);
        Map<BlockPos, BlockState> previous = new HashMap<>();
        // No nearby planting cell is available: both input paths must consume
        // one meal without falling through to the source mod's normal spread.
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -1; y <= 2; y++) {
            BlockPos pos = root.offset(x, y, z);
            previous.put(pos, level.getBlockState(pos));
            level.getChunkAt(pos).setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
        }
        var source = block("farmersdelight:wild_carrots");
        level.getChunkAt(root).setBlockState(root.below(), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        level.getChunkAt(root).setBlockState(root, source, false);
        try {
            ItemStack dispenser = new ItemStack(Items.BONE_MEAL, 3);
            helper.assertTrue(BoneMealItem.growCrop(dispenser, level, root) && dispenser.getCount() == 2,
                    "Dispenser/BoneMealItem calls must consume one meal even when wild spread has no space");
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 3));
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, root,
                    new BlockHitResult(Vec3.atCenterOf(root), Direction.UP, root, false));
            BoneMeal.onRightClickBlock(event);
            helper.assertTrue(event.isCanceled() && player.getMainHandItem().getCount() == 2,
                    "Player use must handle wild crops once and consume one meal on failed spread");
            helper.assertTrue(level.getBlockState(root).equals(source), "Failed spreading must preserve its original plant");
        } finally {
            previous.forEach((pos, state) -> level.getChunkAt(pos).setBlockState(pos, state, false));
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wholePumpkinsAndMelonsCraftNineSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        var menu = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        for (Item[] conversion : new Item[][]{{Items.PUMPKIN, Items.PUMPKIN_SEEDS}, {Items.MELON, Items.MELON_SEEDS},
                {Items.MELON_SLICE, Items.MELON_SEEDS}}) {
            grid.clearContent();
            grid.setItem(3, new ItemStack(conversion[0]));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(recipe.isPresent(), "Missing loaded seed recipe for " + conversion[0]);
            ItemStack output = recipe.orElseThrow().assemble(grid, level.registryAccess());
            int expected = conversion[0] == Items.MELON_SLICE ? 1 : 9;
            helper.assertTrue(output.is(conversion[1]) && output.getCount() == expected,
                    "Wrong seed conversion for " + conversion[0] + ": " + output);
            grid.setItem(0, new ItemStack(Items.DIRT));
            helper.assertFalse(recipe.orElseThrow().matches(grid, level), "Seed conversion cannot ignore an unrelated ingredient");
        }
        helper.succeed();
    }

    private static BlockState block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState();
    }

    private static List<ItemStack> drops(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        return level.getServer().getLootData().getLootTable(state.getBlock().getLootTable()).getRandomItems(params, seed);
    }
}
