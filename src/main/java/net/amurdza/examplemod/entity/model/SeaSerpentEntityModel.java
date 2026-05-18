package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.entity.sea_serpent.SeaSerpentEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import static net.amurdza.examplemod.entity.sea_serpent.SeaSerpentEntity.PART_HEIGHT;
import static net.amurdza.examplemod.entity.sea_serpent.SeaSerpentEntity.PART_LENGTH;

public class SeaSerpentEntityModel<T extends SeaSerpentEntity> extends HierarchicalModel<T> {
    private static final float[] TOP_FIN_Y = new float[] {-6.0F, -4.0F, -1.0F, -1.0F, 1.5F, 2.5F};
    private static final float[] TOP_FIN_Z = new float[] {13.0F, 13.0F, 10.0F, 8.0F, 8.0F, 8.0F};

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart[] body = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart fin1;
    private final ModelPart fin2;
    private final ModelPart fin3;
    private final ModelPart fin4;
    private final ModelPart[][] tailFin = new ModelPart[2][3];
    private final ModelPart[] topFin = new ModelPart[6];
    private final ModelPart[] anatomy;

    public SeaSerpentEntityModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.mouth = this.head.getChild("mouth");

        this.body[0] = this.head.getChild("body0");
        this.body[1] = this.body[0].getChild("body1");
        this.body[2] = this.body[1].getChild("body2");
        this.body[3] = this.body[2].getChild("body3");

        this.tail[0] = this.body[3].getChild("tail0");
        this.tail[1] = this.tail[0].getChild("tail1");
        this.tail[2] = this.tail[1].getChild("tail2");

        this.fin1 = this.body[0].getChild("fin1");
        this.fin2 = this.body[0].getChild("fin2");
        this.fin3 = this.body[3].getChild("fin3");
        this.fin4 = this.body[3].getChild("fin4");

        for (int i = 0; i < this.tailFin.length; ++i) {
            for (int j = 0; j < this.tailFin[i].length; ++j) {
                this.tailFin[i][j] = this.tail[2].getChild("tailFin" + i + "_" + j);
            }
        }

        for (int i = 0; i < 4; ++i) {
            this.topFin[i] = this.body[i].getChild("topFin" + i);
        }

        for (int i = 0; i < 2; ++i) {
            this.topFin[4 + i] = this.tail[i].getChild("topFin" + (4 + i));
        }

        this.anatomy = new ModelPart[] {
                this.head,
                this.body[0],
                this.body[1],
                this.body[2],
                this.body[3],
                this.tail[0],
                this.tail[1],
                this.tail[2]
        };
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -4.0F, -18.0F, 10.0F, PART_HEIGHT[0], PART_LENGTH[0]),
                PartPose.offset(0.0F, 17.0F, 0.0F)
        );

        head.addOrReplaceChild(
                "mouth",
                CubeListBuilder.create()
                        .texOffs(38, 0)
                        .addBox(-4.0F, -2.0F, -16.0F, 8.0F, 2.0F, 16.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F)
        );

        PartDefinition body0 = head.addOrReplaceChild(
                "body0",
                CubeListBuilder.create()
                        .texOffs(0, 26)
                        .addBox(-8.0F, -7.0F, 0.0F, 16.0F, PART_HEIGHT[1], PART_LENGTH[1]),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition body1 = body0.addOrReplaceChild(
                "body1",
                CubeListBuilder.create()
                        .texOffs(0, 64)
                        .addBox(-6.0F, -5.0F, 0.0F, 12.0F, PART_HEIGHT[2], PART_LENGTH[2]),
                PartPose.offset(0.0F, 0.0F, 23.0F)
        );

        PartDefinition body2 = body1.addOrReplaceChild(
                "body2",
                CubeListBuilder.create()
                        .texOffs(56, 18)
                        .addBox(-5.0F, -3.0F, 0.0F, 10.0F, PART_HEIGHT[3], PART_LENGTH[3]),
                PartPose.offset(0.0F, 0.0F, 23.0F)
        );

        PartDefinition body3 = body2.addOrReplaceChild(
                "body3",
                CubeListBuilder.create()
                        .texOffs(66, 50)
                        .addBox(-4.0F, -1.0F, 0.0F, 8.0F, PART_HEIGHT[4], PART_LENGTH[4]),
                PartPose.offset(0.0F, 0.0F, 19.0F)
        );

        PartDefinition tail0 = body3.addOrReplaceChild(
                "tail0",
                CubeListBuilder.create()
                        .texOffs(30, 100)
                        .addBox(-3.0F, 1.0F, 0.0F, 6.0F, PART_HEIGHT[5], PART_LENGTH[5]),
                PartPose.offset(0.0F, 0.0F, 13.0F)
        );

        PartDefinition tail1 = tail0.addOrReplaceChild(
                "tail1",
                CubeListBuilder.create()
                        .texOffs(58, 104)
                        .addBox(-2.5F, 2.0F, 0.0F, 5.0F, PART_HEIGHT[6], PART_LENGTH[6]),
                PartPose.offset(0.0F, 0.0F, 15.0F)
        );

        PartDefinition tail2 = tail1.addOrReplaceChild(
                "tail2",
                CubeListBuilder.create()
                        .texOffs(0, 100)
                        .addBox(-2.0F, 3.0F, 0.0F, 4.0F, PART_HEIGHT[7], PART_LENGTH[7]),
                PartPose.offset(0.0F, 0.0F, 17.0F)
        );

        body0.addOrReplaceChild(
                "fin1",
                CubeListBuilder.create()
                        .texOffs(96, 0)
                        .addBox(-1.0F, 0.0F, -6.0F, 2.0F, 16.0F, 12.0F),
                PartPose.offset(-5.0F, 2.0F, 14.0F)
        );

        body0.addOrReplaceChild(
                "fin2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(96, 0)
                        .addBox(-1.0F, 0.0F, -6.0F, 2.0F, 16.0F, 12.0F),
                PartPose.offset(5.0F, 2.0F, 14.0F)
        );

        body3.addOrReplaceChild(
                "fin3",
                CubeListBuilder.create()
                        .texOffs(0, 26)
                        .addBox(-0.5F, 0.0F, -4.0F, 1.0F, 10.0F, 8.0F),
                PartPose.offset(-2.0F, 4.0F, 8.0F)
        );

        body3.addOrReplaceChild(
                "fin4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 26)
                        .addBox(-0.5F, 0.0F, -4.0F, 1.0F, 10.0F, 8.0F),
                PartPose.offset(2.0F, 4.0F, 8.0F)
        );

        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 3; ++j) {
                int k = j * 2;
                int l = 22 - k;
                boolean left = i == 0;

                CubeListBuilder builder = CubeListBuilder.create().texOffs(48 + k, 72);
                if (!left) {
                    builder.mirror();
                }

                tail2.addOrReplaceChild(
                        "tailFin" + i + "_" + j,
                        builder.addBox(left ? -l : 0.0F, 4.0F, -2.0F, l, 2.0F, 4.0F),
                        PartPose.offset(0.0F, 0.0F, (float)j * 7.0F + 3.0F)
                );
            }
        }

        PartDefinition[] topFinParents = new PartDefinition[] {
                body0, body1, body2, body3, tail0, tail1
        };

        for (int i = 0; i < 6; ++i) {
            CubeListBuilder builder = CubeListBuilder.create();

            if (i < 3) {
                builder.texOffs(72, 78)
                        .addBox(-0.5F, -8.0F, -4.0F, 1.0F, 16.0F, 8.0F);
            } else {
                builder.texOffs(0, 0)
                        .addBox(-0.5F, -4.0F, -2.0F, 1.0F, 8.0F, 4.0F);
            }

            topFinParents[i].addOrReplaceChild(
                    "topFin" + i,
                    builder,
                    PartPose.offset(0.0F, TOP_FIN_Y[i], TOP_FIN_Z[i])
            );
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public @NotNull ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(T serpent, float limbSwing, float limbSwingAmount, float partialTick) {
        serpent.resetBoneAngles();
        serpent.updatePitchRotations(partialTick);
        serpent.updateYawRotations(partialTick);
    }

    @Override
    public void setupAnim(@NotNull T serpent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.setAngles();
        this.animate(serpent, ageInTicks);
    }

    public void setAngles() {
        for (ModelPart model : this.anatomy) {
            resetAngles(model);
        }

        setRotation(this.mouth, 0.02617994F, 0.0F, 0.0F);

        setRotation(this.fin1, 0.62831855F, -0.15707964F, 1.0471976F);
        setRotation(this.fin2, 0.62831855F, 0.15707964F, -1.0471976F);

        setRotationToModel(this.fin3, this.fin1);
        setRotationToModel(this.fin4, this.fin2);

        for (int i = 0; i < this.tailFin[0].length; ++i) {
            setRotation(this.tailFin[0][i], 0.0F, 0.7853982F, 0.0F);
            setRotation(this.tailFin[1][i], 0.0F, -0.7853982F, 0.0F);
        }

        for (ModelPart model : this.topFin) {
            setRotation(model, -1.2566371F, 0.0F, 0.0F);
        }
    }

    public void animate(T serpent, float ageInTicks) {
        float breatheAnim = Mth.sin(ageInTicks * 0.1F);

        for (int i = 0; i < serpent.getBoneList().length; ++i) {
            this.anatomy[i].xRot += serpent.getBoneList()[i].getRotation().x * 0.017453292F;
            this.anatomy[i].yRot += serpent.getBoneList()[i].getRotation().y * 0.017453292F;
        }

        this.mouth.xRot += Math.max(0.0F, breatheAnim) * 0.05F
                + Mth.sin(this.attackTime * Mth.PI) * 0.4F;

        this.fin1.zRot += breatheAnim * 0.1F;
        this.fin2.zRot -= breatheAnim * 0.1F;
        this.fin3.zRot -= breatheAnim * 0.2F;
        this.fin4.zRot += breatheAnim * 0.2F;

        for (int i = 0; i < 2; ++i) {
            this.tailFin[i][0].zRot += this.tail[2].xRot * 0.4F;
            this.tailFin[i][1].zRot -= this.tail[2].xRot * 0.4F;
            this.tailFin[i][2].zRot += this.tail[2].xRot * 0.4F;
        }
    }

    private static void resetAngles(ModelPart model) {
        setRotation(model, 0.0F, 0.0F, 0.0F);
    }

    private static void setRotation(ModelPart model, float x, float y, float z) {
        model.xRot = x;
        model.yRot = y;
        model.zRot = z;
    }

    private static void setRotationToModel(ModelPart model, ModelPart source) {
        model.xRot = source.xRot;
        model.yRot = source.yRot;
        model.zRot = source.zRot;
    }
}