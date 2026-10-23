package net.amurdza.examplemod.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.entity.SwordfishEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Original cuboid geometry, colored at render time using the game's white concrete texture. */
public final class SwordfishModel extends EntityModel<SwordfishEntity> {
    private final ModelPart body;
    private final ModelPart tail;

    public SwordfishModel(ModelPart root) {
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 18, 0));
        body.addOrReplaceChild("back", CubeListBuilder.create().addBox(-2.5F, -3.5F, -10, 5, 4, 20), PartPose.ZERO);
        body.addOrReplaceChild("belly", CubeListBuilder.create().addBox(-2.4F, 0.5F, -10, 4.8F, 3, 20), PartPose.ZERO);
        body.addOrReplaceChild("head", CubeListBuilder.create().addBox(-1.75F, -2, -16, 3.5F, 4, 7), PartPose.ZERO);
        body.addOrReplaceChild("bill", CubeListBuilder.create().addBox(-0.5F, -0.7F, -33, 1, 1.2F, 18), PartPose.ZERO);
        body.addOrReplaceChild("eyes", CubeListBuilder.create()
                .addBox(-1.9F, -1, -14, 0.3F, 1, 1)
                .addBox(1.6F, -1, -14, 0.3F, 1, 1), PartPose.ZERO);
        body.addOrReplaceChild("dorsal", CubeListBuilder.create().addBox(-0.3F, -9, -1, 0.6F, 9, 5),
                PartPose.offsetAndRotation(0, -3, -5, -0.42F, 0, 0));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().addBox(0, 0, -1, 9, 0.5F, 3),
                PartPose.offsetAndRotation(2, 1, -6, 0, -0.45F, 0.2F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().addBox(-9, 0, -1, 9, 0.5F, 3),
                PartPose.offsetAndRotation(-2, 1, -6, 0, 0.45F, -0.2F));
        body.addOrReplaceChild("anal_fin", CubeListBuilder.create().addBox(-0.2F, 0, 0, 0.4F, 5, 4),
                PartPose.offsetAndRotation(0, 3, 4, 0.35F, 0, 0));
        var tail = body.addOrReplaceChild("tail", CubeListBuilder.create().addBox(-1, -1.5F, 0, 2, 3, 9),
                PartPose.offset(0, 0, 9));
        tail.addOrReplaceChild("upper_lobe", CubeListBuilder.create().addBox(-0.3F, -9, -0.5F, 0.6F, 9, 3),
                PartPose.offsetAndRotation(0, 0, 8, -0.55F, 0, 0));
        tail.addOrReplaceChild("lower_lobe", CubeListBuilder.create().addBox(-0.3F, 0, -0.5F, 0.6F, 9, 3),
                PartPose.offsetAndRotation(0, 0, 8, 0.55F, 0, 0));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(SwordfishEntity entity, float walk, float amount, float age, float yaw, float pitch) {
        this.body.xRot = entity.isInWater() ? entity.getXRot() * Mth.DEG_TO_RAD : 0;
        this.body.zRot = entity.isInWater() ? 0 : Mth.HALF_PI;
        this.tail.yRot = Mth.sin(age * (entity.isInWater() ? 0.38F : 0.7F)) * 0.25F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        pose.pushPose();
        this.body.translateAndRotate(pose);
        for (String part : new String[]{"back", "dorsal", "left_fin", "right_fin", "anal_fin", "tail"}) {
            this.body.getChild(part).render(pose, vertices, light, overlay, red * 0.13F, green * 0.28F, blue * 0.39F, alpha);
        }
        this.body.getChild("belly").render(pose, vertices, light, overlay, red * 0.78F, green * 0.84F, blue * 0.84F, alpha);
        for (String part : new String[]{"head", "bill"}) {
            this.body.getChild(part).render(pose, vertices, light, overlay, red * 0.4F, green * 0.55F, blue * 0.62F, alpha);
        }
        this.body.getChild("eyes").render(pose, vertices, light, overlay, red * 0.025F, green * 0.035F, blue * 0.04F, alpha);
        pose.popPose();
    }
}
