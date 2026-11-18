package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.entity.cephalopod.ArrowSquidEntity;
import dev.hybridlabs.hapi.client.model.entity.aquatic.BaseCephalopodEntityModel;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseCephalopodEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the revised model and its eye UVs paired without depending on pack order. */
@Mixin(value = BaseCephalopodEntityModel.class, remap = false)
public abstract class ArrowSquidModel {
    @Inject(method = "getModelResource(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCephalopodEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$arrowSquidModel(BaseCephalopodEntity squid, CallbackInfoReturnable<ResourceLocation> cir) {
        if (squid instanceof ArrowSquidEntity) {
            cir.setReturnValue(new ResourceLocation("aoemod", "geo/cephalopod/arrow_squid/arrow_squid.geo.json"));
        }
    }

    @Inject(method = "getTextureResource(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCephalopodEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$arrowSquidTexture(BaseCephalopodEntity squid, CallbackInfoReturnable<ResourceLocation> cir) {
        if (squid instanceof ArrowSquidEntity) {
            cir.setReturnValue(new ResourceLocation("aoemod", "textures/entity/cephalopod/arrow_squid/arrow_squid.png"));
        }
    }
}
