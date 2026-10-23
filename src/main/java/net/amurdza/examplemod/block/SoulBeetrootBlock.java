package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

public class SoulBeetrootBlock extends BeetrootBlock {
    public SoulBeetrootBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        var biome = level.getBiome(pos);
        return mayPlaceOn(level.getBlockState(below), level, below)
                && (biome.is(ModTags.Biomes.soulSandValleyBiomes) || biome.is(Biomes.SOUL_SAND_VALLEY));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.SOUL_BEETROOT_SEEDS.get();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean clientSide) {
        return canSurvive(state, level, pos) && super.isValidBonemealTarget(level, pos, state, clientSide);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || !canSurvive(state, level, pos) || isMaxAge(state)) {
            return;
        }

        // Match beetroot's best normal growth rate: a 2/3 attempt and a 1/3 growth roll.
        // Soul sediment needs neither hydration nor spacing, so neighboring crops do not affect this rate.
        if (random.nextInt(3) != 0 && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(3) == 0)) {
            level.setBlock(pos, getStateForAge(getAge(state) + 1), 2);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }
}
