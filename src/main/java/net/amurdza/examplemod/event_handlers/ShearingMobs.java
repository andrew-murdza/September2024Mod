package net.amurdza.examplemod.event_handlers;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class ShearingMobs {
    @SubscribeEvent
    public static void shearingMobs(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            if(mob instanceof MushroomCow){
                if(event.getItemStack().is(Items.SHEARS)){
                    event.setCanceled(true);
                }
            }
        }
    }
}
