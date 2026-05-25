package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record MountainLakeConfig(
        int waterY,
        int radius,
        int depth,
        int rampW,
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
            Codec.INT.fieldOf("water_y").forGetter(MountainLakeConfig::waterY),

            Codec.INT.fieldOf("radius").forGetter(MountainLakeConfig::radius),

            Codec.INT.fieldOf("depth").forGetter(MountainLakeConfig::depth),

            Codec.INT.fieldOf("ramp_w").forGetter(MountainLakeConfig::rampW),

            SeafloorBlocks.CODEC.fieldOf("seafloor_blocks").forGetter(c -> new SeafloorBlocks(c.seafloorBlocks())),
            BlockState.CODEC.fieldOf("ramp_fill").forGetter(MountainLakeConfig::rampFill)
    ).apply(i, (w, r, d, rampW, sf, rampFill) ->
            new MountainLakeConfig(w, r, d, rampW, sf.layers(), rampFill)
    ));
}
