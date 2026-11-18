package net.amurdza.examplemod.mixins.othermods.orcz;

import com.duke.orcz.client.renderer.BigorcbossRenderer;
import com.duke.orcz.entity.BigorcbossEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep both native sword meshes and their exact UVs, with only their metal recolored as bone. */
@Mixin(value = BigorcbossRenderer.class, remap = false)
public abstract class BoneBossSwordsTexture {
    private static final ResourceLocation AOEMOD_BONE_TEXTURE =
            new ResourceLocation("aoemod", "textures/entity/orcz/orc_savage_big_2sword.png");

    @Inject(method = "getTextureLocation(Lcom/duke/orcz/entity/BigorcbossEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$boneSwords(BigorcbossEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(AOEMOD_BONE_TEXTURE);
    }
}
