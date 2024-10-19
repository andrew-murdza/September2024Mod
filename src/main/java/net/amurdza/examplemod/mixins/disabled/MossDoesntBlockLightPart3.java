package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.class)
public class MossDoesntBlockLightPart3 {
//    @Redirect(method = "getLightBlock",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
//    protected boolean hi(BlockState instance, BlockGetter blockGetter, BlockPos blockPos){
//        return instance.isSolidRender(blockGetter,blockPos)&&!instance.is(Blocks.MOSS_BLOCK);
//    }
}
