//package net.amurdza.examplemod.disabled;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import net.amurdza.examplemod.disabled.SeaSerpentE;
//import net.minecraft.client.model.EntityModel;
//import net.minecraft.client.model.geom.ModelPart;
//import net.minecraft.client.model.geom.PartPose;
//import net.minecraft.client.model.geom.builders.CubeListBuilder;
//import net.minecraft.client.model.geom.builders.LayerDefinition;
//import net.minecraft.client.model.geom.builders.MeshDefinition;
//import net.minecraft.client.model.geom.builders.PartDefinition;
//import net.minecraft.util.Mth;
//import org.jetbrains.annotations.NotNull;
//
//import static net.amurdza.examplemod.entity.SeaSerpentEntity.PART_HEIGHT;
//import static net.amurdza.examplemod.entity.SeaSerpentEntity.PART_LENGTH;
//
//public class SeaSerpentModel<T extends SeaSerpentEntity> extends EntityModel<T> {
//    private static final float[] TOP_FIN_Y = new float[] {-6.0F, -4.0F, -1.0F, -1.0F, 1.5F, 2.5F};
//    private static final float[] TOP_FIN_Z = new float[] {13.0F, 13.0F, 10.0F, 8.0F, 8.0F, 8.0F};
//
//    private final ModelPart head;
//    private final ModelPart mouth;
//    private final ModelPart[] body = new ModelPart[4];
//    private final ModelPart[] tail = new ModelPart[3];
//    private final ModelPart fin1;
//    private final ModelPart fin2;
//    private final ModelPart fin3;
//    private final ModelPart fin4;
//    private final ModelPart[][] tailFin = new ModelPart[2][3];
//    private final ModelPart[] topFin = new ModelPart[6];
//    private final ModelPart[] anatomy = new ModelPart[8];
//
//    public SeaSerpentModel(ModelPart root) {
//        this.head = root.getChild("head");
//        this.mouth = this.head.getChild("mouth");
//
//        this.body[0] = this.head.getChild("body0");
//        this.body[1] = this.body[0].getChild("body1");
//        this.body[2] = this.body[1].getChild("body2");
//        this.body[3] = this.body[2].getChild("body3");
//
//        this.tail[0] = this.body[3].getChild("tail0");
//        this.tail[1] = this.tail[0].getChild("tail1");
//        this.tail[2] = this.tail[1].getChild("tail2");
//
//        this.fin1 = this.body[0].getChild("fin1");
//        this.fin2 = this.body[0].getChild("fin2");
//        this.fin3 = this.body[3].getChild("fin3");
//        this.fin4 = this.body[3].getChild("fin4");
//
//        for (int i = 0; i < this.tailFin.length; i++) {
//            for (int j = 0; j < this.tailFin[i].length; j++) {
//                this.tailFin[i][j] = this.tail[2].getChild("tail_fin_" + i + "_" + j);
//            }
//        }
//
//        for (int i = 0; i < 4; i++) {
//            this.topFin[i] = this.body[i].getChild("top_fin_" + i);
//        }
//
//        for (int i = 0; i < 2; i++) {
//            this.topFin[4 + i] = this.tail[i].getChild("top_fin_" + (4 + i));
//        }
//
//        this.anatomy[0] = this.head;
//        this.anatomy[1] = this.body[0];
//        this.anatomy[2] = this.body[1];
//        this.anatomy[3] = this.body[2];
//        this.anatomy[4] = this.body[3];
//        this.anatomy[5] = this.tail[0];
//        this.anatomy[6] = this.tail[1];
//        this.anatomy[7] = this.tail[2];
//    }
//
//    public static LayerDefinition createBodyLayer() {
//        MeshDefinition mesh = new MeshDefinition();
//        PartDefinition root = mesh.getRoot();
//
//        PartDefinition head = root.addOrReplaceChild("head",
//                CubeListBuilder.create().texOffs(0, 0)
//                        .addBox(-5.0F, -4.0F, -18.0F, 10.0F, PART_HEIGHT[0], PART_LENGTH[0]),
//                PartPose.offset(0.0F, 17.0F, 0.0F));
//
//        head.addOrReplaceChild("mouth",
//                CubeListBuilder.create().texOffs(38, 0)
//                        .addBox(-4.0F, -2.0F, -16.0F, 8.0F, 2.0F, 16.0F),
//                PartPose.offset(0.0F, 6.0F, 0.0F));
//
//        PartDefinition body0 = head.addOrReplaceChild("body0",
//                CubeListBuilder.create().texOffs(0, 26)
//                        .addBox(-8.0F, -7.0F, 0.0F, 16.0F, PART_HEIGHT[1], PART_LENGTH[1]),
//                PartPose.ZERO);
//
//        PartDefinition body1 = body0.addOrReplaceChild("body1",
//                CubeListBuilder.create().texOffs(0, 64)
//                        .addBox(-6.0F, -5.0F, 0.0F, 12.0F, PART_HEIGHT[2], PART_LENGTH[2]),
//                PartPose.offset(0.0F, 0.0F, 23.0F));
//
//        PartDefinition body2 = body1.addOrReplaceChild("body2",
//                CubeListBuilder.create().texOffs(56, 18)
//                        .addBox(-5.0F, -3.0F, 0.0F, 10.0F, PART_HEIGHT[3], PART_LENGTH[3]),
//                PartPose.offset(0.0F, 0.0F, 23.0F));
//
//        PartDefinition body3 = body2.addOrReplaceChild("body3",
//                CubeListBuilder.create().texOffs(66, 50)
//                        .addBox(-4.0F, -1.0F, 0.0F, 8.0F, PART_HEIGHT[4], PART_LENGTH[4]),
//                PartPose.offset(0.0F, 0.0F, 19.0F));
//
//        PartDefinition tail0 = body3.addOrReplaceChild("tail0",
//                CubeListBuilder.create().texOffs(30, 100)
//                        .addBox(-3.0F, 1.0F, 0.0F, 6.0F, PART_HEIGHT[5], PART_LENGTH[5]),
//                PartPose.offset(0.0F, 0.0F, 13.0F));
//
//        PartDefinition tail1 = tail0.addOrReplaceChild("tail1",
//                CubeListBuilder.create().texOffs(58, 104)
//                        .addBox(-2.5F, 2.0F, 0.0F, 5.0F, PART_HEIGHT[6], PART_LENGTH[6]),
//                PartPose.offset(0.0F, 0.0F, 15.0F));
//
//        PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
//                CubeListBuilder.create().texOffs(0, 100)
//                        .addBox(-2.0F, 3.0F, 0.0F, 4.0F, PART_HEIGHT[7], PART_LENGTH[7]),
//                PartPose.offset(0.0F, 0.0F, 17.0F));
//
//        body0.addOrReplaceChild("fin1",
//                CubeListBuilder.create().texOffs(96, 0)
//                        .addBox(-1.0F, 0.0F, -6.0F, 2.0F, 16.0F, 12.0F),
//                PartPose.offsetAndRotation(-5.0F, 2.0F, 14.0F, 0.62831855F, -0.15707964F, 1.0471976F));
//
//        body0.addOrReplaceChild("fin2",
//                CubeListBuilder.create().mirror().texOffs(96, 0)
//                        .addBox(-1.0F, 0.0F, -6.0F, 2.0F, 16.0F, 12.0F),
//                PartPose.offsetAndRotation(5.0F, 2.0F, 14.0F, 0.62831855F, 0.15707964F, -1.0471976F));
//
//        body3.addOrReplaceChild("fin3",
//                CubeListBuilder.create().texOffs(0, 26)
//                        .addBox(-0.5F, 0.0F, -4.0F, 1.0F, 10.0F, 8.0F),
//                PartPose.offsetAndRotation(-2.0F, 4.0F, 8.0F, 0.62831855F, -0.15707964F, 1.0471976F));
//
//        body3.addOrReplaceChild("fin4",
//                CubeListBuilder.create().mirror().texOffs(0, 26)
//                        .addBox(-0.5F, 0.0F, -4.0F, 1.0F, 10.0F, 8.0F),
//                PartPose.offsetAndRotation(2.0F, 4.0F, 8.0F, 0.62831855F, 0.15707964F, -1.0471976F));
//
//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < 3; j++) {
//                int k = j * 2;
//                int l = 22 - k;
//                boolean left = i == 0;
//
//                CubeListBuilder builder = CubeListBuilder.create().texOffs(48 + k, 72);
//                if (!left) builder.mirror();
//
//                builder.addBox(left ? -l : 0.0F, 4.0F, -2.0F, l, 2.0F, 4.0F);
//
//                tail2.addOrReplaceChild("tail_fin_" + i + "_" + j, builder,
//                        PartPose.offsetAndRotation(0.0F, 0.0F, j * 7.0F + 3.0F, 0.0F, left ? 0.7853982F : -0.7853982F, 0.0F));
//            }
//        }
//
//        PartDefinition[] bodyParts = new PartDefinition[] {body0, body1, body2, body3};
//        PartDefinition[] tailParts = new PartDefinition[] {tail0, tail1};
//
//        for (int i = 0; i < 4; i++) {
//            bodyParts[i].addOrReplaceChild("top_fin_" + i,
//                    CubeListBuilder.create().texOffs(72, 78)
//                            .addBox(-0.5F, -8.0F, -4.0F, 1.0F, 16.0F, 8.0F),
//                    PartPose.offsetAndRotation(0.0F, TOP_FIN_Y[i], TOP_FIN_Z[i], -1.2566371F, 0.0F, 0.0F));
//        }
//
//        for (int i = 4; i < 6; i++) {
//            tailParts[i - 4].addOrReplaceChild("top_fin_" + i,
//                    CubeListBuilder.create().texOffs(0, 0)
//                            .addBox(-0.5F, -4.0F, -2.0F, 1.0F, 8.0F, 4.0F),
//                    PartPose.offsetAndRotation(0.0F, TOP_FIN_Y[i], TOP_FIN_Z[i], -1.2566371F, 0.0F, 0.0F));
//        }
//
//        return LayerDefinition.create(mesh, 128, 128);
//    }
//
//    @Override
//    public void setupAnim(T serpent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
//        this.resetAngles();
//
//        float partialTick = Mth.clamp(ageInTicks - serpent.tickCount, 0.0F, 1.0F);
//        serpent.setupAnimationBones(partialTick);
//
//        float breatheAnim = Mth.sin(ageInTicks * 0.1F);
//
//        for (int i = 0; i < serpent.getBoneList().length; i++) {
//            this.anatomy[i].xRot += serpent.getBoneList()[i].getRotation().x * 0.017453292F;
//            this.anatomy[i].yRot += serpent.getBoneList()[i].getRotation().y * 0.017453292F;
//        }
//
//        this.mouth.xRot += Math.max(0.0F, breatheAnim) * 0.05F;
//        this.fin1.zRot += breatheAnim * 0.1F;
//        this.fin2.zRot -= breatheAnim * 0.1F;
//        this.fin3.zRot -= breatheAnim * 0.2F;
//        this.fin4.zRot += breatheAnim * 0.2F;
//    }
//
//    private void resetAngles() {
//        for (ModelPart part : this.anatomy) {
//            part.xRot = 0.0F;
//            part.yRot = 0.0F;
//            part.zRot = 0.0F;
//        }
//
//        this.mouth.xRot = 0.02617994F;
//        this.mouth.yRot = 0.0F;
//        this.mouth.zRot = 0.0F;
//    }
//
//    @Override
//    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay,
//                               float red, float green, float blue, float alpha) {
//        this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
//    }
//}