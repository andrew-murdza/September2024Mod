package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinNoGolems {
    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void aoemod$disableVanillaCarvedPumpkinGolems(Level level, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }
}
