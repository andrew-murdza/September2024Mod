package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.client.model.entity.crustacean.FiddlerCrabEntityModel;
import dev.hybridlabs.aquatic.entity.crustacean.FiddlerCrabEntity;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the native UUID variant choice and UVs; resolve the repaired atlas unambiguously. */
@Mixin(value = FiddlerCrabEntityModel.class, remap = false)
public abstract class FiddlerCrabTextures {
    @Inject(method = "getTextureResource(Ldev/hybridlabs/aquatic/entity/crustacean/FiddlerCrabEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"), cancellable = true)
    private void aoemod$repairedAtlas(FiddlerCrabEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        var nativeTexture = cir.getReturnValue();
        if (nativeTexture.getNamespace().equals("hybrid_aquatic")
                && nativeTexture.getPath().startsWith("textures/entity/crustacean/fiddler_crab/")) {
            cir.setReturnValue(new ResourceLocation(AOEMod.MOD_ID, nativeTexture.getPath()));
        }
    }
}
