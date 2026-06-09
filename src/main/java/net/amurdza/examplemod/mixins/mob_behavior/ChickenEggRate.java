package net.amurdza.examplemod.mixins.mob_behavior;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EntityEmu.class, remap = false)
public class ChickenEggRate {
    @Shadow public int timeUntilNextEgg;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/alexsmobs/entity/EntityEmu;isAlive()Z"
            )
    )
    private boolean aoe$changeEggRate(EntityEmu chicken, Operation<Boolean> original) {
        if (!chicken.isAlive()) {
            return false;
        }

        float multiplier = MobConfig.mobGrowthChance(chicken);
        int ticks = Helper.computeIncrements(chicken.getRandom(),multiplier);

        if (ticks > 0) {
            this.timeUntilNextEgg -= ticks;
        }

        return false;
    }
}
