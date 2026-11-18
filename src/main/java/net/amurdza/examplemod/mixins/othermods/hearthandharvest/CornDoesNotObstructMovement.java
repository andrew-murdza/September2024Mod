package net.amurdza.examplemod.mixins.othermods.hearthandharvest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains native corn outlines and harvesting while allowing entities and paths through it. */
@Mixin(CornStalkBlock.class)
public abstract class CornDoesNotObstructMovement {
    @Inject(method = "cornCollisionEnabled", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void aoemod$allowMovementThroughCorn(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }
}
