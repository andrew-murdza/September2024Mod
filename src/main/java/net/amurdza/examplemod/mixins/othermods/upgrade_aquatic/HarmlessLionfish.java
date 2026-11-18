package net.amurdza.examplemod.mixins.othermods.upgrade_aquatic;

import com.teamabnormals.upgrade_aquatic.common.entity.animal.Lionfish;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disable the native proximity sting; food effects remain entirely native. */
@Mixin(Lionfish.class)
public final class HarmlessLionfish {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$noContactSting(LivingEntity target, CallbackInfo ci) {
        ci.cancel();
    }
}
