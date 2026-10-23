package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DocumentedBalanceTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void documentedCropsKeepSeedsAndRegionalYields(GameTestHelper helper) {
        var savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var cabbage = (CropBlock) ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight:cabbages"));
        var produce = ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight:cabbage"));
        var seeds = ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight:cabbage_seeds"));
        var ripe = cabbage.getStateForAge(cabbage.getMaxAge());
        var young = cabbage.getStateForAge(0);
        var entries = BlockConfig.blockDropEntries(savanna, BlockPos.ZERO, ripe);
        helper.assertTrue(entries.size() == 2, "Cabbage should have one produce and one seed rule per dimension");
        helper.assertTrue(entries.stream().filter(e -> e.item() == produce)
                        .mapToDouble(e -> BlockConfig.blockDropAmount(ripe, e)).sum() == 2,
                "Savanna cabbage should yield two mature crops");
        helper.assertTrue(entries.stream().filter(e -> e.item() == produce)
                        .mapToDouble(e -> BlockConfig.blockDropAmount(young, e)).sum() == 0,
                "Immature plants must not yield mature produce");
        helper.assertTrue(entries.stream().filter(e -> e.item() == seeds)
                        .mapToDouble(e -> BlockConfig.blockDropAmount(young, e)).sum() == 1,
                "Immature plants must return their planting seed");
        helper.assertTrue(BlockConfig.blockGrowthChance(savanna, BlockPos.ZERO, young)
                        > BlockConfig.blockGrowthChance(plains, BlockPos.ZERO, young),
                "Savanna should retain its farming advantage over plains");
        helper.assertTrue(BlockConfig.blockDropEntries(helper.getLevel(), BlockPos.ZERO, ripe).isEmpty(),
                "The ordinary Overworld should retain the mod's native crop drops");
        var workerDrops = net.amurdza.examplemod.event_handlers.CustomBlockDrops.configuredDrops(savanna,
                BlockPos.ZERO, ripe, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.util.RandomSource.create(17));
        helper.assertTrue(workerDrops.stream().filter(stack -> stack.is(produce)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum() == 2,
                "Worker loot rolls must share the same regional crop yield as player harvests");
        helper.assertTrue(net.amurdza.examplemod.event_handlers.CustomBlockDrops.configuredDrops(helper.getLevel(),
                BlockPos.ZERO, ripe, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.util.RandomSource.create(17)) == null,
                "Unconfigured worker loot must delegate to the original table");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void documentedFruitAndLivestockUseRealItems(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (String[] mapping : new String[][]{{"dark_oak_leaves", "aoemod:plum"},
                {"cherry_leaves", "hearthandharvest:cherry"}}) {
            var leaves = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", mapping[0]));
            var fruit = ForgeRegistries.ITEMS.getValue(new ResourceLocation(mapping[1]));
            helper.assertTrue(fruit != null && fruit != Items.AIR, "Missing documented fruit " + mapping[1]);
            helper.assertTrue(BlockConfig.blockDropEntries(tropical, BlockPos.ZERO, leaves.defaultBlockState())
                    .stream().anyMatch(e -> e.item() == fruit && BlockConfig.blockDropAmount(leaves.defaultBlockState(), e) > 0),
                    "Wrong fruit mapped to " + mapping[0]);
            String recipeName = mapping[0].equals("dark_oak_leaves") ? "plum_pie" : "cherry_pie";
            var recipe = tropical.getRecipeManager().byKey(new ResourceLocation(AOEMod.MOD_ID, "food/" + recipeName));
            helper.assertTrue(recipe.isPresent() && recipe.get().getIngredients().stream()
                            .anyMatch(ingredient -> ingredient.test(new net.minecraft.world.item.ItemStack(fruit))),
                    "The matching pie must consume the documented tree fruit: " + recipeName);
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("biodiverse:wildebeest"));
        var info = MobConfig.MOB_INFO_BY_MOB.get(type);
        helper.assertTrue(info != null && info.aging().savanna() > info.aging().plains(),
                "Wildebeest should mature fastest in their savanna home");
        helper.assertTrue(info.drops().stream().anyMatch(e -> e.item() == Items.BEEF && e.savanna() == 5),
                "Wildebeest should supply the configured beef yield");
        helper.succeed();
    }
}
