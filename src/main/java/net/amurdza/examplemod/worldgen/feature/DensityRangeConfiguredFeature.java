package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class DensityRangeConfiguredFeature extends Feature<DensityRangeConfiguredFeatureConfiguration> {
    public DensityRangeConfiguredFeature(Codec<DensityRangeConfiguredFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DensityRangeConfiguredFeatureConfiguration> context) {
        DensityRangeConfiguredFeatureConfiguration config = context.config();
        BlockPos origin = context.origin();

        double value = config.density().compute(
                new DensityFunction.SinglePointContext(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ()
                )
        );

        Holder<ConfiguredFeature<?, ?>> selectedFeature = config.getFeature(value);

        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        RandomSource random = context.random();

        return selectedFeature.value().place(
                level,
                chunkGenerator,
                random,
                origin
        );
    }
}