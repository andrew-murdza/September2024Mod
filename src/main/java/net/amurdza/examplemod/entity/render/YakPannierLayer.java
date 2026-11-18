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

/** Vanilla donkey panniers, anchored to the yak's wider shaggy torso. */
public final class YakPannierLayer extends RenderLayer<YakEntity, CowModel<YakEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/horse/donkey.png");
    private final ModelPart leftChest;
    private final ModelPart rightChest;

    public YakPannierLayer(RenderLayerParent<YakEntity, CowModel<YakEntity>> parent, EntityRendererProvider.Context context) {
        super(parent);
        var body = context.bakeLayer(ModelLayers.DONKEY).getChild("body");
        leftChest = body.getChild("left_chest");
        rightChest = body.getChild("right_chest");
        // Retain both native 8x8x3 boxes, their rotations and their 64x64 UVs.
        // The yak's fur ends at +/-6.5 pixels; each chest meets it there.
        // Native horse body translation is folded into these standalone parts.
        leftChest.x = 7.5F;
        rightChest.x = -7.5F;
        leftChest.y = rightChest.y = 3;
        leftChest.z = rightChest.z = 5;
    }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, YakEntity yak,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (!yak.hasChest() || yak.isBaby() || yak.isInvisible()) return;
        var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        leftChest.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
        rightChest.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
    }
}
