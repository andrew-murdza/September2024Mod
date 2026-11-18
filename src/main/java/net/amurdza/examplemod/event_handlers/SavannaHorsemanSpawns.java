package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.SkeletonHorsemanEntity;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Horsemen use the normal hostile budget; lightning is a visual, not four bonus mounts. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class SavannaHorsemanSpawns {
    private SavannaHorsemanSpawns() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || event.loadedFromDisk()) return;
        if (event.getEntity().getType() == EntityType.SKELETON_HORSE
                && event.getEntity() instanceof SkeletonHorse horse && horse.isTrap()) {
            // ServerLevel's thunder pass would bypass the monster cap and later
            // turn this persistent passive horse into four mounted archers.
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof SkeletonHorsemanEntity horse && horse.getSpawnType() == MobSpawnType.NATURAL
                && hasThunderstorm(level)) {
            var lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning == null) return;
            lightning.moveTo(horse.position());
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
    }

    private static boolean hasThunderstorm(ServerLevel level) {
        ServerLevel weatherOwner = level.getServer().overworld();
        // Visual-only bolts still play thunder. Ordinary night-time monster
        // spawning must not create storms, and /weather clear must stop these
        // effects immediately while the displayed storm intensity fades out.
        return weatherOwner.getLevelData().isRaining() && weatherOwner.getLevelData().isThundering()
                && weatherOwner.isThundering();
    }
}
