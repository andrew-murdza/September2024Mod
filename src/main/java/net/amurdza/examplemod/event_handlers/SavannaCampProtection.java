package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class SavannaCampProtection {
    private static final ThreadLocal<Boolean> CREATIVE_BREAK = new ThreadLocal<>();
    private SavannaCampProtection() { }

    public static boolean creativeBreak() { return Boolean.TRUE.equals(CREATIVE_BREAK.get()); }
    public static boolean withBreakingPlayer(Player player, java.util.function.Supplier<Boolean> action) {
        Boolean previous = CREATIVE_BREAK.get();
        CREATIVE_BREAK.set(player.isCreative());
        try { return action.get(); }
        finally { if (previous == null) CREATIVE_BREAK.remove(); else CREATIVE_BREAK.set(previous); }
    }

    public static boolean rewardsLocked(ServerLevel level, BlockPos pos) {
        var camp = SavannaCampProgress.at(level, pos);
        return camp != null && !camp.cleared();
    }

    public static boolean protectedBlock(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) return false;
        var camp = SavannaCampProgress.at(level, pos);
        return camp != null && (!camp.cleared() || !SavannaCampProgress.isRewardBlock(level.getBlockState(pos), pos, SavannaCampProgress.bounds(camp)));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getEntity().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) explain(event.getEntity(), rewardsLocked(level, event.getPos()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true); explain(event.getPlayer(), rewardsLocked(level, event.getPos()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()) return;
        var camp = SavannaCampProgress.at(level, event.getPos());
        if (camp == null && event.getFace() != null) camp = SavannaCampProgress.at(level, event.getPos().relative(event.getFace()));
        if (camp == null) return;
        if (!camp.cleared() && AuthoredRewardLocks.isRewardBlock(level.getBlockState(event.getPos()))) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); explain(event.getEntity(), true);
        }
        var held = event.getItemStack();
        if (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem
                || held.getItem() instanceof FlintAndSteelItem || held.is(Items.FIRE_CHARGE)) event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity() instanceof Player player && player.isCreative()) return;
        if (SavannaCampProgress.at(level, event.getPos()) != null) event.setCanceled(true);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple && multiple.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> SavannaCampProgress.at(level, snapshot.getPos()) != null)) event.setCanceled(true);
    }

    @SubscribeEvent public static void tool(BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (event.getPlayer() == null || !event.getPlayer().isCreative())
                && SavannaCampProgress.at(level, event.getPos()) != null) event.setCanceled(true);
    }
    @SubscribeEvent public static void trample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && SavannaCampProgress.at(level, event.getPos()) != null) event.setCanceled(true);
    }
    @SubscribeEvent public static void fluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && SavannaCampProgress.at(level, event.getPos()) != null) event.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) event.getAffectedBlocks().removeIf(pos -> level.hasChunkAt(pos)
                && SavannaCampProgress.at(level, pos) != null);
    }
    @SubscribeEvent public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve() && (resolver.getToPush().stream().anyMatch(pos -> SavannaCampProgress.at(level, pos) != null)
                || resolver.getToDestroy().stream().anyMatch(pos -> SavannaCampProgress.at(level, pos) != null))) event.setCanceled(true);
    }

    public static boolean lockedLivestock(Entity entity) {
        return entity.level() instanceof ServerLevel level && AuthoredEncounterProgress.isLivestock(entity)
                && SavannaCampProgress.KIND.equals(AuthoredEncounterProgress.kind(entity))
                && !AuthoredEncounterProgress.get(level).isCleared(AuthoredEncounterProgress.encounterId(entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (lockedLivestock(event.getEntity())) { event.setCanceled(true); return; }
        if (event.getSource().getEntity() instanceof Mob attacker && forbiddenTarget(attacker, event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null && forbiddenTarget(mob, event.getNewTarget())) event.setNewTarget(null);
    }

    public static boolean forbiddenTarget(Mob mob, Entity target) {
        if (!(mob.level() instanceof ServerLevel level) || !restrictedOutsider(mob)) return false;
        var camp = SavannaCampProgress.at(level, target.blockPosition());
        return camp != null && !SavannaCampProgress.belongs(mob, camp);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) { interact(event, event.getTarget()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event, event.getTarget()); }

    private static void interact(PlayerInteractEvent event, Entity target) {
        if (!(target instanceof Mob mob) || event.getEntity().isCreative() || !(mob.level() instanceof ServerLevel level)
                || !AuthoredEncounterProgress.isLivestock(mob) || !SavannaCampProgress.KIND.equals(AuthoredEncounterProgress.kind(mob))) return;
        if (lockedLivestock(mob)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); explain(event.getEntity(), true);
        } else if (event.getItemStack().is(Items.LEAD)) StructureSpawnedMobData.release(mob);
    }

    public static boolean restrictedOutsider(Mob mob) {
        return GameplayDimensions.is(mob.level(), GameplayDimensions.SAVANNA)
                && (mob.getMobType() == MobType.UNDEAD || mob.getType().getCategory() == MobCategory.CREATURE);
    }

    /** Horizontal clipping keeps the normal vertical collision/gravity path intact. No chunk loads. */
    public static Vec3 restrictMovement(Entity entity, Vec3 movement) {
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level) || !restrictedOutsider(mob)) return movement;
        var box = mob.getBoundingBox();
        double x = movement.x, z = movement.z;
        for (var camp : SavannaCampProgress.nearby(level, box.expandTowards(movement))) {
            var boundary = SavannaCampProgress.boundary(camp);
            if (SavannaCampProgress.belongs(mob, camp)) {
                if (!lockedLivestock(mob)) continue;
                if (box.minX >= boundary.minX && box.maxX <= boundary.maxX) x = Math.max(boundary.minX - box.minX, Math.min(x, boundary.maxX - box.maxX));
                if (box.minZ >= boundary.minZ && box.maxZ <= boundary.maxZ) z = Math.max(boundary.minZ - box.minZ, Math.min(z, boundary.maxZ - box.maxZ));
                continue;
            }
            if (box.intersects(boundary)) continue; // Tick fallback evacuates old/teleported outsiders.
            if (box.maxZ + z > boundary.minZ && box.minZ + z < boundary.maxZ) {
                if (box.maxX <= boundary.minX && box.maxX + x > boundary.minX) x = Math.max(0, boundary.minX - box.maxX - 1e-6);
                else if (box.minX >= boundary.maxX && box.minX + x < boundary.maxX) x = Math.min(0, boundary.maxX - box.minX + 1e-6);
            }
            if (box.maxX + x > boundary.minX && box.minX + x < boundary.maxX) {
                if (box.maxZ <= boundary.minZ && box.maxZ + z > boundary.minZ) z = Math.max(0, boundary.minZ - box.maxZ - 1e-6);
                else if (box.minZ >= boundary.maxZ && box.minZ + z < boundary.maxZ) z = Math.min(0, boundary.maxZ - box.minZ + 1e-6);
            }
        }
        return x == movement.x && z == movement.z ? movement : new Vec3(x, movement.y, z);
    }

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !restrictedOutsider(mob)) return;
        if (mob.getTarget() != null && forbiddenTarget(mob, mob.getTarget())) { mob.setTarget(null); mob.getNavigation().stop(); }
        if (Math.floorMod(level.getGameTime() + mob.getId(), 10) != 0) return;
        if (lockedLivestock(mob)) {
            var own = AuthoredEncounterProgress.get(level).find(AuthoredEncounterProgress.encounterId(mob));
            if (own != null && !SavannaCampProgress.boundary(own).intersects(mob.getBoundingBox())) {
                var home = StructureSpawnedMobData.home(mob);
                mob.teleportTo(home.getX() + .5, home.getY(), home.getZ() + .5); mob.getNavigation().stop();
            }
            if (mob.isLeashed()) mob.dropLeash(true, true);
            return;
        }
        for (var camp : SavannaCampProgress.nearby(level, mob.getBoundingBox())) {
            var boundary = SavannaCampProgress.boundary(camp);
            if (SavannaCampProgress.belongs(mob, camp) || !boundary.intersects(mob.getBoundingBox())) continue;
            double padding = mob.getBbWidth() / 2 + .1;
            double[] distances = {mob.getX() - boundary.minX, boundary.maxX - mob.getX(), mob.getZ() - boundary.minZ, boundary.maxZ - mob.getZ()};
            int side = 0;
            for (int i = 1; i < 4; i++) if (distances[i] < distances[side]) side = i;
            double x = side == 0 ? boundary.minX - padding : side == 1 ? boundary.maxX + padding : mob.getX();
            double z = side == 2 ? boundary.minZ - padding : side == 3 ? boundary.maxZ + padding : mob.getZ();
            var pos = BlockPos.containing(x, mob.getY(), z);
            if (level.hasChunkAt(pos)) {
                double y = Math.max(mob.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()));
                mob.teleportTo(x, y, z); mob.getNavigation().stop(); mob.setDeltaMovement(Vec3.ZERO);
            }
            break;
        }
    }

    @SubscribeEvent public static void cleared(AuthoredEncounterProgress.Cleared event) {
        if (!event.kind().equals(SavannaCampProgress.KIND)) return;
        var camp = AuthoredEncounterProgress.get(event.level()).find(event.id());
        if (camp != null) SavannaCampProtectionSnapshot.refresh(event.level(), SavannaCampProgress.bounds(camp));
    }

    private static void explain(Player player, boolean locked) {
        player.displayClientMessage(Component.literal(locked ? "Defeat all 127 camp orcs to claim the chests, crops, and livestock."
                : "Camp buildings and terrain cannot be harvested."), true);
    }
}
