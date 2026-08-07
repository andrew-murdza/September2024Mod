package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.entity.ArchlichEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ArchlichModel extends GeoModel<ArchlichEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation("aoemod", "geo/archlich.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("aoemod", "textures/entity/archlich.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "aoemod",
            "animations/archlich.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(ArchlichEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArchlichEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArchlichEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            ArchlichEntity entity,
            long instanceId,
            AnimationState<ArchlichEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float yaw = radians(modelData.netHeadYaw());
        float pitch = radians(modelData.headPitch());
        this.getBone("head").ifPresent(head -> {
            head.setRotY(head.getRotY() + yaw);
            head.setRotX(head.getRotX() + pitch);
        });
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
