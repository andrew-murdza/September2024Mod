package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GrowingPlantBlock.class)
public class WeepingVinesOnLeaves {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean hi(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, Direction direction){
        return instance.isFaceSturdy(blockGetter,blockPos,direction)||direction==Direction.DOWN&&
                blockGetter.getBlockState(blockPos).is(BlockTags.LEAVES);
    }
}
