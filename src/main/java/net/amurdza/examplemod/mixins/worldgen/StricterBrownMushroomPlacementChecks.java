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
    private void aoemod$checkBrownMushroomCanopyLayers(
            int pY,
            int pMaxHeight,
            int pFoliageRadius,
            int pCurrentY,
            CallbackInfoReturnable<Integer> cir
    ) {
        int radius = 0;

        if (pCurrentY < pMaxHeight + 2 && pCurrentY >= pMaxHeight - 3) {
            radius = pFoliageRadius;
        }

        cir.setReturnValue(radius);
    }
}
