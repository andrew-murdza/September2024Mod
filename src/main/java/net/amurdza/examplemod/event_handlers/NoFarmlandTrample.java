package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class NoFarmlandTrample {
    @SubscribeEvent
    public static void noTrample(BlockEvent.FarmlandTrampleEvent event) {
        event.setCanceled(true);
    }
}
