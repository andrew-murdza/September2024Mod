package net.amurdza.examplemod.block;

import net.amurdza.examplemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class CaveVinesHeadNew extends CaveVinesBlock {

    private static final float CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;
    public CaveVinesHeadNew(Properties p_152959_) {
        super(p_152959_);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BlockStateProperties.ENABLED).add(WATERLOGGED);
    }

    protected BlockState updateBodyAfterConvertedFromHead(BlockState head, BlockState body) {
        return body.setValue(BERRIES, head.getValue(BERRIES)).setValue(WATERLOGGED,head.getValue(WATERLOGGED));
    }

    public ItemStack getCloneItemStack(BlockGetter p_152966_, BlockPos p_152967_, BlockState p_152968_) {
        return new ItemStack(ModItems.GLOW_BERRIES.get());
    }

    @Override
    public FluidState getFluidState(final BlockState p_52362_) {
        return p_52362_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_52362_);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext p_54200_) {
        BlockState stateForPlacement=super.getStateForPlacement(p_54200_);
        final FluidState fluidstate = p_54200_.getLevel().getFluidState(p_54200_.getClickedPos());
        return stateForPlacement!=null?stateForPlacement.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER):null;
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir()||state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pEntity, InteractionHand hand, BlockHitResult result){
        return BlockHelper.useCaveVines(pState,pLevel,pPos,pEntity,hand);
    }


    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.relative(this.growthDirection.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (!this.canAttachTo(blockstate)) {
            return false;
        } else {
            boolean sturdy=blockstate.isFaceSturdy(pLevel, blockpos, this.growthDirection)||blockstate.is(BlockTags.LEAVES);
            return blockstate.is(this.getHeadBlock()) || blockstate.is(this.getBodyBlock()) || sturdy;
        }
    }

    protected @NotNull Block getBodyBlock() {
        return ModBlocks.CAVE_VINES_PLANT.get();
    }
    public boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(AGE) < 25 || !pState.getValue(BERRIES)&&pState.getValue(BlockStateProperties.ENABLED);
    }
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(pLevel, pPos.relative(this.growthDirection), pLevel.getBlockState(pPos.relative(this.growthDirection)),pRandom.nextDouble() < CHANCE_OF_BERRIES_ON_GROWTH)) {
            if(!pState.getValue(BERRIES)&&pState.getValue(BlockStateProperties.ENABLED)){
                pLevel.setBlock(pPos, pState.setValue(BERRIES,true), 2);
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
            }
            else{
                if (pState.getValue(AGE) < 25 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(pLevel, pPos.relative(this.growthDirection), pLevel.getBlockState(pPos.relative(this.growthDirection)),pRandom.nextDouble() < 0.1D)) {
                    BlockPos blockpos = pPos.relative(this.growthDirection);
                    if (this.canGrowInto(pLevel.getBlockState(blockpos))) {
                        boolean inWater=pLevel.getFluidState(blockpos).is(FluidTags.WATER);
                        BlockState growIntoState=this.getGrowIntoState(pState, pLevel.random);
                        pLevel.setBlockAndUpdate(blockpos, growIntoState.setValue(WATERLOGGED,inWater));
                        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(pLevel, blockpos, pLevel.getBlockState(blockpos));
                    }
                }
            }
        }
    }
}
