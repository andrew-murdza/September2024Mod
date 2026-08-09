package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.model.BabyParrotModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.phys.Vec3;

public final class BabyAwareParrotRenderer extends EntityRenderer<Parrot> {
    private final ParrotRenderer adultRenderer;
    private final BabyParrotGeoRenderer babyRenderer;

    public BabyAwareParrotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.adultRenderer = new ParrotRenderer(context);
        this.babyRenderer = new BabyParrotGeoRenderer(context);
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(
            Parrot parrot,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        rendererFor(parrot).render(parrot, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Parrot parrot) {
        return parrot.isBaby()
                ? BabyParrotModel.textureForVariant(parrot.getVariant())
                : this.adultRenderer.getTextureLocation(parrot);
    }

    @Override
    public Vec3 getRenderOffset(Parrot parrot, float partialTick) {
        return rendererFor(parrot).getRenderOffset(parrot, partialTick);
    }

    @Override
    public boolean shouldRender(Parrot parrot, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return rendererFor(parrot).shouldRender(parrot, frustum, cameraX, cameraY, cameraZ);
    }

    private EntityRenderer<Parrot> rendererFor(Parrot parrot) {
        return parrot.isBaby() ? this.babyRenderer : this.adultRenderer;
    }
}
