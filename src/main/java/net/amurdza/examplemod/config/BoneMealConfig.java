package net.amurdza.examplemod.config;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoneMealConfig {
    public static final Map<TagKey<Biome>, BiomeBonemealMultipliers> BIOME_TAG_TO_BONEMEAL_MULTIPLIERS = new LinkedHashMap<>();

    public static void registerBiome(TagKey<Biome> biome, float plant, float mushroom){
        BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.put(biome,new BiomeBonemealMultipliers(plant, mushroom));
    }

    static {
        registerBiome(ModTags.Biomes.tropicalBiomes, 3.0f, 1.5f);
        registerBiome(ModTags.Biomes.savannaBiomes, 2.0f, 1.0f);
        registerBiome(ModTags.Biomes.plainsBiomes, 1.0f, 1.0f);
        registerBiome(ModTags.Biomes.mushroomCaves, 0.5f, 2.0f);
        registerBiome(ModTags.Biomes.mountainBiomes, 0.5f, 1.0f);
        registerBiome(ModTags.Biomes.desertBiomes, 0.2f, 0.6f);
        registerBiome(ModTags.Biomes.deepDarkBiomes, 0.2f, 0.2f);
        registerBiome(ModTags.Biomes.soulSandValleyBiomes, 0.2f, 0.1f);
        registerBiome(ModTags.Biomes.warpedForestBiomes, 0.3f, 0.15f);
        registerBiome(ModTags.Biomes.crimsonForestBiomes, 0.3f, 0.15f);
        registerBiome(ModTags.Biomes.basaltDeltasBiomes, 0.2f, 0.1f);
    }

    public record BiomeBonemealMultipliers(float plant, float mushroom) {}
}
