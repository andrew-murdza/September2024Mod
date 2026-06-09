package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherSproutsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetherSproutsBlock.class,priority = 1001)
public class NetherSproutsPlacement {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    protected void mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(pState.is(Blocks.WARPED_NYLIUM));
    }
}
