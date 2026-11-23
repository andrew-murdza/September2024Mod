package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionResidentialPlan;
import net.amurdza.examplemod.worldgen.structure.MansionRoomAccess;
import net.amurdza.examplemod.worldgen.structure.MansionRoomCatalogue;
import net.amurdza.examplemod.worldgen.structure.MansionUpperLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember21UpperTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1000)
    public static void nativeBedroomsAndCenteredFlushEntrancesSurviveSeedsAndRotations(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var names = names(manager);
        var origin = new BlockPos(37, 140, -93);
        var centers = List.of(new BlockPos(-37, 0, -33), new BlockPos(7, 0, 11),
                new BlockPos(-37, 0, 55), new BlockPos(-81, 0, 11));
        for (int seed = 0; seed < 8; seed++) for (var rotation : Rotation.values()) {
            var pieces = new ArrayList<WoodlandMansionPiece>();
            MansionUpperLayout.generate(manager, origin, rotation, pieces, RandomSource.create(seed));
            h.assertTrue(MansionResidentialPlan.bedCapacity(manager, pieces) >= 180, "Native beds must provide at least 180 places");
            h.assertTrue(pieces.stream().filter(piece -> MansionResidentialPlan.isMapRoom(manager, piece)).count() == 1,
                    "Keep exactly one map room");
            h.assertTrue(pieces.stream().noneMatch(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("2x2_b3")),
                    "Neither the removed library nor the invented four-bed room may be selected");
            var entries = pieces.stream().filter(piece -> names.get(piece.template()).equals(MansionRoomCatalogue.ENTRANCE)).toList();
            var actual = entries.stream().map(piece -> local(piece, 3, 0, 0)).collect(Collectors.toSet());
            var expected = centers.stream().map(pos -> pos.rotate(rotation).offset(origin)).collect(Collectors.toSet());
            h.assertTrue(actual.equals(expected), "All four entrances must sit exactly on their facade's central wall plane: " + rotation + "/" + actual);
            var topWindows = pieces.stream().filter(piece -> names.get(piece.template()).equals("aoe_v9_wall_window_top")).toList();
            h.assertTrue(!topWindows.isEmpty() && topWindows.stream().allMatch(piece -> piece.templatePosition().getY() == origin.getY() + 32),
                    "Only fifth-floor windows lose their top cobblestone band");
            h.assertTrue(pieces.stream().filter(piece -> names.get(piece.template()).equals("aoe_v9_wall_window"))
                            .noneMatch(piece -> piece.templatePosition().getY() == origin.getY() + 32),
                    "Every fifth-floor facade section must use the new top trim");
            if (seed == 0) assertEntranceCarpets(h, pieces, entries, manager);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void tightBedBudgetSplitsLargeSlotsIntoNativeRoomsWithSingleDoors(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var pieces = new ArrayList<WoodlandMansionPiece>();
        for (int index = 0; index < 34; index++) {
            var origin = new BlockPos(index * 32, 140, 0);
            pieces.add(new WoodlandMansionPiece(manager, "aoe_compact_2x2_b1", origin, Rotation.NONE));
            pieces.add(new WoodlandMansionPiece(manager, "indoors_door_1", origin.offset(15, 0, 0), Rotation.NONE));
            for (int z : new int[]{0, 8})
                pieces.add(new WoodlandMansionPiece(manager, "corridor_floor", origin.offset(16, 0, z), Rotation.NONE));
        }
        MansionResidentialPlan.finish(manager, pieces, 140);
        var names = names(manager);
        var bedrooms = pieces.stream().filter(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("1x2_d1")).toList();
        h.assertTrue(bedrooms.size() == 60 && MansionResidentialPlan.bedCapacity(manager, pieces) == 180,
                "Thirty old slots must become sixty native three-wide beds");
        h.assertTrue(MansionRoomCatalogue.bedCapacity("aoe_v7_1x1_b3") == 2
                        && MansionRoomCatalogue.bedCapacity("aoe_v7_1x2_c3") == 6
                        && MansionRoomCatalogue.bedCapacity("aoe_v7_1x2_d1") == 3,
                "Bed capacity must count each bed's full width");
        var doors = pieces.stream().filter(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("indoors_door_1")).toList();
        for (var bedroom : bedrooms) {
            var box = bedroom.getBoundingBox();
            h.assertTrue(doors.stream().filter(door -> {
                var wall = door.getBoundingBox();
                return wall.minY() == box.minY() && wall.minX() == box.maxX() + 1
                        && Math.min(wall.maxZ(), box.maxZ()) - Math.max(wall.minZ(), box.minZ()) >= 2;
            }).count() == 1, "Every split bedroom must have one native doorway and no leftover entrance");
            h.assertTrue(box.getXSpan() == 15 && box.getZSpan() == 7, "Native room dimensions must survive splitting");
        }
        var halls = pieces.stream().filter(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("corridor_floor"))
                .map(WoodlandMansionPiece::getBoundingBox).toList();
        for (var passage : MansionRoomAccess.plan(manager, pieces))
            h.assertTrue(halls.stream().anyMatch(hall -> hall.intersects(passage.box())),
                    "Doorway repairs must meet a hallway instead of punching holes between bedrooms");
        h.succeed();
    }

    private static void assertEntranceCarpets(GameTestHelper h, List<WoodlandMansionPiece> pieces,
            List<WoodlandMansionPiece> entries, StructureTemplateManager manager) {
        var probes = entries.stream().flatMap(piece -> java.util.stream.IntStream.rangeClosed(1, 5).boxed()
                .flatMap(x -> java.util.stream.IntStream.rangeClosed(0, 3).mapToObj(z -> local(piece, x, 1, z))))
                .collect(Collectors.toSet());
        var blocks = new HashMap<BlockPos, String>();
        for (var piece : pieces) {
            if (probes.stream().noneMatch(piece.getBoundingBox()::isInside)) continue;
            var data = piece.template().save(new CompoundTag());
            var palette = data.getList("palette", Tag.TAG_COMPOUND);
            for (var value : data.getList("blocks", Tag.TAG_COMPOUND)) {
                var block = (CompoundTag) value;
                var pos = block.getList("pos", Tag.TAG_INT);
                var world = local(piece, pos.getInt(0), pos.getInt(1), pos.getInt(2));
                if (probes.contains(world)) blocks.put(world, palette.getCompound(block.getInt("state")).getString("Name"));
            }
        }
        var repairs = MansionRoomAccess.carpetPlan(manager, pieces);
        repairs.red().forEach(pos -> { if (blocks.containsKey(pos)) blocks.put(pos, "minecraft:red_carpet"); });
        repairs.white().forEach(pos -> { if (blocks.containsKey(pos)) blocks.put(pos, "minecraft:white_carpet"); });
        for (var entry : entries) for (int x = 1; x <= 5; x++) for (int z = 0; z <= 3; z++) {
            String expected = z == 0 && (x == 1 || x == 5) ? "minecraft:dark_oak_planks" : z <= 1 ? "minecraft:air" : z == 2 || x == 1 || x == 5 ? "minecraft:white_carpet" : "minecraft:red_carpet";
            var pos = local(entry, x, 1, z);
            h.assertTrue(expected.equals(blocks.get(pos)),
                    "The pictured rug must end with a white border after one bare plank row: " + pos
                            + ", local=" + x + "/1/" + z + ", rotation=" + entry.getRotation()
                            + ", expected=" + expected + ", actual=" + blocks.get(pos)
                            + ", repairedRed=" + repairs.red().contains(pos) + ", repairedWhite=" + repairs.white().contains(pos));
        }
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void splitBedroomsMirrorSideDoorsToTheActualHallwayHalf(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var pieces = new ArrayList<WoodlandMansionPiece>();
        for (int index = 0; index < 16; index++) {
            var origin = new BlockPos(index * 48, 140, 64);
            pieces.add(new WoodlandMansionPiece(manager, "aoe_compact_2x2_b1", origin, Rotation.NONE));
            pieces.add(new WoodlandMansionPiece(manager, "indoors_door_1", origin.offset(15, 0, 8), Rotation.NONE));
            for (int x : new int[]{-8, 16})
                pieces.add(new WoodlandMansionPiece(manager, "corridor_floor", origin.offset(x, 0, 8), Rotation.NONE));
        }
        MansionResidentialPlan.finish(manager, pieces, 140);
        var names = names(manager);
        var halls = pieces.stream().filter(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("corridor_floor"))
                .map(WoodlandMansionPiece::getBoundingBox).toList();
        var bedrooms = pieces.stream().filter(piece -> MansionRoomCatalogue.nativeName(names.get(piece.template())).equals("1x2_c3")).toList();
        h.assertTrue(bedrooms.size() == 30 && MansionResidentialPlan.bedCapacity(manager, pieces) == 180,
                "Fifteen slots need two native six-place bedrooms each");
        h.assertTrue(bedrooms.stream().anyMatch(piece -> piece.placeSettings().getMirror() != net.minecraft.world.level.block.Mirror.NONE),
                "The east side bedroom must mirror its doorway into the far hallway half");
        for (var bedroom : bedrooms) for (int across = 2; across <= 4; across++) {
            var outside = local(bedroom, 7, 1, across).relative(bedroom.getRotation().rotate(net.minecraft.core.Direction.EAST));
            h.assertTrue(halls.stream().anyMatch(hall -> hall.isInside(outside)),
                    "Every block of the native three-wide doorway must face the actual hallway: " + outside);
        }
        h.succeed();
    }

    private static BlockPos local(WoodlandMansionPiece piece, int x, int y, int z) {
        return StructureTemplate.calculateRelativePosition(piece.placeSettings(), new BlockPos(x, y, z)).offset(piece.templatePosition());
    }

    private static Map<StructureTemplate, String> names(StructureTemplateManager manager) {
        var result = new IdentityHashMap<StructureTemplate, String>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
                .forEach(id -> manager.get(id).ifPresent(template -> result.put(template, id.getPath().substring("woodland_mansion/".length()))));
        return result;
    }
}
