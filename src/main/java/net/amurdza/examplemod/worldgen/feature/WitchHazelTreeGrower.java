package net.amurdza.examplemod.worldgen.feature;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.configured.BWGOverworldTreeConfiguredFeatures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WitchHazelTreeGrower extends AbstractTreeGrower {
    @Override
    protected @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(@NotNull RandomSource pRandom, boolean pHasFlowers) {
        return BWGOverworldTreeConfiguredFeatures.WITCH_HAZEL3;
    }
}
