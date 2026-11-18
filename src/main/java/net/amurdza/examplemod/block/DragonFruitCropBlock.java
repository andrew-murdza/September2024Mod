package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.RetainedDragonFruit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;

public final class DragonFruitCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty ROPELOGGED = BooleanProperty.create("ropelogged");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public DragonFruitCropBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(ROPELOGGED, false));
    }

    public static boolean isFarmland(BlockState state) {
        return state.getBlock() instanceof FarmBlock;
    }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return isFarmland(state); }
    @Override public IntegerProperty getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return 3; }
    @Override protected ItemLike getBaseSeedId() { return RetainedDragonFruit.SEEDS.get(); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE, ROPELOGGED); }
    @Override public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isMaxAge(state)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(RetainedDragonFruit.FRUIT.get(), 1 + level.random.nextInt(2)));
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1, .8F + level.random.nextFloat() * .4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9) return;
        if (!isMaxAge(state) && ForgeHooks.onCropsGrowPre(level, pos, state,
                random.nextInt((int) (25 / getGrowthSpeed(this, level, pos)) + 1) == 0)) {
            level.setBlock(pos, state.setValue(AGE, getAge(state) + 1), Block.UPDATE_CLIENTS);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
        attemptRopeClimb(level, pos, random);
    }

    public void attemptRopeClimb(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= .2F || !level.getBlockState(pos.above()).is(vectorwing.farmersdelight.common.tag.ModTags.ROPES)) return;
        int height = 1;
        while (height < 3 && level.getBlockState(pos.below(height)).is(this)) height++;
        if (height < 3) level.setBlockAndUpdate(pos.above(), defaultBlockState().setValue(ROPELOGGED, true));
    }

    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(AGE, Math.min(getMaxAge(), getAge(state) + Math.max(1, getBonemealAgeIncrease(level) / 2))));
        attemptRopeClimb(level, pos, random);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(ROPELOGGED) ? level.getBlockState(pos.below()).is(this)
                && (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos)) : super.canSurvive(state, level, pos);
    }
    @Override public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return state.getValue(ROPELOGGED) && state.is(BlockTags.CLIMBABLE);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) level.scheduleTick(pos, this, 1);
        return state;
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            if (state.getValue(ROPELOGGED)) restoreRope(level, pos);
        }
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        if (state.getValue(ROPELOGGED)) restoreRope(level, pos);
    }
    private static void restoreRope(Level level, BlockPos pos) {
        Block rope = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(vectorwing.farmersdelight.common.Configuration.DEFAULT_TOMATO_VINE_ROPE.get()));
        if (rope == null || rope == net.minecraft.world.level.block.Blocks.AIR) rope = vectorwing.farmersdelight.common.registry.ModBlocks.ROPE.get();
        level.setBlockAndUpdate(pos, rope.defaultBlockState());
    }
}
