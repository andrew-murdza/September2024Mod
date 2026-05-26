package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSurfaceRules {

    public static final DeferredRegister<Codec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITIONS =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, AOEMod.MOD_ID);

    public static final RegistryObject<Codec<? extends SurfaceRules.ConditionSource>> NOISE_THRESHOLD_3D =
            MATERIAL_CONDITIONS.register("noise_threshold_3d", NoiseThreshold3DConditionSource.CODEC::codec);


    public static void register(IEventBus bus) {
        MATERIAL_CONDITIONS.register(bus);
    }
}