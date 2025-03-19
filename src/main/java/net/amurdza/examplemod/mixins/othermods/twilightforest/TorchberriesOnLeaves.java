package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.block.TFPlantBlock;
import twilightforest.block.TorchberryPlantBlock;

@Mixin(TorchberryPlantBlock.class)
public class TorchberriesOnLeaves {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Ltwilightforest/block/TFPlantBlock;canPlaceRootAt(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hi(LevelReader reader, BlockPos pos, BlockState state, LevelReader reader1, BlockPos pos1){
        return TFPlantBlock.canPlaceRootAt(reader, pos)||reader.getBlockState(pos.above()).is(BlockTags.LEAVES);
    }
}
