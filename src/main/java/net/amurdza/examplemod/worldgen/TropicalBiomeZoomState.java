package net.amurdza.examplemod.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** Keeps Tropical's column rule when world generation replaces the lookup source. */
public interface TropicalBiomeZoomState {
    void aoemod$setTropicalColumns(boolean tropical);
    void aoemod$setTropicalLayout(TropicalLayout layout, Holder<Biome> mangrove, Holder<Biome> rainforest,
                                Holder<Biome> cherry);
}
