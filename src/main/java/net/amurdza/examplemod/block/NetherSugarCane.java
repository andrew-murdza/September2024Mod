package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class NetherSugarCane extends SugarCaneBlock {
    public NetherSugarCane(Properties pProperties) {
        super(pProperties);
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (blockstate.is(this)) {
            return true;
        } else {
            if (blockstate.is(BlockTags.NYLIUM) || blockstate.is(Blocks.SOUL_SOIL) || blockstate.is(Blocks.NETHERRACK)|| blockstate.is(Blocks.BLACKSTONE) || blockstate.is(Blocks.BASALT) || blockstate.is(Blocks.GRAVEL)) {
                for(Direction direction : Direction.Plane.HORIZONTAL) {
                    FluidState fluidstate = pLevel.getFluidState(blockpos.relative(direction));
                    if(fluidstate.is(Fluids.LAVA)){
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
