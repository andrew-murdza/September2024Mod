package net.amurdza.examplemod.mixins.othermods.backport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AOE owns biome layout, natural populations and placed features. */
@Pseudo
@Mixin(targets = {
        "com.blackgear.vanillabackport.common.integrations.worldgen.WorldGeneration",
        "com.blackgear.vanillabackport.common.integrations.worldgen.BiomeGeneration"
}, remap = false)
public abstract class BackportWorldgenPolicy {
    @Inject(method = "bootstrap", at = @At("HEAD"), cancellable = true)
    private static void aoe$keepAoeWorldgen(CallbackInfo ci) {
        ci.cancel();
    }
}
