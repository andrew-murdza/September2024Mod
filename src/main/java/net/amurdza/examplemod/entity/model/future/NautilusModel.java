package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.NautilusEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class NautilusModel extends HierarchicalModel<NautilusEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart upperMouth;
    private final ModelPart lowerMouth;

    public NautilusModel(ModelPart modelRoot) {
        this.root = modelRoot.getChild("root");
        this.body = this.root.getChild("body");
        this.upperMouth = this.body.getChild("upper_mouth");
        this.lowerMouth = this.body.getChild("lower_mouth");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 29.0F, -6.0F));
        root.addOrReplaceChild(
                "shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F)
                        .texOffs(0, 26).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F)
                        .texOffs(48, 26).addBox(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F),
                PartPose.offset(0.0F, -13.0F, 5.0F)
        );
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 54).addBox(-5.0F, -4.51F, -3.0F, 10.0F, 8.0F, 14.0F)
                        .texOffs(0, 76).addBox(-5.0F, -4.51F, 7.0F, 10.0F, 8.0F, 0.0F),
                PartPose.offset(0.0F, -8.5F, 12.3F)
        );
        body.addOrReplaceChild(
                "upper_mouth",
                CubeListBuilder.create().texOffs(54, 54).addBox(-5.0F, -2.0F, 0.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)),
                PartPose.offset(0.0F, -2.51F, 7.0F)
        );
        body.addOrReplaceChild(
                "inner_mouth",
                CubeListBuilder.create().texOffs(54, 70).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -0.51F, 7.5F)
        );
        body.addOrReplaceChild(
                "lower_mouth",
                CubeListBuilder.create().texOffs(54, 62).addBox(-5.0F, -1.98F, 0.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)),
                PartPose.offset(0.0F, 1.49F, 7.0F)
        );
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(NautilusEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.body.yRot = Mth.clamp(yaw, -10.0F, 10.0F) * ((float) Math.PI / 180.0F);
        this.body.xRot = Mth.clamp(pitch, -10.0F, 10.0F) * ((float) Math.PI / 180.0F);
        float mouth = 0.12F + Mth.sin(age * 0.25F) * 0.08F;
        this.upperMouth.xRot = -mouth;
        this.lowerMouth.xRot = mouth;
        this.root.yRot = Mth.sin(age * 0.08F) * 0.08F;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
