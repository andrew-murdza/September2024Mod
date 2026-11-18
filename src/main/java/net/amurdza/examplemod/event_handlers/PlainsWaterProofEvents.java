package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.PlainsWaterProof;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge exposes custom NBT for both unfinished and completed chunks. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlainsWaterProofEvents {
    private PlainsWaterProofEvents() { }

    @SubscribeEvent
    public static void load(ChunkDataEvent.Load event) {
        PlainsWaterProof.load(event.getChunk(), event.getData().getLongArray(PlainsWaterProof.NBT_KEY));
    }

    @SubscribeEvent
    public static void save(ChunkDataEvent.Save event) {
        long[] paths = PlainsWaterProof.save(event.getChunk());
        if (paths.length > 0) event.getData().putLongArray(PlainsWaterProof.NBT_KEY, paths);
    }
}
