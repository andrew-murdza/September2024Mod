package net.amurdza.examplemod.gametest;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaFeatherTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void arrowRecipesAcceptEmuAndChickenFeathers(GameTestHelper helper) {
        var level = helper.getLevel();
        var menu = new AbstractContainerMenu(null, 0) {
            @Override
            public boolean stillValid(Player player) { return true; }
            @Override
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        var boneArrow = ForgeRegistries.ITEMS.getValue(new ResourceLocation("aoemod", "bone_arrow"));
        grid.setItem(4, new ItemStack(Items.STICK));
        for (Item feather : new Item[]{Items.FEATHER, AMItemRegistry.EMU_FEATHER.get()}) {
            grid.setItem(7, new ItemStack(feather));
            for (Item tip : new Item[]{Items.FLINT, Items.BONE}) {
                grid.setItem(1, new ItemStack(tip));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
                var result = recipe.map(r -> r.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
                helper.assertTrue(result.is(tip == Items.FLINT ? Items.ARROW : boneArrow) && result.getCount() == 4,
                        "Expected four arrows from " + tip + " and " + feather + ", got " + result);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void adultParrotShedsAFeatherWhenItsSavedTimerExpires(GameTestHelper helper) {
        var parrot = EntityType.PARROT.create(helper.getLevel());
        var data = new CompoundTag();
        parrot.addAdditionalSaveData(data);
        data.putInt("AOEModFeatherDropTime", 0);
        parrot.readAdditionalSaveData(data);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        parrot.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        parrot.aiStep();
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, parrot.getBoundingBox().inflate(1),
                item -> item.getItem().is(Items.FEATHER));
        parrot.addAdditionalSaveData(data);
        helper.assertTrue(drops.size() == 1 && data.getInt("AOEModFeatherDropTime") >= 6000,
                "An adult parrot must drop a feather and restart its shedding timer");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }
}
