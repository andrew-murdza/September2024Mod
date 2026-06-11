package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class KelpFeatureConfig implements FeatureConfiguration {

    public static final Codec<KelpFeatureConfig> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BlockState.CODEC.fieldOf("kelp").forGetter(config -> config.kelp),
                    BlockState.CODEC.fieldOf("kelp_plant").forGetter(config -> config.kelpPlant),
                    Codec.BOOL.optionalFieldOf("lava", false).forGetter(config -> config.lava)
            ).apply(instance, KelpFeatureConfig::new));

    public final BlockState kelp;
    public final BlockState kelpPlant;
    public final boolean lava;

    public KelpFeatureConfig(BlockState kelp, BlockState kelpPlant, boolean lava) {
        this.kelp = kelp;
        this.kelpPlant = kelpPlant;
        this.lava = lava;
    }
}