package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The complete fortress estate, including its fishing grounds, shares one saved encounter. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class GoblinFortressProtection {
    private GoblinFortressProtection() { }

    public static BossStructureBounds.Boundary boundaryAt(ServerLevel level, BlockPos pos) {
        for (var boundary : BossStructureBounds.get(level).nearby(new AABB(pos))) {
            if (boundary.key().startsWith(GoblinFortressPiece.ENCOUNTER_KIND + ":")) return boundary;
        }
        return null;
    }

    public static boolean rewardsLocked(ServerLevel level, BlockPos pos) {
        var boundary = boundaryAt(level, pos);
        return boundary != null && !AuthoredEncounterProgress.get(level).isCleared(boundary.encounter());
    }

    public static int remaining(ServerLevel level, BlockPos pos) {
        var boundary = boundaryAt(level, pos);
        if (boundary == null) return 0;
        var encounter = AuthoredEncounterProgress.get(level).find(boundary.encounter());
        // A chunk may arrive before its encounter registration. Do not temporarily unlock it.
        return encounter == null ? GoblinFortressPiece.DEFENDER_COUNT
                : Math.max(0, encounter.expected() - encounter.defeatedCount());
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        if (boundaryAt(level, pos) == null) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof BaseFireBlock) return false;
        return !AuthoredRewardLocks.isRewardBlock(state) || rewardsLocked(level, pos);
    }

    /** Authored water stays stable, and outside water/lava cannot wash out rooms or crops. */
    public static boolean blocksFluid(ServerLevel level, BlockPos source, BlockPos target) {
        return boundaryAt(level, source) != null || boundaryAt(level, target) != null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getEntity().isCreative()
                && isProtected(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START)
                explain(event.getEntity(), level, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative()
                && isProtected(level, event.getPos())) {
            event.setCanceled(true);
            explain(event.getPlayer(), level, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()) return;
        var pos = event.getPos();
        var boundary = boundaryAt(level, pos);
        if (boundary == null && event.getFace() != null) boundary = boundaryAt(level, pos.relative(event.getFace()));
        if (boundary == null) return;
        var state = level.getBlockState(pos);
        if (rewardsLocked(level, pos) && (AuthoredRewardLocks.isRewardBlock(state)
                || state.getBlock() instanceof FenceGateBlock)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            explain(event.getEntity(), level, pos);
            return;
        }
        var item = event.getItemStack();
        if (item.getItem() instanceof BlockItem || item.getItem() instanceof BucketItem
                || item.getItem() instanceof FlintAndSteelItem || item.is(Items.FIRE_CHARGE)
                || item.is(Items.BONE_MEAL)) event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void bucket(FillBucketEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()
                || !(event.getTarget() instanceof BlockHitResult target)) return;
        if (boundaryAt(level, target.getBlockPos()) != null
                || boundaryAt(level, target.getBlockPos().relative(target.getDirection())) != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getEntity() instanceof Player player && player.isCreative()) return;
        if (boundaryAt(level, event.getPos()) != null) event.setCanceled(true);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple && multiple.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> boundaryAt(level, snapshot.getPos()) != null)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tool(BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (event.getPlayer() == null || !event.getPlayer().isCreative())
                && boundaryAt(level, event.getPos()) != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void trample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && boundaryAt(level, event.getPos()) != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && boundaryAt(level, event.getPos()) != null) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level)
            event.getAffectedBlocks().removeIf(pos -> boundaryAt(level, pos) != null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (boundaryAt(level, event.getPos()) != null || boundaryAt(level, event.getFaceOffsetPos()) != null) {
            event.setCanceled(true);
            return;
        }
        var resolver = event.getStructureHelper();
        var direction = event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND
                ? event.getDirection() : event.getDirection().getOpposite();
        if (resolver != null && resolver.resolve() && (resolver.getToPush().stream()
                .anyMatch(pos -> boundaryAt(level, pos) != null || boundaryAt(level, pos.relative(direction)) != null)
                || resolver.getToDestroy().stream().anyMatch(pos -> boundaryAt(level, pos) != null))) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) { interact(event, event.getTarget()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event, event.getTarget()); }

    private static void interact(PlayerInteractEvent event, Entity target) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()
                || !AuthoredEncounterProgress.isLivestock(target)
                || !GoblinFortressPiece.ENCOUNTER_KIND.equals(AuthoredEncounterProgress.kind(target))) return;
        if (!AuthoredEncounterProgress.get(level).isCleared(AuthoredEncounterProgress.encounterId(target))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            explain(event.getEntity(), level, target.blockPosition());
        }
        // The shared BossStructureProtection handler releases claimed livestock from its home when leashed.
    }

    private static void explain(Player player, ServerLevel level, BlockPos pos) {
        int remaining = remaining(level, pos);
        player.displayClientMessage(Component.literal(remaining > 0
                ? "Defeat the remaining " + remaining + " fortress enemies to claim the chests, crops, and livestock."
                : "Fortress buildings and terrain cannot be harvested."), true);
    }
}
