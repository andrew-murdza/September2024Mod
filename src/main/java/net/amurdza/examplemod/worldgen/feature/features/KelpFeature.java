package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.KelpFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class KelpFeature extends Feature<KelpFeatureConfig> {

    public KelpFeature(Codec<KelpFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<KelpFeatureConfig> context) {
        int placed = 0;

        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();
        KelpFeatureConfig config = context.config();

        BlockState kelp = config.kelp;
        BlockState kelpPlant = config.kelpPlant;

        if (matchesFluid(level, pos, config.lava)) {
            int height = 1 + random.nextInt(10);

            for (int i = 0; i <= height; ++i) {
                if (
                        matchesFluid(level, pos, config.lava)
                                && matchesFluid(level, pos.above(), config.lava)
                                && kelpPlant.canSurvive(level, pos)
                ) {
                    if (i == height) {
                        level.setBlock(pos, withAgeIfPossible(kelp, random), 2);
                        placed++;
                    } else {
                        level.setBlock(pos, kelpPlant, 2);
                    }
                } else if (i > 0) {
                    BlockPos topPos = pos.below();

                    if (
                            kelp.canSurvive(level, topPos)
                                    && !level.getBlockState(topPos.below()).is(kelp.getBlock())
                    ) {
                        level.setBlock(topPos, withAgeIfPossible(kelp, random), 2);
                        placed++;
                    }

                    break;
                }

                pos = pos.above();
            }
        }

        return placed > 0;
    }

    private static boolean matchesFluid(WorldGenLevel level, BlockPos pos, boolean lava) {
        return lava
                ? level.getBlockState(pos).getFluidState().is(FluidTags.LAVA)
                : level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    private static BlockState withAgeIfPossible(BlockState state, RandomSource random) {
        if (state.hasProperty(KelpBlock.AGE)) {
            return state.setValue(KelpBlock.AGE, random.nextInt(4) + 20);
        }

        return state;
    }
}