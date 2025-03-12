package net.amurdza.examplemod.worldgen.biome;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.entity.MobCategory;

public class ModBiomes {
    public static final ResourceKey<Biome> RAINFOREST_OLD=ResourceKey.create(Registries.BIOME,
            new ResourceLocation(AOEMod.MOD_ID,"rainforest"));
    public static final ResourceKey<Biome> RAINFOREST=ResourceKey.create(Registries.BIOME,
            new ResourceLocation(AOEMod.MOD_ID,"rainforestnew"));
    public static void bootstrap(BootstapContext<Biome> context){
        context.register(RAINFOREST_OLD, rainforestBiome(context));
    }

    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder builder) {
        BiomeDefaultFeatures.addSurfaceFreezing(builder);
    }


    private static Biome rainforestBiome(BootstapContext<Biome> context) {
        BiomeGenerationSettings.Builder biomeBuilder =
                new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
        globalOverworldGeneration(biomeBuilder);
        biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_PLAINS);

        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));


        BiomeSpecialEffects effects = (new BiomeSpecialEffects.Builder()).waterColor(937679).waterFogColor(329011).skyColor(7907327).fogColor(12638463).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build();

        return new Biome.BiomeBuilder().hasPrecipitation(true).downfall(1.0F)
                .temperature(1.0F).generationSettings(biomeBuilder.build()).mobSpawnSettings(spawnBuilder.build()).specialEffects(effects).build();
    }
}
