package net.amurdza.examplemod.block;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.ENABLED;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class CaveVinesPlantNew extends CaveVinesPlantBlock {

    public CaveVinesPlantNew(Properties p_153000_) {
        super(p_153000_);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    protected BlockState updateHeadAfterConvertedFromBody(BlockState p_153028_, BlockState p_153029_) {
        return p_153029_.setValue(BERRIES,p_153028_.getValue(BERRIES)).setValue(WATERLOGGED,p_153028_.getValue(WATERLOGGED))
                .setValue(ENABLED,p_153028_.getValue(ENABLED));
    }


    public ItemStack getCloneItemStack(BlockGetter p_152966_, BlockPos p_152967_, BlockState p_152968_) {
        return new ItemStack(ModItems.GLOW_BERRIES.get());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BlockStateProperties.ENABLED).add(WATERLOGGED);
    }
    public boolean isRandomlyTicking(BlockState pState) {
        return !pState.getValue(BERRIES)&&pState.getValue(BlockStateProperties.ENABLED);
    }
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) ModBlocks.CAVE_VINES.get();
    }

    @Override
    public FluidState getFluidState(final BlockState p_52362_) {
        return p_52362_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_52362_);
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.relative(this.growthDirection.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (!this.canAttachTo(blockstate)) {
            return false;
        } else {
            return blockstate.is(this.getHeadBlock()) || blockstate.is(this.getBodyBlock()) ||
                    blockstate.isFaceSturdy(pLevel, blockpos, this.growthDirection)||blockstate.is(BlockTags.LEAVES);
        }
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext p_54200_) {
        BlockState stateForPlacement=super.getStateForPlacement(p_54200_);
        final FluidState fluidstate = p_54200_.getLevel().getFluidState(p_54200_.getClickedPos());
        return stateForPlacement!=null?stateForPlacement.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER):null;
    }

    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pLevel.getRawBrightness(pPos, 0) >= 9 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(pLevel, pPos, pState, pRandom.nextInt(3) == 0)) {
            BlockState blockstate = pState.setValue(BERRIES,true);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(blockstate));
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
        }
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand, BlockHitResult result){
        return BlockHelper.useCaveVines(pState,pLevel,pPos,pEntity,hand);
    }
}
