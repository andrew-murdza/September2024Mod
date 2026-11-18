package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.block.BullKelpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BullKelpBlock.class)
public abstract class BullKelpGrowth extends GrowingPlantHeadBlock {
    protected BullKelpGrowth(Properties properties, Direction direction, VoxelShape shape, boolean fluidTicks, double chance) {
        super(properties, direction, shape, fluidTicks, chance);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 25;
    }

    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void aoemod$submergedHead(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if (!aoemod$deepEnough(context.getLevel(), context.getClickedPos())) cir.setReturnValue(null);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void aoemod$maturePlacedHead(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue().hasProperty(AGE))
            cir.setReturnValue(cir.getReturnValue().setValue(AGE, 25));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return level.getFluidState(pos.above(2)).is(FluidTags.WATER)
                && super.isValidBonemealTarget(level, pos, state, client);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!level.getFluidState(pos.above(2)).is(FluidTags.WATER)) return;
        super.performBonemeal(level, random, pos, state);
        BlockState head = level.getBlockState(pos.above());
        if (head.is((BullKelpBlock) (Object) this)) level.setBlock(pos.above(), head.setValue(AGE, 25), 2);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return aoemod$deepEnough(level, pos) && super.canSurvive(state, level, pos);
    }

    private static boolean aoemod$deepEnough(LevelReader level, BlockPos pos) {
        if (!level.getFluidState(pos.above()).is(FluidTags.WATER)
                || !level.getFluidState(pos.above()).isSource()) return false;
        BlockPos bottom = pos;
        while (bottom.getY() > level.getMinBuildHeight() && level.getFluidState(bottom.below()).is(FluidTags.WATER)) bottom = bottom.below();
        return net.amurdza.examplemod.worldgen.AquaticPlantPlacement.waterColumnHeight(level, bottom, 3) >= 3;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(Blocks.WATER) && above.getFluidState().isSource()
                && level.getFluidState(pos.above(2)).is(FluidTags.WATER)) {
            // Mature generated heads must stay at their chosen height.
            super.randomTick(state, level, pos, random);
        }
    }
}
