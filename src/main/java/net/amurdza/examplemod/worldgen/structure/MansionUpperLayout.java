package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Eleven-by-eleven native room cells, five complete eight-block storeys, and versioned templates. */
public final class MansionUpperLayout {
    public static final int GRID_SIZE = 11;
    public static final int FLOOR_COUNT = 5;
    private static final int ONE_CELL = 65536, ORIGIN = 1048576, DOOR = 2097152;
    public static final String SINGLE_STAIRS = "aoe_v6_stair_1x1";
    public static final String STAIR_FRAME = "aoe_v6_stair_frame";
    public static final String BASEMENT_LOBBY = "aoe_v6_basement_lobby";
    public static final String BASEMENT_FRAME = "aoe_v6_basement_frame";
    public static final String ENTRANCE = MansionRoomCatalogue.ENTRANCE;

    private MansionUpperLayout() { }

    public static void generate(StructureTemplateManager manager, BlockPos origin, Rotation rotation,
            List<WoodlandMansionPiece> output, RandomSource random) {
        var grid = new WoodlandMansionPieces.MansionGrid(random);
        grid.entranceX = 9;
        for (int x = 0; x < GRID_SIZE; x++) for (int z = 0; z < GRID_SIZE; z++) {
            int value = x == 2 || x == 5 || x == 8 || z == 2 || z == 5 || z == 8 ? 1 : 2;
            // Short branches meet the upper stair doors and the four exterior
            // entrances; every branch meets the continuous corridor ring.
            if (value != 5 && (x == 4 && z == 3 || x == 7 && z == 6 || x == 5 && z == 0
                    || z == 4 && (x <= 2 || x >= 7)
                    || x == 4 && (z <= 2 || z >= 8))) value = 1;
            if (x == 5 && z == 1) value = 2;
            grid.baseGrid.set(x, z, value);
            grid.thirdFloorGrid.set(x, z, value);
            for (var rooms : grid.floorRooms) rooms.set(x, z, 0);
        }
        // Reserve the exact cells before native room grouping so neither a
        // two-cell room nor its furniture can straddle a staircase.
        reserve(grid, 0, 3, 3, 1001);
        reserve(grid, 1, 3, 3, 1002);
        reserve(grid, 1, 6, 6, 1003);
        reserve(grid, 2, 6, 6, 1004);
        // The raised room ceilings must fit below the actual facade, whose
        // footprint is narrower than the projecting roof. Mines sit one
        // block lower and can safely continue beneath the lawn to the west.
        reserve(grid, 0, 5, 1, 1005);
        for (int floor = 0; floor < 3; floor++) reserveHallAccess(grid, floor, random);
        grid.identifyRooms(grid.baseGrid, grid.floorRooms[0]);
        grid.identifyRooms(grid.baseGrid, grid.floorRooms[1]);
        grid.identifyRooms(grid.thirdFloorGrid, grid.floorRooms[2]);
        new WoodlandMansionPieces.MansionPiecePlacer(manager, random).createMansion(origin, rotation, output, grid);

        Map<StructureTemplate, String> names = new HashMap<>();
        manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("woodland_mansion/"))
                .forEach(id -> manager.get(id).ifPresent(template -> names.put(template, id.getPath().substring("woodland_mansion/".length()))));
        var adapted = new ArrayList<WoodlandMansionPiece>();
        for (var piece : output) {
            String name = names.get(piece.template());
            if (name == null) throw new IllegalStateException("Unknown native mansion template");
            int height = piece.templatePosition().getY() - origin.getY();
            if (name.startsWith("small_wall") || name.equals("entrance")) continue;
            if (name.startsWith("roof")) {
                var roof = MansionRoofLayout.adapt(manager, name, piece);
                roof.move(0, 16, 0);
                adapted.add(roof);
                continue;
            }
            int nativeFloor = height >= 19 ? 2 : height >= 8 ? 1 : 0;
            // Reuse the middle-floor layout for two additional full storeys.
            int[] targetFloors = nativeFloor == 1 ? new int[]{1, 2, 3} : new int[]{nativeFloor == 2 ? 4 : 0};
            for (int floor : targetFloors) {
                String replacement = MansionRoomCatalogue.replacement(name, floor == 0, random);
                if (name.equals("wall_corner")) replacement = "aoe_compact_wall_corner_v3";
                if (replacement.equals(MansionRoomCatalogue.compact("1x2_d1"))) replacement = "aoe_compact_1x2_d1_v3";
                if (replacement.equals(MansionRoomCatalogue.compact("1x2_c4"))) replacement = "aoe_compact_1x2_c4_v4";
                if (name.startsWith("1x") || name.startsWith("2x")) {
                    var box = piece.getBoundingBox();
                    boolean reserved = (floor < 4 && containsColumn(box, cell(origin, rotation, 3, 3, 0)))
                            || (floor > 0 && containsColumn(box, cell(origin, rotation, 6, 6, 0)))
                            || (floor == 0 && containsColumn(box, cell(origin, rotation, 5, 1, 0)));
                    if (reserved) continue;
                }
                int nativeY = nativeFloor == 2 ? 19 : nativeFloor * 8;
                adapted.add(new WoodlandMansionPiece(manager, replacement, piece.templatePosition().above(floor * 8 - nativeY),
                        piece.getRotation(), piece.placeSettings().getMirror()));
            }
        }
        // The native perimeter omits the two foyer wall sections on its first
        // two floors. Replace them now that the foyer is ordinary room space.
        for (int floor = 0; floor < 4; floor++) for (int z : new int[]{0, 8})
            adapted.add(new WoodlandMansionPiece(manager, "wall_window", transform(origin, rotation, 7, floor * 8, z), rotation));

        // Ceiling tiles precede roof pieces so the stair slope may shape only
        // its outer edge while the full third-storey interior stays covered.
        var roofs = new ArrayList<WoodlandMansionPiece>();
        adapted.removeIf(piece -> {
            if (MansionRoofLayout.isRoof(piece)) {
                roofs.add(piece);
                return true;
            }
            return false;
        });
        for (int x = 0; x < GRID_SIZE; x++) for (int z = 0; z < GRID_SIZE; z++)
            adapted.add(new WoodlandMansionPiece(manager, "aoe_compact_ceiling_v3", cell(origin, rotation, x, z, 40), rotation));
        roofs.sort(java.util.Comparator.comparingInt(MansionRoofLayout::placementOrder));
        adapted.addAll(roofs);
        addStair(manager, adapted, origin, rotation, 3, 3, 0, Rotation.COUNTERCLOCKWISE_90);
        addStair(manager, adapted, origin, rotation, 6, 6, 8, Rotation.CLOCKWISE_90);
        addStair(manager, adapted, origin, rotation, 3, 3, 16, Rotation.COUNTERCLOCKWISE_90);
        addStair(manager, adapted, origin, rotation, 6, 6, 24, Rotation.CLOCKWISE_90);
        var lobbyOrigin = cell(origin, rotation, 5, 1, 0).relative(rotation.rotate(Direction.NORTH));
        adapted.add(new WoodlandMansionPiece(manager, BASEMENT_LOBBY, lobbyOrigin, rotation));
        adapted.add(new WoodlandMansionPiece(manager, BASEMENT_FRAME,
                lobbyOrigin.offset(new BlockPos(-1, 0, 0).rotate(rotation)), rotation));
        addEntrance(manager, adapted, origin, rotation, 4, 0, Rotation.NONE);
        addEntrance(manager, adapted, origin, rotation, 10, 4, Rotation.CLOCKWISE_90);
        addEntrance(manager, adapted, origin, rotation, 4, 10, Rotation.CLOCKWISE_180);
        addEntrance(manager, adapted, origin, rotation, 0, 4, Rotation.COUNTERCLOCKWISE_90);
        MansionResidentialPlan.finish(manager, adapted, origin.getY());
        output.clear();
        output.addAll(adapted);
    }

    public record Landing(BlockPos position, Rotation rotation) { }

    /** Main-floor doorway at which the independently versioned cellar begins. */
    public static Landing basementLanding(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        var template = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + BASEMENT_LOBBY));
        var lobby = pieces.stream().filter(piece -> piece.template() == template).findFirst().orElseThrow();
        return new Landing(StructureTemplate.calculateRelativePosition(lobby.placeSettings(), new BlockPos(3, 0, 0))
                .offset(lobby.templatePosition()), lobby.getRotation());
    }

    private static void reserve(WoodlandMansionPieces.MansionGrid grid, int floor, int x, int z, int id) {
        grid.floorRooms[floor].set(x, z, ONE_CELL | ORIGIN | DOOR | id);
    }

    /** Keep deep cells in a room that actually borders a hall, never a secret pocket. */
    private static void reserveHallAccess(WoodlandMansionPieces.MansionGrid grid, int floor, RandomSource random) {
        var shape = floor == 2 ? grid.thirdFloorGrid : grid.baseGrid;
        var rooms = grid.floorRooms[floor];
        int id = 1100;
        for (int x = 0; x < GRID_SIZE; x++) for (int z = 0; z < GRID_SIZE; z++) {
            if (shape.get(x, z) != 2 || rooms.get(x, z) != 0 || shape.edgesTo(x, z, 1)) continue;
            var choices = new ArrayList<int[]>();
            for (int dx : new int[]{-1, 0}) for (int dz : new int[]{-1, 0}) {
                int minX = x + dx, minZ = z + dz;
                boolean available = true, hall = false;
                for (int a = minX; a <= minX + 1; a++) for (int b = minZ; b <= minZ + 1; b++) {
                    available &= shape.get(a, b) == 2 && rooms.get(a, b) == 0;
                    hall |= shape.edgesTo(a, b, 1);
                }
                if (available && hall) choices.add(new int[]{minX, minZ, minX + 1, minZ + 1});
            }
            if (choices.isEmpty()) for (var direction : Direction.Plane.HORIZONTAL) {
                int a = x + direction.getStepX(), b = z + direction.getStepZ();
                if (shape.get(a, b) == 2 && rooms.get(a, b) == 0 && shape.edgesTo(a, b, 1))
                    choices.add(new int[]{Math.min(x, a), Math.min(z, b), Math.max(x, a), Math.max(z, b)});
            }
            if (choices.isEmpty()) throw new IllegalStateException("No hall-facing mansion room for cell " + x + "/" + z);
            int[] rectangle = choices.get(random.nextInt(choices.size()));
            var doors = new ArrayList<BlockPos>();
            for (int a = rectangle[0]; a <= rectangle[2]; a++) for (int b = rectangle[1]; b <= rectangle[3]; b++)
                if (shape.edgesTo(a, b, 1)) doors.add(new BlockPos(a, 0, b));
            var door = doors.get(random.nextInt(doors.size()));
            int size = rectangle[0] != rectangle[2] && rectangle[1] != rectangle[3] ? 262144 : 131072;
            for (int a = rectangle[0]; a <= rectangle[2]; a++) for (int b = rectangle[1]; b <= rectangle[3]; b++)
                rooms.set(a, b, size | id | (a == door.getX() && b == door.getZ() ? ORIGIN | DOOR : 0));
            id++;
        }
    }

    private static boolean containsColumn(net.minecraft.world.level.levelgen.structure.BoundingBox box, BlockPos cell) {
        return cell.getX() >= box.minX() && cell.getX() <= box.maxX()
                && cell.getZ() >= box.minZ() && cell.getZ() <= box.maxZ();
    }

    private static BlockPos cell(BlockPos origin, Rotation rotation, int x, int z, int y) {
        return transform(origin, rotation, (x - 10) * 8, y, (z - 4) * 8);
    }

    private static BlockPos transform(BlockPos origin, Rotation rotation, int x, int y, int z) {
        return new BlockPos(x, y, z).rotate(rotation).offset(origin);
    }

    private static void addStair(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces,
            BlockPos origin, Rotation rotation, int x, int z, int y, Rotation direction) {
        // The 7x7 interior begins at template Z1; transform that interior's
        // minimum corner, then allow its two doors to overlap the shared walls.
        BlockPos offset = StructureTemplate.getZeroPositionWithTransform(BlockPos.ZERO, Mirror.NONE, direction, 7, 7)
                .subtract(new BlockPos(0, 0, 1).rotate(direction));
        var stairOrigin = cell(origin, rotation, x, z, y).offset(offset.rotate(rotation));
        var stairRotation = rotation.getRotated(direction);
        // The shared frame seals the native side doors without changing the
        // authored interior ceiling or projecting into the lower hallway.
        pieces.add(new WoodlandMansionPiece(manager, STAIR_FRAME,
                stairOrigin.offset(new BlockPos(-1, 0, 0).rotate(stairRotation)), stairRotation));
        pieces.add(new WoodlandMansionPiece(manager, SINGLE_STAIRS, stairOrigin, stairRotation));
    }

    private static void addEntrance(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces,
            BlockPos origin, Rotation rotation, int x, int z, Rotation direction) {
        BlockPos offset = StructureTemplate.getZeroPositionWithTransform(BlockPos.ZERO, Mirror.NONE, direction, 7, 7)
                .subtract(new BlockPos(0, 0, 2).rotate(direction));
        var position = cell(origin, rotation, x, z, 0).offset(offset.rotate(rotation));
        var entryRotation = rotation.getRotated(direction);
        pieces.add(new WoodlandMansionPiece(manager, ENTRANCE, position, entryRotation));
        pieces.add(new WoodlandMansionPiece(manager, MansionRoomCatalogue.ENTRANCE_PLATFORM,
                position.offset(MansionRoomCatalogue.ENTRANCE_PLATFORM_OFFSET.rotate(entryRotation)), entryRotation));
    }
}
