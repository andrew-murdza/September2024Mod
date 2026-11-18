package net.amurdza.examplemod.mixins.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(Ingredient.class)
public abstract class PeanutSeedsAreNotIngredients {
    @Unique
    private static final ResourceLocation aoe$PEANUT_SEEDS = new ResourceLocation("legumedelight", "peanuts");

    @Inject(method = "getItems", at = @At("RETURN"), cancellable = true)
    private void aoe$excludePlantingSeeds(CallbackInfoReturnable<ItemStack[]> cir) {
        ItemStack[] items = cir.getReturnValue();
        if (Arrays.stream(items).anyMatch(stack -> aoe$PEANUT_SEEDS.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())))) {
            // Ingredient.test, shapeless stacking IDs, and recipe displays all use this list.
            cir.setReturnValue(Arrays.stream(items)
                    .filter(stack -> !aoe$PEANUT_SEEDS.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())))
                    .toArray(ItemStack[]::new));
        }
    }
}
