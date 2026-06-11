package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> AOEDIM_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(AOEMod.MOD_ID, "aoedim"));
}
