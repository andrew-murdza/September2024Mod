package net.amurdza.examplemod.mixins.despawning;

import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamdraco.finsandstails.common.entities.RubberBellyGliderEntity;

/** The source mod extends Animal, whose permanent-resident policy bypasses fish despawning. */
@Mixin(Animal.class)
public abstract class RubberBellyGliderDespawn {
    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void aoemod$ambientGlider(double distance, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof RubberBellyGliderEntity glider) {
            // Match native fish: names protect kept animals even without an explicit persistence flag.
            cir.setReturnValue(!glider.hasCustomName());
        }
    }
}
