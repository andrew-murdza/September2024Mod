package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.ai.DolphinBreedingAccess;
import net.amurdza.examplemod.entity.model.BabyDolphinModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.DolphinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.phys.Vec3;

public final class BabyAwareDolphinRenderer extends DolphinRenderer {
    private static final float ADULT_SHADOW_RADIUS = 0.7F;
    private static final float BABY_SHADOW_RADIUS = 0.5F;

    private final BabyDolphinGeoRenderer babyRenderer;

    public BabyAwareDolphinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.babyRenderer = new BabyDolphinGeoRenderer(context);
        this.shadowRadius = ADULT_SHADOW_RADIUS;
    }

    @Override
    public void render(
            Dolphin dolphin,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (isBaby(dolphin)) {
            this.shadowRadius = BABY_SHADOW_RADIUS;
            this.babyRenderer.render(dolphin, entityYaw, partialTick, poseStack, buffer, packedLight);
        } else {
            this.shadowRadius = ADULT_SHADOW_RADIUS;
            super.render(dolphin, entityYaw, partialTick, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Dolphin dolphin) {
        return isBaby(dolphin) ? BabyDolphinModel.TEXTURE : super.getTextureLocation(dolphin);
    }

    @Override
    public Vec3 getRenderOffset(Dolphin dolphin, float partialTick) {
        return isBaby(dolphin)
                ? this.babyRenderer.getRenderOffset(dolphin, partialTick)
                : super.getRenderOffset(dolphin, partialTick);
    }

    @Override
    public boolean shouldRender(Dolphin dolphin, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return isBaby(dolphin)
                ? this.babyRenderer.shouldRender(dolphin, frustum, cameraX, cameraY, cameraZ)
                : super.shouldRender(dolphin, frustum, cameraX, cameraY, cameraZ);
    }

    private static boolean isBaby(Dolphin dolphin) {
        return dolphin instanceof DolphinBreedingAccess breeding && breeding.aoe$isBaby();
    }
}
