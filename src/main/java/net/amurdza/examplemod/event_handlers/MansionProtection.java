package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class MansionProtection {
    private static final ResourceKey<Structure> MANSION = ResourceKey.create(Registries.STRUCTURE, AOEMod.makeID("illager_mansion"));

    private MansionProtection() {}

    public static MansionBasementPiece door(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.IRON_DOOR) && !level.getBlockState(pos).is(Blocks.OAK_DOOR)) return null;
        for (var layout : PlacedMansions.get(level).at(pos))
            if (layout.basement().isDoor(pos)) return layout.basement();
        for (var piece : level.structureManager().getStructureWithPieceAt(pos, MANSION).getPieces())
            if (piece instanceof MansionBasementPiece basement && basement.isDoor(pos)) return basement;
        return null;
    }

    /** Includes authored air, so natural vegetation cannot colonize the cellar after structure placement. */
    public static boolean insideBasement(net.minecraft.world.level.WorldGenLevel level, BlockPos pos) {
        if (level instanceof ServerLevel server)
            for (var layout : PlacedMansions.get(server).at(pos))
                if (layout.basement().containsAuthoredSpace(pos)) return true;
        var manager = level.getLevel().structureManager();
        if (level instanceof net.minecraft.server.level.WorldGenRegion region) manager = manager.forWorldGenRegion(region);
        for (var piece : manager.getStructureWithPieceAt(pos, MANSION).getPieces())
            if (piece instanceof MansionBasementPiece basement && basement.containsAuthoredSpace(pos)) return true;
        return false;
    }

    public static boolean rewardsLocked(ServerLevel level, BlockPos pos) {
        for (var layout : PlacedMansions.get(level).at(pos))
            if (layout.rooms().stream().anyMatch(box -> box.isInside(pos)) || layout.basement().containsAuthoredSpace(pos))
                return locked(level, layout.basement());
        var start = level.structureManager().getStructureWithPieceAt(pos, MANSION);
        if (start.isValid()) for (var piece : start.getPieces())
            if (piece instanceof MansionBasementPiece basement) return locked(level, basement);
        return false;
    }

    private static boolean locked(ServerLevel level, MansionBasementPiece basement) {
        return basement.encounterId() != null && !AuthoredEncounterProgress.get(level).isCleared(basement.encounterId());
    }

    public static boolean protectedAt(ServerLevel level, BlockPos pos) {
        if (AuthoredRewardLocks.isRewardBlock(level.getBlockState(pos))) return rewardsLocked(level, pos);
        return constructionAt(level, pos);
    }

    private static boolean constructionAt(ServerLevel level, BlockPos pos) {
        for (var layout : PlacedMansions.get(level).at(pos)) {
            if (layout.rooms().stream().anyMatch(box -> box.isInside(pos))) return true;
            if (layout.basement().getBoundingBox().isInside(pos) && layout.basement().isBuiltCell(pos)) return true;
        }
        for (var piece : level.structureManager().getStructureWithPieceAt(pos, MANSION).getPieces()) {
            if (!piece.getBoundingBox().isInside(pos)) continue;
            if (piece instanceof WoodlandMansionPiece
                    || piece instanceof MansionBasementPiece basement && basement.isBuiltCell(pos)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void hit(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getEntity().isCreative() && protectedAt(level, event.getPos())) {
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START)
                message(event.getEntity(), "mansion_protected");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative() && protectedAt(level, event.getPos())) {
            event.setCanceled(true);
            message(event.getPlayer(), "mansion_protected");
        }
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pos = event.getPos();
        var basement = door(level, pos);
        if (basement == null) {
            if (!event.getEntity().isCreative() && AuthoredRewardLocks.isRewardBlock(level.getBlockState(pos)) && rewardsLocked(level, pos)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                message(event.getEntity(), "mansion_rewards_locked");
            }
            return;
        }
        // New iron entrances use their physical levers and ordinary redstone.
        if (basement.layoutVersion() >= 11) return;
        // Both leaves work together; saved iron entrances remain usable after retirement of the key.
        boolean open = !level.getBlockState(pos).getValue(DoorBlock.OPEN);
        for (BlockPos leaf : basement.doorLeaves())
            if (level.getBlockState(leaf).getBlock() instanceof DoorBlock door)
                door.setOpen(event.getEntity(), level, level.getBlockState(leaf), leaf, open);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cleared(AuthoredEncounterProgress.Cleared event) {
        if (event.kind().equals("mansion")) MansionProtectionSnapshot.refreshEncounter(event.level(), event.id());
    }

    @SubscribeEvent
    public static void retryDefenders(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.getGameTime() % 40 == 0) net.amurdza.examplemod.worldgen.structure.MansionDefenderSpawns.retryLoaded(level);
    }

    public static boolean livestockLocked(net.minecraft.world.entity.Entity entity) {
        return entity.level() instanceof ServerLevel level && AuthoredEncounterProgress.isLivestock(entity)
                && AuthoredEncounterProgress.kind(entity).equals("mansion")
                && !AuthoredEncounterProgress.get(level).isCleared(AuthoredEncounterProgress.encounterId(entity));
    }

    @SubscribeEvent
    public static void livestockDamage(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (livestockLocked(event.getEntity()) && !(event.getSource().getEntity() instanceof Player player && player.isCreative()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void livestockUse(PlayerInteractEvent.EntityInteract event) {
        if (!event.getEntity().isCreative() && livestockLocked(event.getTarget())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL);
            message(event.getEntity(), "mansion_rewards_locked");
        } else if (event.getTarget() instanceof net.minecraft.world.entity.Mob mob
                && AuthoredEncounterProgress.isLivestock(mob) && AuthoredEncounterProgress.kind(mob).equals("mansion")
                && event.getItemStack().is(net.minecraft.world.item.Items.LEAD))
            net.amurdza.examplemod.util.StructureSpawnedMobData.release(mob);
    }

    @SubscribeEvent
    public static void livestockUseSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getEntity().isCreative() && livestockLocked(event.getTarget())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level)
            event.getAffectedBlocks().removeIf(pos -> level.hasChunkAt(pos) && protectedAt(level, pos));
    }

    @SubscribeEvent
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve() && (resolver.getToPush().stream().anyMatch(pos -> protectedAt(level, pos))
                || resolver.getToDestroy().stream().anyMatch(pos -> protectedAt(level, pos)))) event.setCanceled(true);
    }

    private static void message(Player player, String name) {
        player.displayClientMessage(Component.translatable("message.aoemod." + name), true);
    }
}
