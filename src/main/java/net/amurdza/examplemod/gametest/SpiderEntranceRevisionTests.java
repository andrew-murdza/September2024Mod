package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiderEntranceRevisionTests {
    private SpiderEntranceRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void actualEntranceSurroundSealsCapSlitsAcrossChunkSeamsAndReload(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var serial = StructurePieceSerializationContext.fromLevel(level);
        var views = new ArrayList<Map<String, Object>>();
        for (int seed = 0; seed < 4; seed++) {
            // Put the rounded mouth and its terminal wall columns on opposite
            // sides of chunk boundaries, including all four mirrored layouts.
            BlockPos center = new BlockPos(28006 + seed * 160, level.getMaxBuildHeight() - 48, 28006);
            int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
            BlockPos corner1 = center.offset(40 * sx, -8, 40 * sz);
            BlockPos corner2 = center.offset(63 * sx, 16, 63 * sz);
            var box = BoundingBox.fromCorners(corner1, corner2);
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            var chunks = new ArrayList<ChunkPos>();
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
            }
            Collections.reverse(chunks);
            var piece = new BossLairPiece("spider", center, seed);
            for (int i = 0; i < chunks.size(); i++) {
                place(level, piece, center, chunks.get(i));
                if (i == chunks.size() / 2) piece = new BossLairPiece(serial, piece.createTag(serial));
            }
            for (int x = 49; x <= 62; x++) for (int z = 49; z <= 62; z++) {
                int across = Math.abs(x - z), forward = x + z;
                if (across < 6 || across > 10 || forward < 108 || forward > 114) continue;
                for (int y = 1; y <= 8; y++) {
                    BlockPos pos = center.offset(x * sx, y, z * sz);
                    helper.assertTrue(level.getBlockState(pos).is(Blocks.MOSS_BLOCK),
                            "The complete wall beside the rounded mouth must be moss, including both former slits: " + pos);
                }
            }
            for (int y = 1; y <= 8; y++) assertWallConnected(helper, level, center, sx, sz, y);
            for (int step = 44; step <= 58; step++) for (int y = 1; y <= 6; y++) {
                helper.assertTrue(level.getBlockState(center.offset(step * sx, y, step * sz)).isAir(),
                        "Filling side-wall gaps must keep the six-high entrance open");
            }
            var before = snapshot(level, box);
            piece = new BossLairPiece(serial, piece.createTag(serial));
            Collections.reverse(chunks);
            for (var chunk : chunks) place(level, piece, center, chunk);
            helper.assertTrue(before.equals(snapshot(level, box)), "Entrance repairs must survive NBT reload and reversed chunk replay");
            if (seed == 0) {
                views.add(view(level, "spider-entrance-wall-outside", center, 63.5, 2, 63.5, 135));
                views.add(view(level, "spider-entrance-wall-inside", center, 51.5, 2, 51.5, -45));
            }
        }
        Files.writeString(Path.of("spider-entrance-round3-views.json"), new GsonBuilder().setPrettyPrinting().create().toJson(views));
        helper.succeed();
    }

    private static void place(ServerLevel level, BossLairPiece piece, BlockPos center, ChunkPos chunk) {
        var clip = new BoundingBox(chunk.getMinBlockX(), center.getY() - 8, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), center.getY() + 16, chunk.getMaxBlockZ());
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                RandomSource.create(3), clip, chunk, center);
    }

    private static void assertWallConnected(GameTestHelper helper, ServerLevel level, BlockPos center, int sx, int sz, int y) {
        var wall = new HashSet<BlockPos>();
        for (int x = 40; x <= 63; x++) for (int z = 40; z <= 63; z++) {
            if (level.getBlockState(center.offset(x * sx, y, z * sz)).is(Blocks.MOSS_BLOCK)) wall.add(new BlockPos(x, 0, z));
        }
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        // The crop begins inside the descending tunnel: its rear roof and two
        // walls connect to the hillside beyond these two back boundaries.
        // Only those boundaries are roots; isolated outer cap pillars still fail.
        for (var pos : wall) if (pos.getX() == 40 || pos.getZ() == 40) queue.add(pos);
        while (!queue.isEmpty()) {
            var pos = queue.removeFirst();
            if (!wall.contains(pos) || !visited.add(pos)) continue;
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) queue.addLast(pos.relative(direction));
        }
        var disconnected = wall.stream().filter(pos -> !visited.contains(pos)).toList();
        helper.assertTrue(disconnected.isEmpty(), "Every entrance wall cell must connect to the hill at height " + y
                + "; disconnected=" + disconnected.stream().limit(12).toList());
    }

    private static Map<BlockPos, BlockState> snapshot(ServerLevel level, BoundingBox box) {
        var result = new HashMap<BlockPos, BlockState>();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            result.put(pos.immutable(), level.getBlockState(pos));
        }
        return result;
    }

    private static Map<String, Object> view(ServerLevel level, String label, BlockPos center, double x, double y, double z, float yaw) {
        return Map.of("label", label, "dimension", level.dimension().location().toString(),
                "x", center.getX() + x, "y", center.getY() + y, "z", center.getZ() + z, "yaw", yaw, "pitch", 0);
    }
}
