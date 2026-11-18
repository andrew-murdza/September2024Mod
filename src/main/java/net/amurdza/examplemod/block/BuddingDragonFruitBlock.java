package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.RetainedDragonFruit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.BuddingBushBlock;

/** Four budding stages precede the harvestable vine, preserving the source crop's lifecycle. */
public final class BuddingDragonFruitBlock extends BuddingBushBlock implements BonemealableBlock {
    public BuddingDragonFruitBlock(Properties properties) { super(properties); }

    @Override protected ItemLike getBaseSeedId() { return RetainedDragonFruit.SEEDS.get(); }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return DragonFruitCropBlock.isFarmland(state);
    }
    @Override public boolean canGrowPastMaxAge() { return true; }
    @Override public void growPastMaxAge(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlockAndUpdate(pos, RetainedDragonFruit.CROP.get().defaultBlockState());
    }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return true; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(7, getAge(state) + random.nextInt(4) + 1);
        level.setBlockAndUpdate(pos, age <= getMaxAge() ? getStateForAge(age)
                : RetainedDragonFruit.CROP.get().getStateForAge(age - getMaxAge() - 1));
    }
}
