package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember15Tests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void authoredTwentyRoomCellarKeepsItsContentsLightingAndRotatedConnections(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 4);
            var piece = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(piece.layoutVersion() == 4, "The authored cellar requires its own saved blueprint version");
            var blocks = MansionBasementV3Tests.place(h, piece);
            java.util.function.Function<BlockPos, BlockState> at = pos -> blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            for (int row = 0; row < 5; row++) for (boolean outer : new boolean[]{false, true}) for (boolean east : new boolean[]{false, true}) {
                var room = MansionBasementLayoutV4.room(east, row, outer);
                int side = east ? 1 : -1, firstX = outer ? 19 : 7, firstZ = 14 + row * 12;
                int lamps = 0, crops = 0, podzol = 0, submergedCane = 0;
                for (int depth = 0; depth < 11; depth++) for (int across = 0; across < 11; across++) {
                    var pos = piece.world(side * (firstX + depth), 1, firstZ + across);
                    if (at.apply(pos.above(3)).is(Blocks.LANTERN)) lamps++;
                    if (at.apply(pos).getBlock() instanceof CropBlock) crops++;
                    if (at.apply(pos.below()).is(Blocks.PODZOL)) podzol++;
                    if (at.apply(pos.below()).is(Blocks.SUGAR_CANE)
                            && at.apply(pos.below()).getValue(BlockStateProperties.WATERLOGGED)) submergedCane++;
                    h.assertTrue(at.apply(pos.above(5)).is(Blocks.STONE_BRICKS), "A room has a hole in its six-high ceiling");
                }
                h.assertTrue(lamps == 5, "Each 11x11 room needs exactly five lanterns: " + room + "/" + lamps);
                for (int depth : new int[]{1, 9}) for (int across : new int[]{1, 9})
                    h.assertTrue(at.apply(piece.world(side * (firstX + depth), 4, firstZ + across)).is(Blocks.LANTERN),
                            "Corner lanterns must sit two diagonal block steps inward from the walls");
                if (room == MansionBasementLayoutV4.Room.MUSHROOM) h.assertTrue(podzol == 64, "Preserve the player's irrigated mushroom quadrants");
                if (room == MansionBasementLayoutV4.Room.CANE) h.assertTrue(submergedCane == 24, "Preserve all three submerged cane rows");
                if (row < 2 || row == 2 && !outer) {
                    h.assertTrue(crops == 64, "Each of the ten paired crop rooms needs its complete authored 64 plants: " + room + "/" + crops);
                    h.assertTrue(room == MansionBasementLayoutV4.room(!east, row, outer), "Matching crops must face each other across the hall");
                }
                for (int a = 4; a <= 6; a++) for (int y = 1; y <= 3; y++) {
                    for (int depth : new int[]{-1, 0}) assertPassable(h, at.apply(piece.world(side * (firstX + depth), y, firstZ + a)), "Room entrance " + room);
                    if (!outer) for (int depth : new int[]{10, 11})
                        assertPassable(h, at.apply(piece.world(side * (firstX + depth), y, firstZ + a)), "Connection to outer room " + room);
                }
                if (row < 4 && room != MansionBasementLayoutV4.Room.SMITH
                        && MansionBasementLayoutV4.room(east, row + 1, outer) != MansionBasementLayoutV4.Room.SMITH)
                    for (int d = 4; d <= 6; d++) for (int a = 10; a <= 12; a++) for (int y = 1; y <= 3; y++)
                        assertPassable(h, at.apply(piece.world(side * (firstX + d), y, firstZ + a)), "Side doorway behind " + room);
            }
            for (int x = -5; x <= 5; x++) for (int z = 14; z <= 72; z++) {
                h.assertTrue(at.apply(piece.world(x, 1, z)).isAir(), "The entire central hall must be eleven blocks wide");
                int light = 0;
                for (int lx : new int[]{-4, 0, 4}) for (int lz = 16; lz <= 72; lz += 4)
                    if (at.apply(piece.world(lx, 4, lz)).is(Blocks.LANTERN))
                        light = Math.max(light, 15 - Math.abs(x - lx) - Math.abs(z - lz) - 3);
                h.assertTrue(light >= 8, "Hall floor-air illumination falls below eight at " + x + "/" + z + ": " + light);
            }
            for (int step = 0; step < 10; step++) for (int x = -2; x <= 2; x++)
                h.assertTrue(at.apply(piece.world(x, 10 - step, step)).is(Blocks.STONE_BRICK_STAIRS)
                                && at.apply(piece.world(x, 12 - step, step)).isAir(), "All five authored stair lanes need ascending headroom");
            for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.CHEST)
                    && entry.getValue().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                var partner = at.apply(entry.getKey().relative(ChestBlock.getConnectedDirection(entry.getValue())));
                h.assertTrue(partner.is(Blocks.CHEST) && partner.getValue(ChestBlock.TYPE) != entry.getValue().getValue(ChestBlock.TYPE),
                        "A doorway removed only half of a rotated double chest at " + entry.getKey());
            }
            h.assertTrue(MansionBasementLayoutV4.animals().size() == 16
                    && MansionBasementLayoutV4.animals().stream().map(MansionBasementLayoutV4.Animal::id).distinct().count() == 4,
                    "Four separate pens must each receive their own four animals");
            for (int x = -30; x <= 30; x++) h.assertTrue(at.apply(piece.world(x, 1, 73)).is(Blocks.STONE_BRICKS),
                    "There must be no outward room doorway through the rear perimeter");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sixLitMineTunnelsHaveMineableStoneAndProtectedBrickBacking(GameTestHelper h) {
        var piece = new MansionBasementPiece(new BlockPos(0, 150, 0), Rotation.NONE, 4);
        for (int row = 2; row < 5; row++) for (int across : new int[]{2, 8}) for (int depth = 0; depth <= 16; depth++) {
            int x = -30 - depth, z = 14 + row * 12 + across;
            for (int y = 1; y <= 3; y++) {
                var state = MansionBasementLayoutV4.cell(x, y, z);
                h.assertTrue(state.isAir() || state.is(Blocks.WALL_TORCH), "The author's two-shaft pattern must stay clear through every mine");
            }
            for (int y : new int[]{0, 4}) h.assertTrue(MansionBasementLayoutV4.cell(x, y, z).is(Blocks.STONE)
                    && !piece.isBuiltCell(piece.world(x, y, z)), "Mine floors and ceilings remain ordinary breakable stone");
            for (int y : new int[]{-1, 5}) h.assertTrue(piece.isBuiltCell(piece.world(x, y, z)), "Mineable stone needs protected outer brick backing");
            h.assertTrue(piece.isBuiltCell(piece.world(-47, 2, z)), "The end of every shaft must retain its solid brick backing");
        }
        h.assertTrue(Blocks.TORCH.defaultBlockState().getLightEmission() == 15
                        && Blocks.WALL_TORCH.defaultBlockState().getLightEmission() == 15
                        && Blocks.SOUL_TORCH.defaultBlockState().getLightEmission() == 10
                        && Blocks.REDSTONE_TORCH.defaultBlockState().getLightEmission() == 7,
                "Only ordinary standing and wall torches change to light level fifteen");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void protectionPacketsKeepNaturalAndCommandGeometryAndMineExceptions(GameTestHelper h) {
        var level = h.getLevel();
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        for (boolean natural : new boolean[]{true, false}) {
            var entrance = h.absolutePos(new BlockPos(natural ? 96 : 256, 0, -96)).atY(210);
            var cellar = new MansionBasementPiece(entrance, Rotation.CLOCKWISE_90, 9);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
            var owner = new ChunkPos(entrance);
            if (natural) level.getChunk(owner.x, owner.z).setStartForStructure(mansion,
                    new StructureStart(mansion, owner, 0, new PiecesContainer(List.of(room, cellar))));
            else PlacedMansions.get(level).record(cellar, List.of(room.getBoundingBox()));
            var brick = cellar.world(-35, -1, 40);
            var stone = brick.above();
            var upstairs = room.getBoundingBox().getCenter();
            for (var point : List.of(brick, upstairs)) {
                var chunk = new ChunkPos(point);
                if (natural) level.getChunk(chunk.x, chunk.z).addReferenceForStructure(mansion, owner.toLong());
                var snapshot = MansionProtectionSnapshot.decode(MansionProtectionSnapshot.capture(level, chunk).encode());
                h.assertTrue(snapshot.protects(point), "The first chunk packet must protect " + (natural ? "natural" : "command") + " mansion masonry");
                if (point.equals(brick)) h.assertTrue(!snapshot.protects(stone), "Client protection must retain the server's mineable-stone exception");
                var packet = new CompoundTag();
                MansionProtectionSnapshot.write(level.getChunkAt(point), packet);
                MansionProtectionSnapshot.read(level.getChunkAt(point), packet);
                h.assertTrue(MansionProtectionSnapshot.protects(level, point), "The actual chunk packet encode/decode path discarded protection");
                MansionProtectionSnapshot.read(level.getChunkAt(point), new CompoundTag());
                h.assertTrue(!MansionProtectionSnapshot.protects(level, point), "Replacing a chunk without mansion data must clear stale client protection");
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedBlueprintsRemainStableAndExplicitReplacementRefreshesTheirProtection(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        var entrance = new BlockPos(8, 150, 8);
        for (int version = 1; version <= 11; version++) {
            var piece = new MansionBasementPiece(entrance, Rotation.COUNTERCLOCKWISE_90, version);
            var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, piece.createTag(serial));
            h.assertTrue(restored.layoutVersion() == version && restored.keyAnchor().equals(piece.keyAnchor()), "Saved layout identity changed");
            for (int x = -48; x <= 44; x += 3) for (int z = 0; z <= 73; z += 3) for (int y : new int[]{-1, 0, 1, 4, 6}) {
                var point = piece.world(x, y, z);
                h.assertTrue(restored.isBuiltCell(point) == piece.isBuiltCell(point), "A saved old blueprint changed its protected geometry");
            }
        }
        var index = new PlacedMansions();
        var old = new MansionBasementPiece(entrance, Rotation.NONE, 3);
        var current = new MansionBasementPiece(entrance);
        var room = BoundingBox.fromCorners(entrance, entrance.offset(7, 8, 7));
        index.record(old, List.of(room));
        index.record(current, List.of(room));
        var restored = PlacedMansions.load(index.save(new CompoundTag()));
        var layouts = restored.at(current.world(0, 1, 72));
        h.assertTrue(layouts.size() == 1 && layouts.get(0).basement().layoutVersion() == current.layoutVersion(),
                "An explicit replacement command must update and deduplicate the saved client/server protection index");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 240)
    public static void widenedAuthoredLandingContainsWaterAndLightsTheActualHallFloor(GameTestHelper h) {
        var level = h.getLevel();
        var cellar = new MansionBasementPiece(h.absolutePos(new BlockPos(768, 0, 768)).atY(180), Rotation.NONE, 4);
        var low = cellar.world(-8, -2, 7);
        var high = cellar.world(8, 8, 19);
        var clip = BoundingBox.fromCorners(low, high);
        var forced = new java.util.ArrayList<ChunkPos>();
        for (int x = clip.minX() >> 4; x <= clip.maxX() >> 4; x++) for (int z = clip.minZ() >> 4; z <= clip.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            if (!level.getForcedChunks().contains(chunk.toLong())) {
                level.setChunkForced(x, z, true);
                forced.add(chunk);
            }
        }
        for (var pos : BlockPos.betweenClosed(low, high)) {
            boolean edge = pos.getX() == low.getX() || pos.getX() == high.getX() || pos.getZ() == low.getZ()
                    || pos.getZ() == high.getZ() || pos.getY() == low.getY() || pos.getY() == high.getY();
            level.setBlock(pos, (edge ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
        }
        for (int x = clip.minX() >> 4; x <= clip.maxX() >> 4; x++) for (int z = clip.minZ() >> 4; z <= clip.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            var chunkClip = new BoundingBox(Math.max(clip.minX(), chunk.getMinBlockX()), clip.minY(), Math.max(clip.minZ(), chunk.getMinBlockZ()),
                    Math.min(clip.maxX(), chunk.getMaxBlockX()), clip.maxY(), Math.min(clip.maxZ(), chunk.getMaxBlockZ()));
            cellar.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(0),
                    chunkClip, chunk, cellar.keyAnchor());
        }
        for (int x : new int[]{-4, 4}) for (int y = 1; y <= 3; y++) {
            var water = cellar.world(x, y, 8);
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
            level.scheduleTick(water, net.minecraft.world.level.material.Fluids.WATER, 5);
        }
        var roofWater = cellar.world(0, 5, 12);
        level.setBlock(roofWater, Blocks.WATER.defaultBlockState(), 3);
        level.scheduleTick(roofWater, net.minecraft.world.level.material.Fluids.WATER, 5);
        h.runAfterDelay(80, () -> {
            try {
                h.assertTrue(!level.getFluidState(cellar.world(0, 5, 11)).isEmpty(), "The fixture must execute real water propagation");
                for (int x = -5; x <= 5; x++) for (int z = 10; z <= 18; z++) for (int y = 1; y <= 3; y++) {
                    var point = cellar.world(x, y, z);
                    h.assertTrue(level.getFluidState(point).isEmpty(), "Water crossed a widened authored landing shoulder or the locked header at " + point);
                }
                for (int x = -5; x <= 5; x++) for (int z = 14; z <= 18; z++) {
                    var point = cellar.world(x, 1, z);
                    int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, point);
                    h.assertTrue(light >= 8, "Actual hallway floor-air block light must be at least eight: " + point + " = " + light);
                }
                h.succeed();
            } finally {
                for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            }
        });
    }

    private static void assertPassable(GameTestHelper h, BlockState state, String detail) {
        h.assertTrue(state.isAir() || state.getBlock() instanceof FenceGateBlock, detail + " is blocked by " + state);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void smallerReplacementInvalidatesWatchedChunksFromThePreviousMansion(GameTestHelper h) {
        var level = h.getLevel();
        var entrance = h.absolutePos(new BlockPos(360, 0, -360)).atY(240);
        var cellar = new MansionBasementPiece(entrance);
        var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
        // StructureStart also includes the 76-block estate margin; put the old wing beyond it.
        var abandoned = BoundingBox.fromCorners(entrance.offset(250, 0, 0), entrance.offset(260, 8, 10));
        var point = abandoned.getCenter();
        var index = PlacedMansions.get(level);
        index.record(cellar, List.of(room.getBoundingBox(), abandoned));
        var chunk = level.getChunkAt(point);
        var oldPacket = new CompoundTag();
        MansionProtectionSnapshot.write(chunk, oldPacket);
        MansionProtectionSnapshot.read(chunk, oldPacket);
        h.assertTrue(MansionProtectionSnapshot.protects(level, point), "The simulated watched chunk must start with the old footprint");
        index.record(cellar, List.of(room.getBoundingBox()));
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var start = new StructureStart(mansion, new ChunkPos(entrance), 0, new PiecesContainer(List.of(room, cellar)));
        var original = start.getBoundingBox();
        h.assertTrue(!original.isInside(point), "The previous wing must lie outside the replacement's bounds");
        var refreshed = MansionProtectionSnapshot.replacementRefreshBounds(level, start);
        h.assertTrue(refreshed.isInside(point) && !start.getBoundingBox().isInside(point),
                "Refresh must include old-only chunks without expanding the saved new structure");
        var packet = new CompoundTag();
        MansionProtectionSnapshot.write(chunk, packet);
        MansionProtectionSnapshot.read(chunk, packet);
        h.assertTrue(!MansionProtectionSnapshot.protects(level, point), "The replacement packet must remove stale client-side mining protection");
        h.assertTrue(!MansionProtectionSnapshot.replacementRefreshBounds(level, start).isInside(point),
                "Consumed old bounds must not permanently expand future refreshes");
        h.succeed();
    }
}
