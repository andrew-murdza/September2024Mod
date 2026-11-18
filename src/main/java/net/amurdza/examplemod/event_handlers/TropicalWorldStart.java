package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The Overworld remains the save-data owner, but is no longer the starting region. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class TropicalWorldStart {
    private TropicalWorldStart() { }

    @SubscribeEvent
    public static void createSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Level.OVERWORLD)
                || !level.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                        .containsKey(GameplayDimensions.TROPICAL.location())) {
            return;
        }
        // Tropical has not been constructed yet. Defer the real spawn search
        // to prepareLevels, without generating an unused Overworld spawn area.
        event.getSettings().setSpawn(new BlockPos(0, 64, 0), 0.0F);
        event.setCanceled(true);
    }
}
