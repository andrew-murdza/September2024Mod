package net.amurdza.examplemod.mixins.block_behavior.light;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockBehaviour.class)
public class MossDoesntBlockLightPart3 {
    @Redirect(method = "getLightBlock",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    protected boolean hi(BlockState instance, BlockGetter blockGetter, BlockPos blockPos){
        return instance.isSolidRender(blockGetter,blockPos)&&!instance.is(Blocks.MOSS_BLOCK);
    }
}
