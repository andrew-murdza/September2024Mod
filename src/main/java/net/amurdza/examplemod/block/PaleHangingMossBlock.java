package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PaleHangingMossBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    private static final VoxelShape BASE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    private static final VoxelShape TIP_SHAPE = Block.box(1.0D, 2.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public PaleHangingMossBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TIP) ? TIP_SHAPE : BASE_SHAPE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) || above.is(BlockTags.LEAVES) || above.isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return state.setValue(TIP, !level.getBlockState(pos.below()).is(this));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }

    private BlockPos findTip(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (level.getBlockState(cursor.below()).is(this)) {
            cursor.move(Direction.DOWN);
        }
        return cursor.immutable();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean clientSide) {
        BlockPos growthPos = findTip(level, pos).below();
        return level.isEmptyBlock(growthPos) && level.isOutsideBuildHeight(growthPos) == false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos growthPos = findTip(level, pos).below();
        if (level.isEmptyBlock(growthPos)) {
            level.setBlock(growthPos, defaultBlockState(), 3);
        }
    }
}
