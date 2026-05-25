package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ModConfiguredFeatures {
    // In ModConfiguredFeatures

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String path) {
        return ResourceKey.create(
                Registries.CONFIGURED_FEATURE,
                new ResourceLocation(AOEMod.MOD_ID, path)
        );
    }

    public static final ResourceKey<ConfiguredFeature<?, ?>> PLAINS_FLOWERS = key("plains/flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAVANNA_FLOWERS = key("savanna/flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOUL_SAND_VALLEY_FLOWERS = key("soul_sand_valley/flowers");

    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_DELTAS_FLOOR = key("basalt_deltas/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_DELTAS_SEAFLOOR = key("basalt_deltas/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_DELTAS_SEAFLOOR_SHALLOW = key("basalt_deltas/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FOREST_FLOOR = key("crimson_forest/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FOREST_SEAFLOOR = key("crimson_forest/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FOREST_SEAFLOOR_SHALLOW = key("crimson_forest/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> DESERT_FLOOR = key("desert/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DESERT_SEAFLOOR = key("desert/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DESERT_SEAFLOOR_SHALLOW = key("desert/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MOUNTAINS_FLOOR = key("mountains/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOUNTAINS_SEAFLOOR = key("mountains/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOUNTAINS_SEAFLOOR_SHALLOW = key("mountains/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEP_DARK_FLOOR = key("deep_dark/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEP_DARK_SEAFLOOR = key("deep_dark/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEP_DARK_SEAFLOOR_SHALLOW = key("deep_dark/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_CAVES_FLOOR = key("mushroom_caves/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_CAVES_SEAFLOOR = key("mushroom_caves/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_CAVES_SEAFLOOR_SHALLOW = key("mushroom_caves/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> PLAINS_FLOOR = key("plains/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PLAINS_SEAFLOOR = key("plains/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PLAINS_SEAFLOOR_SHALLOW = key("plains/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_FLOOR = key("rainforest/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_SEAFLOOR = key("rainforest/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_SEAFLOOR_SHALLOW = key("rainforest/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> SAVANNA_FLOOR = key("savanna/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAVANNA_SEAFLOOR = key("savanna/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAVANNA_SEAFLOOR_SHALLOW = key("savanna/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> SOUL_SAND_VALLEY_FLOOR = key("soul_sand_valley/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOUL_SAND_VALLEY_SEAFLOOR = key("soul_sand_valley/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOUL_SAND_VALLEY_SEAFLOOR_SHALLOW = key("soul_sand_valley/water/water_plants_shallow_full");

    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FOREST_FLOOR = key("warped_forest/floor/forest_floor");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FOREST_SEAFLOOR = key("warped_forest/water/water_plants_full");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FOREST_SEAFLOOR_SHALLOW = key("warped_forest/water/water_plants_shallow_full");
}
