package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.scouter.netherdepthsupgrade.blocks.NDUBlocks;
import com.scouter.netherdepthsupgrade.blocks.TallWarpedSeagrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

public class WarpedSeagrassFeature extends Feature<ProbabilityFeatureConfiguration> {
    public WarpedSeagrassFeature(Codec<ProbabilityFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        ProbabilityFeatureConfiguration config = context.config();

        if (!level.getBlockState(pos).is(Blocks.LAVA)) {
            return false;
        }

        boolean tall = random.nextDouble() < config.probability;

        if (tall && level.getBlockState(pos.above()).is(Blocks.LAVA) && level.getBlockState(pos.above().above()).is(Blocks.LAVA)) {
            BlockState lower = NDUBlocks.TALL_WARPED_SEAGRASS.get()
                    .defaultBlockState()
                    .setValue(TallWarpedSeagrassBlock.HALF, DoubleBlockHalf.LOWER);

            BlockState upper = NDUBlocks.TALL_WARPED_SEAGRASS.get()
                    .defaultBlockState()
                    .setValue(TallWarpedSeagrassBlock.HALF, DoubleBlockHalf.UPPER);

            if (lower.canSurvive(level, pos)) {
                level.setBlock(pos, lower, 2);
                level.setBlock(pos.above(), upper, 2);
                return true;
            }
        }

        BlockState shortState = NDUBlocks.WARPED_SEAGRASS.get().defaultBlockState();

        if (shortState.canSurvive(level, pos)) {
            level.setBlock(pos, shortState, 2);
            return true;
        }

        return false;
    }
}
