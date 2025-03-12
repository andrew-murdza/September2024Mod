package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RandomSelectionFeature extends Feature<RandomSelectionFeatureConfig> {
    public RandomSelectionFeature(Codec<RandomSelectionFeatureConfig> p_66619_) {
        super(p_66619_);
    }

    public boolean place(FeaturePlaceContext<RandomSelectionFeatureConfig> context) {
        RandomSelectionFeatureConfig randomfeatureconfiguration = context.config();
        RandomSource randomsource = context.random();
        RandomCollection<PlacedFeature> features=new RandomCollection<>();
        for(WeightedPlacedFeature weightedplacedfeature : randomfeatureconfiguration.features) {
            features.add(weightedplacedfeature.chance,weightedplacedfeature.feature.value());
        }
        return features.next(randomsource).place(context.level(),context.chunkGenerator(),randomsource,context.origin());
    }
}
