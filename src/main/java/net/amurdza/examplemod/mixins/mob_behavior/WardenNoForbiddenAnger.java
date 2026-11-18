package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.event_handlers.MobAI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class WardenNoForbiddenAnger {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void aoemod$ignoreNeutralMobAngerAndVibrations(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // This predicate also gates vibration reception, sniffing and anger accumulation.
        // Filtering only ATTACK_TARGET happens after the listening sounds and roar begin.
        if (entity instanceof LivingEntity living
                && MobAI.isNotAllowedMobTarget((Warden) (Object) this, living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void aoemod$ignoreNeutralMobRetaliation(LivingEntity target, CallbackInfo ci) {
        // Direct damage can use this path without consulting canTargetEntity first.
        if (target != null && MobAI.isNotAllowedMobTarget((Warden) (Object) this, target)) {
            ci.cancel();
        }
    }
}
