package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionRoomAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionCurrentDocumentTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void entranceBorderRepairPreservesUnrelatedCorridorCorners(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        for (var rotation : Rotation.values()) {
            var origin = new BlockPos(71, 150, -29);
            var pieces = new java.util.ArrayList<WoodlandMansionPiece>();
            for (var offset : List.of(BlockPos.ZERO, new BlockPos(8, 0, 0), new BlockPos(0, 0, 8)))
                pieces.add(new WoodlandMansionPiece(manager, "corridor_floor", origin.offset(offset.rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_east", origin.offset(new BlockPos(5, 1, 1).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_south_2", origin.offset(new BlockPos(-1, 0, 5).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_west_2", origin.offset(new BlockPos(7, 0, -1).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, "carpet_north", origin.offset(new BlockPos(1, 1, 8).rotate(rotation)), rotation));
            var corner = origin.offset(new BlockPos(5, 1, 5).rotate(rotation));
            h.assertTrue(!MansionRoomAccess.carpetPlan(manager, pieces).red().contains(corner),
                    "The native concave corridor corner must retain its white border: " + rotation);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void continuousWindowHeadersAndExtendedCornersKeepSavedVersions(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        for (String name : List.of("wall_window", "wall_window_top", "wall_flat", "wall_flat_top")) {
            var blocks = cells(manager, "aoe_v9_" + name);
            for (int x = 0; x < 2; x++) for (int z = 0; z < 8; z++)
                h.assertTrue(blocks.get(new BlockPos(x, 6, z)).is(Blocks.DARK_OAK_LOG),
                        "The entire row above the windows must be logs: " + name + "/" + x + "/" + z);
        }
        var oldWindow = cells(manager, "aoe_v8_wall_window_top");
        h.assertTrue(oldWindow.get(new BlockPos(0, 6, 0)).is(Blocks.DARK_OAK_PLANKS),
                "Saved V8 facade resources retain their old band");
        var corner = cells(manager, "aoe_v9_wall_corner_top");
        for (int x : new int[]{7, 8}) for (int z = 0; z < 2; z++)
            h.assertTrue(corner.get(new BlockPos(x, 8, z)).is(x == 8 && z == 1 ? Blocks.COBBLESTONE_WALL : Blocks.COBBLESTONE),
                    "The top corner's complete masonry profile must extend another course");
        h.assertTrue(corner.keySet().stream().noneMatch(pos -> pos.getY() > 8),
                "The extended corner must stop at the eave without a detached stone cap above the roof");
        h.assertTrue(!cells(manager, "aoe_v8_wall_corner").containsKey(new BlockPos(8, 9, 1)),
                "Saved corner height remains unchanged");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void longRoomWallsGainMiddleBracketsWithoutChangingSingleWidthWalls(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        for (String name : List.of("1x2_c1", "2x2_b2")) {
            var blocks = cells(manager, "aoe_v9_" + name);
            int width = name.startsWith("2x2") ? 15 : 7;
            var positions = new java.util.ArrayList<>(List.of(new BlockPos(0, 3, 7), new BlockPos(width - 1, 3, 7)));
            if (width == 15) positions.addAll(List.of(new BlockPos(7, 3, 0), new BlockPos(7, 3, 14)));
            for (var pos : positions) h.assertTrue(blocks.get(pos).is(Blocks.OAK_FENCE)
                            && blocks.get(pos.above()).is(Blocks.TORCH),
                    "Every two-cell side needs its center bracket: " + name + "/" + pos);
            if (width == 7) {
                var old = cells(manager, "aoe_v8_" + name);
                for (var entry : old.entrySet()) if (entry.getKey().getZ() == 0 || entry.getKey().getZ() == 14)
                    h.assertTrue(entry.getValue().equals(blocks.get(entry.getKey())), "Single-cell end walls stay unchanged");
            }
        }
        h.assertTrue(cells(manager, "aoe_v9_1x1_b3").equals(cells(manager, "aoe_v8_1x1_b3")),
                "Single-cell rooms keep all their existing details");
        h.succeed();
    }

    private static Map<BlockPos, BlockState> cells(StructureTemplateManager manager, String name) {
        var template = manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + name)).orElseThrow();
        var data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var result = new HashMap<BlockPos, BlockState>();
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }
}
