package net.amurdza.examplemod.mixins.othermods.vintagedelight;

import net.amurdza.examplemod.mixins.accessor.PotionBrewingInvoker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use a remapped invoker instead of reflecting on a development-only vanilla name. */
@Pseudo
@Mixin(targets = "net.ribs.vintagedelight.mobEffects.CustomPotionBrewing", remap = false)
public abstract class ProductionPotionBrewing {
    @Inject(method = "addMix", at = @At("HEAD"), cancellable = true, require = 1)
    private static void aoe$registerMappedRecipe(Potion input, Item ingredient, Potion output, CallbackInfo ci) {
        PotionBrewingInvoker.aoe$addMix(input, ingredient, output);
        ci.cancel();
    }
}
