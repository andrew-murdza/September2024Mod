package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES, AOEMod.MOD_ID);
    public static void register(IEventBus eventBus){
        FEATURES.register(eventBus);
    }
    public static final RegistryObject<Feature<?>> AOE_TREE_FEATURE= FEATURES.register("special_biome_tree", ()->new AOETree(AOETreeConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> SEA_PICKLE_POND=FEATURES.register("sea_pickle_pond",()->new SeaPicklePond(NoneFeatureConfiguration.CODEC));
}
