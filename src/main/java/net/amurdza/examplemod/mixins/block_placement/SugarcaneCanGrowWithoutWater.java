package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(SugarCaneBlock.class)
    public class SugarcaneCanGrowWithoutWater {
    @Redirect(method = "canSurvive",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;canBeHydrated(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean growWithoutWater(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, FluidState fluidState, BlockPos blockPos1){
        if(blockGetter instanceof LevelAccessor level&&level.getBlockState(blockPos.below()).is(Blocks.MOSS_BLOCK)){
            return true;
        }
        return instance.canBeHydrated(blockGetter, blockPos, fluidState, blockPos1);
    }
}
