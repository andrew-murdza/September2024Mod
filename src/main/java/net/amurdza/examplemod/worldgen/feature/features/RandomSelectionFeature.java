package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.RandomCollection;
import net.amurdza.examplemod.worldgen.feature.configs.RandomSelectionFeatureConfig;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Predicate;

public class RandomSelectionFeature extends Feature<RandomSelectionFeatureConfig> {
    public RandomSelectionFeature(Codec<RandomSelectionFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RandomSelectionFeatureConfig> context) {
        RandomSelectionFeatureConfig config = context.config();
        RandomSource random = context.random();

        RandomCollection<Holder<ConfiguredFeature<?, ?>>> features = new RandomCollection<>();

        for (WeightedConfiguredFeature weighted : config.features()) {
            features.add(weighted.chance(), weighted.feature());
        }

        Holder<ConfiguredFeature<?, ?>> selected = features.next(random);
        PlacedFeature placed = new PlacedFeature(selected, config.placement());

        return placed.place(
                context.level(),
                context.chunkGenerator(),
                random,
                context.origin()
        );
    }

    public void placeSkippingFeatures(
            FeaturePlaceContext<RandomSelectionFeatureConfig> context,
            Predicate<Feature<?>> rejectedFeature,
            int maxRerolls
    ) {
        RandomSelectionFeatureConfig config = context.config();
        RandomSource random = context.random();

        RandomCollection<Holder<ConfiguredFeature<?, ?>>> features = new RandomCollection<>();

        for (WeightedConfiguredFeature weighted : config.features()) {
            features.add(weighted.chance(), weighted.feature());
        }

        for (int i = 0; i < maxRerolls; i++) {
            Holder<ConfiguredFeature<?, ?>> selected = features.next(random);
            ConfiguredFeature<?, ?> configured = selected.value();

            if (!rejectedFeature.test(configured.feature())) {
                PlacedFeature placed = new PlacedFeature(selected, config.placement());

                placed.place(
                        context.level(),
                        context.chunkGenerator(),
                        random,
                        context.origin()
                );
                return;
            }
        }
    }
}
