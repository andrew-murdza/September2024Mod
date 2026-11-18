package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.event_handlers.BlockGrowthRate;
import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetainedCropGrowthTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cactusProducesMaturingHarvestablePricklyPearOnlyInDesert(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var pear = samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get();
        var fruit = samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get();
        BlockPos base = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 230, 8);
        BlockPos top = base.above(3);
        BlockPos fruitPos = top.above();
        for (var level : java.util.List.of(desert, tropical)) {
            try {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y <= 5; y++) {
                    level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
                }
                // Emulate an existing/world-generated cactus independently of the placement restrictions.
                var chunk = level.getChunkAt(base);
                chunk.setBlockState(base, Blocks.SAND.defaultBlockState(), false);
                for (int y = 1; y <= 3; y++) chunk.setBlockState(base.above(y), Blocks.CACTUS.defaultBlockState(), false);
                helper.assertTrue(level.getBlockState(top).is(Blocks.CACTUS)
                                && level.getBlockState(top.below()).is(Blocks.CACTUS)
                                && level.getBlockState(top.below(2)).is(Blocks.CACTUS),
                        "Both dimensions need an existing three-block cactus before checking fruit creation");
                level.getBlockState(top).randomTick(level, top, RandomSource.create(150));
                helper.assertTrue(level.getBlockState(fruitPos).is(pear) == (level == desert),
                        "The actual cactus growth hook must create fruit only in Desert");
                if (level != desert) continue;
                helper.assertTrue(BlockConfig.blockGrowthChance(level, fruitPos, pear.defaultBlockState()) == 4.0F,
                        "Desert prickly pear must retain its configured growth rate");
                for (int tick = 0; tick < 3; tick++) {
                    BlockGrowthRate.blockGrowthPre(new BlockEvent.CropGrowEvent.Pre(level, fruitPos, level.getBlockState(fruitPos)));
                }
                BlockState ripe = level.getBlockState(fruitPos);
                helper.assertTrue(net.amurdza.examplemod.config.DocumentedContentBalance.mature(ripe),
                        "The Desert growth event must mature fruit on its cactus support");
                var drops = CustomBlockDrops.configuredDrops(level, fruitPos, ripe, ItemStack.EMPTY, RandomSource.create(1));
                helper.assertTrue(drops != null && drops.stream().filter(stack -> stack.is(fruit)).mapToInt(ItemStack::getCount).sum() == 1,
                        "Mature Desert fruit must produce the configured edible harvest");
            } finally {
                for (int y = 0; y <= 5; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 18);
            }
        }
        helper.assertTrue(BlockConfig.blockGrowthChance(tropical, fruitPos, pear.defaultBlockState()) < 0
                        && BlockConfig.blockDropEntries(tropical, fruitPos, mature(pear.defaultBlockState())).isEmpty(),
                "Prickly pear must no longer have Tropical growth or harvest tuning");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void retainedCropStagesHaveGrowthAndMatureFoodDropsInTheirAssignedDimension(GameTestHelper helper) {
        for (String[] crop : new String[][]{
                {"pineapple_delight:pineapple_crop", "pineapple_delight:pineapple", "tropical"},
                {"hearthandharvest:cotton", "hearthandharvest:cotton", "tropical"},
                {"hearthandharvest:corn_stalk", "hearthandharvest:corn", "tropical"},
                {"legumedelight:beans", "legumedelight:beans", "tropical"},
                {"legumedelight:beans_on_rope", "legumedelight:beans", "tropical"},
                {"vintagedelight:peanut_crop", "vintagedelight:peanut", "desert"}}) {
            var dimension = crop[2].equals("tropical") ? GameplayDimensions.TROPICAL : GameplayDimensions.DESERT;
            var level = helper.getLevel().getServer().getLevel(dimension);
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(crop[0]));
            var produce = ForgeRegistries.ITEMS.getValue(new ResourceLocation(crop[1]));
            helper.assertTrue(block != null && block != Blocks.AIR && produce != null,
                    "Documented crop stages and produce must resolve: " + crop[0]);
            BlockState ripe = mature(block.defaultBlockState());
            helper.assertTrue(BlockConfig.blockGrowthChance(level, BlockPos.ZERO, ripe) > 0,
                    "Retained crop must have a positive regional growth rate: " + crop[0]);
            var drops = CustomBlockDrops.configuredDrops(level, BlockPos.ZERO, ripe, ItemStack.EMPTY, RandomSource.create(7));
            if (drops == null) drops = Block.getDrops(ripe, level, new BlockPos(0, 230, 0), null);
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(produce) && !stack.isEmpty()),
                    "Mature retained crop must preserve its produce through the actual loot route: " + crop[0]);
            if (crop[0].equals("hearthandharvest:cotton")) {
                var recipe = level.getRecipeManager().byKey(new ResourceLocation("hearthandharvest:string_from_cotton")).orElseThrow();
                var result = recipe.getResultItem(level.registryAccess());
                helper.assertTrue(recipe.getIngredients().size() == 3 && recipe.getIngredients().stream()
                                .allMatch(ingredient -> ingredient.test(new ItemStack(produce)))
                                && result.is(net.minecraft.world.item.Items.STRING) && result.getCount() == 2,
                        "Three harvested cotton must craft two string without combat");
            }
        }
        for (String id : new String[]{"legumedelight:budding_beans"}) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
            var dimension = id.startsWith("legume") ? GameplayDimensions.TROPICAL : GameplayDimensions.DESERT;
            helper.assertTrue(BlockConfig.blockGrowthChance(helper.getLevel().getServer().getLevel(dimension),
                            BlockPos.ZERO, block.defaultBlockState()) > 0,
                    "Intermediate budding crop must not stall before its harvestable stage: " + id);
        }
        helper.succeed();
    }

    private static BlockState mature(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                state = state.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElseThrow());
            }
        }
        return state;
    }
}
