package net.amurdza.examplemod.config;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockBehaviorConfig {
    private BlockBehaviorConfig() {}

    public static final Map<TagKey<Biome>, Float> BIOME_TO_GLOW_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_FARMLAND_DISTANCE = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_GRASS_SPREAD_NUM_TRIES = new LinkedHashMap<>();

    static {
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.tropicalBiomes, 2f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.savannaBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.plainsBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.mountainBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.mushroomCaves, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.desertBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.deepDarkBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.soulSandValleyBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.warpedForestBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.crimsonForestBiomes, 0f);
        BIOME_TO_GLOW_BERRY_AMOUNTS.put(ModTags.Biomes.basaltDeltasBiomes, 0f);


        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.tropicalBiomes, 2f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.savannaBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.plainsBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.mountainBiomes, 1f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.mushroomCaves, 1f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.desertBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.deepDarkBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.soulSandValleyBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.warpedForestBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.crimsonForestBiomes, 0f);
        BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.basaltDeltasBiomes, 0f);

        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.tropicalBiomes, 0f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.savannaBiomes, 0f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.plainsBiomes, 0f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.mountainBiomes, 0f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.mushroomCaves, 0f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.desertBiomes, 1f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.deepDarkBiomes, 1f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.soulSandValleyBiomes, 1f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.warpedForestBiomes, 1f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.crimsonForestBiomes, 1f);
        BIOME_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(ModTags.Biomes.basaltDeltasBiomes, 1f);


        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.tropicalBiomes, 4f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.savannaBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.plainsBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.mountainBiomes, 2f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.mushroomCaves, 2f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.desertBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.deepDarkBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.soulSandValleyBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.warpedForestBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.crimsonForestBiomes, 0f);
        BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.put(ModTags.Biomes.basaltDeltasBiomes, 0f);

        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.tropicalBiomes, 0f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.savannaBiomes, 0f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.plainsBiomes, 0f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.mountainBiomes, 0f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.mushroomCaves, 0f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.desertBiomes, 1f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.deepDarkBiomes, 1f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.soulSandValleyBiomes, 2f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.warpedForestBiomes, 1f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.crimsonForestBiomes, 1f);
        BIOME_TO_MATURE_SOUL_BERRY_AMOUNTS.put(ModTags.Biomes.basaltDeltasBiomes, 1f);


        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.tropicalBiomes, 14);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.savannaBiomes, 8);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.plainsBiomes, 4);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.mountainBiomes, 4);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.mushroomCaves, 4);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.desertBiomes, 2);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.deepDarkBiomes, 0);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.soulSandValleyBiomes, 0);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.warpedForestBiomes, 0);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.crimsonForestBiomes, 0);
        BIOME_TO_FARMLAND_DISTANCE.put(ModTags.Biomes.basaltDeltasBiomes, 0);


        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.tropicalBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.savannaBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.plainsBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.mountainBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.mushroomCaves, 8);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.desertBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.deepDarkBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.soulSandValleyBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.warpedForestBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.crimsonForestBiomes, 0);
        BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES.put(ModTags.Biomes.basaltDeltasBiomes, 0);


        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.tropicalBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.savannaBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.plainsBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.mountainBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.mushroomCaves, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.desertBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.deepDarkBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.soulSandValleyBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.warpedForestBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.crimsonForestBiomes, 0);
        BIOME_TO_GRASS_SPREAD_NUM_TRIES.put(ModTags.Biomes.basaltDeltasBiomes, 0);
    }

    public static final int MAX_MUSHROOMS_FOR_GROWTH = 5;
    public static final double PLACE_CHORUS_FLOWER_CHANCE = 0.1;
}
