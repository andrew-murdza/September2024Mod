package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Strider.class)
public abstract class StriderDiet {
    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void aoemod$warpedFungusDiet(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(stack.is(Items.WARPED_FUNGUS));
    }
}
