package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class ChunkHabitats {
    @SubscribeEvent
    public static void read(ChunkDataEvent.Load event) {
        var chunk = event.getChunk();
        SurfaceHabitatColumns.read(chunk, event.getData());
        if (chunk instanceof LevelChunk full && full.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && SurfaceHabitatColumns.data(full).aoemod$getHabitats() == null)
            SurfaceHabitatColumns.capture(level, full, true);
    }

    @SubscribeEvent
    public static void write(ChunkDataEvent.Save event) {
        SurfaceHabitatColumns.write(event.getChunk(), event.getData());
    }
}
