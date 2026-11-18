package net.amurdza.examplemod.worldgen.feature.features;

import net.amurdza.examplemod.worldgen.MushroomCaveVegetation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class MushroomCaveVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public MushroomCaveVegetationFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return MushroomCaveVegetation.place(context.level(), context.chunkGenerator(), new ChunkPos(context.origin()));
    }
}
