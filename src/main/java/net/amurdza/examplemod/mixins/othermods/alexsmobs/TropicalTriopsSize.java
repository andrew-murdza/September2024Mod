package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityTriops;
import net.amurdza.examplemod.util.GameplayDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityTriops.class, remap = false)
public abstract class TropicalTriopsSize {
    @ModifyVariable(method = "setTriopsScale", at = @At("HEAD"), argsOnly = true)
    private float aoemod$maximumStoredScale(float value) {
        return GameplayDimensions.is(((EntityTriops) (Object) this).level(), GameplayDimensions.TROPICAL) ? 1.1F : value;
    }

    @Inject(method = "getTriopsScale", at = @At("HEAD"), cancellable = true)
    private void aoemod$maximumRenderedScale(CallbackInfoReturnable<Float> cir) {
        if (GameplayDimensions.is(((EntityTriops) (Object) this).level(), GameplayDimensions.TROPICAL)) cir.setReturnValue(1.1F);
    }
}
