package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProgress;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

/** Construction stays protected; authored chests and crops unlock after the entire roster is defeated. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class NetherCitadelProtection {
    private NetherCitadelProtection() { }

    public static boolean rewardsLocked(ServerLevel level, BlockPos pos) {
        var encounter = NetherCitadelProgress.at(level, pos);
        return encounter != null && !encounter.cleared();
    }

    public static boolean protectedBlock(ServerLevel level, BlockPos pos) {
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)) return false;
        var encounter = NetherCitadelProgress.at(level, pos);
        if (encounter == null) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof BaseFireBlock) return false;
        return !encounter.cleared() || !AuthoredRewardLocks.isRewardBlock(state);
    }

    public static int remainingEnemies(ServerLevel level, BlockPos pos) {
        var encounter = NetherCitadelProgress.at(level, pos);
        return encounter == null ? 0 : Math.max(0, NetherCitadelProgress.DEFENDERS - encounter.defeatedCount());
    }

    public static Component rewardsMessage(ServerLevel level, BlockPos pos) {
        return Component.translatable("message.aoemod.nether_citadel_rewards_locked", remainingEnemies(level, pos));
    }

    public static Optional<Component> breakingMessage(ServerLevel level, BlockPos pos) {
        if (!protectedBlock(level, pos)) return Optional.empty();
        return Optional.of(AuthoredRewardLocks.isRewardBlock(level.getBlockState(pos)) ? rewardsMessage(level, pos)
                : Component.translatable("message.aoemod.nether_citadel_protected"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getEntity().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START)
                breakingMessage(level, event.getPos()).ifPresent(message -> event.getEntity().displayClientMessage(message, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative() && protectedBlock(level, event.getPos())) {
            event.setCanceled(true);
            breakingMessage(level, event.getPos()).ifPresent(message -> event.getPlayer().displayClientMessage(message, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()) return;
        var encounter = NetherCitadelProgress.at(level, event.getPos());
        if (encounter == null && event.getFace() != null)
            encounter = NetherCitadelProgress.at(level, event.getPos().relative(event.getFace()));
        if (encounter == null) return;
        if (!encounter.cleared() && AuthoredRewardLocks.isRewardBlock(level.getBlockState(event.getPos()))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.getEntity().displayClientMessage(rewardsMessage(level, event.getPos()), true);
        }
        var held = event.getItemStack();
        if (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem
                || held.getItem() instanceof FlintAndSteelItem || held.is(Items.FIRE_CHARGE))
            event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity() instanceof Player player && player.isCreative()) return;
        if (NetherCitadelProgress.at(level, event.getPos()) != null) event.setCanceled(true);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple && multiple.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> NetherCitadelProgress.at(level, snapshot.getPos()) != null)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void bucket(FillBucketEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()
                || !(event.getTarget() instanceof BlockHitResult hit)) return;
        if (NetherCitadelProgress.at(level, hit.getBlockPos()) != null
                || NetherCitadelProgress.at(level, hit.getBlockPos().relative(hit.getDirection())) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tool(BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (event.getPlayer() == null || !event.getPlayer().isCreative())
                && NetherCitadelProgress.at(level, event.getPos()) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void trample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && NetherCitadelProgress.at(level, event.getPos()) != null)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && NetherCitadelProgress.at(level, event.getPos()) != null)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) event.getAffectedBlocks().removeIf(pos -> level.hasChunkAt(pos)
                && NetherCitadelProgress.at(level, pos) != null);
    }

    @SubscribeEvent
    public static void mobDestruction(LivingDestroyBlockEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && NetherCitadelProgress.at(level, event.getPos()) != null)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (NetherCitadelProgress.at(level, event.getPos()) != null
                || NetherCitadelProgress.at(level, event.getFaceOffsetPos()) != null) {
            event.setCanceled(true);
            return;
        }
        var resolver = event.getStructureHelper();
        var direction = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
        if (resolver != null && resolver.resolve() && (resolver.getToPush().stream().anyMatch(pos -> NetherCitadelProgress.at(level, pos) != null
                || NetherCitadelProgress.at(level, pos.relative(direction)) != null)
                || resolver.getToDestroy().stream().anyMatch(pos -> NetherCitadelProgress.at(level, pos) != null))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void retryDefenders(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 20 == 0)
            NetherCitadelProgress.retryLoaded(level);
    }

    @SubscribeEvent
    public static void cleared(AuthoredEncounterProgress.Cleared event) {
        if (!NetherCitadelProgress.KIND.equals(event.kind())) return;
        var encounter = AuthoredEncounterProgress.get(event.level()).find(event.id());
        if (encounter != null) NetherCitadelProtectionSnapshot.refresh(event.level(), NetherCitadelProgress.bounds(encounter));
    }

    public static boolean alliedDefenders(Entity first, Entity second) {
        if (first == null || second == null || !AuthoredEncounterProgress.isDefender(first)
                || !AuthoredEncounterProgress.isDefender(second)
                || !NetherCitadelProgress.KIND.equals(AuthoredEncounterProgress.kind(first))
                || !NetherCitadelProgress.KIND.equals(AuthoredEncounterProgress.kind(second))) return false;
        return AuthoredEncounterProgress.encounterId(first).equals(AuthoredEncounterProgress.encounterId(second));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void friendlyFire(LivingAttackEvent event) {
        if (alliedDefenders(event.getSource().getEntity(), event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void friendlyTarget(LivingChangeTargetEvent event) {
        if (alliedDefenders(event.getEntity(), event.getNewTarget())) event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void clearFriendlyTargets(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !NetherCitadelProgress.KIND.equals(AuthoredEncounterProgress.kind(mob))) return;
        if (alliedDefenders(mob, mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        for (var memory : List.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_ATTACKABLE)) {
            if (mob.getBrain().hasMemoryValue(memory)) mob.getBrain().getMemory(memory)
                    .filter(target -> alliedDefenders(mob, target)).ifPresent(target -> mob.getBrain().eraseMemory(memory));
        }
        // Wither side heads store independent entity IDs rather than Mob.target or brain memories.
        if (mob instanceof WitherBoss wither) for (int head = 0; head < 3; head++)
            if (alliedDefenders(wither, level.getEntity(wither.getAlternativeTarget(head)))) wither.setAlternativeTarget(head, 0);
    }
}
