package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.GameplayDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers native randomization, saved fish, buckets/nets, breeding, and client metadata. */
@Mixin(value = BaseWaterAnimal.class, remap = false)
public abstract class TropicalAquaticSize {
    @ModifyVariable(method = "setSize", at = @At("HEAD"), argsOnly = true)
    private int aoemod$storeMaximumIndividualSize(int requested) {
        BaseWaterAnimal animal = (BaseWaterAnimal) (Object) this;
        return AquaticMobSizes.usesMaximumSize(animal)
                ? AquaticMobSizes.maximum(animal) : requested;
    }

    @Inject(method = "getSize", at = @At("HEAD"), cancellable = true)
    private void aoemod$renderMaximumIndividualSize(CallbackInfoReturnable<Integer> cir) {
        BaseWaterAnimal animal = (BaseWaterAnimal) (Object) this;
        if (AquaticMobSizes.usesMaximumSize(animal)) {
            // Reading old synchronized metadata can bypass setSize. Keep both
            // rendering and serialization correct before the next server join.
            cir.setReturnValue(AquaticMobSizes.maximum(animal));
        }
    }
}
