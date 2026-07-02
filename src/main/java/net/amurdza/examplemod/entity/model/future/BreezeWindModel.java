package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.BreezeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BreezeWindModel extends HierarchicalModel<BreezeEntity> {
    private final ModelPart root;
    private final ModelPart bottom;
    private final ModelPart middle;
    private final ModelPart top;

    public BreezeWindModel(ModelPart root) {
        this.root = root;
        this.bottom = root.getChild("wind_bottom");
        this.middle = this.bottom.getChild("wind_mid");
        this.top = this.middle.getChild("wind_top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bottom = mesh.getRoot().addOrReplaceChild(
                "wind_bottom",
                CubeListBuilder.create().texOffs(1, 83).addBox(-2.5F, -7.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        PartDefinition middle = bottom.addOrReplaceChild(
                "wind_mid",
                CubeListBuilder.create().texOffs(74, 28).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 6.0F, 12.0F)
                        .texOffs(78, 32).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F)
                        .texOffs(49, 71).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F)
        );
        middle.addOrReplaceChild(
                "wind_top",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -8.0F, -9.0F, 18.0F, 8.0F, 18.0F)
                        .texOffs(6, 6).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F)
                        .texOffs(105, 57).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(BreezeEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.bottom.yRot = age;
        this.middle.yRot = -age * 0.8F;
        this.top.yRot = age * 0.6F;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
