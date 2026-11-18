package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.AquaticPlantPlacement;
import net.amurdza.examplemod.worldgen.feature.configs.KelpFeatureConfig;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class KelpFeature extends Feature<KelpFeatureConfig> {

    public KelpFeature(Codec<KelpFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<KelpFeatureConfig> context) {
        KelpFeatureConfig config = context.config();
        return AquaticPlantPlacement.placeKelpColumn(context.level(), context.origin(), config.kelp,
                config.kelpPlant, config.lava, context.random());
    }
}
