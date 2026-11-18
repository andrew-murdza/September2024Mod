package net.amurdza.examplemod.mixins.othermods.quark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the modern cow models paired with their matching Backport textures. */
@Mixin(targets = "org.violetmoon.quark.content.client.module.VariantAnimalTexturesModule$Client", remap = false)
public abstract class CowVariantTextureCompatibility {
    // Quark's random cow skins use the old 64x32 UV layout. Both its renderer
    // mixin and Backport's compatibility check consult this method; returning
    // null lets Backport select the correct temperate, cold or warm texture.
    @Inject(method = "getCowTexture", at = @At("HEAD"), cancellable = true)
    private static void aoemod$useMatchingCowTexture(Cow cow, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(null);
    }
}
