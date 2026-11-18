package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.AuthoredEncounterEvents;
import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember18Tests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void twelveWideHallAndOakDoubleDoorPreserveSavedNine(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var cellar = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 10);
            cellar.encounter(UUID.randomUUID(), 23);
            var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, cellar.createTag(serial));
            h.assertTrue(restored.layoutVersion() == 10 && restored.encounterId().equals(cellar.encounterId())
                    && restored.defenderCount() == 23, "Current geometry and encounter identity must survive a piece reload");
            var blocks = MansionBasementV3Tests.place(h, restored);
            for (int z = 14; z <= 72; z++) {
                for (int x = -5; x <= 6; x++) {
                    h.assertTrue(blocks.get(cellar.world(x, 0, z)).is(Blocks.STONE_BRICKS), "All twelve aisle columns need a continuous floor");
                    h.assertTrue(blocks.get(cellar.world(x, 1, z)).isAir(), "The widened aisle must remain clear");
                }
                int lanterns = 0;
                for (int x = -5; x <= 6; x++) if (blocks.get(cellar.world(x, 4, z)).is(Blocks.LANTERN)) lanterns++;
                h.assertTrue(lanterns == ((z - 15) % 4 == 0 ? 4 : 0), "Each of the fifteen lighting rows needs exactly four hanging lanterns");
            }
            for (int x = 0; x <= 1; x++) for (int y = 1; y <= 2; y++) {
                var state = blocks.get(cellar.world(x, y, 13));
                h.assertTrue(state.is(Blocks.OAK_DOOR) && state.getValue(DoorBlock.FACING) == rotation.rotate(Direction.SOUTH)
                        && state.getValue(DoorBlock.HINGE) == (x == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT),
                        "The entrance must have two matching oak leaves and opposing hinges");
            }
            for (int x = 7; x <= 31; x++) for (int z = 14; z <= 73; z++) for (int y = -2; y <= 6; y++) {
                var old = MansionBasementLayoutV9.cell(x - 1, y, z);
                h.assertTrue(java.util.Objects.equals(blocks.get(cellar.world(x, y, z)), old == null ? null : old.rotate(rotation)),
                        "Widening the aisle must preserve the complete east room contents");
            }
            var old = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 9);
            var saved = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            var oldBlocks = MansionBasementV3Tests.place(h, saved);
            h.assertTrue(saved.layoutVersion() == 9 && saved.encounterId() == null && oldBlocks.get(old.firstDoor()).is(Blocks.IRON_DOOR)
                    && saved.doorLeaves().size() == 1 && oldBlocks.get(old.world(0, 4, 15)).is(Blocks.LANTERN),
                    "Existing v9 cellars keep their original geometry without inventing a missing defender roster");
            var rooms = List.of(BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8)));
            var placements = new PlacedMansions(); placements.record(cellar, rooms);
            var loaded = PlacedMansions.load(placements.save(new CompoundTag())).at(cellar.firstDoor()).get(0).basement();
            h.assertTrue(loaded.encounterId().equals(cellar.encounterId()) && loaded.defenderCount() == 23,
                    "Command placement must preserve its roster across world restart");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void mansionRewardsWaitForEveryRecordedDeathAndKeepConstructionProtected(GameTestHelper h) {
        var level = h.getLevel();
        var cellar = new MansionBasementPiece(h.absolutePos(new BlockPos(40, 0, -50)).atY(170), Rotation.NONE, 11);
        var id = UUID.randomUUID(); cellar.encounter(id, 2);
        var room = BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8));
        PlacedMansions.get(level).record(cellar, List.of(room));
        var progress = AuthoredEncounterProgress.get(level);
        progress.register(id, cellar.keyAnchor(), 2, "mansion");
        var player = h.makeMockSurvivalPlayer();
        int roomZ = 15 + MansionBasementLayoutV11.FIRST_ROOM_SHIFT;
        var chestPos = cellar.world(-8, 1, roomZ);
        var cropPos = cellar.world(-9, 1, roomZ);
        var stonePos = cellar.world(-10, 0, roomZ);
        h.assertTrue(cellar.containsAuthoredSpace(chestPos) && cellar.containsAuthoredSpace(cropPos)
                        && cellar.isBuiltCell(stonePos),
                "Reward probes must be inside the shifted first-floor room, with masonry on an authored floor cell");
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 18);
        level.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), 18);
        level.setBlock(stonePos, Blocks.STONE_BRICKS.defaultBlockState(), 18);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var cow = EntityType.COW.create(level);
        AuthoredEncounterProgress.markLivestock(cow, id, cellar.keyAnchor(), 2, "mansion");
        var first = EntityType.VINDICATOR.create(level); var second = EntityType.EVOKER.create(level);
        AuthoredEncounterProgress.markDefender(first, id, cellar.keyAnchor(), 2, "mansion", 0);
        AuthoredEncounterProgress.markDefender(second, id, cellar.keyAnchor(), 2, "mansion", 1);
        progress.track(first); progress.track(second);
        h.assertTrue(MansionProtection.rewardsLocked(level, chestPos), "An authored mansion begins locked");
        h.assertTrue(chest.getItem(0).isEmpty() && chest.removeItem(0, 1).isEmpty(), "Locked chests must deny hopper and capability extraction");
        var blockedUse = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, chestPos,
                new BlockHitResult(Vec3.atCenterOf(chestPos), Direction.UP, chestPos, false));
        MansionProtection.use(blockedUse);
        h.assertTrue(blockedUse.isCanceled(), "Opening a chest must wait for the complete roster");
        var damage = new LivingAttackEvent(cow, level.damageSources().generic(), 2);
        MansionProtection.livestockDamage(damage);
        h.assertTrue(damage.isCanceled(), "Livestock cannot be killed before clearing the mansion");
        var interaction = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        MansionProtection.livestockUse(interaction);
        h.assertTrue(interaction.isCanceled(), "Milk, shears, feeding and leads cannot harvest locked livestock");
        for (var pos : List.of(chestPos, cropPos, stonePos)) assertBreak(h, player, pos, true);
        first.discard();
        h.assertTrue(!progress.isCleared(id), "Discarding or unloading a defender must never unlock its rewards");
        var canceled = new LivingDeathEvent(second, level.damageSources().generic()); canceled.setCanceled(true);
        AuthoredEncounterEvents.died(canceled);
        h.assertTrue(progress.defeatedCount(id) == 0, "A canceled death must not count");
        AuthoredEncounterEvents.died(new LivingDeathEvent(first, level.damageSources().generic()));
        h.assertTrue(!progress.isCleared(id) && progress.defeatedCount(id) == 1, "One death cannot unlock a two-defender roster");
        var savedProgress = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        h.assertTrue(savedProgress.defeatedCount(id) == 1 && !savedProgress.isCleared(id), "Partial victories must survive restart");
        AuthoredEncounterEvents.died(new LivingDeathEvent(second, level.damageSources().generic()));
        h.assertTrue(progress.isCleared(id) && !MansionProtection.rewardsLocked(level, chestPos), "The final distinct death must release rewards");
        h.assertTrue(chest.getItem(0).getCount() == 3 && chest.removeItem(0, 1).getCount() == 1,
                "Unlocking must reveal the original unchanged chest contents");
        for (var pos : List.of(chestPos, cropPos)) assertBreak(h, player, pos, false);
        assertBreak(h, player, stonePos, true);
        h.assertTrue(!MansionProtection.livestockLocked(cow), "Livestock remains harvestable after clear");
        var snapshot = MansionProtectionSnapshot.decode(MansionProtectionSnapshot.capture(level, new ChunkPos(chestPos)).encode());
        h.assertTrue(snapshot.rewardsUnlocked(chestPos) && snapshot.protects(stonePos), "Client packets must release rewards while retaining masonry");
        for (var box : snapshot.unlockedRewards()) h.assertTrue(box.intersects(new ChunkPos(chestPos).getMinBlockX(), new ChunkPos(chestPos).getMinBlockZ(),
                new ChunkPos(chestPos).getMaxBlockX(), new ChunkPos(chestPos).getMaxBlockZ()), "Reward packet bounds must be chunk scoped");
        cow.discard(); second.discard(); h.succeed();
    }

    // Current full-estate placement, connectivity and roster coverage lives in MansionExpandedEstateTests.

    @GameTest(template = "plant_gameplay_empty")
    public static void failedMansionSpawnRetriesAfterSaveWithoutDuplicatingOrRevivingGuards(GameTestHelper h) {
        var level = h.getLevel(); var origin = h.absolutePos(new BlockPos(70, 0, -70)).atY(180);
        var id = UUID.randomUUID(); var room = BoundingBox.fromCorners(origin.offset(-3, 0, -3), origin.offset(3, 4, 3));
        for (var pos : BlockPos.betweenClosed(origin.offset(-3, 0, -3), origin.offset(3, 4, 3))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        var plan = new MansionDefenderSpawns.Plan(id, origin, 1, 0, "minecraft:vindicator", origin.above(), room);
        var queue = MansionDefenderSpawns.get(level); queue.plan(level, plan);
        h.assertFalse(MansionDefenderSpawns.retry(level, plan), "A failed floor search must leave the authored slot pending");
        var restored = MansionDefenderSpawns.load(queue.save(new CompoundTag()));
        h.assertTrue(restored.plans().contains(plan), "Failed spawn type, position and roster identity must persist");
        level.setBlock(origin, Blocks.STONE_BRICKS.defaultBlockState(), 18);
        h.assertTrue(MansionDefenderSpawns.retry(level, plan), "Restoring a safe floor must place the same pending guard");
        var mob = (Mob) level.getEntity(MansionDefenderSpawns.entityId(id, 0));
        h.assertTrue(mob != null && level.noCollision(mob), "The replacement must use its deterministic UUID and clear floor");
        h.assertFalse(MansionDefenderSpawns.retry(level, plan), "A loaded guard must not be duplicated");
        mob.discard();
        h.assertFalse(MansionDefenderSpawns.retry(level, plan), "An observed but absent guard must not be replaced");
        AuthoredEncounterEvents.died(new LivingDeathEvent(mob, level.damageSources().generic()));
        h.assertTrue(AuthoredEncounterProgress.get(level).isCleared(id), "A recorded death must clear the one-member roster");
        h.assertFalse(MansionDefenderSpawns.retry(level, plan), "A recorded dead guard must never respawn");
        h.succeed();
    }

    private static void assertBreak(GameTestHelper h, net.minecraft.world.entity.player.Player player, BlockPos pos, boolean blocked) {
        var level = h.getLevel();
        var event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        MansionProtection.breakBlock(event);
        h.assertTrue(event.isCanceled() == blocked, "Reward/construction mining lock mismatch at " + pos);
    }

    private static Map<String, Object> view(MansionBasementPiece cellar, String label, int x, int y, int z, float pitch) {
        var pos = cellar.world(x, y, z);
        return Map.of("label", label, "dimension", GameplayDimensions.PLAINS.location().toString(),
                "x", pos.getX() + .5, "y", (double) pos.getY(), "z", pos.getZ() + .5,
                "yaw", cellar.stairRotation().rotate(Direction.SOUTH).toYRot(), "pitch", pitch);
    }
}
