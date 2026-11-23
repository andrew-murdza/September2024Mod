package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelLayout;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelApproach;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelPiece;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Opt-in real-world fixture, built one chunk per tick to keep the desktop responsive. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherCitadelVisualSceneTests {
    @GameTest(template = "plant_gameplay_empty", batch = "citadel_visual", timeoutTicks = 3600)
    public static void exportCompleteCitadel(GameTestHelper helper) {
        if (!Boolean.getBoolean("aoemod.citadelVisual")) { helper.succeed(); return; }
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var server = level.getServer();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        BlockPos origin = null;
        // The authored random-spread grid uses spacing64/separation32. Its last
        // quarter is clear of every natural225-wide citadel, including adjacent regions.
        for (int dz = 0; dz <= 8192; dz += 1024) {
            var candidate = new BlockPos(2824, NetherCitadelLayout.MIN_Y, 2824 + dz);
            var approach = NetherCitadelApproach.sample(candidate, level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState(), level);
            if (approach.isOpen(Direction.SOUTH, 3)) { origin = candidate; break; }
        }
        helper.assertTrue(origin != null, "The visual fixture needs a naturally accessible south gate");
        var piece = new NetherCitadelPiece(origin, 20261121L);
        var box = piece.getBoundingBox();
        var chunks = new ArrayList<ChunkPos>();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        build(helper, level, piece, chunks, 0);
    }

    private static void build(GameTestHelper helper, ServerLevel level, NetherCitadelPiece piece, List<ChunkPos> chunks, int index) {
        if (index < chunks.size()) {
            var chunk = chunks.get(index);
            level.setChunkForced(chunk.x, chunk.z, true);
            level.getChunk(chunk.x, chunk.z);
            var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1),
                    clip, chunk, piece.origin());
            freeze(level, piece);
            if (index % 15 == 0) AOEMod.LOGGER.info("Citadel visual construction: {}/{} chunks", index, chunks.size());
            helper.runAfterDelay(1, () -> build(helper, level, piece, chunks, index + 1));
            return;
        }
        NetherCitadelProgress.retryLoaded(level);
        freeze(level, piece);
        if (!AuthoredEncounterProgress.get(level).pendingDefenders(piece.encounterId()).isEmpty()) {
            helper.runAfterDelay(5, () -> build(helper, level, piece, chunks, index + 1));
            return;
        }
        int verifiedDefenders = 0;
        for (int slot = 0; slot < NetherCitadelProgress.DEFENDERS; slot++) {
            var id = NetherCitadelProgress.entityId(piece.encounterId(), slot);
            var entity = level.getEntity(id);
            helper.assertTrue(entity instanceof Mob mob && mob.isAlive()
                            && AuthoredEncounterProgress.isDefender(mob)
                            && piece.encounterId().equals(AuthoredEncounterProgress.encounterId(mob)),
                    "Visual scene requires a loaded, living defender in slot " + slot + " (" + id
                            + "); saved encounter progress alone does not prove this fixture is complete");
            verifiedDefenders++;
        }
        try {
            var approach = NetherCitadelApproach.sample(piece.origin(), level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState(), level);
            int gateFeetY = approach.floorAt(Direction.SOUTH, 3, 109, 0) + 1;
            var views = new ArrayList<Map<String, Object>>();
            views.add(view("soul-gate", piece.origin(), new Vec3(0, gateFeetY, 109), new Vec3(0, 109, 64), 85));
            views.add(view("soul-grand-hall", piece.origin(), new Vec3(0, 107, 14), new Vec3(0, 110, -8), 85));
            views.add(view("warped-gallery", piece.origin(), new Vec3(0, 75, 14), new Vec3(0, 78, -8), 85));
            views.add(view("crimson-gallery", piece.origin(), new Vec3(0, 43, 14), new Vec3(0, 46, -8), 85));
            views.add(view("wither-sanctum", piece.origin(), new Vec3(0, 19, 14), new Vec3(0, 22, -8), 85));
            views.add(view("crimson-cultivation-room", piece.origin(), new Vec3(36, 43, -66), new Vec3(36, 43, -86), 80));
            views.add(view("connecting-staircase", piece.origin(), new Vec3(36, 92, 13), new Vec3(36, 88, -9), 80));
            Files.writeString(Path.of("citadel-views.json"), new GsonBuilder().setPrettyPrinting().create().toJson(views) + "\n");
            for (var chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
            level.getServer().saveEverything(true, true, true);
            AOEMod.LOGGER.info("Citadel visual scene complete: {} verified loaded, living defenders", verifiedDefenders);
            helper.succeed();
        } catch (Exception failure) {
            throw new IllegalStateException("Could not export citadel visual scene", failure);
        }
    }

    private static void freeze(ServerLevel level, NetherCitadelPiece piece) {
        for (var mob : level.getEntitiesOfClass(Mob.class, AABB.of(piece.getBoundingBox()))) {
            if (piece.encounterId().equals(AuthoredEncounterProgress.encounterId(mob))) {
                mob.setNoAi(true); mob.setNoGravity(true); mob.setInvulnerable(true); mob.setSilent(true);
            }
        }
    }

    private static Map<String, Object> view(String label, BlockPos origin, Vec3 feet, Vec3 target, int fov) {
        Vec3 delta = target.subtract(feet.add(0, 1.62, 0));
        return Map.of("label", label, "dimension", GameplayDimensions.NETHER.location().toString(),
                "x", origin.getX() + feet.x, "y", feet.y, "z", origin.getZ() + feet.z, "fov", fov,
                "yaw", Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                "pitch", -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
    }
}
