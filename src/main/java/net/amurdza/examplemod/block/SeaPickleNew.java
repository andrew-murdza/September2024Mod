package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

public class SeaPickleNew extends SeaPickleBlock {

    public SeaPickleNew(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(PICKLES)<4&&pState.getValue(WATERLOGGED);
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if(ForgeHooks.onCropsGrowPre(pLevel,pPos,pState, pRandom.nextInt(4)==0)){
            pLevel.setBlockAndUpdate(pPos,pState.cycle(PICKLES));
        }
    }
}
