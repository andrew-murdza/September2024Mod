package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record FixedCountRandomPatchConfiguration(
        int count,
        int xzSpread,
        int ySpread,
        Holder<PlacedFeature> feature
) implements FeatureConfiguration {

    public static final Codec<FixedCountRandomPatchConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.POSITIVE_INT
                            .fieldOf("count")
                            .forGetter(FixedCountRandomPatchConfiguration::count),

                    ExtraCodecs.NON_NEGATIVE_INT
                            .fieldOf("xz_spread")
                            .orElse(7)
                            .forGetter(FixedCountRandomPatchConfiguration::xzSpread),

                    ExtraCodecs.NON_NEGATIVE_INT
                            .fieldOf("y_spread")
                            .orElse(0)
                            .forGetter(FixedCountRandomPatchConfiguration::ySpread),

                    PlacedFeature.CODEC
                            .fieldOf("feature")
                            .forGetter(FixedCountRandomPatchConfiguration::feature)
            ).apply(instance, FixedCountRandomPatchConfiguration::new)
    );

    public FixedCountRandomPatchConfiguration {
        if (ySpread != 0 && ySpread != 1) {
            throw new IllegalArgumentException("FixedCountRandomPatchConfiguration only supports y_spread 0 or 1.");
        }

        if (count > 85) {
            throw new IllegalArgumentException("FixedCountRandomPatchConfiguration table assumes count <= 85.");
        }
    }
}