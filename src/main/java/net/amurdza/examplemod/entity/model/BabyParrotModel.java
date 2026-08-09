package net.amurdza.examplemod.entity.model;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.render.BabyReplacedEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public final class BabyParrotModel extends GeoModel<BabyReplacedEntity<Parrot>> {
    private static final ResourceLocation MODEL = AOEMod.makeID("geo/baby_parrot.geo.json");
    private static final ResourceLocation ANIMATION = AOEMod.makeID("animations/baby_animals.animation.json");
    private static final ResourceLocation RED_BLUE = texture("parrot_red_blue.png");
    private static final ResourceLocation BLUE = texture("parrot_blue.png");
    private static final ResourceLocation GREEN = texture("parrot_green.png");
    private static final ResourceLocation YELLOW_BLUE = texture("parrot_yellow_blue.png");
    private static final ResourceLocation GREY = texture("parrot_grey.png");

    @Override
    public ResourceLocation getModelResource(BabyReplacedEntity<Parrot> animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BabyReplacedEntity<Parrot> animatable) {
        Parrot parrot = animatable.getCurrentEntity();
        return parrot == null ? RED_BLUE : textureForVariant(parrot.getVariant());
    }

    @Override
    public ResourceLocation getAnimationResource(BabyReplacedEntity<Parrot> animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            BabyReplacedEntity<Parrot> animatable,
            long instanceId,
            AnimationState<BabyReplacedEntity<Parrot>> animationState
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Parrot parrot = animatable.getCurrentEntity();
        if (parrot == null) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float partialTick = animationState.getPartialTick();
        float flap = Mth.lerp(partialTick, parrot.oFlap, parrot.flap);
        float flapSpeed = Mth.lerp(partialTick, parrot.oFlapSpeed, parrot.flapSpeed);
        float wingFlap = (Mth.sin(flap) + 1.0F) * flapSpeed;
        float displayedWingFlap = parrot.isInSittingPose() ? 0.0F : wingFlap;
        float limbSwing = animationState.getLimbSwing();
        float limbAmount = animationState.getLimbSwingAmount();

        this.getBone("head").ifPresent(head -> {
            head.setRotX(radians(modelData.headPitch()));
            head.setRotY(radians(modelData.netHeadYaw()));
            head.setRotZ(parrot.isPartyParrot()
                    ? Mth.sin(parrot.tickCount + partialTick) * 0.4F
                    : 0.0F);
        });
        this.getBone("body").ifPresent(body -> body.setRotX(-0.4937F));
        this.getBone("tail").ifPresent(tail -> tail.setRotX(
                parrot.isInSittingPose()
                        ? 1.5388988F
                        : 1.015F + Mth.cos(limbSwing * 0.6662F) * 0.3F * limbAmount
        ));
        this.getBone("left_wing").ifPresent(wing -> {
            wing.setRotX(-0.6981F);
            wing.setRotZ(-0.0873F - displayedWingFlap);
        });
        this.getBone("right_wing").ifPresent(wing -> {
            wing.setRotX(-0.6981F);
            wing.setRotZ(0.0873F + displayedWingFlap);
        });
        this.getBone("left_leg").ifPresent(leg -> leg.setRotX(legRotation(
                parrot,
                Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount
        )));
        this.getBone("right_leg").ifPresent(leg -> leg.setRotX(legRotation(
                parrot,
                Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbAmount
        )));
    }

    private static float legRotation(Parrot parrot, float walkRotation) {
        if (parrot.isInSittingPose()) {
            return 0.9701F;
        }
        return parrot.isFlying() ? 0.6682F : -0.0299F + walkRotation;
    }

    public static ResourceLocation textureForVariant(Parrot.Variant variant) {
        return switch (variant) {
            case RED_BLUE -> RED_BLUE;
            case BLUE -> BLUE;
            case GREEN -> GREEN;
            case YELLOW_BLUE -> YELLOW_BLUE;
            case GRAY -> GREY;
        };
    }

    private static ResourceLocation texture(String filename) {
        return AOEMod.makeID("textures/entity/baby_parrot/" + filename);
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }
}
