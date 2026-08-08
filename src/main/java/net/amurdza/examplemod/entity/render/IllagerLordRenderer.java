package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.amurdza.examplemod.entity.model.IllagerLordModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class IllagerLordRenderer extends GeoEntityRenderer<IllagerLordEntity> {
    private static final float WEAPON_SCALE = 1.35F;

    public IllagerLordRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerLordModel());
        this.shadowRadius = 0.6F;
        this.addRenderLayer(new BlockAndItemGeoLayer<IllagerLordEntity>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, IllagerLordEntity entity) {
                return switch (bone.getName()) {
                    case "left_arm_item" -> entity.getOffhandItem();
                    case "right_arm_item" -> entity.getMainHandItem();
                    default -> super.getStackForBone(bone, entity);
                };
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(
                    GeoBone bone,
                    ItemStack stack,
                    IllagerLordEntity entity
            ) {
                return switch (bone.getName()) {
                    case "left_arm_item" -> ItemDisplayContext.NONE;
                    case "right_arm_item" -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    default -> ItemDisplayContext.NONE;
                };
            }

            @Override
            protected void renderStackForBone(
                    PoseStack poseStack,
                    GeoBone bone,
                    ItemStack stack,
                    IllagerLordEntity entity,
                    MultiBufferSource buffer,
                    float partialTick,
                    int packedLight,
                    int packedOverlay
            ) {
                poseStack.pushPose();

                if (bone.getName().equals("left_arm_item")) {
                    // Turn the plate toward the front, place it slightly ahead
                    // of the body, then cancel the item renderer's offset.
                    poseStack.translate(0.0D, 0.0D, -0.375D);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.translate(0.5D, 0.5D, 0.5D);
                } else if (bone.getName().equals("right_arm_item")) {
                    // ItemInHandLayer applies these same transforms after a
                    // Vindicator's arm transform. This attachment is already
                    // at the wrist, so it does not need the layer's arm-length
                    // translation.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.translate(0.0D, 0.125D, 0.0D);
                    // Flip the axe within its rendered plane while keeping
                    // the Vindicator-style grip anchored to the wrist.
                    poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                    poseStack.scale(WEAPON_SCALE, WEAPON_SCALE, WEAPON_SCALE);
                }

                super.renderStackForBone(
                        poseStack,
                        bone,
                        stack,
                        entity,
                        buffer,
                        partialTick,
                        packedLight,
                        packedOverlay
                );
                poseStack.popPose();
            }
        });
    }
}
