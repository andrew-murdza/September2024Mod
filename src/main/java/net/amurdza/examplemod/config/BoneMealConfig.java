package net.amurdza.examplemod.config;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoneMealConfig {
    public static final Map<ResourceKey<Level>, BonemealMultipliers> DIMENSION_TO_BONEMEAL_MULTIPLIERS = new LinkedHashMap<>();

    public static void registerDimension(ResourceKey<Level> dimension, float plant, float mushroom){
        DIMENSION_TO_BONEMEAL_MULTIPLIERS.put(dimension,new BonemealMultipliers(plant, mushroom));
    }

    static {
        registerDimension(GameplayDimensions.TROPICAL, 3, 1.5f);
        registerDimension(GameplayDimensions.SAVANNA, 2, 1);
        registerDimension(GameplayDimensions.PLAINS, 1, 1);
        registerDimension(GameplayDimensions.SNOWY, 1, 2);
        registerDimension(GameplayDimensions.DESERT, 1, 1);
        registerDimension(GameplayDimensions.NETHER, 1, 1);
    }

    public record BonemealMultipliers(float plant, float mushroom) {}
}
