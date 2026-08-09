package net.amurdza.examplemod.mixins.othermods.snowrealmagic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import snownee.snow.Hooks;

@Mixin(Hooks.class)
public abstract class SnowDoesNotMeltFromLight {

    @ModifyConstant(
            method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;F)V",
            constant = {
                    @Constant(intValue = 10),
                    @Constant(intValue = 11)
            },
            remap = false
    )
    private static int aoemod$raiseSnowRealMagicLightMeltThreshold(int originalThreshold) {
        return 15;
    }
}
