package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MobsInLava6 {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void aoemod$treatLavaAsWater(Vec3 pTravelVector, CallbackInfo ci) {
        if (!LavaMobs.isLavaMob(this)) {
            return;
        }

        LivingEntity living = (LivingEntity)(Object)this;
        Mob mob = (Mob)(Object)this;

        if (living.isEffectiveAi() && living.isInLava()) {
            living.moveRelative(0.01F, pTravelVector);
            living.move(MoverType.SELF, living.getDeltaMovement());
            living.setDeltaMovement(living.getDeltaMovement().scale(0.9D));

            if (mob.getTarget() == null) {
                living.setDeltaMovement(living.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }

            ci.cancel();
        }
    }



}
