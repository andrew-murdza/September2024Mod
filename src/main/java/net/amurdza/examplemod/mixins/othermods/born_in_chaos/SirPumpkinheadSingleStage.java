package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.entity.SirPumpkinheadEntity;
import net.mcreator.borninchaosv.procedures.SerPumpkinheadGProcedure;
import net.mcreator.borninchaosv.procedures.SerPumpkinheadPiProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Sir Pumpkin Head in its original form and disables its summoned mobs. */
@Mixin(
        value = {
                SerPumpkinheadGProcedure.class,
                SerPumpkinheadPiProcedure.class
        },
        remap = false
)
public abstract class SirPumpkinheadSingleStage {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aoemod$disableStageChangeAndSummons(
            LevelAccessor level,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity instanceof SirPumpkinheadEntity) {
            ci.cancel();
        }
    }
}
