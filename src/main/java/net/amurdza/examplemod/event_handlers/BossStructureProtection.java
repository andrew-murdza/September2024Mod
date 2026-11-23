package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** The same resident, outsider and livestock rules apply to every existing boss structure. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class BossStructureProtection {
    private record Discovery(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) { }
    private static final java.util.Queue<Discovery> DISCOVERIES = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private BossStructureProtection() { }

    public static boolean restrictedOutsider(Mob mob) {
        return !(mob instanceof AbstractPiglin) && !(mob instanceof Villager);
    }

    public static boolean belongs(Mob mob, BossStructureBounds.Boundary boundary) {
        if (boundary.encounter() != null) return boundary.encounter().equals(AuthoredEncounterProgress.encounterId(mob));
        return StructureSpawnedMobData.isMarked(mob)
                && boundary.box().contains(Vec3.atCenterOf(StructureSpawnedMobData.home(mob)));
    }

    public static boolean lockedLivestock(Entity entity) {
        return entity.level() instanceof ServerLevel level && AuthoredEncounterProgress.isLivestock(entity)
                && !AuthoredEncounterProgress.get(level).isCleared(AuthoredEncounterProgress.encounterId(entity));
    }

    private static boolean confined(Mob mob) {
        return AuthoredEncounterProgress.isDefender(mob) || lockedLivestock(mob)
                || StructureSpawnedMobData.isMarked(mob) && !AuthoredEncounterProgress.isLivestock(mob);
    }

    public static boolean forbiddenTarget(Mob mob, Entity target) {
        if (!(mob.level() instanceof ServerLevel level) || !restrictedOutsider(mob)) return false;
        for (var boundary : BossStructureBounds.nearby(level, target.getBoundingBox()))
            if (!belongs(mob, boundary)) return true;
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (lockedLivestock(event.getEntity()) || event.getSource().getEntity() instanceof Mob mob
                && forbiddenTarget(mob, event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null && forbiddenTarget(mob, event.getNewTarget()))
            event.setNewTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) { interact(event, event.getTarget()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event, event.getTarget()); }

    private static void interact(PlayerInteractEvent event, Entity target) {
        if (!(target instanceof Mob mob) || !AuthoredEncounterProgress.isLivestock(mob) || event.getEntity().isCreative()) return;
        if (lockedLivestock(mob)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        } else if (event.getItemStack().is(Items.LEAD)) StructureSpawnedMobData.release(mob);
    }

    /** Clip all six faces, including flyers and entities pushed diagonally across a corner. */
    public static Vec3 restrictMovement(Entity entity, Vec3 movement) {
        if (!(entity.level() instanceof ServerLevel level)) return movement;
        if (!(entity instanceof Mob mob)) {
            Vec3 result = movement;
            for (var passenger : entity.getPassengers()) result = restrictMovement(passenger, result);
            return result;
        }
        Vec3 result = movement;
        for (var boundary : BossStructureBounds.nearby(level, mob.getBoundingBox().expandTowards(movement).inflate(1e-7))) {
            var box = mob.getBoundingBox();
            var limit = boundary.box();
            if (belongs(mob, boundary)) {
                if (confined(mob) && contains(limit, box)) result = new Vec3(
                        clamp(result.x, limit.minX - box.minX, limit.maxX - box.maxX),
                        clamp(result.y, limit.minY - box.minY, limit.maxY - box.maxY),
                        clamp(result.z, limit.minZ - box.minZ, limit.maxZ - box.maxZ));
            } else if (restrictedOutsider(mob) && !box.intersects(limit)) {
                var shapes = List.of(Shapes.create(limit));
                double y = Shapes.collide(Direction.Axis.Y, box, shapes, result.y);
                box = box.move(0, y, 0);
                double x = Shapes.collide(Direction.Axis.X, box, shapes, result.x);
                box = box.move(x, 0, 0);
                double z = Shapes.collide(Direction.Axis.Z, box, shapes, result.z);
                result = new Vec3(x, y, z);
            }
        }
        return result;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(value, max)); }
    private static boolean contains(AABB outer, AABB inner) {
        return inner.minX >= outer.minX && inner.maxX <= outer.maxX && inner.minY >= outer.minY
                && inner.maxY <= outer.maxY && inner.minZ >= outer.minZ && inner.maxZ <= outer.maxZ;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        adoptNativeBoss(level, mob);
        for (var memory : List.of(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET,
                net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY,
                net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_ATTACKABLE)) {
            if (!mob.getBrain().hasMemoryValue(memory)) continue;
            mob.getBrain().getMemory(memory).filter(target -> forbiddenTarget(mob, target)).ifPresent(target -> {
                mob.getBrain().eraseMemory(memory);
                mob.getNavigation().stop();
            });
        }
        if (mob.getTarget() != null && forbiddenTarget(mob, mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
        }
        if (Math.floorMod(level.getGameTime() + mob.getId(), 10) != 0) return;
        if (lockedLivestock(mob) && mob.isLeashed()) mob.dropLeash(true, true);
        if (confined(mob) && StructureSpawnedMobData.isMarked(mob)) {
            var home = StructureSpawnedMobData.home(mob);
            for (var boundary : BossStructureBounds.nearby(level, new AABB(home))) {
                if (belongs(mob, boundary) && !contains(boundary.box(), mob.getBoundingBox())) {
                    mob.stopRiding();
                    mob.teleportTo(home.getX() + .5, home.getY(), home.getZ() + .5);
                    mob.getNavigation().stop(); mob.setDeltaMovement(Vec3.ZERO);
                    return;
                }
            }
        }
        if (!restrictedOutsider(mob)) return;
        for (var boundary : BossStructureBounds.nearby(level, mob.getBoundingBox())) {
            if (belongs(mob, boundary)) continue;
            var box = boundary.box();
            double padding = mob.getBbWidth() / 2 + .1;
            double[] distances = {mob.getX() - box.minX, box.maxX - mob.getX(), mob.getZ() - box.minZ, box.maxZ - mob.getZ()};
            int side = 0;
            for (int i = 1; i < 4; i++) if (distances[i] < distances[side]) side = i;
            double x = side == 0 ? box.minX - padding : side == 1 ? box.maxX + padding : mob.getX();
            double z = side == 2 ? box.minZ - padding : side == 3 ? box.maxZ + padding : mob.getZ();
            if (level.hasChunkAt(BlockPos.containing(x, mob.getY(), z))) {
                mob.stopRiding();
                mob.teleportTo(x, mob.getY(), z);
                mob.getNavigation().stop(); mob.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (var start : event.getChunk().getAllStarts().values()) BossStructureBounds.discover(level, start);
            if (!event.getChunk().getAllReferences().isEmpty()) DISCOVERIES.add(new Discovery(level, event.getChunk().getPos()));
        }
    }

    @SubscribeEvent
    public static void discoverReferencedStarts(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        // Resolve old reference-only edge chunks on the server thread, never in movement or a worldgen worker.
        for (int count = 0; count < 4; count++) {
            var pending = DISCOVERIES.poll();
            if (pending == null) return;
            if (!pending.level.hasChunk(pending.chunk.x, pending.chunk.z)) continue;
            for (var start : pending.level.structureManager().startsForStructure(pending.chunk,
                    structure -> structure instanceof net.amurdza.examplemod.worldgen.structure.NetherCitadelStructure || structure instanceof net.amurdza.examplemod.worldgen.structure.BossLairStructure
                            || structure instanceof net.amurdza.examplemod.worldgen.structure.GoblinFortressStructure
                            || structure instanceof net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure))
                BossStructureBounds.discover(pending.level, start);
        }
    }

    @SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { DISCOVERIES.clear(); }

    @SubscribeEvent
    public static void nativeBossJoined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob) adoptNativeBoss(level, mob);
    }

    private static void adoptNativeBoss(ServerLevel level, Mob mob) {
        if (!(mob instanceof twilightforest.entity.boss.AlphaYeti) || StructureSpawnedMobData.isMarked(mob)) return;
        // Twilight Forest creates this authored boss from its one-use native spawner.
        for (var boundary : BossStructureBounds.nearby(level, mob.getBoundingBox())) {
            if (boundary.key().startsWith("lair:yeti:")) {
                StructureSpawnedMobData.mark(mob, mob.blockPosition());
                return;
            }
        }
    }
}
