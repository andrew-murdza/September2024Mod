package net.amurdza.examplemod.entity.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.item.MaterialShieldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MaterialShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private final ShieldModel model;

    public MaterialShieldRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
        this.model = new ShieldModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD)
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof MaterialShieldItem shield)) {
            return;
        }

        poseStack.pushPose();
        float modelScale = shield.modelScale();
        poseStack.scale(modelScale, -modelScale, -modelScale);
        VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(
                buffer,
                RenderType.entitySolid(shield.texture()),
                true,
                stack.hasFoil()
        );
        this.model.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
        poseStack.popPose();
    }
}
