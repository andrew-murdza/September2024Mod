package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EntityEmu.class, remap = false)
public class EmuEggRate {
    @Shadow public int timeUntilNextEgg;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/alexsmobs/entity/EntityEmu;isAlive()Z"
            )
    )
    private boolean aoe$changeEggRate(EntityEmu emu, Operation<Boolean> original) {
        boolean alive = original.call(emu);
        if (!alive) {
            return false;
        }

        int extraTicks = Math.max(0, Helper.computeIncrements(emu.getRandom(), MobConfig.mobGrowthChance(emu)) - 1);
        this.timeUntilNextEgg -= extraTicks;

        return true;
    }
}
