package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(FarmBlock.class)
public class FarmlandToMoss {
    @Redirect(method = "turnToDirt",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState hi(Block instance, @Nullable Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos){
        return Helper.isSpecialBiome(pLevel,pPos)? Blocks.MOSS_BLOCK.defaultBlockState():instance.defaultBlockState();
    }
}
