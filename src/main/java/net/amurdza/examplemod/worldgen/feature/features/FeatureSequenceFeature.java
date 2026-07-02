package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.FeatureSequenceConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class FeatureSequenceFeature extends Feature<FeatureSequenceConfig> {

    public FeatureSequenceFeature(Codec<FeatureSequenceConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FeatureSequenceConfig> context) {
        boolean placedAnything = false;

        for (Holder<ConfiguredFeature<?, ?>> feature : context.config().features()) {
            placedAnything |= feature.value().place(
                    context.level(),
                    context.chunkGenerator(),
                    context.random(),
                    context.origin()
            );
        }

        return placedAnything;
    }
}
