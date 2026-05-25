package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Predicate;

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


    public void placeSkippingFeatures(
            FeaturePlaceContext<RandomSelectionFeatureConfig> context,
            Predicate<Feature<?>> rejectedFeature,
            int maxRerolls
    ) {
        RandomSelectionFeatureConfig config = context.config();
        RandomSource random = context.random();

        RandomCollection<PlacedFeature> features = new RandomCollection<>();

        for (WeightedPlacedFeature weighted : config.features) {
            features.add(weighted.chance, weighted.feature.value());
        }

        for (int i = 0; i < maxRerolls; i++) {
            PlacedFeature placed = features.next(random);

            boolean bad = placed.getFeatures()
                    .map(ConfiguredFeature::feature)
                    .anyMatch(rejectedFeature);

            if (!bad) {
                placed.place(
                        context.level(),
                        context.chunkGenerator(),
                        random,
                        context.origin()
                );
                return;
            }
        }

    }
}
