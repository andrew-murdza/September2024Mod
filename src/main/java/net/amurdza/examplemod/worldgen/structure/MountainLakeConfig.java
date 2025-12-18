package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record MountainLakeConfig(
        int minWaterY,
        int maxWaterY,

        int radiusMin,
        int radiusMax,

        int depthMin,
        int depthMax,

        int rampW,                      // ✅ configurable ramp width

        List<BlockState> seafloorBlocks, // ✅ variable length; [0]=waterY-1, [1]=waterY-2, ...
        BlockState rampFill              // ✅ fill block used for the ramp
) implements FeatureConfiguration {

    // Nested codec avoids RecordCodecBuilder.group arity problems
    public record SeafloorBlocks(List<BlockState> layers) {
        public static final Codec<SeafloorBlocks> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.listOf().fieldOf("layers").forGetter(SeafloorBlocks::layers)
        ).apply(i, SeafloorBlocks::new));
    }

    public static final Codec<MountainLakeConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("min_water_y").forGetter(MountainLakeConfig::minWaterY),
            Codec.INT.fieldOf("max_water_y").forGetter(MountainLakeConfig::maxWaterY),

            Codec.INT.fieldOf("radius_min").forGetter(MountainLakeConfig::radiusMin),
            Codec.INT.fieldOf("radius_max").forGetter(MountainLakeConfig::radiusMax),

            Codec.INT.fieldOf("depth_min").forGetter(MountainLakeConfig::depthMin),
            Codec.INT.fieldOf("depth_max").forGetter(MountainLakeConfig::depthMax),

            Codec.INT.fieldOf("ramp_w").forGetter(MountainLakeConfig::rampW),

            SeafloorBlocks.CODEC.fieldOf("seafloor_blocks").forGetter(c -> new SeafloorBlocks(c.seafloorBlocks())),
            BlockState.CODEC.fieldOf("ramp_fill").forGetter(MountainLakeConfig::rampFill)
    ).apply(i, (minW, maxW, rMin, rMax, dMin, dMax, rampW, sf, rampFill) ->
            new MountainLakeConfig(minW, maxW, rMin, rMax, dMin, dMax, rampW, sf.layers(), rampFill)
    ));
}
