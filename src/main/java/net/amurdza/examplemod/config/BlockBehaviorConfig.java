package net.amurdza.examplemod.config;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockBehaviorConfig {
    private BlockBehaviorConfig() {}

    public static final Map<ResourceKey<Level>, Float> DIMENSION_TO_GLOW_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<ResourceKey<Level>, Float> DIMENSION_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS = new LinkedHashMap<>();
    public static final Map<ResourceKey<Level>, Float> DIMENSION_TO_MATURE_SWEET_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<ResourceKey<Level>, Float> DIMENSION_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS = new LinkedHashMap<>();
    public static final Map<ResourceKey<Level>, Float> DIMENSION_TO_MATURE_SOUL_BERRY_AMOUNTS = new LinkedHashMap<>();
    public static final Map<ResourceKey<Level>, Integer> DIMENSION_TO_GRASS_SPREAD_NUM_TRIES = new LinkedHashMap<>();

    static {
        DIMENSION_TO_GLOW_BERRY_AMOUNTS.put(GameplayDimensions.TROPICAL, 2f);

        DIMENSION_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(GameplayDimensions.TROPICAL, 2f);
        DIMENSION_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(GameplayDimensions.SNOWY, 1f);

        DIMENSION_TO_SOUL_BERRIES_PARTIALLY_GROWN_AMOUNTS.put(GameplayDimensions.NETHER, 1f);

        DIMENSION_TO_MATURE_SWEET_BERRY_AMOUNTS.put(GameplayDimensions.TROPICAL, 4f);
        DIMENSION_TO_MATURE_SWEET_BERRY_AMOUNTS.put(GameplayDimensions.SNOWY, 2f);

        DIMENSION_TO_MATURE_SOUL_BERRY_AMOUNTS.put(GameplayDimensions.NETHER, 2f);

        DIMENSION_TO_GRASS_SPREAD_NUM_TRIES.put(GameplayDimensions.TROPICAL, 12);
        DIMENSION_TO_GRASS_SPREAD_NUM_TRIES.put(GameplayDimensions.SAVANNA, 8);
        DIMENSION_TO_GRASS_SPREAD_NUM_TRIES.put(GameplayDimensions.PLAINS, 4);
        DIMENSION_TO_GRASS_SPREAD_NUM_TRIES.put(GameplayDimensions.SNOWY, 4);
    }

    public static final int RICH_FARMLAND_HYDRATION_DISTANCE = 14;

    public static final int MAX_MUSHROOMS_FOR_GROWTH = 5;
    public static final double PLACE_CHORUS_FLOWER_CHANCE = 0.1;
}
