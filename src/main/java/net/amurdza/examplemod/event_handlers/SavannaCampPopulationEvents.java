package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Failed placement remains pending across saves without treating unloaded guards as dead. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class SavannaCampPopulationEvents {
    private SavannaCampPopulationEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && GameplayDimensions.is(level, GameplayDimensions.SAVANNA) && level.getGameTime() % 20 == 0) retryLoaded(level);
    }

    public static void retryLoaded(ServerLevel level) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return;
        var progress = AuthoredEncounterProgress.get(level);
        var activeOrigins = new java.util.HashSet<BlockPos>();
        for (var camp : progress.encounters(SavannaCampProgress.KIND)) {
            // The records are newest-first; replacement camps supersede old queues.
            if (!activeOrigins.add(camp.origin()) || camp.cleared()) continue;
            progress.queueMissingDefenders(camp.id());
            var pending = progress.pendingDefenders(camp.id());
            for (int index = pending.nextSetBit(0); index >= 0 && index < SavannaOrcVillage.RESIDENT_COUNT; index = pending.nextSetBit(index + 1)) {
                var pos = camp.origin().offset(SavannaOrcVillage.residentPlan().get(index).local());
                if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
                    SavannaOrcVillage.retryResident(level, camp.origin(), camp.id(), index);
            }
        }
    }
}
