package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.entity.LordPumpkinheadEntity;
import net.mcreator.borninchaosv.procedures.LordPumpkinheadPriObnovlieniiTikaSushchnostiProcedure;
import net.mcreator.borninchaosv.procedures.LordPumpkinheadPriRanieniiSushchnostiProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes Lord Pumpkin Head's arena-block placement, mounted/second-stage
 * transformations, and damage-triggered legacy minion roster.
 */
@Mixin(
        value = {
                LordPumpkinheadPriObnovlieniiTikaSushchnostiProcedure.class,
                LordPumpkinheadPriRanieniiSushchnostiProcedure.class
        },
        remap = false
)
public abstract class LordPumpkinheadOriginalProcedures {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aoemod$disableOriginalBossProcedure(
            LevelAccessor level,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity instanceof LordPumpkinheadEntity) {
            ci.cancel();
        }
    }
}
