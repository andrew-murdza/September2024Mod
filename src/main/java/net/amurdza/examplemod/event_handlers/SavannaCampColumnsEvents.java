package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SavannaCampColumnsEvents {
    private SavannaCampColumnsEvents() { }

    @SubscribeEvent
    public static void load(ChunkDataEvent.Load event) {
        var data = event.getData();
        SavannaCampColumns.load(event.getChunk(), data.contains(SavannaCampColumns.NBT_KEY, Tag.TAG_LONG_ARRAY)
                ? data.getLongArray(SavannaCampColumns.NBT_KEY) : null);
        SavannaCampColumns.loadVegetation(event.getChunk(), data.contains(SavannaCampColumns.VEGETATION_NBT_KEY, Tag.TAG_LONG_ARRAY)
                ? data.getLongArray(SavannaCampColumns.VEGETATION_NBT_KEY) : null);
    }

    @SubscribeEvent
    public static void save(ChunkDataEvent.Save event) {
        long[] columns = SavannaCampColumns.save(event.getChunk());
        if (columns != null) event.getData().putLongArray(SavannaCampColumns.NBT_KEY, columns);
        long[] vegetation = SavannaCampColumns.saveVegetation(event.getChunk());
        if (vegetation != null) event.getData().putLongArray(SavannaCampColumns.VEGETATION_NBT_KEY, vegetation);
    }
}
