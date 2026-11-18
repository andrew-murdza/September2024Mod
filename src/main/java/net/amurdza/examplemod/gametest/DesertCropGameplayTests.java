package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertCropGameplayTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void dryGrassDropsPeanutAndDragonFruitSeedsAtMatchingRates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        for (boolean tall : new boolean[]{false, true}) {
            var state = (tall ? net.amurdza.examplemod.registry.ModBlocks.DESERT_TALL_GRASS.get()
                    : net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get()).defaultBlockState();
            RandomSource random = RandomSource.create(164097);
            int peanuts = 0, dragon = 0;
            for (int sample = 0; sample < 20000; sample++) {
                var drops = net.amurdza.examplemod.event_handlers.CustomBlockDrops.configuredDrops(level, BlockPos.ZERO,
                        state, ItemStack.EMPTY, random);
                helper.assertTrue(drops != null, "Both dry grasses must use the configured seed lottery");
                for (var drop : drops) {
                    if (drop.is(item("legumedelight:peanuts"))) peanuts += drop.getCount();
                    else if (drop.is(net.amurdza.examplemod.registry.RetainedDragonFruit.SEEDS.get())) dragon += drop.getCount();
                    else helper.fail("Unexpected desert grass drop " + drop);
                }
            }
            int expected = tall ? 1600 : 800;
            helper.assertTrue(Math.abs(peanuts - expected) < expected * .12 && Math.abs(dragon - expected) < expected * .12,
                    "Peanut and dragon-fruit seeds must share matching short/tall grass rates: " + peanuts + "," + dragon);
            helper.assertTrue(net.amurdza.examplemod.event_handlers.CustomBlockDrops.configuredDrops(level, BlockPos.ZERO,
                    state, new ItemStack(Items.SHEARS), random) == null, "Shears must retain the grass instead of rolling seeds");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void shortCactiGrowBeforeProducingFruit(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        BlockPos base = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 230, 24);
        try {
            for (int height = 1; height <= 2; height++) {
                for (int y = 0; y <= 5; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 18);
                var chunk = level.getChunkAt(base);
                chunk.setBlockState(base.below(), Blocks.STONE.defaultBlockState(), false);
                chunk.setBlockState(base, Blocks.SAND.defaultBlockState(), false);
                for (int y = 1; y <= height; y++) chunk.setBlockState(base.above(y), Blocks.CACTUS.defaultBlockState(), false);
                BlockPos top = base.above(height);
                level.getBlockState(top).randomTick(level, top, RandomSource.create(15));
                helper.assertTrue(level.getBlockState(top).getValue(CactusBlock.AGE) == 4
                                && level.isEmptyBlock(top.above()),
                        "A short cactus must spend its configured growth increments on height");
                for (int tick = 0; tick < 3; tick++) {
                    level.getBlockState(top).randomTick(level, top, RandomSource.create(15));
                }
                helper.assertTrue(level.getBlockState(top.above()).is(Blocks.CACTUS)
                                && level.isEmptyBlock(top.above(2)),
                        "One- and two-block columns must grow another cactus block before producing pears");
            }
            BlockPos top = base.above(3);
            level.getBlockState(top).randomTick(level, top, RandomSource.create(15));
            helper.assertTrue(level.getBlockState(top.above()).is(samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get()),
                    "A column that has reached three blocks must grow a prickly pear next");
        } finally {
            for (int y = -1; y <= 5; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void peanutSeedsPlantVintagePeanutsButCannotBeFoodOrIngredients(GameTestHelper helper) {
        Item seeds = item("legumedelight:peanuts");
        Item peanut = item("vintagedelight:peanut");
        Block crop = block("vintagedelight:peanut_crop");
        helper.assertTrue(seeds instanceof BlockItem && ((BlockItem) seeds).getBlock() == crop,
                "The existing Legume Delight item must plant the Vintage Delight crop");
        helper.assertTrue(!seeds.isEdible() && seeds.getFoodProperties() == null && peanut.isEdible(),
                "Peanut seeds must be nonfood while ordinary peanuts remain edible");
        Ingredient explicitSeed = Ingredient.of(seeds);
        Ingredient peanutTag = Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation("forge:peanut")));
        helper.assertTrue(!explicitSeed.test(new ItemStack(seeds)) && explicitSeed.getItems().length == 0
                        && explicitSeed.getStackingIds().isEmpty(),
                "Direct recipes and shapeless ingredient matching must reject planting seeds");
        helper.assertTrue(!peanutTag.test(new ItemStack(seeds)) && peanutTag.test(new ItemStack(peanut)),
                "Peanut recipe tags must accept ordinary peanuts but reject planting seeds");
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        var shapeless = new ShapelessRecipe(new ResourceLocation(AOEMod.MOD_ID, "test_peanut_ingredient"), "",
                CraftingBookCategory.MISC, new ItemStack(Items.STICK), NonNullList.of(Ingredient.EMPTY, peanutTag));
        grid.setItem(0, new ItemStack(seeds));
        helper.assertFalse(shapeless.matches(grid, helper.getLevel()), "Shapeless crafting must reject peanut seeds");
        grid.setItem(0, new ItemStack(peanut));
        helper.assertTrue(shapeless.matches(grid, helper.getLevel()), "Shapeless crafting must still accept edible peanuts");
        for (String suffix : List.of("", "_from_smoking", "_from_campfire_cooking")) {
            var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(AOEMod.MOD_ID,
                    "retained/vintagedelight/roasted_peanut" + suffix)).orElseThrow();
            helper.assertTrue(recipe instanceof AbstractCookingRecipe, "The retained peanut cooking recipe must load");
            var cooking = (AbstractCookingRecipe) recipe;
            helper.assertFalse(cooking.matches(new SimpleContainer(new ItemStack(seeds)), helper.getLevel()),
                    "Peanut seeds must not cook into food through the retained recipe");
            helper.assertTrue(cooking.matches(new SimpleContainer(new ItemStack(peanut)), helper.getLevel()),
                    "Ordinary peanuts must retain all three cooking routes");
        }

        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        BlockPos soil = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 230, 40);
        var player = FakePlayerFactory.getMinecraft(level);
        try {
            for (Item plantingItem : List.of(seeds, peanut)) {
                level.setBlock(soil.above(), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(soil, block("biomeswevegone:sandy_farmland").defaultBlockState(), 18);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(plantingItem));
                plantingItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false)));
                helper.assertTrue(level.getBlockState(soil.above()).is(crop),
                        "Both planting seeds and ordinary peanuts must successfully plant the same crop");
            }
        } finally {
            level.setBlock(soil.above(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(soil, Blocks.AIR.defaultBlockState(), 18);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wildCropsDropExactlyOneSeedEvenWithFortuneOrShears(GameTestHelper helper) {
        ItemStack fortune = new ItemStack(Items.DIAMOND_HOE);
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        ItemStack silk = new ItemStack(Items.DIAMOND_HOE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (String[] pair : new String[][]{{"vintagedelight:wild_peanuts", "legumedelight:peanuts"}}) {
            BlockState state = block(pair[0]).defaultBlockState();
            for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS), fortune, silk)) {
                LootParams params = new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                        .withParameter(LootContextParams.BLOCK_STATE, state)
                        .withParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.BLOCK);
                var drops = helper.getLevel().getServer().getLootData().getLootTable(state.getBlock().getLootTable())
                        .getRandomItems(params, 4);
                helper.assertTrue(drops.size() == 1 && drops.get(0).is(item(pair[1])) && drops.get(0).getCount() == 1,
                        "Wild crop must yield exactly one planting seed: " + pair[0]);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void desertWildCropsSurviveSandAndCactusBobblesHaveNoHosts(GameTestHelper helper) {
        BlockPos soil = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block ground : List.of(Blocks.SAND, Blocks.RED_SAND)) {
            helper.getLevel().setBlock(soil, ground.defaultBlockState(), 18);
            helper.assertTrue(block("vintagedelight:wild_peanuts").defaultBlockState()
                            .canSurvive(helper.getLevel(), soil.above()) == (ground == Blocks.SAND),
                    "Wild peanuts must survive ordinary sand and reject red sand");
        }
        TagKey<Block> hosts = TagKey.create(Registries.BLOCK, new ResourceLocation("environmental:cactus_bobble_plantable_on"));
        helper.assertTrue(ForgeRegistries.BLOCKS.tags().getTag(hosts).isEmpty(),
                "Environmental world generation and bonemeal must have no cactus-bobble hosts");
        helper.succeed();
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
    }

    private static Block block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
    }
}
