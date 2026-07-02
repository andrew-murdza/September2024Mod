package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Strider.class)
public class StridersEatAshenWheat {
    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void aoemod$eatAshenWheat(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(stack.is(ModItems.ASHEN_WHEAT.get()));
    }
}
