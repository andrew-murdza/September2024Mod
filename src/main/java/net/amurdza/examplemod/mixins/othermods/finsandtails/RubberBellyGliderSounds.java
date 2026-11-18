package net.amurdza.examplemod.mixins.othermods.finsandtails;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamdraco.finsandstails.common.entities.RubberBellyGliderEntity;

/** Suppress glider calls and puffing sounds, including gliders loaded from existing worlds. */
@Mixin(Entity.class)
public abstract class RubberBellyGliderSounds {
    @Inject(method = "isSilent", at = @At("HEAD"), cancellable = true)
    private void aoemod$silentGlider(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof RubberBellyGliderEntity) cir.setReturnValue(true);
    }
}
