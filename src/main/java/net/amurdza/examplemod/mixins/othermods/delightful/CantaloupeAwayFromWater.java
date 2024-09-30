package net.amurdza.examplemod.mixins.othermods.delightful;

import net.brnbrd.delightful.common.block.CantaloupePlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CantaloupePlantBlock.class)
public class CantaloupeAwayFromWater {
    @Redirect(method = "canSurvive", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canBeHydrated(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hi(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, FluidState fluidState, BlockPos blockPos1){
        return true;
    }
}
