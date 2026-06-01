package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public record WeightedConfiguredFeature(
        float chance,
        Holder<ConfiguredFeature<?, ?>> feature
) {
    public static final Codec<WeightedConfiguredFeature> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.floatRange(0.0F, 1.0F)
                            .fieldOf("chance")
                            .forGetter(WeightedConfiguredFeature::chance),

                    ConfiguredFeature.CODEC
                            .fieldOf("feature")
                            .forGetter(WeightedConfiguredFeature::feature)
            ).apply(instance, WeightedConfiguredFeature::new)
    );
}
