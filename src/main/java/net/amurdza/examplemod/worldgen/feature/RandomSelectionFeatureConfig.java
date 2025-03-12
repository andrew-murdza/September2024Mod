package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;
import java.util.stream.Stream;

public class RandomSelectionFeatureConfig implements FeatureConfiguration {
    public static final Codec<RandomSelectionFeatureConfig> CODEC = RecordCodecBuilder.create((p_67866_)
            -> p_67866_.group(WeightedPlacedFeature.CODEC.listOf()
                    .fieldOf("features").forGetter((p_161053_) -> p_161053_.features))
            .apply(p_67866_, RandomSelectionFeatureConfig::new));
    public final List<WeightedPlacedFeature> features;

    public RandomSelectionFeatureConfig(List<WeightedPlacedFeature> p_204811_) {
        this.features = p_204811_;
    }

    public Stream<ConfiguredFeature<?, ?>> getFeatures() {
        return this.features.stream().flatMap((p_204814_) -> p_204814_.feature.value().getFeatures());
    }
}
