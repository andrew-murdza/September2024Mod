package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SculkWallScanFeatureConfig(
        BlockState wallState,
        BlockState backingState,
        int minY,
        int maxY,
        int biomeCheckY,
        float chance,
        int backingDepth
) implements FeatureConfiguration {

    public static final Codec<SculkWallScanFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("wall_state").forGetter(SculkWallScanFeatureConfig::wallState),
                    BlockState.CODEC.fieldOf("backing_state").forGetter(SculkWallScanFeatureConfig::backingState),
                    Codec.INT.optionalFieldOf("min_y", 0).forGetter(SculkWallScanFeatureConfig::minY),
                    Codec.INT.optionalFieldOf("max_y", 32).forGetter(SculkWallScanFeatureConfig::maxY),
                    Codec.INT.optionalFieldOf("biome_check_y", 40).forGetter(SculkWallScanFeatureConfig::biomeCheckY),
                    Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(SculkWallScanFeatureConfig::chance),
                    Codec.INT.optionalFieldOf("backing_depth", 1).forGetter(SculkWallScanFeatureConfig::backingDepth)
            ).apply(instance, SculkWallScanFeatureConfig::new)
    );
}