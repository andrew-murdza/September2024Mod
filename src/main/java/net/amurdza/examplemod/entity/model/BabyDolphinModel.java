package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.render.BabyReplacedEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Dolphin;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public final class BabyDolphinModel extends GeoModel<BabyReplacedEntity<Dolphin>> {
    public static final ResourceLocation TEXTURE = AOEMod.makeID("textures/entity/baby_dolphin/dolphin_baby.png");
    private static final ResourceLocation MODEL = AOEMod.makeID("geo/baby_dolphin.geo.json");
    private static final ResourceLocation ANIMATION = AOEMod.makeID("animations/baby_animals.animation.json");

    @Override
    public ResourceLocation getModelResource(BabyReplacedEntity<Dolphin> animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BabyReplacedEntity<Dolphin> animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BabyReplacedEntity<Dolphin> animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            BabyReplacedEntity<Dolphin> animatable,
            long instanceId,
            AnimationState<BabyReplacedEntity<Dolphin>> animationState
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Dolphin dolphin = animatable.getCurrentEntity();
        if (dolphin == null) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float ageInTicks = dolphin.tickCount + animationState.getPartialTick();
        boolean swimming = dolphin.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7D;
        float swimWave = swimming ? (float) Math.cos(ageInTicks * 0.3F) : 0.0F;

        this.getBone("body").ifPresent(body -> {
            body.setRotX(radians(modelData.headPitch()) + (swimming ? -0.05F - 0.05F * swimWave : 0.0F));
            body.setRotY(radians(modelData.netHeadYaw()));
        });
        this.getBone("tail").ifPresent(tail -> tail.setRotX(-0.1F * swimWave));
        this.getBone("tail_fin").ifPresent(tailFin -> tailFin.setRotX(-0.2F * swimWave));
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
