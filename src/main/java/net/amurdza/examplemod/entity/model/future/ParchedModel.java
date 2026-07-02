package net.amurdza.examplemod.entity.model.future;

import net.amurdza.examplemod.entity.future.ParchedEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class ParchedModel extends SkeletonModel<ParchedEntity> {
    public ParchedModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(0, 32).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                        .texOffs(28, 0).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F)
                        .texOffs(16, 48).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.025F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                        .texOffs(42, 33).addBox(-1.55F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                        .texOffs(40, 48).addBox(-1.45F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                        .texOffs(0, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                        .texOffs(4, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }
}
