package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.util.AnimalFoodOverrides;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Rabbit.class)
public abstract class RabbitLegumeFood {
    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void aoemod$legumes(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (stack.is(AnimalFoodOverrides.RABBIT_LEGUMES)) result.setReturnValue(true);
    }
}
