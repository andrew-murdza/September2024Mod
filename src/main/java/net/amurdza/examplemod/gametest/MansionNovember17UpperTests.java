package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionUpperLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember17UpperTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void authoredStairsKeepTheirTreadsWhileCeilingsAndSideWallsChange(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getStructureManager();
        int variant = 0;
        for (var rotation : Rotation.values()) {
            var origin = h.absolutePos(new BlockPos(48 + 32 * variant++, 0, 64)).atY(220);
            var frame = new WoodlandMansionPiece(manager, MansionUpperLayout.STAIR_FRAME,
                    origin.offset(new BlockPos(-1, 0, 0).rotate(rotation)), rotation);
            var core = new WoodlandMansionPiece(manager, MansionUpperLayout.SINGLE_STAIRS, origin, rotation);
            var bounds = frame.getBoundingBox();
            for (var pos : BlockPos.betweenClosed(bounds.minX() - 3, 219, bounds.minZ() - 3, bounds.maxX() + 3, 237, bounds.maxZ() + 3))
                level.setBlock(pos, pos.getY() == 219 ? Blocks.BIRCH_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
            // Shared side walls surround the unchanged one-cell flight.
            for (var piece : new WoodlandMansionPiece[]{frame, core})
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0),
                        bounds, new ChunkPos(origin), origin);
            for (int z = 1; z <= 8; z++) for (int x = 2; x <= 4; x++) {
                var tread = point(origin, rotation, x, z, z);
                h.assertTrue(level.getBlockState(tread).is(Blocks.OAK_STAIRS), "All eight authored treads survive " + rotation + "/" + tread);
                for (int dy = 1; dy <= 4; dy++) {
                    var air = tread.above(dy);
                    h.assertTrue(level.getBlockState(air).getCollisionShape(level, air).isEmpty(),
                            "The authored flight retains its four clear blocks above each tread: " + air);
                }
                h.assertTrue(level.getBlockState(tread.above(5)).is(Blocks.OAK_PLANKS),
                        "The diagonal ceiling must retain its original complete profile above " + tread);
            }
            for (int y = 1; y <= 5; y++) {
                var doorway = point(origin, rotation, 3, y, 0);
                h.assertTrue(level.isEmptyBlock(doorway), "Only the lower doorway header rises by one block");
            }
            h.assertTrue(level.getBlockState(point(origin, rotation, 3, 6, 0)).is(Blocks.OAK_PLANKS)
                            && level.getBlockState(point(origin, rotation, 3, 13, 8)).is(Blocks.OAK_PLANKS),
                    "The lower doorway moves up one while the upper doorway ceiling stays authored");
            h.assertTrue(level.isEmptyBlock(point(origin, rotation, 3, 3, -3)),
                    "There must be no added stair ceiling over the lower hallway");
            for (int x : new int[]{-1, 7}) for (int z = 1; z <= 7; z++) for (int y = 1; y <= 15; y++) if (y != 8) {
                var state = level.getBlockState(point(origin, rotation, x, y, z));
                int centerY = y < 8 ? 4 : 12;
                boolean cross = y == centerY && Math.abs(z - 4) <= 2 || z == 4 && Math.abs(y - centerY) <= 2;
                h.assertTrue(state.is(cross ? Blocks.OAK_LOG : Blocks.OAK_PLANKS), "Sealed side doors need complete centered log crosses");
            }
            for (int x : new int[]{2, 4}) for (int[] yz : new int[][]{{7, 3}, {11, 7}}) {
                var torch = point(origin, rotation, x, yz[0], yz[1]);
                var state = level.getBlockState(torch);
                h.assertTrue(state.is(Blocks.WALL_TORCH) && state.canSurvive(level, torch),
                        "The authored torch pairs retain their original positions and support: " + torch);
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void corniceFillsTheSixPostGapsAndKeepsLegacyTemplates(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var revised = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_1x2_c4_v4"));
        var legacy = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_1x2_c4"));
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 14; z++) if (x == 0 || x == 6 || z == 0 || z == 14) {
            h.assertTrue(nameAt(revised, x, 7, z).equals("minecraft:oak_planks"),
                    "Both the stair trim and its six solid posts need continuous wood above them");
            h.assertTrue(nameAt(legacy, x, 7, z).equals("minecraft:air"), "Saved older rooms must retain their original template");
        }
        var oldStair = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_stair_1x1_v3"));
        var newStair = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/" + MansionUpperLayout.SINGLE_STAIRS));
        h.assertTrue(nameAt(oldStair, 3, 4, 0).equals("minecraft:oak_planks")
                        && nameAt(newStair, 3, 4, 0).equals("minecraft:air"),
                "The taller authored doorway must use a new template identity");
        var priorStair = manager.getOrCreate(new ResourceLocation("minecraft:woodland_mansion/aoe_compact_stair_1x1_v4"));
        h.assertTrue(nameAt(priorStair, 3, 13, 8).equals("minecraft:air")
                        && nameAt(newStair, 3, 13, 8).equals("minecraft:oak_planks"),
                "Restoring the authored ceiling must preserve saved revision-four templates");
        h.succeed();
    }

    private static String nameAt(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
            int x, int y, int z) {
        var data = template.save(new net.minecraft.nbt.CompoundTag());
        var palette = data.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (var entry : data.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var cell = (net.minecraft.nbt.CompoundTag) entry;
            var pos = cell.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
            if (pos.getInt(0) == x && pos.getInt(1) == y && pos.getInt(2) == z)
                return palette.getCompound(cell.getInt("state")).getString("Name");
        }
        return "";
    }

    private static BlockPos point(BlockPos origin, Rotation rotation, int x, int y, int z) {
        return new BlockPos(x, y, z).rotate(rotation).offset(origin);
    }
}
