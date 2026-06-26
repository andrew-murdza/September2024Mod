package net.amurdza.examplemod.registry;

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

    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_DRIP_LEAF = key("small_dripleaf");

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

    public static final ResourceKey<ConfiguredFeature<?,?>> HUGE_GLOW_SHROOM = key("giant_mushrooms/huge_glow_shroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FUNGUS_TREE_FROM_FUNGUS = key("giant_mushrooms/crimson_fungus_tree_from_fungus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FUNGUS_TREE_WORLDGEN = key("giant_mushrooms/crimson_fungus_tree_worldgen");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FUNGUS_TREE_FROM_FUNGUS = key("giant_mushrooms/warped_fungus_tree_from_fungus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FUNGUS_TREE_WORLDGEN = key("giant_mushrooms/warped_fungus_tree_worldgen");

    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_TREE = key("trees/oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_TREE = key("trees/birch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRUCE_TREE = key("trees/spruce");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ACACIA_TREE = key("trees/acacia");

    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/oak_large_tree_from_sapling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/dark_oak_large_tree_from_sapling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MANGROVE_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/mangrove_large_tree_from_sapling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CHERRY_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/cherry_large_tree_from_sapling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/azalea_large_tree_from_sapling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUNGLE_LARGE_TREE_FROM_SAPLING = key("rainforest/tree/jungle_large_tree_from_sapling");
}
