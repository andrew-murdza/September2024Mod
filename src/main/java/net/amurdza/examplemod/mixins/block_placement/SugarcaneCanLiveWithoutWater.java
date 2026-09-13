package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

@Mixin(SugarCaneBlock.class)
public abstract class SugarcaneCanLiveWithoutWater extends Block implements SimpleWaterloggedBlock {
    protected SugarcaneCanLiveWithoutWater(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aoe$setDefaultWaterloggedState(BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void aoe$addWaterloggedProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(WATERLOGGED);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        boolean isBottomBlock = !context.getLevel().getBlockState(pos.below()).is(Blocks.SUGAR_CANE);
        boolean isInWater = context.getLevel().getFluidState(pos).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, isBottomBlock && isInWater);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void aoe$scheduleWaterTick(BlockState state, Direction direction, BlockState neighborState,
                                       LevelAccessor level, BlockPos pos, BlockPos neighborPos,
                                       CallbackInfoReturnable<BlockState> cir) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)
                && SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)
                && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Redirect(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canBeHydrated(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;)Z",
                    remap = false
            )
    )
    private boolean aoe$canGrowWithoutAdjacentWater(BlockState instance, BlockGetter level, BlockPos pos,
                                                     FluidState fluidState, BlockPos fluidPos) {
        if (instance.getValue(WATERLOGGED) && level.getFluidState(pos).getType() == Fluids.WATER) {
            return true;
        }
        if (level instanceof LevelAccessor accessor
                && accessor.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK)) {
            return true;
        }
        return instance.canBeHydrated(level, pos, fluidState, fluidPos);
    }
}
