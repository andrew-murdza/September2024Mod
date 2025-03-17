package net.amurdza.examplemod.datagen;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.amurdza.examplemod.worldgen.feature.ModConfiguredFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER=new RegistrySetBuilder();//.add(Registries.BIOME, ModBiomes::bootstrap)
            //.add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(AOEMod.MOD_ID));
    }
}
