
package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BowlFoodRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void localBowlRecipesMatchTheirReferenceFoods(GameTestHelper helper) {
        check(helper, "twilightforest:mushgloom", 2, ModItems.MUSHGLOOM_STEW.get(), ModItems.GLOWSHROOM_STEW.get());
        check(helper, "mynethersdelight:bullet_pepper", 2, ModItems.BULLET_PEPPER_SOUP.get(), ModItems.GLOWSHROOM_STEW.get());
        check(helper, "minecraft:carrot", 3, ModItems.CARROT_SOUP.get(), Items.BEETROOT_SOUP);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, String ingredient, int count, Item result, Item reference) {
        var level = helper.getLevel();
        var menu = new AbstractContainerMenu(null, 0) {
            public boolean stillValid(Player player) { return true; }
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        };
        CraftingContainer grid = new TransientCraftingContainer(menu, 3, 3);
        var food = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ingredient));
        for (int i = 0; i < count; i++) grid.setItem(i, new ItemStack(food));
        grid.setItem(8, new ItemStack(Items.BOWL));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(recipe.isPresent() && recipe.get().assemble(grid, level.registryAccess()).is(result),
                "The ordinary crafting grid must produce " + result);
        var props = result.getFoodProperties(new ItemStack(result), null);
        var expected = reference.getFoodProperties(new ItemStack(reference), null);
        helper.assertTrue(props != null && expected != null && props.getNutrition() == expected.getNutrition()
                && props.getSaturationModifier() == expected.getSaturationModifier(), "Bowl food properties differ from " + reference);
    }
}
