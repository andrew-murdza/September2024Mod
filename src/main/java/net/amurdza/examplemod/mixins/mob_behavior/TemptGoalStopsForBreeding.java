package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.entity.ai.DolphinBreedingAccess;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public class TemptGoalStopsForBreeding {
    @Final
    @Shadow
    private PathfinderMob mob;

    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void aoemod$stopTemptingWhenInLove(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Animal animal && animal.isInLove()) {
            cir.setReturnValue(false);
        } else if (this.mob instanceof DolphinBreedingAccess dolphin && dolphin.aoe$isInLove()) {
            cir.setReturnValue(false);
        }
    }
}
