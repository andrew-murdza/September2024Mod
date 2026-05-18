package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.block.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.CaveVines.BERRIES;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.ENABLED;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

// Cave vines are waterloggeable
// Cave vines grow berries unless hit with axes
// Cave vines can be placed beneath leaves
// Caves vines can give more berries when harvested
@Mixin(CaveVinesPlantBlock.class)
public abstract class CaveVinePlantBehavior extends GrowingPlantBodyBlock implements SimpleWaterloggedBlock {

    protected CaveVinePlantBehavior(Properties pProperties, Direction pGrowthDirection, VoxelShape pShape, boolean pScheduleFluidTicks) {
        super(pProperties, pGrowthDirection, pShape, pScheduleFluidTicks);
    }

    @Shadow protected abstract @NotNull GrowingPlantHeadBlock getHeadBlock();

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void aoe$addProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(ENABLED).add(WATERLOGGED);
    }

    @Inject(method = "updateHeadAfterConvertedFromBody", at = @At("RETURN"), cancellable = true)
    private void aoe$updateHeadAfterConvertedFromBody(BlockState body, BlockState head,
                                                      CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(
                cir.getReturnValue()
                        .setValue(BERRIES, body.getValue(BERRIES))
                        .setValue(WATERLOGGED, body.getValue(WATERLOGGED))
                        .setValue(ENABLED, body.getValue(ENABLED))
        );
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state=super.getStateForPlacement(ctx);
        assert state != null;
        return state.setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attachPos = pos.relative(this.growthDirection.getOpposite());
        BlockState attachState = level.getBlockState(attachPos);
        return super.canSurvive(state,level,pos) || attachState.is(BlockTags.LEAVES);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(BERRIES) && state.getValue(BlockStateProperties.ENABLED);
    }

    @Override
    public void randomTick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getRawBrightness(pos, 0) >= 9
                && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(
                level,
                pos,
                state,
                random.nextInt(3) == 0
        )) {
            BlockState newState = state.setValue(BERRIES, true);

            level.setBlock(pos, newState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));

            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void aoe$use(BlockState state, Level level, BlockPos pos, Player player,
                         InteractionHand hand, BlockHitResult hit,
                         CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(BlockHelper.useCaveVines(state, level, pos, player, hand));
    }
}
