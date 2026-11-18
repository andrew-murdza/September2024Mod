package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class IllagerLordModel extends GeoModel<IllagerLordEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation("aoemod", "geo/illager_king.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("aoemod", "textures/entity/illager_lord.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "aoemod",
            "animations/illager_lord.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(IllagerLordEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(IllagerLordEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(IllagerLordEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            IllagerLordEntity entity,
            long instanceId,
            AnimationState<IllagerLordEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        this.getBone("head").ifPresent(head -> {
            // Set the look rotation absolutely so it cannot accumulate every
            // render frame. The king only turns his head from side to side.
            head.setRotX(0.0F);
            head.setRotY(radians(modelData.netHeadYaw()));
            head.setRotZ(0.0F);
        });
        this.getBone("left_arm_item").ifPresent(shield -> {
            boolean hidden = entity.isShieldBroken();
            shield.setHidden(hidden);
            shield.setChildrenHidden(hidden);
        });
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
