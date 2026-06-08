package net.amurdza.examplemod.mixins.othermods.ecologics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
@Mixin(CactusBlock.class)
public abstract class CactusAlwaysGrowsFruitPart2 {

    /**
     * Makes ForgeHooks.onCropsGrowPre run even when the cactus is already 3+ blocks tall.
     * Vanilla only calls onCropsGrowPre inside `if (i < 3)`.
     * This version calls it whenever the block above is empty, then only performs vanilla growth
     * when `i < 3`.
     * @author amurdza
     * @reason simplest way to get the wanted behavior.
     */
    @Overwrite
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockPos blockpos = pPos.above();
        if (pLevel.isEmptyBlock(blockpos)) {
            if(ForgeHooks.onCropsGrowPre(pLevel, blockpos, pState, true)){
                ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
            }
        }
    }
}
