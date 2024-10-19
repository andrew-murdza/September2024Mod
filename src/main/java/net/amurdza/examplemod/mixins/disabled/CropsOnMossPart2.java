package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StemBlock.class)
public class CropsOnMossPart2 {
//    @Redirect(method = "mayPlaceOn",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
//    private boolean hi(BlockState instance, Block block, BlockState pState, BlockGetter pLevel, BlockPos pPos){
//        return instance.is(block)||instance.is(Blocks.MOSS_BLOCK);
//    }
}
