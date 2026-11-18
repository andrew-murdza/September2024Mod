package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionTerrainTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void floorPaddingPreservesUpperRoomsAndRoofWhileOutsideGroundStillGrades(GameTestHelper h) {
        var level = h.getLevel();
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var origin = h.absolutePos(new BlockPos(1280, 0, 1280)).atY(180);
        var manager = level.getStructureManager();
        var lower = new WoodlandMansionPiece(manager, "corridor_floor", origin, Rotation.NONE);
        var upper = new WoodlandMansionPiece(manager, "carpet_south_1", origin.above(8), Rotation.NONE);
        var roof = new WoodlandMansionPiece(manager, net.amurdza.examplemod.worldgen.structure.MansionRoomCatalogue.compact("roof"), origin.above(24), Rotation.NONE);
        var basement = new net.amurdza.examplemod.worldgen.structure.MansionBasementPiece(origin);
        var pieces = new PiecesContainer(List.of(lower, upper, roof, basement));
        var clip = BoundingBox.fromCorners(origin.offset(0, -4, 0), origin.offset(10, 27, 8));
        for (var pos : BlockPos.betweenClosed(clip.minX(), clip.minY(), clip.minZ(), clip.maxX(), clip.maxY(), clip.maxZ()))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        for (var piece : List.of(lower, upper, roof)) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                RandomSource.create(0), clip, new ChunkPos(origin), origin);
        var padded = origin.offset(7, 0, 1);
        var outside = origin.offset(9, 0, 1);
        h.assertTrue(level.isEmptyBlock(padded) && level.getBlockState(padded.above(8)).is(Blocks.BIRCH_PLANKS)
                        && level.getBlockState(padded.above(25)).is(Blocks.OAK_SLAB),
                "Fixture must have an empty ground-floor padding cell below an upper room and capped roof");
        level.setBlock(outside.above(2), Blocks.DIRT.defaultBlockState(), 18);
        for (int pass = 0; pass < 2; pass++) {
            mansion.afterPlace(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0),
                    clip, new ChunkPos(origin), pieces);
            h.assertTrue(level.getBlockState(padded.above(8)).is(Blocks.BIRCH_PLANKS)
                            && level.getBlockState(padded.above(24)).is(Blocks.DARK_OAK_PLANKS)
                            && level.getBlockState(padded.above(25)).is(Blocks.OAK_SLAB),
                    "Ground-floor padding must not erase the authored upper floor or roof, including repeated placement");
            h.assertTrue(level.isEmptyBlock(outside.above(2)) && level.getBlockState(outside.below()).is(Blocks.GRASS_BLOCK),
                    "Ground outside the building must still be cleared and graded");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void mansionEstateFlattensSixtyFourBlocksThenBlendsOverTwelve(GameTestHelper helper) {
        var level = helper.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "illager_mansion"));
        var entrance = helper.absolutePos(new BlockPos(160, 0, 220)).atY(112);
        var corridor = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
        var basement = new net.amurdza.examplemod.worldgen.structure.MansionBasementPiece(entrance, Rotation.NONE, 7);
        var pieces = new PiecesContainer(List.of(corridor, basement));
        int edge = corridor.getBoundingBox().maxX(), z = corridor.getBoundingBox().getCenter().getZ();
        int flat = entrance.getY() - 1;
        // The three columns exercise leveling, half of the taper, and the untouched far edge.
        for (int distance : new int[]{64, 70, 76}) {
            int x = edge + distance;
            BlockPos top = new BlockPos(x, flat + 12, z);
            for (BlockPos pos : BlockPos.betweenClosed(top.below(16), top)) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            }
            var chunk = new ChunkPos(top);
            var clip = new BoundingBox(x, level.getMinBuildHeight(), z, x, level.getMaxBuildHeight()-1, z);
            structure.afterPlace(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    RandomSource.create(519), clip, chunk, pieces);
            int expected = flat + Math.max(0, distance - 64);
            helper.assertTrue(level.getBlockState(new BlockPos(x, expected, z)).is(distance == 76 ? Blocks.DIRT : Blocks.GRASS_BLOCK)
                            && !level.getBlockState(new BlockPos(x, expected+1, z)).is(Blocks.DIRT),
                    "Unexpected estate grade at distance " + distance + ", expected height " + expected);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void roofMoundsClearPerChunkWithoutRemovingUpperRoofsOrOutsideTerrain(GameTestHelper helper) {
        var level = helper.getLevel();
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "illager_mansion"));
        var generator = level.getChunkSource().getGenerator();
        int variant = 0;
        for (Rotation rotation : Rotation.values()) {
            BlockPos origin = helper.absolutePos(new BlockPos(512 + variant++ * 64, 0, 1024)).atY(220);
            var roof = new WoodlandMansionPiece(level.getStructureManager(), "roof", origin, rotation);
            var upper = new WoodlandMansionPiece(level.getStructureManager(), "roof", origin.above(8), rotation);
            upper = new WoodlandMansionPiece(level.getStructureManager(),
                    upper.createTag(StructurePieceSerializationContext.fromLevel(level)));
            var pieces = new PiecesContainer(List.of(roof, upper));
            var box = pieces.calculateBoundingBox();
            int top = box.maxY() + 5;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX() - 1, box.minY(), box.minZ() - 1,
                    box.maxX() + 1, top, box.maxZ() + 1)) {
                level.getChunkAt(pos).setBlockState(pos,
                        pos.getY() == top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), false);
            }
            var completed = new HashSet<ChunkPos>();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    ChunkPos chunk = new ChunkPos(cx, cz);
                    var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                    for (var piece : pieces.pieces()) piece.postProcess(level, level.structureManager(), generator,
                            RandomSource.create(42), clip, chunk, origin);
                    var structureBefore = new HashMap<BlockPos, BlockState>();
                    for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                        if (clip.isInside(pos)) structureBefore.put(pos.immutable(), level.getBlockState(pos));
                    }
                    mansion.afterPlace(level, level.structureManager(), generator, RandomSource.create(42), clip, chunk, pieces);
                    mansion.afterPlace(level, level.structureManager(), generator, RandomSource.create(42), clip, chunk, pieces);
                    structureBefore.forEach((pos, state) -> helper.assertTrue(level.getBlockState(pos).equals(state),
                            "Clearing a lower roof's mound damaged the overlapping upper roof: " + rotation + " / " + pos));
                    completed.add(chunk);
                    for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        for (int y = box.maxY() + 1; y <= top; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            helper.assertTrue(completed.contains(new ChunkPos(pos)) ? level.getBlockState(pos).isAir()
                                            : level.getBlockState(pos).is(y == top ? Blocks.GRASS_BLOCK : Blocks.DIRT),
                                    "Roof terrain clearing must remove the whole mound only in the current chunk: " + pos);
                        }
                    }
                    helper.assertTrue(level.getBlockState(new BlockPos(box.minX() - 1, top, box.minZ())).is(Blocks.GRASS_BLOCK),
                            "Natural terrain beside the roof footprint must survive");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mansionRoomsDoNotRaiseTerrainAtEveryFloor(GameTestHelper helper) {
        var mansion = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "illager_mansion"));
        helper.assertTrue(mansion != null && mansion.terrainAdaptation() == TerrainAdjustment.NONE,
                "Per-piece beard adaptation creates stone and grass shelves around upper mansion floors");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void rotatedReloadedMansionFoundationsReachGroundThroughWaterAndPlants(GameTestHelper helper) {
        var level = helper.getLevel();
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "illager_mansion"));
        var generator = level.getChunkSource().getGenerator();
        int variant = 0;
        for (Rotation rotation : Rotation.values()) {
            BlockPos origin = helper.absolutePos(new BlockPos(256 + variant++ * 64, 0, 512)).atY(220);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", origin, rotation);
            room = new WoodlandMansionPiece(level.getStructureManager(),
                    room.createTag(StructurePieceSerializationContext.fromLevel(level)));
            // An upper room outside the ground-floor footprint must never grow a
            // separate support tower. Also exercise gaps inside the total bounds.
            var upper = new WoodlandMansionPiece(level.getStructureManager(), "1x1_b1", origin.offset(24, 8, 0), rotation);
            var pieces = new PiecesContainer(List.of(room, upper));
            BoundingBox box = room.getBoundingBox();
            int floorY = box.minY();
            for (BlockPos pos : BlockPos.betweenClosed(box.minX() - 1, floorY - 8, box.minZ() - 1,
                    box.maxX() + 1, floorY, box.maxZ() + 1)) {
                int ground = floorY - 8 + Math.floorMod(pos.getX() + pos.getZ(), 3);
                BlockState state = pos.getY() <= ground ? Blocks.STONE.defaultBlockState()
                        : pos.getY() == floorY ? Blocks.AIR.defaultBlockState()
                        : pos.getY() == floorY - 2 ? Blocks.OAK_LEAVES.defaultBlockState()
                        : pos.getY() == floorY - 3 ? Blocks.SEAGRASS.defaultBlockState()
                        : pos.getY() == floorY - 4 ? Blocks.SNOW.defaultBlockState()
                        : Blocks.WATER.defaultBlockState();
                level.getChunkAt(pos).setBlockState(pos, state, false);
            }
            BlockPos unsupportedUpper = upper.getBoundingBox().getCenter().atY(floorY - 1);
            level.getChunkAt(unsupportedUpper).setBlockState(unsupportedUpper, Blocks.WATER.defaultBlockState(), false);
            Set<ChunkPos> completed = new HashSet<>();
            BoundingBox mansionBox = pieces.calculateBoundingBox();
            for (int cx = mansionBox.minX() >> 4; cx <= mansionBox.maxX() >> 4; cx++) {
                for (int cz = mansionBox.minZ() >> 4; cz <= mansionBox.maxZ() >> 4; cz++) {
                    ChunkPos chunk = new ChunkPos(cx, cz);
                    BoundingBox clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                    room.postProcess(level, level.structureManager(), generator, RandomSource.create(42), clip, chunk, origin);
                    mansion.afterPlace(level, level.structureManager(), generator, RandomSource.create(42), clip, chunk, pieces);
                    // Running again must leave the already-built supports intact.
                    mansion.afterPlace(level, level.structureManager(), generator, RandomSource.create(42), clip, chunk, pieces);
                    completed.add(chunk);
                    for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        BlockPos top = new BlockPos(x, floorY - 1, z);
                        if (!completed.contains(new ChunkPos(top))) {
                            helper.assertTrue(level.getBlockState(top).is(Blocks.WATER),
                                    "Foundation wrote into a neighboring chunk before that chunk was placed");
                            continue;
                        }
                        int ground = floorY - 8 + Math.floorMod(x + z, 3);
                        for (int y = ground + 1; y < floorY; y++) {
                            helper.assertTrue(level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBBLESTONE),
                                    "Mansion support stopped on water, leaves or plants above solid ground: " + rotation);
                        }
                        helper.assertTrue(level.getBlockState(new BlockPos(x, ground, z)).is(Blocks.STONE),
                                "Foundation replaced its solid ground anchor");
                    }
                }
            }
            helper.assertTrue(level.getBlockState(new BlockPos(box.minX() - 1, floorY - 1, box.minZ())).is(Blocks.WATER),
                    "Foundation extended outside the ground-floor footprint");
            helper.assertTrue(level.getBlockState(unsupportedUpper).is(Blocks.WATER),
                    "Upper-storey overhang incorrectly grew a support tower");
        }
        helper.succeed();
    }
}
