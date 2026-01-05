package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DesertTallGrassBlock extends DoublePlantBlock {
    public DesertTallGrassBlock(Properties pProperties) {
        super(pProperties);
    }

    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.SAND) || pState.is(BlockTags.DIRT)
                &&!pState.is(Blocks.GRASS_BLOCK)&&!pState.is(Blocks.MYCELIUM) || pState.is(Blocks.FARMLAND);
    }
}
