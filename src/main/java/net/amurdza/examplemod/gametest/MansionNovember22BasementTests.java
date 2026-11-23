package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember22BasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void upperCropRoomFrontIsBrickAndOldCellarsKeepTheirBlueprint(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var entrance = new BlockPos(11, 150, -7);
            var original = new MansionBasementPiece(entrance, rotation, 13);
            var cellar = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(cellar.layoutVersion() == 13, "Persist the repaired upper crop-room wall");
            var blocks = MansionBasementV3Tests.place(h, cellar);
            for (int x = -30; x <= 31; x++) if (Math.abs(x) > 7) {
                for (int y = 1; y <= 5; y++) {
                    var pos = cellar.world(x, y, 18);
                    h.assertTrue(blocks.get(pos) != null && blocks.get(pos).is(Blocks.STONE_BRICKS),
                            "The exposed upper crop-room wall must replace terrain with brick: " + pos);
                    h.assertTrue(cellar.isBuiltCell(pos) && cellar.isBasementBrick(pos),
                            "The new masonry must be included in encounter protection");
                }
            }
            var old = new MansionBasementPiece(entrance, rotation, 12);
            var oldBlocks = MansionBasementV3Tests.place(h, old);
            var restoredOld = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            h.assertTrue(restoredOld.layoutVersion() == 12
                            && !restoredOld.containsAuthoredSpace(old.world(12, 3, 18)),
                    "Do not retrofit an existing saved V12 cellar");
            for (var entry : oldBlocks.entrySet())
                h.assertTrue(entry.getValue().equals(blocks.get(entry.getKey())),
                        "Filling missing terrain must not move crops, lights, passages or existing masonry: " + entry.getKey());
        }
        h.succeed();
    }
}
