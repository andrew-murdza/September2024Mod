package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = samebutdifferent.ecologics.block.AzaleaLogBlock.class, remap = false)
public abstract class AzaleaLogsDoNotFlower {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$doNotFlower(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$notRandomlyTicking(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
