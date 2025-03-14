package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
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
    public static final RegistryObject<Feature<?>> SEAGRASS=FEATURES.register("seagrass",()->new SeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> KELP=FEATURES.register("kelp",()->new KelpFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> ALL_SURFACE=FEATURES.register("all_surface",()->new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC));
    public static final RegistryObject<Feature<?>> RANDOM_SELECTION=FEATURES.register("random_selector",()->new RandomSelectionFeature(RandomSelectionFeatureConfig.CODEC));
    public static final RegistryObject<Feature<?>> CAVE_VINES=FEATURES.register("cave_vines",()->new CaveVineColumn(CaveVineConfig.CODEC));
    public static final RegistryObject<Feature<?>> PITAYA=FEATURES.register("pitaya",()->new PitayaFeature(NoneFeatureConfiguration.CODEC));

}
