package net.amurdza.examplemod.mixins.othermods.orcz;

import com.duke.orcz.client.renderer.OrcsavagemaceRenderer;
import com.duke.orcz.entity.OrcsavagemaceEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bind our atlas directly so the dependency's resource pack cannot restore iron gear. */
@Mixin(value = OrcsavagemaceRenderer.class, remap = false)
public abstract class BoneMaceTexture {
    private static final ResourceLocation AOEMOD_BONE_TEXTURE =
            new ResourceLocation("aoemod", "textures/entity/orcz/orc_savage.png");

    @Inject(method = "getTextureLocation(Lcom/duke/orcz/entity/OrcsavagemaceEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$boneGear(OrcsavagemaceEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(AOEMOD_BONE_TEXTURE);
    }
}
