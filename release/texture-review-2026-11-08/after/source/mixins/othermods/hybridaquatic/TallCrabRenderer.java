package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hybridlabs.hapi.client.render.entity.aquatic.BaseCrustaceanEntityRenderer;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseCrustaceanEntity;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseCrustaceanEntityRenderer.class, remap = false)
public abstract class TallCrabRenderer {
    @Inject(method = "render(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCrustaceanEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void aoemod$scaleCrab(BaseCrustaceanEntity crab, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!AquaticMobSizes.isTallCrab(crab)) return;
        pose.pushPose();
        float scale = AquaticMobSizes.tallCrabScale(crab);
        pose.scale(scale, scale, scale);
        // Cancel GeckoLib's .01-block model lift after the native 1.25 size
        // adjustment so the measured shell height is its actual world height.
        pose.translate(0, -0.0125D, 0);
        ((CrabRendererShadow) this).aoemod$setCrabShadowRadius(AquaticMobSizes.tallCrabShadowRadius(crab));
    }

    @Inject(method = "render(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCrustaceanEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void aoemod$restoreCrabPose(BaseCrustaceanEntity crab, float yaw, float partialTick, PoseStack pose,
                                      MultiBufferSource buffers, int light, CallbackInfo ci) {
        // The native renderer scales before GeckoLib's own push/pop. Restore
        // both scales before EntityRenderDispatcher draws the world-space shadow.
        if (AquaticMobSizes.isTallCrab(crab)) pose.popPose();
    }
}
