package net.amurdza.examplemod.mixins.mob_behavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleGoHomeGoal")
public abstract class TurtleDoesNotReturnHome {
    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void aoemod$stayNearCurrentHabitat(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
