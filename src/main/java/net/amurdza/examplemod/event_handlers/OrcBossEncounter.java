package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Boss state follows normal entity tracking; no searches across worlds or nearby chunks. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class OrcBossEncounter {
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private OrcBossEncounter() { }

    public static boolean isEncounter(Entity entity) {
        return entity instanceof Mob mob && StructureSpawnedMobData.isMarked(mob)
                && "orcz:bigorcboss".equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isEncounter(event.getEntity())) return;
        Mob mob = (Mob) event.getEntity();
        float fraction = mob.getHealth() / mob.getMaxHealth();
        var maximum = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maximum != null && maximum.getBaseValue() != 200.0D) {
            maximum.setBaseValue(200.0D);
            mob.setHealth(mob.getMaxHealth() * fraction);
        }
        ServerBossEvent bar = new ServerBossEvent(mob.getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        bar.setProgress(Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0.0F, 1.0F));
        ServerBossEvent previous = BARS.put(mob.getUUID(), bar);
        if (previous != null) previous.removeAllPlayers();
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        ServerBossEvent bar = BARS.get(event.getTarget().getUUID());
        if (bar != null && event.getEntity() instanceof ServerPlayer player) bar.addPlayer(player);
    }

    @SubscribeEvent
    public static void stoppedTracking(PlayerEvent.StopTracking event) {
        ServerBossEvent bar = BARS.get(event.getTarget().getUUID());
        if (bar != null && event.getEntity() instanceof ServerPlayer player) bar.removePlayer(player);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().tickCount % 5 != 0) return;
        ServerBossEvent bar = BARS.get(event.getEntity().getUUID());
        if (bar != null) bar.setProgress(Mth.clamp(event.getEntity().getHealth() / event.getEntity().getMaxHealth(), 0.0F, 1.0F));
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) { remove(event.getEntity()); }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) { remove(event.getEntity()); }

    private static void remove(Entity entity) {
        if (entity.level().isClientSide()) return;
        ServerBossEvent bar = BARS.remove(entity.getUUID());
        if (bar != null) bar.removeAllPlayers();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
    }
}
