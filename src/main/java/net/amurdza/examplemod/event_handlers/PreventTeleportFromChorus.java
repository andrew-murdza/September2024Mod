package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class PreventTeleportFromChorus {
    @SubscribeEvent
    public static void noTeleportationChorusFruit(EntityTeleportEvent.ChorusFruit event){
        if(event.getEntity().isShiftKeyDown()|| !(event.getEntity() instanceof Player)){
            event.setCanceled(true);
        }
    }
}
