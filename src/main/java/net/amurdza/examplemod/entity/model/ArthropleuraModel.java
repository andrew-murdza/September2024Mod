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

/** Original fourteen-plate millipede with twenty-eight independently animated leg pairs. */
public final class ArthropleuraModel extends EntityModel<ArthropleuraEntity> {
    private static final int SEGMENTS = 14;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public ArthropleuraModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < SEGMENTS; i++) this.segments[i] = root.getChild("segment_" + i);
    }

    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        for (int i = 0; i < SEGMENTS; i++) {
            float width = i < 10 ? 9 - Math.abs(4 - i) * 0.22F : 8.3F - (i - 10) * 0.7F;
            var plate = root.addOrReplaceChild("segment_" + i, CubeListBuilder.create()
                    .addBox(-width / 2, -2.2F, -1.45F, width, 3, 2.9F),
                    PartPose.offset(0, 20.5F, -17 + i * 2.8F));
            plate.addOrReplaceChild("ridge", CubeListBuilder.create()
                    .addBox(-width / 2 - 0.5F, -2.4F, 0.3F, width + 1, 1, 1.2F), PartPose.ZERO);
            for (int pair = 0; pair < 2; pair++) {
                for (int side : new int[]{-1, 1}) {
                    var leg = plate.addOrReplaceChild("leg_" + pair + "_" + side, CubeListBuilder.create()
                            .addBox(side < 0 ? -3.5F : 0, -0.35F, -0.35F, 3.5F, 0.7F, 0.7F),
                            PartPose.offsetAndRotation(side * (width / 2 - 0.5F), 0.8F, pair * 1.3F - 0.6F,
                                    0, 0, side * 0.4F));
                    leg.addOrReplaceChild("foot", CubeListBuilder.create()
                            .addBox(-0.35F, 0, -0.35F, 0.7F, 2, 0.7F),
                            PartPose.offsetAndRotation(side * 3.2F, 0, 0, 0, 0, side * -0.25F));
                }
            }
        }
        var head = root.addOrReplaceChild("head", CubeListBuilder.create().addBox(-3.5F, -2, -3, 7, 3.5F, 4),
                PartPose.offset(0, 20.5F, -19));
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .addBox(-3.65F, -1.4F, -2.5F, 0.3F, 0.65F, 1)
                .addBox(3.35F, -1.4F, -2.5F, 0.3F, 0.65F, 1), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            var antenna = head.addOrReplaceChild("antenna_" + side, CubeListBuilder.create()
                    .addBox(-0.3F, -0.3F, -4, 0.6F, 0.6F, 4),
                    PartPose.offsetAndRotation(side * 2, -1, -2.5F, -0.25F, side * -0.5F, 0));
            var bend = antenna.addOrReplaceChild("bend", CubeListBuilder.create()
                    .addBox(-0.25F, -0.25F, -3, 0.5F, 0.5F, 3),
                    PartPose.offsetAndRotation(0, 0, -4, 0.15F, side * 0.65F, 0));
            bend.addOrReplaceChild("tip", CubeListBuilder.create().addBox(-0.2F, -0.2F, -2, 0.4F, 0.4F, 2),
                    PartPose.offsetAndRotation(0, 0, -3, 0.35F, side * 0.35F, 0));
        }
        return LayerDefinition.create(mesh, 16, 16);
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
            this.head.getChild("antenna_" + side).yRot = side * -0.5F + Mth.sin(age * 0.07F) * 0.08F;
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
        // Warm alternating plates distinguish the low, broad body from a generic spider.
        for (int i = 0; i < SEGMENTS; i++) {
            float shade = i % 2 == 0 ? 1 : 0.82F;
            this.segments[i].render(pose, vertices, light, overlay,
                    red * 0.45F * shade, green * 0.23F * shade, blue * 0.095F * shade, alpha);
        }
        this.head.render(pose, vertices, light, overlay, red * 0.28F, green * 0.13F, blue * 0.055F, alpha);
        pose.popPose();
    }
}
