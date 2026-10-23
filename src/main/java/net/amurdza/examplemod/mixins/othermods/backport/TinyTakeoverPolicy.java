package net.amurdza.examplemod.mixins.othermods.backport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.evandev.tiny_takeover_backport.config.ModConfig", remap = false)
public abstract class TinyTakeoverPolicy {
    @Shadow public boolean enableRabbit;
    @Shadow public boolean replaceAdultRabbit;
    @Shadow public boolean rabbitBoundingBox;
    @Shadow public boolean spawnBabyDolphin;

    @Inject(method = "get", at = @At("RETURN"))
    private static void aoe$preserveRabbitAppearance(CallbackInfoReturnable<Object> cir) {
        TinyTakeoverPolicy config = (TinyTakeoverPolicy) cir.getReturnValue();
        config.enableRabbit = false;
        config.replaceAdultRabbit = false;
        // The exception is visual; retain Tiny Takeover's updated rabbit hitbox.
        config.rabbitBoundingBox = true;
        // AOE already persists dolphin age and controls natural babies/breeding.
        config.spawnBabyDolphin = false;
    }
}
