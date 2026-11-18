package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.entity.animal.jellyfish.ColoredSizableJellyfish;
import net.amurdza.examplemod.util.GameplayDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ColoredSizableJellyfish.class, remap = false)
public abstract class TropicalJellyfishSize {
    @ModifyVariable(method = "setSize", at = @At("HEAD"), argsOnly = true)
    private float aoemod$maximumStoredSize(float value) {
        var jellyfish = (ColoredSizableJellyfish) (Object) this;
        return GameplayDimensions.is(jellyfish.level(), GameplayDimensions.TROPICAL)
                ? jellyfish.getNaturalSizeMap().lastKey() : value;
    }

    @Inject(method = "getSize", at = @At("HEAD"), cancellable = true)
    private void aoemod$maximumRenderedSize(CallbackInfoReturnable<Float> cir) {
        var jellyfish = (ColoredSizableJellyfish) (Object) this;
        if (GameplayDimensions.is(jellyfish.level(), GameplayDimensions.TROPICAL)) cir.setReturnValue(jellyfish.getNaturalSizeMap().lastKey());
    }
}
