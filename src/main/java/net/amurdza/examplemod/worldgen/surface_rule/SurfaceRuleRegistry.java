package net.amurdza.examplemod.worldgen.surface_rule;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;

public class SurfaceRuleRegistry {

    @SubscribeEvent
    public static void onRegistry(final RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.MATERIAL_CONDITION)) {
            event.register(
                    Registries.MATERIAL_CONDITION,
                    new ResourceLocation(AOEMod.MOD_ID, "noise_threshold_3d"),
                    NoiseThreshold3DConditionSource.CODEC::codec
            );
        }
    }
}
