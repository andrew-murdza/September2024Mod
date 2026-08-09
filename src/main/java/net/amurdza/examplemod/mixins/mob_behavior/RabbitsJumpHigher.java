package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.animal.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Rabbit.class)
public class RabbitsJumpHigher {
    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void aoemod$increaseJumpHeight(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(), 0.6F));
    }
}
