package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.world.level.levelgen.feature.HugeBrownMushroomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HugeBrownMushroomFeature.class)
public abstract class StricterBrownMushroomPlacementChecks {

    @Inject(
            method = "getTreeRadiusForHeight",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoemod$checkTopThreeBrownMushroomLayers(
            int pY,
            int pMaxHeight,
            int pFoliageRadius,
            int pCurrentY,
            CallbackInfoReturnable<Integer> cir
    ) {
        /*
         * Your AbstractHugeMushroomFeature mixin changes the call so that
         * pMaxHeight is the real mushroom height instead of -1.
         *
         * Brown mushrooms only place their cap at the top, but this makes
         * placement validation reserve the top 3 layers instead of only
         * being permissive below the cap.
         */
        if (pMaxHeight >= 0 && pCurrentY >= pMaxHeight - 2) {
            cir.setReturnValue(pFoliageRadius);
        } else {
            cir.setReturnValue(0);
        }
    }
}