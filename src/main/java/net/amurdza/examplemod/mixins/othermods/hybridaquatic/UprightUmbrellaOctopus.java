package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import dev.hybridlabs.hapi.client.model.entity.aquatic.BaseOctopusEntityModel;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseOctopusEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimationState;

/** Keep the umbrella's bell upright while its native tentacle animation swims. */
@Mixin(value = BaseOctopusEntityModel.class, remap = false)
public abstract class UprightUmbrellaOctopus {
    @Inject(method = "setCustomAnimations(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseOctopusEntity;JLsoftware/bernie/geckolib/core/animation/AnimationState;)V",
            at = @At("TAIL"))
    private void aoemod$uprightBell(BaseOctopusEntity entity, long instanceId, AnimationState<?> state, CallbackInfo ci) {
        if (!(entity instanceof UmbrellaOctopusEntity)) return;
        var bone = ((BaseOctopusEntityModel<?>) (Object) this).getAnimationProcessor().getBone("octopus");
        if (bone != null) {
            bone.setRotX(0);
            bone.setRotZ(0);
        }
    }
}
