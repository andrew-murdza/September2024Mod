package net.amurdza.examplemod.mixins.othermods.backport;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.SoulCaveClock;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.blackgear.vanillabackport.core.util.WorldUtilities$EnvironmentUtils", remap = false)
public abstract class SoulCaveNight {
    @Inject(method = "isNaturalNight", at = @At("HEAD"), cancellable = true)
    private static void aoemod$caveClock(Level level, CallbackInfoReturnable<Boolean> cir) {
        if (GameplayDimensions.is(level, GameplayDimensions.NETHER)) cir.setReturnValue(SoulCaveClock.isNight(level));
    }
}
