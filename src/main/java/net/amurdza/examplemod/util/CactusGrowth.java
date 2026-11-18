package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import samebutdifferent.ecologics.registry.ModBlocks;

/** One growth path for ordinary and accelerated cactus ticks. */
public final class CactusGrowth {
    private CactusGrowth() { }

    public static void advance(ServerLevel level, BlockPos top, BlockState state, int increments) {
        if (increments <= 0 || !state.is(Blocks.CACTUS) || !level.isEmptyBlock(top.above())) return;
        boolean tallEnough = level.getBlockState(top.below()).is(Blocks.CACTUS)
                && level.getBlockState(top.below(2)).is(Blocks.CACTUS);
        if (tallEnough) {
            if (level.dimension().equals(GameplayDimensions.DESERT)) {
                level.setBlockAndUpdate(top.above(), ModBlocks.PRICKLY_PEAR.get().defaultBlockState());
            }
            return;
        }

        int age = state.getValue(CactusBlock.AGE) + increments;
        if (age > 15) {
            level.setBlockAndUpdate(top, state.setValue(CactusBlock.AGE, 0));
            level.setBlockAndUpdate(top.above(), Blocks.CACTUS.defaultBlockState());
        } else {
            level.setBlock(top, state.setValue(CactusBlock.AGE, age), 4);
        }
    }
}
