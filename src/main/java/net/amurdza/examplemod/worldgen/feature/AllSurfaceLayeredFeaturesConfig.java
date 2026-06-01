package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public class AllSurfaceLayeredFeaturesConfig implements FeatureConfiguration {

    public static final Codec<AllSurfaceLayeredFeaturesConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    AllSurfacesFeatureConfig.CODEC.listOf().fieldOf("features")
                            .forGetter(c -> c.features)
            ).apply(inst, AllSurfaceLayeredFeaturesConfig::new));

    public final List<AllSurfacesFeatureConfig> features;

    public AllSurfaceLayeredFeaturesConfig(List<AllSurfacesFeatureConfig> features) {
        this.features = features;
    }
}