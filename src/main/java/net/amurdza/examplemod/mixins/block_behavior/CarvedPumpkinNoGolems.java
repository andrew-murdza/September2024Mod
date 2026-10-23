package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Let every HEAD hook (including copper-golem construction) run before blocking vanilla patterns.
@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinNoGolems {
    @Inject(method = "trySpawnGolem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;getOrCreateSnowGolemFull()Lnet/minecraft/world/level/block/state/pattern/BlockPattern;"), cancellable = true)
    private void aoemod$disableVanillaCarvedPumpkinGolems(Level level, BlockPos pos, CallbackInfo ci) {
        if (GameplayDimensions.isCustom(level)) ci.cancel();
    }
}
