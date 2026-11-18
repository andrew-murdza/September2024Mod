package net.amurdza.examplemod.mixins.othermods.orcz;

import com.duke.orcz.client.renderer.OrcshieldRenderer;
import com.duke.orcz.entity.OrcshieldEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the shield and tool on the exact original UV layout with the bone palette. */
@Mixin(value = OrcshieldRenderer.class, remap = false)
public abstract class BoneShieldTexture {
    private static final ResourceLocation AOEMOD_BONE_TEXTURE =
            new ResourceLocation("aoemod", "textures/entity/orcz/orc_savage_shield.png");

    @Inject(method = "getTextureLocation(Lcom/duke/orcz/entity/OrcshieldEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$boneGear(OrcshieldEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(AOEMOD_BONE_TEXTURE);
    }
}
