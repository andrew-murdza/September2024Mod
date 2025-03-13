package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ModConfiguredFeatures {
    public static final ResourceLocation MOSS_FOREST_FLOOR_LOC=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
            new ResourceLocation(AOEMod.MOD_ID,"rainforest_forest_floor")).location();
    public static final ResourceLocation MOSS_SEAFLOOR_LOC=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(AOEMod.MOD_ID,"rainforest_water_plants_full")).location();
}
