package net.amurdza.examplemod.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.amurdza.examplemod.entity.ArthropleuraEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Broad overlapping armor, a raised central trunk and two leg pairs under each plate. */
public final class ArthropleuraModel extends EntityModel<ArthropleuraEntity> {
    private static final int SEGMENTS = 8;
    private static final float MODEL_SCALE = 0.65F;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public ArthropleuraModel(ModelPart root) {
        this.root = root.getChild("arthropleura");
        this.root.xScale = MODEL_SCALE;
        this.root.yScale = MODEL_SCALE;
        this.root.zScale = MODEL_SCALE;
        this.head = this.root.getChild("head");
        for (int i = 0; i < SEGMENTS; i++) this.segments[i] = this.root.getChild("segment_" + i);
    }

    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        // Scale around the feet so both the adult and the juvenile stay on the ground.
        var root = mesh.getRoot().addOrReplaceChild("arthropleura", CubeListBuilder.create(),
                PartPose.offset(0, 24, 0));
        for (int i = 0; i < SEGMENTS; i++) {
            var plate = root.addOrReplaceChild("segment_" + i, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-3, -1.5F, -4, 6, 3, 8),
                    PartPose.offsetAndRotation(0, -4.25F, -24.5F + i * 7, 0.035F, 0, 0));
            for (int side : new int[]{-1, 1}) {
                plate.addOrReplaceChild("flange_" + side, CubeListBuilder.create().texOffs(0, 16).mirror(side < 0)
                                .addBox(side < 0 ? -7.5F : 2.5F, -1.3F, -4, 5, 1, 8),
                        PartPose.rotation(0, 0, side * 0.05F));
            }
            for (int pair = 0; pair < 2; pair++) {
                for (int side : new int[]{-1, 1}) {
                    var leg = plate.addOrReplaceChild("leg_" + pair + "_" + side,
                            CubeListBuilder.create().texOffs(64, 0)
                                    .addBox(side < 0 ? -4 : 0, -0.5F, -0.5F, 4, 1, 1),
                            PartPose.offsetAndRotation(side * 2.65F, 1, pair * 4.2F - 2.1F,
                                    0, 0, side * 0.4F));
                    leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(80, 0)
                            .addBox(-0.5F, 0, -0.5F, 1, 2, 1),
                            PartPose.offsetAndRotation(side * 3.2F, 0, 0, 0, 0, side * -0.25F));
                }
            }
            if (i == SEGMENTS - 1) {
                plate.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 16)
                                .addBox(-4, -0.5F, 0, 8, 1, 4),
                        PartPose.offsetAndRotation(0, -0.7F, 3.4F, -0.08F, 0, 0));
            }
        }
        var head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.5F, -1.5F, -5, 7, 3, 5),
                PartPose.offset(0, -4.25F, -27.5F));
        head.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(96, 0)
                .addBox(-3.65F, -0.9F, -4, 0.3F, 0.7F, 1)
                .addBox(3.35F, -0.9F, -4, 0.3F, 0.7F, 1), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            head.addOrReplaceChild("mandible_" + side, CubeListBuilder.create().texOffs(80, 8)
                            .addBox(-1, -0.5F, -3, 2, 1, 3),
                    PartPose.offsetAndRotation(side * 2, 0.8F, -4.6F, 0, side * -0.45F, 0));
            var antenna = head.addOrReplaceChild("antenna_" + side, CubeListBuilder.create().texOffs(64, 8)
                    .addBox(-0.5F, -0.5F, -6, 1, 1, 6),
                    PartPose.offsetAndRotation(side * 1.2F, -1.2F, -4.7F, -0.2F, side * -0.4F, 0));
            antenna.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(64, 8)
                            .addBox(-0.5F, -0.5F, -6, 1, 1, 6),
                    PartPose.offsetAndRotation(0, 0, -6, 0.35F, side * -0.1F, 0));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(ArthropleuraEntity entity, float walk, float amount, float age, float yaw, float pitch) {
        this.head.yRot = Mth.clamp(yaw * Mth.DEG_TO_RAD, -0.3F, 0.3F);
        float stride = Math.min(amount * 2.5F, 1);
        for (int i = 0; i < SEGMENTS; i++) {
            for (int pair = 0; pair < 2; pair++) {
                for (int side : new int[]{-1, 1}) {
                    var leg = this.segments[i].getChild("leg_" + pair + "_" + side);
                    float phase = walk * 2.4F - i * 0.65F - pair * 0.45F + (side < 0 ? Mth.PI : 0);
                    leg.yRot = Mth.sin(phase) * 0.5F * stride;
                    leg.zRot = side * (0.4F + Mth.cos(phase) * 0.22F * stride);
                }
            }
        }
        for (int side : new int[]{-1, 1}) {
            this.head.getChild("antenna_" + side).yRot = side * -0.4F + Mth.sin(age * 0.07F) * 0.08F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        pose.pushPose();
        if (this.young) {
            pose.translate(0, 0.75, 0);
            pose.scale(0.5F, 0.5F, 0.5F);
        }
        this.root.render(pose, vertices, light, overlay, red, green, blue, alpha);
        pose.popPose();
    }
}
