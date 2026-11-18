package net.amurdza.examplemod.client;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.TropicalBiomeSync;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class TropicalBiomeConnection {
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TropicalBiomeSync.clearClientLayout();
    }
}
