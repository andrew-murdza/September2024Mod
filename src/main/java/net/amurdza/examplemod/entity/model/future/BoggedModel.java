package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.BoggedEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BoggedModel extends SkeletonModel<BoggedEntity> {
    private final ModelPart mushrooms;

    public BoggedModel(ModelPart root) {
        super(root);
        this.mushrooms = root.getChild("head").getChild("mushrooms");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition mushrooms = mesh.getRoot().getChild("head").addOrReplaceChild(
                "mushrooms",
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        mushrooms.addOrReplaceChild(
                "red_1",
                CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) Math.PI / 4.0F, 0.0F)
        );
        mushrooms.addOrReplaceChild(
                "red_2",
                CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) Math.PI * 3.0F / 4.0F, 0.0F)
        );
        mushrooms.addOrReplaceChild(
                "brown_1",
                CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) Math.PI / 4.0F, 0.0F)
        );
        mushrooms.addOrReplaceChild(
                "brown_2",
                CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) Math.PI * 3.0F / 4.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BoggedEntity entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, age, yaw, pitch);
        this.mushrooms.visible = true;
    }
}
