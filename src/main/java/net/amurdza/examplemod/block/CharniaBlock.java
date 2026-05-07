package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CharniaBlock extends BushBlock implements LiquidBlockContainer {

    public CharniaBlock(BlockBehaviour.Properties p_154496_) {
        super(p_154496_);
    }
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 14, 12);

    public VoxelShape getShape(BlockState p_154525_, BlockGetter p_154526_, BlockPos p_154527_, CollisionContext p_154528_) {
        return SHAPE;
    }

    protected boolean mayPlaceOn(BlockState p_154539_, BlockGetter p_154540_, BlockPos p_154541_) {
        return p_154539_.isFaceSturdy(p_154540_, p_154541_, Direction.UP) && !p_154539_.is(Blocks.MAGMA_BLOCK);
    }

    public BlockState updateShape(BlockState p_154530_, Direction p_154531_, BlockState p_154532_, LevelAccessor p_154533_, BlockPos p_154534_, BlockPos p_154535_) {
        BlockState blockstate = super.updateShape(p_154530_, p_154531_, p_154532_, p_154533_, p_154534_, p_154535_);
        if (!blockstate.isAir()) {
            p_154533_.scheduleTick(p_154534_, Fluids.WATER, Fluids.WATER.getTickDelay(p_154533_));
        }

        return blockstate;
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext p_154503_) {
        FluidState fluidstate = p_154503_.getLevel().getFluidState(p_154503_.getClickedPos());
        return fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8 ? super.getStateForPlacement(p_154503_) : null;
    }

    public FluidState getFluidState(BlockState p_154537_) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        return false;
    }
}
