package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ravager.class)
public abstract class RavagerSimpleCommon {
    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoemod$dontSpecialBreakShields(LivingEntity defender, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoemod$neverStunLikeEliteRavager(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
