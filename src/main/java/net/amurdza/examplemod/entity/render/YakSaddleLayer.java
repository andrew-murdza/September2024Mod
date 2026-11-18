package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.amurdza.examplemod.entity.YakEntity;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** The native donkey saddle, fitted over the yak's wider shaggy back. */
public final class YakSaddleLayer extends RenderLayer<YakEntity, CowModel<YakEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/horse/donkey.png");
    private final ModelPart saddle;

    public YakSaddleLayer(RenderLayerParent<YakEntity, CowModel<YakEntity>> parent, EntityRendererProvider.Context context) {
        super(parent);
        // Keep Minecraft's saddle mesh, UVs and leather straps intact. Only the
        // width and anchor differ: the cold cow's fur reaches x=±6.5 and y=1.5.
        saddle = context.bakeLayer(ModelLayers.DONKEY).getChild("body").getChild("saddle");
        saddle.xScale = 1.25F;
        saddle.y = 9;
        saddle.z = 4.5F;
    }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, YakEntity yak,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (!yak.isSaddled() || yak.isBaby() || yak.isInvisible()) return;
        saddle.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                light, OverlayTexture.NO_OVERLAY);
    }
}
