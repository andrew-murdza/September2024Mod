package net.amurdza.examplemod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class DebugWorldgenEvents {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        Registry<LevelStem> stems =
                server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);

        ResourceKey<LevelStem> key =
                ResourceKey.create(Registries.LEVEL_STEM,
                        new ResourceLocation("aoemod", "aoedim"));

        LevelStem stem = stems.get(key);

        if (stem != null) {
            ChunkGenerator generator = stem.generator();

            AOEMod.LOGGER.info(
                    "AOE dimension biome source = {}",
                    generator.getBiomeSource().getClass().getName()
            );
        } else {
            AOEMod.LOGGER.warn("Could not find AOE dimension LevelStem");
        }
    }
}