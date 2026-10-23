package net.amurdza.examplemod.registry;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public final class ModBiomeSources {
    private static final DeferredRegister<Codec<? extends BiomeSource>> SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, AOEMod.MOD_ID);
    static { SOURCES.register("dimension_regions", () -> DimensionBiomeSource.CODEC); }
    private ModBiomeSources() { }
    public static void register(IEventBus bus) { SOURCES.register(bus); }
}
