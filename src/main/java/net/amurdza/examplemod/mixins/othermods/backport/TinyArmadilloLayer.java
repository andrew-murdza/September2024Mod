package net.amurdza.examplemod.mixins.othermods.backport;

import com.evandev.tiny_takeover_backport.compat.VanillaBackportClientCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Register the required model even when the dependency config is not ready during layer setup. */
@Pseudo
@Mixin(targets = "com.evandev.tiny_takeover_backport.client.ModClientRegistry", remap = false)
public abstract class TinyArmadilloLayer {
    @Inject(method = "init", at = @At("TAIL"))
    private static void aoe$registerRequiredArmadilloLayer(CallbackInfo ci) {
        VanillaBackportClientCompat.registerArmadilloLayer();
    }
}
