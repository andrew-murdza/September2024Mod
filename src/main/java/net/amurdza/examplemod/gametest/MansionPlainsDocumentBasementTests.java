package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionPlainsDocumentBasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void raisedCellarHasWallOvensEvenHallLightsAndOneCellStair(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        var entrance = new BlockPos(8, 150, 8);
        for (Rotation rotation : Rotation.values()) {
            var original = new MansionBasementPiece(entrance, rotation, 7);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(cellar.layoutVersion() == 7 && cellar.mainFloorY() == entrance.getY()
                    && cellar.keyAnchor().getY() == entrance.getY() - 7, "The saved cellar must sit one ceiling block below the main floor");
            var blocks = MansionBasementV3Tests.place(h, cellar);
            for (int row : new int[]{3, 4}) {
                int sign = row == 3 ? -1 : 1, firstZ = 14 + row * 12;
                Block oven = row == 3 ? Blocks.FURNACE : Blocks.BLAST_FURNACE;
                for (int across = 0; across < 11; across++) for (int y : new int[]{1, 2}) {
                    assertBlock(h, blocks, cellar.world(sign * 30, y, firstZ + across), oven, "Ovens must occupy the rear wall");
                    assertBlock(h, blocks, cellar.world(sign * 29, y, firstZ + across), Blocks.AIR, "The eleventh interior column must remain clear");
                }
                for (int across : new int[]{0, 10}) {
                    assertBlock(h, blocks, cellar.world(sign * 27, 1, firstZ + across), row == 3 ? Blocks.CRAFTING_TABLE : Blocks.ANVIL,
                            "Keep anvils in place and add a ninth kitchen workbench on both sides");
                    for (int x : new int[]{28, 29}) assertBlock(h, blocks, cellar.world(sign * x, 1, firstZ + across), Blocks.AIR,
                            "There must be two clear blocks between the workstations and wall ovens");
                }
            }
            for (int x : new int[]{-3, 0, 3}) for (int z = 14; z <= 72; z++) {
                boolean lamp = (z - 15) % 4 == 0;
                assertBlock(h, blocks, cellar.world(x, 4, z), lamp ? Blocks.LANTERN : Blocks.AIR, "Shift the complete hall light array toward its door");
                assertBlock(h, blocks, cellar.world(x, 5, z), lamp ? Blocks.CHAIN : Blocks.AIR, "Every shifted lantern needs its chain");
            }
            for (int x = -1; x <= 1; x++) {
                assertBlock(h, blocks, cellar.world(x, 7, 0), Blocks.BIRCH_PLANKS, "The top doorway must have a flat floor");
                for (int z = 1; z <= 7; z++) {
                    int y = 8 - z;
                    var pos = cellar.world(x, y, z);
                    assertBlock(h, blocks, pos, Blocks.STONE_BRICK_STAIRS, "The entire three-wide flight must fit inside one room");
                    h.assertTrue(blocks.get(pos).getValue(StairBlock.FACING) == rotation.rotate(Direction.NORTH), "Rotating the cellar must rotate its ascent");
                    for (int head = 1; head <= 4; head++) assertBlock(h, blocks, pos.above(head), Blocks.AIR, "Every step needs clear headroom");
                }
            }
            for (int x = -6; x <= 6; x++) for (int z = 8; z <= 12; z++) {
                assertBlock(h, blocks, cellar.world(x, 0, z), Blocks.STONE_BRICKS, "The landing needs a solid floor");
                for (int y = 1; y <= 3; y++) assertBlock(h, blocks, cellar.world(x, y, z), Blocks.AIR, "Expand the landing two along the flight and one on either side");
            }
            assertBlock(h, blocks, cellar.firstDoor(), Blocks.IRON_DOOR, "Retain the basement lock at the end of the landing");
            assertBlock(h, blocks, cellar.world(12, 6, 19), Blocks.STONE_BRICKS, "The raised cellar needs its single solid ceiling layer");
            for (var entry : blocks.entrySet()) h.assertTrue(cellar.getBoundingBox().isInside(entry.getKey()), "A clipped stair cell escaped its saved bounds");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cellarVersionsAndWallVariantsPreserveTheirSavedHeight(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (int version = 1; version <= 11; version++) for (boolean walls : new boolean[]{true, false}) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), Rotation.CLOCKWISE_90, version, walls);
            var saved = original.createTag(serial);
            var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, saved);
            h.assertTrue(restored.keyAnchor().equals(original.keyAnchor()) && restored.decorativeWalls() == walls
                    && restored.groundsY() == 150 - (walls ? 1 : 0), "The serialized variant must retain its grading height");
            h.assertTrue(restored.mainFloorOffset() == (version >= 7 ? 7 : 10), "Old saved blueprints must retain their ten-block depth");
            saved.remove("DecorativeWalls");
            var oldSave = (MansionBasementPiece) MansionBasementPiece.load(serial, saved);
            h.assertTrue(oldSave.decorativeWalls(), "A pre-variant save must retain its original wall treatment");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void raisedCellarVariantsKeepCommandAndClientProtectionAfterReload(GameTestHelper h) {
        for (Rotation rotation : Rotation.values()) for (boolean walls : new boolean[]{true, false}) {
            var entrance = new BlockPos(8, 150, 8);
            var cellar = new MansionBasementPiece(entrance, rotation, 7, walls);
            var rooms = List.of(BoundingBox.fromCorners(entrance, entrance.offset(7, 7, 7)));
            var manual = new PlacedMansions();
            manual.record(cellar, rooms);
            var restoredIndex = PlacedMansions.load(manual.save(new CompoundTag()));
            var restoredCellar = restoredIndex.at(cellar.firstDoor()).get(0).basement();
            var packet = MansionProtectionSnapshot.decode(new MansionProtectionSnapshot(rooms, List.of(cellar)).encode());
            for (var result : List.of(restoredCellar, packet.basements().get(0))) {
                h.assertTrue(result.layoutVersion() == 7 && result.keyAnchor().equals(cellar.keyAnchor())
                                && result.stairRotation() == rotation && result.decorativeWalls() == walls
                                && result.mainFloorY() == cellar.mainFloorY() && result.groundsY() == cellar.groundsY(),
                        "Command and client codecs must retain the raised cellar's identity, height and wall variant");
                for (var point : List.of(cellar.firstDoor(), cellar.world(30, 1, 67), cellar.world(-48, 1, 16), cellar.world(0, 7, 1)))
                    h.assertTrue(result.isBuiltCell(point), "Reload shifted the lock, oven wall, mine backing or top stair at " + point);
                h.assertTrue(!result.isBuiltCell(cellar.world(-47, 1, 16)), "Reload must not protect the final mineable stone column");
            }
            h.assertTrue(packet.protects(cellar.world(-48, 1, 16)) && !packet.protects(cellar.world(-47, 1, 16)),
                    "The client packet must protect masonry while preserving the mine's terrain exception");
        }
        h.succeed();
    }

    private static void assertBlock(GameTestHelper h, Map<BlockPos, BlockState> blocks, BlockPos pos, Block expected, String message) {
        h.assertTrue(blocks.containsKey(pos) && blocks.get(pos).is(expected), message + " at " + pos + ": " + blocks.get(pos));
    }
}
