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
import vectorwing.farmersdelight.common.registry.ModBlocks;

@Mixin(value = NetherWartBlock.class, priority = 1001)
public class NetherwartPlacement {
    @Inject(method = "mayPlaceOn",at=@At(value = "RETURN"),cancellable = true)
    private void hi(BlockState pState, BlockGetter pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(pState.is(Blocks.MOSS_BLOCK)||pState.is(ModBlocks.RICH_SOIL.get())
                ||pState.is(ModBlocks.RICH_SOIL_FARMLAND.get())||pState.is(Blocks.SOUL_SAND));
                //|| pState.is(ModTags.Blocks.netherRootsPlaceable));
    }
}
