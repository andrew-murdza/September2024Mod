package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MountainLakeConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
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
    public static final RegistryObject<Feature<AOETreeConfiguration>> AOE_TREE_FEATURE= FEATURES.register("special_biome_tree", ()->new AOETree(AOETreeConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SEA_PICKLE_POND=FEATURES.register("sea_pickle_pond",()->new SeaPicklePond(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<ProbabilityFeatureConfiguration>> SEAGRASS=FEATURES.register("seagrass",()->new SeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KELP=FEATURES.register("kelp",()->new KelpFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<AllSurfacesFeatureConfig>> ALL_SURFACE=FEATURES.register("all_surface",()->new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC));
    public static final RegistryObject<Feature<RandomSelectionFeatureConfig>> RANDOM_SELECTION=FEATURES.register("random_selector",()->new RandomSelectionFeature(RandomSelectionFeatureConfig.CODEC));
    public static final RegistryObject<Feature<CaveVineConfig>> CAVE_VINES=FEATURES.register("cave_vines",()->new CaveVineColumn(CaveVineConfig.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PITAYA=FEATURES.register("pitaya",()->new PitayaFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<BlockColumnConfiguration>> BLOCK_COLUMN=FEATURES.register("block_column",()->new BlockColumn(BlockColumnConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GLOW_LICHEN=FEATURES.register("glow_lichen",()->new GlowLichenFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GIANT_GLOW_SHROOM=FEATURES.register("huge_glow_shroom",()->new GiantGlowshroomFeature(NoneFeatureConfiguration.CODEC));


    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_LUSHROOM = createKey("huge_lush_mushroom");

    public static ResourceKey<ConfiguredFeature<?, ?>> createKey(String p_255643_) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("aoemod", p_255643_));
    }
}

//public class ModFeatures {
//    public static Feature<AOETreeConfiguration> AOE_TREE_FEATURE;
//    public static Feature<NoneFeatureConfiguration> SEA_PICKLE_POND_FEATURE;
//    public static Feature<ProbabilityFeatureConfiguration> SEAGRASS_FEATURE;
//    public static Feature<NoneFeatureConfiguration> KELP_FEATURE;
//    public static Feature<AllSurfacesFeatureConfig> ALL_SURFACE_FEATURE;
//    public static Feature<RandomSelectionFeatureConfig> RANDOM_SELECTION_FEATURE;
//    public static Feature<CaveVineConfig> CAVE_VINES_FEATURE;
//    public static Feature<NoneFeatureConfiguration> PITAYA_FEATURE;
//    public static final ResourceKey<Feature<?>> AOE_TREE_FEATURE_KEY=registerKey("special_biome_tree");
//    public static final ResourceKey<Feature<?>> SEA_PICKLE_POND_KEY=registerKey("sea_pickle_pond");
//    public static final ResourceKey<Feature<?>> SEAGRASS_KEY=registerKey("seagrass");
//    public static final ResourceKey<Feature<?>> KELP_FEATURE_KEY=registerKey("kelp");
//    public static final ResourceKey<Feature<?>> ALL_SURFACE_KEY=registerKey("all_surface");
//    public static final ResourceKey<Feature<?>> RANDOM_SELECTION_KEY=registerKey("random_selector");
//    public static final ResourceKey<Feature<?>> CAVE_VINES_KEY=registerKey("cave_vines");
//    public static final ResourceKey<Feature<?>> PITAYA_KEY=registerKey("pitaya");
//    public static void bootstrap(BootstapContext<Feature<?>> context) {
//        AOE_TREE_FEATURE=register(context,AOE_TREE_FEATURE_KEY,new AOETree(AOETreeConfiguration.CODEC));
//        SEA_PICKLE_POND_FEATURE=register(context,SEA_PICKLE_POND_KEY,new SeaPicklePond(NoneFeatureConfiguration.CODEC));
//        SEAGRASS_FEATURE=register(context,SEAGRASS_KEY,new SeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
//        KELP_FEATURE=register(context,KELP_FEATURE_KEY,new KelpFeature(NoneFeatureConfiguration.CODEC));
//        ALL_SURFACE_FEATURE=register(context,ALL_SURFACE_KEY,new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC));
//        RANDOM_SELECTION_FEATURE=register(context,RANDOM_SELECTION_KEY,new RandomSelectionFeature(RandomSelectionFeatureConfig.CODEC));
//        CAVE_VINES_FEATURE=register(context,CAVE_VINES_KEY,new CaveVineColumn(CaveVineConfig.CODEC));
//        PITAYA_FEATURE=register(context,PITAYA_KEY,new PitayaFeature(NoneFeatureConfiguration.CODEC));
//    }
//    public static ResourceKey<Feature<?>> registerKey(String name) {
//        return ResourceKey.create(Registries.FEATURE, new ResourceLocation(AOEMod.MOD_ID, name));
//    }
//    private static <T extends FeatureConfiguration> Feature<T> register
//            (BootstapContext<Feature<?>> context,
//             ResourceKey<Feature<?>> key, Feature<T> feature) {
//        context.register(key, feature);
//        return feature;
//    }
//}
