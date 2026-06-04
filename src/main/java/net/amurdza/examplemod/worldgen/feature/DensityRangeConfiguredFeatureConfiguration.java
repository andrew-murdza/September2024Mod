package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record DensityRangeConfiguredFeatureConfiguration(
        List<Holder<ConfiguredFeature<?, ?>>> features,
        double minValue,
        double maxValue,
        DensityFunction density
) implements FeatureConfiguration {

    public static final Codec<DensityRangeConfiguredFeatureConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    ConfiguredFeature.CODEC.listOf()
                            .fieldOf("features")
                            .forGetter(DensityRangeConfiguredFeatureConfiguration::features),

                    Codec.DOUBLE
                            .fieldOf("min_value")
                            .forGetter(DensityRangeConfiguredFeatureConfiguration::minValue),

                    Codec.DOUBLE
                            .fieldOf("max_value")
                            .forGetter(DensityRangeConfiguredFeatureConfiguration::maxValue),

                    DensityFunction.HOLDER_HELPER_CODEC
                            .fieldOf("density")
                            .forGetter(DensityRangeConfiguredFeatureConfiguration::density)
            ).apply(instance, DensityRangeConfiguredFeatureConfiguration::new));

    public DensityRangeConfiguredFeatureConfiguration {
        if (features.isEmpty()) {
            throw new IllegalArgumentException("DensityRangeConfiguredFeatureConfiguration requires at least one configured feature");
        }

        if (maxValue <= minValue) {
            throw new IllegalArgumentException("max_value must be greater than min_value");
        }

        features = List.copyOf(features);
    }

    public int getIndex(double value) {
        double normalized = (value - minValue) / (maxValue - minValue);

        int index = (int) Math.floor(normalized * features.size());

        if (index < 0) {
            return 0;
        }

        if (index >= features.size()) {
            return features.size() - 1;
        }

        return index;
    }

    public Holder<ConfiguredFeature<?, ?>> getFeature(double value) {
        return features.get(getIndex(value));
    }
}