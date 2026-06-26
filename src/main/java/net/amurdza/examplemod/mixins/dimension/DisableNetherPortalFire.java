package net.amurdza.examplemod.mixins.dimension;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public abstract class DisableNetherPortalFire {

    @Inject(
            method = "inPortalDimension",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aoemod$disableNetherPortalCreation(Level pLevel, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}