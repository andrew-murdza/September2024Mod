package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.feature.configs.*;
import net.amurdza.examplemod.worldgen.feature.features.*;
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
        FEATURES.register("kelp",()->new KelpFeature(KelpFeatureConfig.CODEC));
        FEATURES.register("all_surface",()->new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC));
        FEATURES.register("random_selector",()->new RandomSelectionFeature(RandomSelectionFeatureConfig.CODEC));
        FEATURES.register("block_column",()->new BlockColumn(BlockColumnConfiguration.CODEC));
        FEATURES.register("glow_lichen",()->new GlowLichenFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register("huge_glow_shroom",()->new GiantGlowshroomFeature(HugeMushroomFeatureConfiguration.CODEC));
        FEATURES.register("crimson_seagrass",()->new CrimsonSeaGrassFeature(ProbabilityFeatureConfiguration.CODEC));
        FEATURES.register("warped_seagrass",()->new WarpedSeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
        FEATURES.register("grid_choice", ()->new GridChoiceFeature(GridChoiceConfig.CODEC));
        FEATURES.register("regional_grid_choice", ()->new RegionalGridChoiceFeature(RegionalGridChoiceConfig.CODEC));
        FEATURES.register("feature_sequence", ()->new FeatureSequenceFeature(FeatureSequenceConfig.CODEC));
        FEATURES.register("fixed_count_random_patch", ()->new FixedCountRandomPatchFeature(FixedCountRandomPatchConfiguration.CODEC));
        FEATURES.register("layered_all_surface", ()->new AllSurfaceLayeredFeature(AllSurfaceLayeredFeaturesConfig.CODEC));
        FEATURES.register("rainforest_tree", ()->new RainforestTreeFeature(RainforestTreeFeatureConfig.CODEC));
        FEATURES.register("cave_vines", ()->new CaveVineColumn(CaveVineConfig.CODEC));
        FEATURES.register("sugar_cane", ()->new SugarCaneFeature(NoneFeatureConfiguration.CODEC));
        FEATURES.register("chunk_patch", ()->new FixedCountChunkFeature(FixedCountChunkPatchConfiguration.CODEC));
    }

    public static void register(IEventBus eventBus){
        FEATURES.register(eventBus);
    }
}
