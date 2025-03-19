package net.amurdza.examplemod.datagen;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.amurdza.examplemod.worldgen.feature.ModConfiguredFeatures;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.amurdza.examplemod.worldgen.feature.ModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER=new RegistrySetBuilder()
            .add(Registries.FEATURE, ModFeatures::bootstrap).add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap).add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
                .add(Registries.BIOME, ModBiomes::bootstrap);
    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(AOEMod.MOD_ID));
    }
}
