package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record RainforestTreeFeatureConfig(
        int topY,
        BlockState logState,
        BlockState leavesState,
        int caveVinesDistance
) implements FeatureConfiguration {

    public static final Codec<RainforestTreeFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("top_y").forGetter(RainforestTreeFeatureConfig::topY),
                    BlockState.CODEC.fieldOf("log_state").forGetter(RainforestTreeFeatureConfig::logState),
                    BlockState.CODEC.fieldOf("leaves_state").forGetter(RainforestTreeFeatureConfig::leavesState),
                    ExtraCodecs.POSITIVE_INT.fieldOf("cave_vines_distance").forGetter(RainforestTreeFeatureConfig::caveVinesDistance)
            ).apply(instance, RainforestTreeFeatureConfig::new)
    );
}