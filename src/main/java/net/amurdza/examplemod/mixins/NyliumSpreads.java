package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NyliumBlock.class)
public class NyliumSpreads {
    @Shadow
    private static boolean canBeNylium(BlockState pState, LevelReader pReader, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = pReader.getBlockState(blockpos);
        int i = LightEngine.getLightBlockInto(pReader, pState, pPos, blockstate, blockpos, Direction.UP, blockstate.getLightBlock(pReader, blockpos));
        return i < pReader.getMaxLightLevel();
    }
    @Inject(method = "randomTick",at=@At("HEAD"))
    private void hi(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom, CallbackInfo ci){
        if(canBeNylium(pState,pLevel,pPos)){
            if (!pLevel.isAreaLoaded(pPos, 3)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
            BlockState blockstate = pLevel.getBlockState(pPos);
            for(int i = 0; i < 4; ++i) {
                BlockPos blockpos = pPos.offset(pRandom.nextInt(3) - 1, pRandom.nextInt(5) - 3, pRandom.nextInt(3) - 1);
                if (pLevel.getBlockState(blockpos).is(Blocks.NETHERRACK) && aOEMod1_20_1V2$canPropagate(blockstate, pLevel, blockpos)) {
                    pLevel.setBlockAndUpdate(blockpos, blockstate);
                }
            }
        }
    }
    @Unique
    private boolean aOEMod1_20_1V2$canPropagate(BlockState pState, ServerLevel pLevel, BlockPos pPos){
        BlockPos blockpos = pPos.above();
        return canBeNylium(pState, pLevel, pPos) && !pLevel.getFluidState(blockpos).is(FluidTags.WATER);
    }
}
