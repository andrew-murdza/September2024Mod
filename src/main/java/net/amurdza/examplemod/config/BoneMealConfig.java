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
        registerBiome(ModTags.Biomes.tropicalBiomes, 3, 1.5f);
        registerBiome(ModTags.Biomes.savannaBiomes, 2, 1);
        registerBiome(ModTags.Biomes.plainsBiomes, 1, 1);
        registerBiome(ModTags.Biomes.mountainBiomes, 1, 2);
        registerBiome(ModTags.Biomes.desertBiomes, 1, 1);
        registerBiome(ModTags.Biomes.deepDarkBiomes, 1, 1);
        registerBiome(ModTags.Biomes.soulSandValleyBiomes, 1, 1);
        registerBiome(ModTags.Biomes.warpedForestBiomes, 1, 1);
        registerBiome(ModTags.Biomes.crimsonForestBiomes, 1, 1);
        registerBiome(ModTags.Biomes.basaltDeltasBiomes, 1, 1);
    }

    public record BiomeBonemealMultipliers(float plant, float mushroom) {}
}
