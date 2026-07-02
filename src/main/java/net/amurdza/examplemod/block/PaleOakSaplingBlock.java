package net.amurdza.examplemod.block;

import net.amurdza.examplemod.worldgen.PaleOakTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PaleOakSaplingBlock extends SaplingBlock {
    public PaleOakSaplingBlock(Properties properties) {
        super(new PaleOakTreeGrower(), properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }

        if (random.nextInt(7) == 0) {
            this.advanceTree(level, pos, state, random);
        }
    }
}
