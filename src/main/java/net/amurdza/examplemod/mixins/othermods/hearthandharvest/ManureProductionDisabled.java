package net.amurdza.examplemod.mixins.othermods.hearthandharvest;

import alabaster.hearthandharvest.common.entity.ManureDropHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disable animal manure production at Hearth & Harvest's shared source. */
@Mixin(value = ManureDropHelper.class, remap = false)
public abstract class ManureProductionDisabled {
    @Inject(method = "canPoop", at = @At("HEAD"), cancellable = true)
    private static void aoemod$disablePeriodicProduction(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "schedulePoop", at = @At("HEAD"), cancellable = true)
    private static void aoemod$disableFeedingAndGrassTimers(Animal animal, CallbackInfo ci) {
        ci.cancel();
    }

    // Also guard direct calls and already-saved timers. The original helper
    // creates the item AND plays its sound, so both are stopped together.
    @Inject(method = "dropPoop", at = @At("HEAD"), cancellable = true)
    private static void aoemod$disableManureDropAndSound(LivingEntity entity, CallbackInfo ci) {
        ci.cancel();
    }
}
