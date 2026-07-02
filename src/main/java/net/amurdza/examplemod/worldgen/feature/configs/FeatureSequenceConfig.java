package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record FeatureSequenceConfig(
        List<Holder<ConfiguredFeature<?, ?>>> features
) implements FeatureConfiguration {

    public static final Codec<FeatureSequenceConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ConfiguredFeature.CODEC.listOf().fieldOf("features")
                            .forGetter(FeatureSequenceConfig::features)
            ).apply(inst, FeatureSequenceConfig::new));
}
