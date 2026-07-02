package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class PaleOakTreeGrower extends AbstractMegaTreeGrower {
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, AOEMod.makeID("pale_oak"));

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return null;
    }

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random) {
        return PALE_OAK;
    }
}
