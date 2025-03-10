package net.amurdza.examplemod.biome;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

public class ModTerrablender {
    public static void registerBiomes() {
        Regions.register(new ModOverworldRegion(new ResourceLocation(AOEMod.MOD_ID, "overworld"), 30));
    }
}
