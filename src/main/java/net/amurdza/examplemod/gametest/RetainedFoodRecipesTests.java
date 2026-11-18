package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetainedFoodRecipesTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void retainedFoodsUseAvailableCraftingAndCookingRecipes(GameTestHelper helper) {
        for (String path : new String[]{
                "pineapple_delight/pineapple_pie",
                "vintagedelight/roasted_peanut", "vintagedelight/roasted_peanut_from_smoking",
                "vintagedelight/roasted_peanut_from_campfire_cooking",
                "legumedelight/cooking/refried_beans", "legumedelight/refried_beans_from_smoking",
                "legumedelight/refried_beans_from_campfire_cooking", "upgrade_aquatic/boiled_pickerelweed",
                "upgrade_aquatic/boiled_pickerelweed_from_smoking", "upgrade_aquatic/boiled_pickerelweed_from_campfire_cooking",
                "collectorsreap/cutting/dragon_bush",
                "collectorsreap/dragon_fruit_seeds_from_pink_dragon_fruit",
                "farmersdelight/honey_cookie",
                "farmersdelight/cutting/pumpkin", "farmersdelight/pie_crust"}) {
            var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(AOEMod.MOD_ID, "retained/" + path));
            helper.assertTrue(recipe.isPresent(), "Retained native recipe must load: " + path);
            helper.assertFalse(recipe.orElseThrow().getResultItem(helper.getLevel().registryAccess()).isEmpty(),
                    "Restored recipe must resolve its output: " + path);
            for (var ingredient : recipe.orElseThrow().getIngredients()) {
                helper.assertTrue(ingredient.isEmpty() || ingredient.getItems().length > 0,
                        "Restored recipe must have registered ingredient/tag members: " + path);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedKitchenEquipmentAndItsRecipeTypesStayUnavailable(GameTestHelper helper) {
        for (String path : List.of("cutting_board", "flint_knife", "cooking_pot")) {
            var id = new ResourceLocation("farmersdelight", path);
            var item = ForgeRegistries.ITEMS.getValue(id);
            helper.assertTrue(item != null && item != Items.AIR && RetainedContent.isExplicitlyRemovedItem(id)
                            && !RetainedContent.isKeptItem(item.getDefaultInstance()),
                    "Removed kitchen equipment must stay registered for old saves but hidden: " + path);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                            recipe.getResultItem(helper.getLevel().registryAccess()).is(item)),
                    "Removed kitchen equipment must not remain craftable: " + path);
        }
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            var serializer = ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer());
            helper.assertFalse(new ResourceLocation("farmersdelight", "cutting").equals(serializer)
                            || new ResourceLocation("farmersdelight", "cooking").equals(serializer),
                    "A recipe still requires the removed cutting board or cooking pot: " + recipe.getId());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pumpkinSlicesAndBeansWorkWithoutKitchenEquipment(GameTestHelper helper) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        grid.setItem(0, new ItemStack(Items.PUMPKIN));
        var recipes = helper.getLevel().getRecipeManager();
        var seeds = recipes.getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow()
                .assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(seeds.is(Items.PUMPKIN_SEEDS) && seeds.getCount() == 9,
                "One pumpkin must still craft nine seeds without conflicting with the new slice recipe");
        grid.setItem(1, new ItemStack(Items.PUMPKIN));
        var slices = recipes.getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow()
                .assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(slices.is(ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight", "pumpkin_slice")))
                        && slices.getCount() == 8,
                "Two pumpkins must craft eight slices in the player inventory without a knife");
        var beans = ForgeRegistries.ITEMS.getValue(new ResourceLocation("legumedelight", "beans"));
        var cookedBeans = ForgeRegistries.ITEMS.getValue(new ResourceLocation("legumedelight", "refried_beans"));
        helper.assertTrue(beans != null && beans != Items.AIR && cookedBeans != null && cookedBeans != Items.AIR,
                "Retained beans must resolve to registered items");
        var input = new SimpleContainer(new ItemStack(beans));
        for (var type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
            var cooked = recipes.getRecipeFor(type, input, helper.getLevel()).orElseThrow()
                    .assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(cooked.is(cookedBeans) && cooked.getCount() == 1,
                    "Beans must cook directly into refried beans with " + type);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void existingSimpleFoodRecipesStayAvailable(GameTestHelper helper) {
        for (String path : new String[]{"tomato_sauce", "sweet_berry_cookie", "dumplings", "cabbage_rolls",
                "chocolate_pie", "apple_pie"}) {
            var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(AOEMod.MOD_ID, "food/" + path));
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight", path));
            helper.assertTrue(recipe.isPresent() && recipe.get().getResultItem(helper.getLevel().registryAccess()).is(item),
                    "The existing pack recipe for the retained dish must remain available: " + path);
        }
        helper.succeed();
    }
}
