package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.CreakingEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class CreakingModel extends HierarchicalModel<CreakingEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CreakingModel(ModelPart modelRoot) {
        this.root = modelRoot.getChild("root");
        ModelPart upperBody = this.root.getChild("upper_body");
        this.head = upperBody.getChild("head");
        this.rightArm = upperBody.getChild("right_arm");
        this.leftArm = upperBody.getChild("left_arm");
        this.rightLeg = this.root.getChild("right_leg");
        this.leftLeg = this.root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition upper = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
        upper.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
                        .texOffs(28, 31).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F)
                        .texOffs(12, 40).addBox(3.0F, -13.0F, 0.0F, 9.0F, 14.0F, 0.0F)
                        .texOffs(34, 12).addBox(-12.0F, -14.0F, 0.0F, 9.0F, 14.0F, 0.0F),
                PartPose.offset(-3.0F, -11.0F, 0.0F)
        );
        upper.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F)
                        .texOffs(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, -7.0F, 1.0F)
        );
        upper.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F)
                        .texOffs(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-7.0F, -9.5F, 1.5F)
        );
        upper.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(30, 40).addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
                        .texOffs(52, 12).addBox(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                        .texOffs(52, 19).addBox(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(6.0F, -9.0F, 0.5F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F)
                        .texOffs(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F),
                PartPose.offset(1.5F, -16.0F, 0.5F)
        );
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
                        .texOffs(45, 46).addBox(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
                        .texOffs(12, 34).addBox(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-1.0F, -17.5F, 0.5F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CreakingEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.yRot = yaw * ((float) Math.PI / 180.0F);
        this.head.xRot = pitch * ((float) Math.PI / 180.0F);
        float walk = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        this.rightLeg.xRot = walk;
        this.leftLeg.xRot = -walk;
        this.rightArm.xRot = -walk * 0.65F;
        this.leftArm.xRot = walk * 0.65F;
        if (this.attackTime > 0.0F) {
            float attack = Mth.sin(this.attackTime * (float) Math.PI);
            this.rightArm.xRot -= attack * 1.6F;
            this.leftArm.xRot -= attack * 1.2F;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
