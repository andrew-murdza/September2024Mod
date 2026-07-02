package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AshenWheatCropBlock extends CropBlock {
    public AshenWheatCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.soulSediments);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }

        int age = getAge(state);
        if (age < getMaxAge()) {
            float speed = getGrowthSpeed(this, level, pos);
            if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt((int)(25.0F / speed) + 1) == 0)) {
                level.setBlock(pos, getStateForAge(age + 1), 2);
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
            }
        }
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.ASHEN_WHEAT_SEEDS.get();
    }
}
