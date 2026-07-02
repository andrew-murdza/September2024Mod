package net.amurdza.examplemod.entity.render.future;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.BreezeEntity;
import net.amurdza.examplemod.entity.model.future.BreezeModel;
import net.amurdza.examplemod.entity.model.future.BreezeWindModel;
import net.amurdza.examplemod.entity.render.EntityRenderers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BreezeRenderer extends MobRenderer<BreezeEntity, BreezeModel> {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/breeze.png");

    public BreezeRenderer(EntityRendererProvider.Context context) {
        super(context, new BreezeModel(context.bakeLayer(EntityRenderers.BREEZE_MODEL)), 0.5F);
        this.addLayer(new WindLayer(this, context));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BreezeEntity entity) {
        return TEXTURE;
    }

    private static class WindLayer extends RenderLayer<BreezeEntity, BreezeModel> {
        private static final ResourceLocation WIND = AOEMod.makeID("textures/entity/breeze_wind.png");
        private final BreezeWindModel model;

        private WindLayer(BreezeRenderer parent, EntityRendererProvider.Context context) {
            super(parent);
            this.model = new BreezeWindModel(context.bakeLayer(EntityRenderers.BREEZE_WIND_MODEL));
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffers,
                int light,
                BreezeEntity entity,
                float limbSwing,
                float limbAmount,
                float partialTick,
                float age,
                float yaw,
                float pitch
        ) {
            this.model.setupAnim(entity, limbSwing, limbAmount, age, yaw, pitch);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(WIND));
            this.model.renderToBuffer(
                    poseStack,
                    consumer,
                    light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                    1.0F,
                    1.0F,
                    1.0F,
                    0.8F
            );
        }
    }
}
