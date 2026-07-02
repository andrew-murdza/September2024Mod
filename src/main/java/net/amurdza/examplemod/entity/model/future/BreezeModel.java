package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.BreezeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class BreezeModel extends HierarchicalModel<BreezeEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rods;

    public BreezeModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.rods = body.getChild("rods");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rods = body.addOrReplaceChild("rods", CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
        rods.addOrReplaceChild(
                "rod_1",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(2.5981F, -3.0F, 1.5F, -2.7489F, -1.0472F, 3.1416F)
        );
        rods.addOrReplaceChild(
                "rod_2",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-2.5981F, -3.0F, 1.5F, -2.7489F, 1.0472F, 3.1416F)
        );
        rods.addOrReplaceChild(
                "rod_3",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, 0.3927F, 0.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(4, 24).addBox(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F)
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(BreezeEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.head.yRot = yaw * ((float) Math.PI / 180.0F);
        this.head.xRot = pitch * ((float) Math.PI / 180.0F);
        this.rods.yRot = age * 0.12F;
        this.rods.xRot = Mth.sin(age * 0.1F) * 0.08F;
        this.head.y = 4.0F + Mth.sin(age * 0.16F) * 0.7F;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
