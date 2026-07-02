package net.amurdza.examplemod.entity.render.future;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.BoggedEntity;
import net.amurdza.examplemod.entity.model.future.BoggedModel;
import net.amurdza.examplemod.entity.render.EntityRenderers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BoggedRenderer extends MobRenderer<BoggedEntity, BoggedModel> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/bogged.png");
    private static final ResourceLocation OVERLAY = AOEMod.makeID("textures/entity/bogged_overlay.png");

    public BoggedRenderer(EntityRendererProvider.Context context) {
        super(context, new BoggedModel(context.bakeLayer(EntityRenderers.BOGGED_MODEL)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new MossLayer(this, context));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BoggedEntity entity) {
        return TEXTURE;
    }

    private static class MossLayer extends RenderLayer<BoggedEntity, BoggedModel> {
        private final BoggedModel model;

        private MossLayer(BoggedRenderer parent, EntityRendererProvider.Context context) {
            super(parent);
            this.model = new BoggedModel(context.bakeLayer(EntityRenderers.BOGGED_OVERLAY_MODEL));
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffers,
                int light,
                BoggedEntity entity,
                float limbSwing,
                float limbAmount,
                float partialTick,
                float age,
                float yaw,
                float pitch
        ) {
            this.getParentModel().copyPropertiesTo(this.model);
            this.model.setupAnim(entity, limbSwing, limbAmount, age, yaw, pitch);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(OVERLAY));
            this.model.renderToBuffer(
                    poseStack,
                    consumer,
                    light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }
}
