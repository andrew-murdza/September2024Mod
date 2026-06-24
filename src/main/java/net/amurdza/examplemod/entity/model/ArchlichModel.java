package net.amurdza.examplemod.entity.model;

import com.github.alexthe666.iceandfire.client.model.ModelDreadLich;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.entity.ArchlichEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Temporary adapter around Ice and Fire's Dread Lich model.
 */
public class ArchlichModel extends EntityModel<ArchlichEntity> implements ArmedModel {
    private final ModelDreadLich dreadLichModel = new ModelDreadLich(0.0F);

    @Override
    public void prepareMobModel(ArchlichEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.copyRenderState();
        this.dreadLichModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(
            ArchlichEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        this.copyRenderState();
        this.dreadLichModel.setupAnim(
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch
        );
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        this.dreadLichModel.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.dreadLichModel.translateToHand(arm, poseStack);
    }

    private void copyRenderState() {
        this.dreadLichModel.attackTime = this.attackTime;
        this.dreadLichModel.riding = this.riding;
        this.dreadLichModel.young = this.young;
    }
}
