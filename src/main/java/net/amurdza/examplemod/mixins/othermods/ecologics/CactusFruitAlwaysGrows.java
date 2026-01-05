package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.amurdza.examplemod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import samebutdifferent.ecologics.registry.ModBlocks;

@Mixin(CactusBlock.class)
public class CactusFruitAlwaysGrows {
    @ModifyConstant(method = "randomTick",constant = @Constant(intValue = 3))
    private int hi(int constant, BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom){
        int i;
        for(i = 1; pLevel.getBlockState(pPos.below(i)).is(Blocks.CACTUS); ++i) {

        }
        if(i>=3){
            if(net.minecraftforge.common.ForgeHooks.onCropsGrowPre(pLevel, pPos.above(), pState,
                    pRandom.nextFloat()<Config.PRICKLY_PEAR_CHANCE)&&pLevel.isEmptyBlock(pPos.above())) {
                pLevel.setBlockAndUpdate(pPos.above(), ModBlocks.PRICKLY_PEAR.get().defaultBlockState());
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
            }
        }
        return 3;
    }
}
