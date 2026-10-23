package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla portals and Immersive Portals both consult this Forge travel hook. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class NetherHoglinTravel {
    private NetherHoglinTravel() { }

    @SubscribeEvent
    public static void keepHoglinsInTheirNether(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Hoglin
                && GameplayDimensions.NETHER.equals(event.getEntity().level().dimension())
                && !GameplayDimensions.NETHER.equals(event.getDimension())) {
            // Administrative /teleport uses Entity.teleportTo and remains available.
            event.setCanceled(true);
        }
    }
}
