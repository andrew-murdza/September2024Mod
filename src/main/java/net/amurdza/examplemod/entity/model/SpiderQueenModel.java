package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class SpiderQueenModel extends GeoModel<SpiderQueenEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation("aoemod", "geo/spider_queen.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("aoemod", "textures/entity/spider_queen.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "aoemod",
            "animations/spider_queen.animation.json"
    );
    private static final float[] BASE_Y_ROTATIONS = {
            radians(-45.0F), radians(45.0F), radians(-22.5F), radians(22.5F),
            radians(22.5F), radians(-22.5F), radians(45.0F), radians(-45.0F)
    };
    private static final float[] BASE_Z_ROTATIONS = {
            radians(-45.0F), radians(45.0F), radians(-33.3F), radians(33.3F),
            radians(-33.3F), radians(33.3F), radians(-45.0F), radians(45.0F)
    };
    private static final float[] WALK_PHASES = {
            0.0F, 0.0F,
            (float) Math.PI, (float) Math.PI,
            (float) Math.PI / 2.0F, (float) Math.PI / 2.0F,
            (float) Math.PI * 3.0F / 2.0F, (float) Math.PI * 3.0F / 2.0F
    };

    @Override
    public ResourceLocation getModelResource(SpiderQueenEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SpiderQueenEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpiderQueenEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            SpiderQueenEntity entity,
            long instanceId,
            AnimationState<SpiderQueenEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        this.getBone("head").ifPresent(head -> {
            head.setRotY(radians(modelData.netHeadYaw()));
            head.setRotX(radians(modelData.headPitch()));
        });

        float cycle = animationState.getLimbSwing() * 1.3324F;
        float amount = Math.min(animationState.getLimbSwingAmount(), 1.0F);
        for (int index = 0; index < 8; ++index) {
            GeoBone leg = this.getBone("leg" + (index + 1)).orElse(null);
            if (leg == null) {
                continue;
            }

            boolean leftSide = index % 2 == 0;
            float phase = WALK_PHASES[index];
            float yawSide = leftSide ? 1.0F : -1.0F;
            float rollSide = leftSide ? -1.0F : 1.0F;
            float yawMotion = (float) Math.cos(cycle + phase) * 0.4F * amount * yawSide;
            float rollMotion = Math.abs((float) Math.sin(cycle + phase)) * 0.4F * amount * rollSide;
            leg.setRotY(BASE_Y_ROTATIONS[index] + yawMotion);
            leg.setRotZ(BASE_Z_ROTATIONS[index] + rollMotion);
        }
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
