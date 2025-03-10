package net.amurdza.examplemod.worldgen.dimension;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {
    public static final ResourceKey<LevelStem> AOEDIM_KEY = ResourceKey.create(Registries.LEVEL_STEM,
            new ResourceLocation(AOEMod.MOD_ID, "aoedim"));
    public static final ResourceKey<Level> AOEDIM_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(AOEMod.MOD_ID, "aoedim"));
    public static final ResourceKey<DimensionType> AOEDIM_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            new ResourceLocation(AOEMod.MOD_ID, "aoedim_type"));
}
