package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.block.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.block.CaveVines.BERRIES;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.ENABLED;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

@Mixin(CaveVinesBlock.class)
public abstract class CaveVineHeadBehavior extends GrowingPlantHeadBlock implements SimpleWaterloggedBlock {
    @Unique
    private static final float AOE_CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;

    protected CaveVineHeadBehavior(Properties pProperties, Direction pGrowthDirection, VoxelShape pShape, boolean pScheduleFluidTicks, double pGrowPerTickProbability) {
        super(pProperties, pGrowthDirection, pShape, pScheduleFluidTicks, pGrowPerTickProbability);
    }


    @Shadow protected abstract @NotNull Block getBodyBlock();

    @Shadow protected abstract @NotNull BlockState getGrowIntoState(@NotNull BlockState state, @NotNull RandomSource random);

    @Shadow protected abstract boolean canGrowInto(@NotNull BlockState state);

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void aoe$addProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(ENABLED).add(WATERLOGGED);
    }

    @Inject(method = "updateBodyAfterConvertedFromHead", at = @At("RETURN"), cancellable = true)
    private void aoe$updateBodyAfterConvertedFromHead(BlockState head, BlockState body,
                                                      CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(
                cir.getReturnValue()
                        .setValue(BERRIES, head.getValue(BERRIES))
                        .setValue(WATERLOGGED, head.getValue(WATERLOGGED))
        );
    }

    @Inject(method = "canGrowInto", at = @At("HEAD"), cancellable = true)
    private void aoe$canGrowInto(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.isAir() || state.getFluidState().is(FluidTags.WATER));
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void aoe$use(BlockState state, Level level, BlockPos pos, Player player,
                         InteractionHand hand, BlockHitResult hit,
                         CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(BlockHelper.useCaveVines(state, level, pos, player, hand));
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState stateAtGrowthSide = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(this.growthDirection));

        BlockState state = !stateAtGrowthSide.is(this) && !stateAtGrowthSide.is(this.getBodyBlock())
                ? this.getStateForPlacement(ctx.getLevel())
                : this.getBodyBlock().defaultBlockState();

        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());

        return state.setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attachPos = pos.relative(this.growthDirection.getOpposite());
        BlockState attachState = level.getBlockState(attachPos);
        return super.canSurvive(state,level,pos)||attachState.is(BlockTags.LEAVES);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 25
                || (!state.getValue(BERRIES) && state.getValue(BlockStateProperties.ENABLED));
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(BERRIES) && state.getValue(BlockStateProperties.ENABLED)&&net.minecraftforge.common.ForgeHooks.onCropsGrowPre(
                    level,
                    pos.relative(this.growthDirection),
                    level.getBlockState(pos.relative(this.growthDirection)),
                    random.nextDouble() < AOE_CHANCE_OF_BERRIES_ON_GROWTH
            )) {
                BlockState newState = state.setValue(BERRIES, true);
                level.setBlock(pos, newState, 2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
            } else if (state.getValue(AGE) < 25 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(
                    level,
                    pos.relative(this.growthDirection),
                    level.getBlockState(pos.relative(this.growthDirection)),
                    random.nextDouble() < 0.1D
            )) {
                BlockPos growPos = pos.relative(this.growthDirection);

                if (this.canGrowInto(level.getBlockState(growPos))) {
                    boolean inWater = level.getFluidState(growPos).is(FluidTags.WATER);

                    BlockState growState = this.getGrowIntoState(state, level.random)
                            .setValue(WATERLOGGED, inWater);

                    level.setBlockAndUpdate(growPos, growState);

                    net.minecraftforge.common.ForgeHooks.onCropsGrowPost(
                            level,
                            growPos,
                            level.getBlockState(growPos)
                    );
                }
            }
    }}
