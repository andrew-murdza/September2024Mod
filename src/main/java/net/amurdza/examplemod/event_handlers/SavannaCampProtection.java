package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

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

    public static int remainingEnemies(ServerLevel level, BlockPos pos) {
        var camp = SavannaCampProgress.at(level, pos);
        return camp == null ? 0 : Math.max(0, camp.expected() - camp.defeatedCount());
    }

    public static Component rewardsMessage(ServerLevel level, BlockPos pos) {
        return Component.translatable("message.aoemod.camp_rewards_locked", remainingEnemies(level, pos));
    }

    public static Component gatesMessage(ServerLevel level, BlockPos pos) {
        return Component.translatable("message.aoemod.camp_gates_locked", remainingEnemies(level, pos));
    }

    public static Optional<Component> breakingMessage(ServerLevel level, BlockPos pos) {
        if (!protectedBlock(level, pos)) return Optional.empty();
        var camp = SavannaCampProgress.at(level, pos);
        boolean reward = SavannaCampProgress.isRewardBlock(level.getBlockState(pos), pos, SavannaCampProgress.bounds(camp));
        return Optional.of(reward ? rewardsMessage(level, pos) : Component.translatable("message.aoemod.camp_protected"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getEntity().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) breakingMessage(level, event.getPos()).ifPresent(message -> event.getEntity().displayClientMessage(message, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true); breakingMessage(level, event.getPos()).ifPresent(message -> event.getPlayer().displayClientMessage(message, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()) return;
        var camp = SavannaCampProgress.at(level, event.getPos());
        if (camp == null && event.getFace() != null) camp = SavannaCampProgress.at(level, event.getPos().relative(event.getFace()));
        if (camp == null) return;
        var state = level.getBlockState(event.getPos());
        if (!camp.cleared() && SavannaCampProgress.bounds(camp).isInside(event.getPos())
                && (SavannaCampProgress.isRewardBlock(state, event.getPos(), SavannaCampProgress.bounds(camp))
                || state.getBlock() instanceof FenceGateBlock)) {
            if (state.getBlock() instanceof FenceGateBlock
                    && (state.getValue(FenceGateBlock.OPEN) || state.getValue(FenceGateBlock.POWERED)))
                level.setBlock(event.getPos(), state.setValue(FenceGateBlock.OPEN, false).setValue(FenceGateBlock.POWERED, false), 2);
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL);
            event.getEntity().displayClientMessage(state.getBlock() instanceof FenceGateBlock
                    ? gatesMessage(level, event.getPos()) : rewardsMessage(level, event.getPos()), true);
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

    public static void attack(LivingAttackEvent event) {
        if (lockedLivestock(event.getEntity())) { event.setCanceled(true); return; }
        if (event.getSource().getEntity() instanceof Mob attacker && forbiddenTarget(attacker, event.getEntity())) event.setCanceled(true);
    }

    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() != null && forbiddenTarget(mob, event.getNewTarget())) event.setNewTarget(null);
    }

    public static boolean forbiddenTarget(Mob mob, Entity target) {
        return BossStructureProtection.forbiddenTarget(mob, target);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void interact(PlayerInteractEvent.EntityInteract event) { interact(event, event.getTarget()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { interact(event, event.getTarget()); }

    private static void interact(PlayerInteractEvent event, Entity target) {
        if (!(target instanceof Mob mob) || event.getEntity().isCreative() || !(mob.level() instanceof ServerLevel level)
                || !AuthoredEncounterProgress.isLivestock(mob) || !SavannaCampProgress.KIND.equals(AuthoredEncounterProgress.kind(mob))) return;
        if (lockedLivestock(mob)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL);
            var encounter = AuthoredEncounterProgress.get(level).find(AuthoredEncounterProgress.encounterId(mob));
            int remaining = encounter == null ? SavannaCampProgress.DEFENDERS : Math.max(0, encounter.expected() - encounter.defeatedCount());
            event.getEntity().displayClientMessage(Component.translatable("message.aoemod.camp_rewards_locked", remaining), true);
        } else if (event.getItemStack().is(Items.LEAD)) StructureSpawnedMobData.release(mob);
    }

    public static boolean restrictedOutsider(Mob mob) {
        return BossStructureProtection.restrictedOutsider(mob);
    }

    public static Vec3 restrictMovement(Entity entity, Vec3 movement) {
        return BossStructureProtection.restrictMovement(entity, movement);
    }

    @SubscribeEvent public static void cleared(AuthoredEncounterProgress.Cleared event) {
        if (!event.kind().equals(SavannaCampProgress.KIND)) return;
        var camp = AuthoredEncounterProgress.get(event.level()).find(event.id());
        if (camp != null) SavannaCampProtectionSnapshot.refresh(event.level(), SavannaCampProgress.bounds(camp));
    }

}
