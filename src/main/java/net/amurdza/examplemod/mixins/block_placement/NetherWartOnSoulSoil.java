package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherWartBlock.class)
public class NetherWartOnSoulSoil {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoemod$allowSoulSoil(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) {
            cir.setReturnValue(true);
        }
    }
}
