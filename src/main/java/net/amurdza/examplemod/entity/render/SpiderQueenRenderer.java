package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.amurdza.examplemod.entity.model.SpiderQueenModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SpiderQueenRenderer extends GeoEntityRenderer<SpiderQueenEntity> {
    private static final double MODEL_LIFT = 0.875D;

    public SpiderQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderQueenModel());
        this.shadowRadius = 0.9F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(
            SpiderQueenEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -MODEL_LIFT, 0.0D);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
