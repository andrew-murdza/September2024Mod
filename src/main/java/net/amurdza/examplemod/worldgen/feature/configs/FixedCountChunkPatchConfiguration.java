package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record FixedCountChunkPatchConfiguration(
        int count,
        int ySpread,
        Holder<PlacedFeature> feature,
        boolean landOnly
) implements FeatureConfiguration {

    public static final Codec<FixedCountChunkPatchConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.POSITIVE_INT
                            .fieldOf("count")
                            .forGetter(FixedCountChunkPatchConfiguration::count),

                    ExtraCodecs.NON_NEGATIVE_INT
                            .fieldOf("y_spread")
                            .orElse(0)
                            .forGetter(FixedCountChunkPatchConfiguration::ySpread),

                    PlacedFeature.CODEC
                            .fieldOf("feature")
                            .forGetter(FixedCountChunkPatchConfiguration::feature),

                    Codec.BOOL
                            .optionalFieldOf("land_only", true)
                            .forGetter(FixedCountChunkPatchConfiguration::landOnly)
            ).apply(instance, FixedCountChunkPatchConfiguration::new)
    );

    public FixedCountChunkPatchConfiguration {
        if (ySpread != 0 && ySpread != 1) {
            throw new IllegalArgumentException("FixedCountRandomPatchConfiguration only supports y_spread 0 or 1.");
        }

        if (count > 85) {
            throw new IllegalArgumentException("FixedCountRandomPatchConfiguration table assumes count <= 85.");
        }
    }
}
