package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DesertGrassBlock extends TallGrassBlock {
    public DesertGrassBlock(Properties pProperties) {
        super(pProperties);
    }
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        DoublePlantBlock doubleplantblock = (DoublePlantBlock)ModBlocks.DESERT_TALL_GRASS.get();
        if (doubleplantblock.defaultBlockState().canSurvive(pLevel, pPos) && pLevel.isEmptyBlock(pPos.above())) {
            DoublePlantBlock.placeAt(pLevel, doubleplantblock.defaultBlockState(), pPos, 2);
        }
    }
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.SAND) || pState.is(BlockTags.DIRT)
                &&!pState.is(Blocks.GRASS_BLOCK)&&!pState.is(Blocks.MYCELIUM) || pState.is(Blocks.FARMLAND);
    }
}
