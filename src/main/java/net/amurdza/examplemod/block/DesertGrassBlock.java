package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DesertGrassBlock extends TallGrassBlock {
    public DesertGrassBlock(Properties pProperties) {
        super(pProperties);
    }
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        Block tallGrass = ModBlocks.DESERT_TALL_GRASS.get();
        if (tallGrass.defaultBlockState().canSurvive(pLevel, pPos)) {
            pLevel.setBlock(pPos,tallGrass.defaultBlockState(),2);
        }
    }
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.SAND);
    }
}
