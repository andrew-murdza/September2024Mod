package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Isolated insertion regressions do not require regenerating a full fortress. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressSpawnTests {
    private GoblinFortressSpawnTests() { }

    public static final class CancelResident {
        private final UUID id;
        private int cancellations;

        CancelResident(UUID id) { this.id = id; }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void joined(EntityJoinLevelEvent event) {
            if (event.getEntity().getUUID().equals(id)) {
                cancellations++;
                event.setCanceled(true);
            }
        }
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void canceledInsertionPersistsThenRetriesWithoutDuplicatesOrResurrection(GameTestHelper helper) {
        var level = helper.getLevel();
        var chunk = new ChunkPos(helper.absolutePos(new BlockPos(8, 0, 2)));
        // Keep the complete fixture and every possible candidate inside its own ticketed chunk.
        var origin = chunk.getMiddleBlockPosition(180);
        boolean forcedHere = !level.getForcedChunks().contains(chunk.toLong());
        try {
            if (forcedHere) level.setChunkForced(chunk.x, chunk.z, true);
            level.getChunk(chunk.x, chunk.z);
            helper.runAfterDelay(1, () -> awaitReady(helper, level, origin, chunk, forcedHere, 1));
        } catch (RuntimeException | Error failure) {
            if (forcedHere) level.setChunkForced(chunk.x, chunk.z, false);
            throw failure;
        }
    }

    private static void awaitReady(GameTestHelper helper, ServerLevel level, BlockPos origin,
                                   ChunkPos chunk, boolean forcedHere, int waited) {
        boolean entitiesLoaded = level.areEntitiesLoaded(chunk.toLong());
        boolean entityTicking = level.isPositionEntityTicking(origin.above());
        if ((!entitiesLoaded || !entityTicking) && waited < 200) {
            helper.runAfterDelay(1, () -> awaitReady(helper, level, origin, chunk, forcedHere, waited + 1));
            return;
        }
        try {
            helper.assertTrue(entitiesLoaded && entityTicking,
                    "The fixture chunk must be loaded and entity-ticking before testing insertion: " + chunk
                            + ", entitiesLoaded=" + entitiesLoaded + ", entityTicking=" + entityTicking);
            verifyRetry(helper, level, origin);
            helper.succeed();
        } finally {
            if (forcedHere) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }

    private static void verifyRetry(GameTestHelper helper, ServerLevel level, BlockPos origin) {
        var previous = new LinkedHashMap<BlockPos, BlockState>();
        var residents = new ArrayList<Mob>();
        try {
            for (var mutable : BlockPos.betweenClosed(origin.offset(-3, -1, -2), origin.offset(3, 5, 2))) {
                var pos = mutable.immutable();
                previous.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
            level.setBlock(origin, Blocks.STONE_BRICKS.defaultBlockState(), 18);
            var plan = new GoblinFortressSpawns.Plan(UUID.randomUUID(), origin, 0, "minecraft:stray", origin.above(),
                    BoundingBox.fromCorners(origin.above(), origin.offset(0, 4, 0)), false, false);
            var queue = GoblinFortressSpawns.get(level);
            var cancellation = new CancelResident(GoblinFortressSpawns.entityId(plan));
            MinecraftForge.EVENT_BUS.register(cancellation);
            try {
                queue.plan(level, plan);
                helper.assertFalse(GoblinFortressSpawns.retry(level, plan), "A canceled Forge join is not a successful authored spawn");
                helper.assertTrue(cancellation.cancellations == 1,
                        "The retry must reach and actually cancel one EntityJoinLevelEvent, not return early for an unavailable chunk");
                helper.assertTrue(queue.plans().contains(plan) && level.getEntity(GoblinFortressSpawns.entityId(plan)) == null,
                        "Canceled insertion must leave the exact original plan pending");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(cancellation);
            }
            var saved = GoblinFortressSpawns.load(queue.save(new CompoundTag()));
            helper.assertTrue(saved.plans().contains(plan), "Canceled plans must survive a server save and reload");
            helper.assertTrue(GoblinFortressSpawns.retry(level, plan), "A later allowed insertion must complete the original slot");
            var defender = (Mob) level.getEntity(GoblinFortressSpawns.entityId(plan));
            if (defender != null) residents.add(defender);
            helper.assertTrue(defender != null && AuthoredEncounterProgress.isDefender(defender) && level.noCollision(defender),
                    "The successful retry must be a real tracked collision-free defender");
            helper.assertFalse(GoblinFortressSpawns.retry(level, plan), "A repeated retry must not duplicate a loaded resident");
            var occupied = new GoblinFortressSpawns.Plan(plan.encounter(), origin, 1, "minecraft:stray", plan.preferred(), plan.room(), false, false);
            queue.plan(level, occupied);
            helper.assertFalse(GoblinFortressSpawns.retry(level, occupied),
                    "A different roster slot must not overlap an existing living mob even when vanilla noCollision allows it");
            helper.assertTrue(queue.plans().contains(occupied), "An occupied original floor must leave that distinct slot pending");
            defender.discard();
            helper.assertFalse(GoblinFortressSpawns.retry(level, plan), "An absent previously observed mob is not a failed original spawn");
            helper.assertTrue(GoblinFortressSpawns.retry(level, occupied), "The pending distinct slot may use the floor after its occupant leaves");
            var replacement = (Mob) level.getEntity(GoblinFortressSpawns.entityId(occupied));
            if (replacement != null) residents.add(replacement);
            helper.assertTrue(replacement != null && replacement.getUUID().equals(GoblinFortressSpawns.entityId(occupied)),
                    "The formerly occupied floor must receive the pending slot, never resurrect its previous occupant");
            replacement.discard();
            AuthoredEncounterProgress.get(level).defeat(replacement);
            AuthoredEncounterProgress.get(level).defeat(defender);
            queue.plan(level, plan);
            helper.assertFalse(GoblinFortressSpawns.retry(level, plan), "Recorded dead defenders must never be resurrected");
        } finally {
            residents.forEach(Mob::discard);
            previous.forEach((pos, state) -> level.setBlock(pos, state, 18));
        }
    }
}
