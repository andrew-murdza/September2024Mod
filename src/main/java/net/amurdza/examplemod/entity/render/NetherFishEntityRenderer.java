package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.NetherFishEntity;
import net.amurdza.examplemod.entity.model.EndFishEntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NetherFishEntityRenderer extends MobRenderer<NetherFishEntity, EndFishEntityModel<NetherFishEntity>> {
    private static final ResourceLocation[] TEXTURE = new ResourceLocation[NetherFishEntity.VARIANTS];
    private static final RenderType[] GLOW = new RenderType[NetherFishEntity.VARIANTS];

    public NetherFishEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EndFishEntityModel<>(ctx.bakeLayer(EntityRenderers.END_FISH_MODEL)), 0.5f);

        this.addLayer(new EyesLayer<>(this) {
            @Override
            public @NotNull RenderType renderType() {
                return GLOW[0];
            }

            @Override
            public void render(
                    @NotNull PoseStack matrices,
                    @NotNull MultiBufferSource vertexConsumers,
                    int light,
                    @NotNull NetherFishEntity entity,
                    float limbAngle,
                    float limbDistance,
                    float tickDelta,
                    float animationProgress,
                    float headYaw,
                    float headPitch
            ) {
                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(GLOW[entity.getNetherVariantIndex()]);

                this.getParentModel().renderToBuffer(
                        matrices,
                        vertexConsumer,
                        15728640,
                        OverlayTexture.NO_OVERLAY,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                );
            }
        });
    }

    @Override
    protected void scale(NetherFishEntity entity, PoseStack matrixStack, float f) {
        float scale = entity.getScale();
        matrixStack.scale(scale, scale, scale);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(NetherFishEntity entity) {
        return TEXTURE[entity.getNetherVariantIndex()];
    }

    static {
        for (int i = 0; i < NetherFishEntity.VARIANTS; i++) {
            int textureVariant = NetherFishEntity.MIN_TEXTURE_VARIANT + i;
            TEXTURE[i] = AOEMod.makeID("textures/entity/endfish/end_fish_" + textureVariant + ".png");
            GLOW[i] = RenderType.eyes(
                    AOEMod.makeID("textures/entity/endfish/end_fish_" + textureVariant + "_glow.png")
            );
        }
    }
}
