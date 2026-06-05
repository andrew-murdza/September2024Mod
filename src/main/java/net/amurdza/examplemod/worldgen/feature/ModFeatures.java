package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES, AOEMod.MOD_ID);

    static {
        FEATURES.register("special_biome_tree", ()->new AOETree(AOETreeConfiguration.CODEC));
        FEATURES.register("seagrass",()->new SeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
        FEATURES.register("kelp",()->new KelpFeature(KelpFeatureConfiguration.CODEC));
        FEATURES.register("all_surface",()->new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC));
        FEATURES.register("random_selector",()->new RandomSelectionFeature(RandomSelectionFeatureConfig.CODEC));
        FEATURES.register("cave_vines",()->new CaveVineColumn(CaveVineConfig.CODEC));
        FEATURES.register("block_column",()->new BlockColumn(BlockColumnConfiguration.CODEC));
        FEATURES.register("glow_lichen",()->new GlowLichenFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register("huge_glow_shroom",()->new GiantGlowshroomFeature(HugeMushroomFeatureConfiguration.CODEC));
        FEATURES.register("crimson_seagrass",()->new CrimsonSeaGrassFeature(ProbabilityFeatureConfiguration.CODEC));
        FEATURES.register("warped_seagrass",()->new WarpedSeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
        FEATURES.register("grid_choice", ()->new GridChoiceFeature(GridChoiceConfig.CODEC));
        FEATURES.register("pixie_village", ()->new PixieVillageFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register("leafcutter_anthill", ()->new LeafcutterAnthillFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register("fixed_count_random_patch", ()->new FixedCountRandomPatchFeature(FixedCountRandomPatchConfiguration.CODEC));
        FEATURES.register("layered_all_surface", ()->new AllSurfaceLayeredFeature(AllSurfaceLayeredFeaturesConfig.CODEC));
        FEATURES.register("density_range", ()->new DensityRangeConfiguredFeature(DensityRangeConfiguredFeatureConfiguration.CODEC));
    }

    public static void register(IEventBus eventBus){
        FEATURES.register(eventBus);
    }

    public static final ResourceKey<ConfiguredFeature<?,?>> HUGE_GLOW_SHROOM = createKey("giant_mushrooms/huge_glow_shroom");
    public static final ResourceKey<ConfiguredFeature<?,?>> HUGE_SNOWCAP_MUSHROOM = createKey("giant_mushrooms/huge_snowcap_mushroom");

    public static ResourceKey<ConfiguredFeature<?, ?>> createKey(String p_255643_) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("aoemod", p_255643_));
    }
}
