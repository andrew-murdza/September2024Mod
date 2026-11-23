package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.citadel.animation.Animation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep elephants on all four feet during idle behavior and combat. */
@Mixin(value = EntityElephant.class, remap = false)
public abstract class ElephantsStayGrounded {
    @ModifyVariable(method = "setStanding", at = @At("HEAD"), argsOnly = true)
    private boolean aoemod$neverRear(boolean standing) {
        return false;
    }

    @Inject(method = "isStanding", at = @At("HEAD"), cancellable = true)
    private void aoemod$ignoreOldStandingState(CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(false);
    }

    @ModifyVariable(method = "setAnimation", at = @At("HEAD"), argsOnly = true)
    private Animation aoemod$useGroundedAttack(Animation animation) {
        // Fling retains the native melee damage/knockback path without the stomp's rear-up pose.
        return animation == EntityElephant.ANIMATION_STOMP ? EntityElephant.ANIMATION_FLING : animation;
    }
}
