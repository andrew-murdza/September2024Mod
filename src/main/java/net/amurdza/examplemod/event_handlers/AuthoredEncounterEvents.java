package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class AuthoredEncounterEvents {
    private record Joined(ServerLevel level, Mob mob) { }
    private static final java.util.Queue<Joined> JOINED = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private AuthoredEncounterEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob
                && AuthoredEncounterProgress.encounterId(mob) != null) JOINED.add(new Joined(level, mob));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void finishJoins(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Joined joined;
        while ((joined = JOINED.poll()) != null) {
            // Even a later LOWEST-priority subscriber may cancel the join.
            // Register only after the server's actual entity insertion succeeds.
            if (!joined.mob.isRemoved() && joined.level.getEntity(joined.mob.getUUID()) == joined.mob)
                AuthoredEncounterProgress.get(joined.level).track(joined.mob);
        }
    }

    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { JOINED.clear(); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !AuthoredEncounterProgress.isDefender(mob)) return;
        if (AuthoredEncounterProgress.get(level).defeat(mob)) MinecraftForge.EVENT_BUS.post(
                new AuthoredEncounterProgress.Cleared(level, AuthoredEncounterProgress.encounterId(mob), AuthoredEncounterProgress.kind(mob)));
    }
}
