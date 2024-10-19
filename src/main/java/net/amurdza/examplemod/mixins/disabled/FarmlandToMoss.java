package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.FarmBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FarmBlock.class)
public class FarmlandToMoss {
//    @Redirect(method = "turnToDirt",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
//    private static BlockState hi(Block instance, @Nullable Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos){
//        return Helper.isSpecialBiome(pLevel,pPos)? Blocks.MOSS_BLOCK.defaultBlockState():instance.defaultBlockState();
//    }
}
