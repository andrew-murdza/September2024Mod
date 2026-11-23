package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember21BasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void lowerCellarFrontWallsCoverTerrainAcrossRotatedChunkBoundaries(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(11, 150, -7), rotation, 12);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(cellar.layoutVersion() == 12, "New cellars must persist the completed front-wall layout");
            var blocks = MansionBasementV3Tests.place(h, cellar);

            // These surfaces face the first row of lower rooms and the lower
            // landing. Each must replace terrain, including dirt and ores.
            for (int x = -30; x <= 31; x++) {
                if (Math.abs(x) <= 7) continue;
                for (int y = -9; y <= -1; y++) assertBrick(h, blocks, cellar, x, y, 25);
            }
            for (int x = -48; x <= -31; x++) for (int y = -8; y <= -2; y++)
                assertBrick(h, blocks, cellar, x, y, 25);
            for (int sign : new int[]{-1, 1}) for (int x = 2; x <= 7; x++) for (int y = -9; y <= -1; y++)
                assertBrick(h, blocks, cellar, sign * x, y, 19);

            // Keep the existing two-storey contents and all working controls.
            // Only masonry on the two missing front wall planes may differ.
            var legacy = new MansionBasementPiece(new BlockPos(11, 150, -7), rotation, 11);
            var oldBlocks = MansionBasementV3Tests.place(h, legacy);
            for (var entry : oldBlocks.entrySet()) {
                var local = entry.getKey().subtract(cellar.keyAnchor()).rotate(inverse(rotation));
                boolean frontWall = local.getY() >= -9 && local.getY() <= -1
                        && (local.getZ() == 25 && Math.abs(local.getX()) > 7
                        || local.getZ() == 19 && Math.abs(local.getX()) >= 2 && Math.abs(local.getX()) <= 7);
                if (!frontWall) h.assertTrue(entry.getValue().equals(blocks.get(entry.getKey())),
                        "Completing the front walls must preserve rooms, stairs, doors, levers and lamps at " + local);
            }
            for (var pos : blocks.keySet()) h.assertTrue(cellar.getBoundingBox().isInside(pos),
                    "A front-wall block escaped the serialized cellar bounds");
            h.assertTrue(cellar.doorLeaves().equals(legacy.doorLeaves())
                            && cellar.lowestFloorY() == legacy.lowestFloorY(),
                    "Front walls must not move either floor or any door");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void newFrontWallsRetainProtectionAndSavedElevenKeepsItsOriginalLayout(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var entrance = new BlockPos(11, 150, -7);
            var cellar = new MansionBasementPiece(entrance, rotation, 12);
            var rooms = List.of(BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8)));
            var index = new PlacedMansions();
            index.record(cellar, rooms);
            var restoredIndex = PlacedMansions.load(index.save(new CompoundTag())).at(cellar.firstDoor()).get(0).basement();
            var packet = MansionProtectionSnapshot.decode(new MansionProtectionSnapshot(rooms, List.of(cellar)).encode());
            for (var restored : List.of(restoredIndex, packet.basements().get(0))) {
                h.assertTrue(restored.layoutVersion() == 12 && restored.stairRotation() == rotation,
                        "Both protection codecs must preserve the new wall layout and rotation");
                for (var pos : List.of(cellar.world(12, -5, 25), cellar.world(-12, -5, 25),
                        cellar.world(-40, -5, 25), cellar.world(5, -4, 19)))
                    h.assertTrue(restored.isBuiltCell(pos) && restored.isBasementBrick(pos),
                            "New front-wall masonry must remain protected after reload at " + pos);
            }
            var old = new MansionBasementPiece(entrance, rotation, 11);
            var restoredOld = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            h.assertTrue(restoredOld.layoutVersion() == 11
                            && !restoredOld.containsAuthoredSpace(old.world(12, -5, 25))
                            && !restoredOld.containsAuthoredSpace(old.world(5, -4, 19)),
                    "Saved V11 pieces must retain their original blueprint instead of silently gaining walls");
        }
        h.succeed();
    }

    private static void assertBrick(GameTestHelper h, Map<BlockPos, BlockState> blocks,
                                    MansionBasementPiece cellar, int x, int y, int z) {
        var pos = cellar.world(x, y, z);
        var state = blocks.get(pos);
        h.assertTrue(state != null && state.is(Blocks.STONE_BRICKS),
                "The lower cellar front must have continuous brick instead of exposed terrain at " + pos + ": " + state);
    }

    private static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }
}
