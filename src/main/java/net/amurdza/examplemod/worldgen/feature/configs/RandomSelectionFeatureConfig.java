package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.worldgen.feature.features.WeightedConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class RandomSelectionFeatureConfig implements FeatureConfiguration {
    public static final Codec<RandomSelectionFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PlacementModifier.CODEC
                            .listOf()
                            .fieldOf("placement")
                            .orElse(List.of())
                            .forGetter(RandomSelectionFeatureConfig::placement),

                    WeightedConfiguredFeature.CODEC
                            .listOf()
                            .fieldOf("features")
                            .forGetter(RandomSelectionFeatureConfig::features)
            ).apply(instance, RandomSelectionFeatureConfig::new)
    );

    public final List<PlacementModifier> placement;
    public final List<WeightedConfiguredFeature> features;

    public RandomSelectionFeatureConfig(
            List<PlacementModifier> placement,
            List<WeightedConfiguredFeature> features
    ) {
        this.placement = placement;
        this.features = features;
    }

    public List<PlacementModifier> placement() {
        return placement;
    }

    public List<WeightedConfiguredFeature> features() {
        return features;
    }
}
