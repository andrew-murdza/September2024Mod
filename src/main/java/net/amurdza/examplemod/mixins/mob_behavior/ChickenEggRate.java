package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public class ChickenEggRate {
    @Shadow public int eggTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void aoe$changeEggRate(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (chicken.level().isClientSide || !chicken.isAlive() || chicken.isBaby() || chicken.isChickenJockey()) {
            return;
        }

        int extraTicks = Math.max(0, Helper.computeIncrements(chicken.getRandom(), MobConfig.mobGrowthChance(chicken)) - 1);
        this.eggTime -= extraTicks;
    }
}
