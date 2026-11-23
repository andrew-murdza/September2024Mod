package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.SavannaCampPopulationEvents;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampPopulationTests {
    private SavannaCampPopulationTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void peacefulKeepsSavedAuthoredDefendersWithoutChangingOrdinaryHostiles(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var before = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.PEACEFUL, true);
            for (String typeId : new String[]{"orcz:orcshield", "minecraft:vindicator"}) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(typeId));
                var defender = (Mob) type.create(level);
                AuthoredEncounterProgress.markDefender(defender, UUID.randomUUID(), BlockPos.ZERO,
                        typeId.startsWith("orcz:") ? 127 : 8, typeId.startsWith("orcz:") ? "savanna_camp" : "mansion", 0);
                var saved = new CompoundTag(); defender.saveWithoutId(saved);
                var restored = (Mob) type.create(level); restored.load(saved);
                defender.checkDespawn(); restored.checkDespawn();
                helper.assertTrue(!defender.isRemoved() && !restored.isRemoved()
                                && AuthoredEncounterProgress.encounterId(restored).equals(AuthoredEncounterProgress.encounterId(defender)),
                        "Peaceful must preserve both live and reloaded fixed-roster defenders: " + typeId);
            }
            var ordinary = net.minecraft.world.entity.EntityType.VINDICATOR.create(level);
            ordinary.setPersistenceRequired();
            ordinary.checkDespawn();
            helper.assertTrue(ordinary.isRemoved(),
                    "An ordinary persistent hostile without explicit encounter tags must keep vanilla Peaceful behavior");
        } finally {
            server.setDifficulty(before, true);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void blockedDefenderRetainsPendingIdentityThenRetriesWithoutDuplicatesOrResurrection(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var origin = new BlockPos(548000, 200, 548000);
        var id = UUID.randomUUID();
        int index = 63;
        var preferred = origin.offset(SavannaOrcVillage.residentPlan().get(index).local());
        var forcedHere = new java.util.ArrayList<ChunkPos>();
        // Block reads load chunks without making their entities visible to
        // ServerLevel.getEntity. Activate only the retry/collision halo, then
        // allow its entity sections to become tracked before checking UUIDs.
        for (int x = (preferred.getX() - 8) >> 4; x <= (preferred.getX() + 8) >> 4; x++)
            for (int z = (preferred.getZ() - 8) >> 4; z <= (preferred.getZ() + 8) >> 4; z++) {
                var chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) {
                    level.setChunkForced(x, z, true);
                    forcedHere.add(chunk);
                }
                level.getChunk(x, z);
            }
        helper.runAfterDelay(2, () -> {
            try {
                verifyBlockedResidentRetry(helper, level, origin, id, index, preferred);
            } finally {
                for (var chunk : forcedHere) level.setChunkForced(chunk.x, chunk.z, false);
            }
        });
    }

    private static void verifyBlockedResidentRetry(GameTestHelper helper, ServerLevel level, BlockPos origin,
                                                   UUID id, int index, BlockPos preferred) {
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -1; dy <= 4; dy++)
            level.setBlock(preferred.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(preferred.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        // Stone is unavailable in Savanna and its placement is rejected by the
        // dimension rules. Use a real permitted obstacle to exercise collision.
        level.setBlock(preferred, Blocks.ACACIA_WOOD.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockState(preferred).is(Blocks.ACACIA_WOOD),
                "The blocked-spawn fixture must contain a solid obstacle at the authored position");
        var progress = AuthoredEncounterProgress.get(level);
        SavannaCampProgress.register(level, id, origin);
        helper.assertFalse(SavannaOrcVillage.retryResident(level, origin, id, index),
                "A blocked defender must not be forced into a collision or moved onto a roof");
        helper.assertTrue(progress.pendingDefenders(id).get(index), "Failed placement must remain pending");
        var restored = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        helper.assertTrue(restored.pendingDefenders(id).get(index) && restored.canPlaceDefender(id, index),
                "The pending roster must survive server saves even when the command-placed structure piece no longer exists");
        // Leave the authored cell blocked but provide a safe same-floor forest alternative.
        var alternate = preferred.offset(2, 0, 0);
        level.setBlock(alternate.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockState(alternate.below()).is(Blocks.GRASS_BLOCK)
                        && level.getBlockState(alternate).isAir()
                        && level.getBlockState(preferred).is(Blocks.ACACIA_WOOD),
                "The alternate must have a clear supported floor while the authored cell stays blocked");
        helper.assertTrue(SavannaOrcVillage.retryResident(level, origin, id, index),
                "A permanently blocked authored cell must use a clear nearby floor cell in the same forest corner");
        var entity = level.getEntity(SavannaOrcVillage.residentId(id, index));
        helper.assertTrue(entity instanceof Mob,
                "The successfully inserted retry must be visible under its deterministic UUID");
        helper.assertTrue(entity.blockPosition().equals(alternate) && level.noCollision(entity)
                        && !progress.pendingDefenders(id).get(index),
                "Successful retry must use the clear alternate floor and clear pending state: " + entity.position());
        helper.assertFalse(SavannaOrcVillage.retryResident(level, origin, id, index),
                "An observed defender must not be duplicated by later retry passes");
        var mob = (Mob) entity;
        mob.discard();
        helper.assertFalse(SavannaOrcVillage.retryResident(level, origin, id, index),
                "An absent or unloaded observed defender must not be mistaken for a failed original placement");
        progress.defeat(mob);
        helper.assertFalse(SavannaOrcVillage.retryResident(level, origin, id, index),
                "A dead authored defender must never be recreated by population repair");
        restored = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        helper.assertTrue(restored.defeatedCount(id) == 1 && !restored.canPlaceDefender(id, index)
                        && !restored.pendingDefenders(id).get(index),
                "Saved death records must permanently suppress retries for that defender");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void replacementCampSupersedesOldPendingRosterAndMissingPieceIdsAreStable(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var origin = new BlockPos(549000, 200, 549000);
        var oldId = UUID.randomUUID(); var replacementId = UUID.randomUUID();
        int index = 63;
        var pos = origin.offset(SavannaOrcVillage.residentPlan().get(index).local());
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -1; dy <= 4; dy++)
            level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        SavannaCampProgress.register(level, oldId, origin);
        SavannaCampProgress.register(level, replacementId, origin);
        SavannaCampPopulationEvents.retryLoaded(level);
        helper.assertTrue(level.getEntity(SavannaOrcVillage.residentId(oldId, index)) == null
                        && level.getEntity(SavannaOrcVillage.residentId(replacementId, index)) instanceof Mob,
                "Only the newest camp at an explicitly replaced origin may retry its missing defenders");
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = new SavannaOrcCampPiece(level.getStructureManager(), origin).createTag(context);
        tag.remove("CampEncounterId");
        var first = new SavannaOrcCampPiece(context, tag);
        var second = new SavannaOrcCampPiece(context, tag);
        helper.assertTrue(first.encounterId().equals(second.encounterId())
                        && new SavannaOrcCampPiece(context, first.createTag(context)).encounterId().equals(first.encounterId()),
                "A missing UUID in an incomplete V11 tag must recover one stable identity across repeated loads");
        helper.succeed();
    }
}
