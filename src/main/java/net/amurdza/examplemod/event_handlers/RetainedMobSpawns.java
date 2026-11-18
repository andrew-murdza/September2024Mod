package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enforces the selected species for eggs, spawners, summons and saved entities as well as worldgen. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class RetainedMobSpawns {
    private RetainedMobSpawns() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joined(EntityJoinLevelEvent event) {
        // Filter mobs, not every entity type: source-mod projectiles, boats and
        // block effects are still needed by the retained creatures and items.
        // Registered types remain available so dependency initialization and
        // old world data can still be read without missing-registry failures.
        if (!event.getLevel().isClientSide() && (event.getEntity() instanceof Mob
                || event.getEntity() instanceof com.github.alexthe666.alexsmobs.entity.EntityAnacondaPart)
                && !RetainedContent.isKeptEntity(event.getEntity().getType())) {
            event.setCanceled(true);
        }
    }
}
