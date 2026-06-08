package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.minecraftforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = samebutdifferent.ecologics.forge.EcologicsForge.class, remap = false)
public class CactusFruitAlwaysGrowsPart1 {

    @Inject(
            method = "onCropGrow",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void aoemod$disablePricklyPearGrowth(BlockEvent.CropGrowEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
