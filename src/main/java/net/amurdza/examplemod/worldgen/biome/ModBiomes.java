package net.amurdza.examplemod.worldgen.biome;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.feature.ModPlacedFeatures;
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
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import terrablender.util.LevelUtils;
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModBiomes {
    public static final ResourceKey<Biome> RAINFOREST_OLD=ResourceKey.create(Registries.BIOME,
            new ResourceLocation(AOEMod.MOD_ID,"rainforest"));
    public static final ResourceKey<Biome> RAINFOREST_KEY=ResourceKey.create(Registries.BIOME,
            new ResourceLocation(AOEMod.MOD_ID,"jungle"));
    public static Biome RAINFOREST;
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        RAINFOREST=event.getServer().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(RAINFOREST_KEY);
    }
}
