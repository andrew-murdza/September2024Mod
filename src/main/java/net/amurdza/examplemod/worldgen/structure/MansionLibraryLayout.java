package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

/** The player's November 22 library, kept indoors with its authored short-end doorway. */
public final class MansionLibraryLayout {
    public static final String LIBRARY = "aoe_v9_library";
    public static final String FRAME = "aoe_v9_library_frame";
    private static final int GRID_X = 6, GRID_Z = 3;
    private static final int TWO_CELLS = 131072, ORIGIN = 1048576, DOOR = 2097152;

    private MansionLibraryLayout() { }

    /** Reserve one connected pair before the native room grouper can split it. */
    public static void reserve(WoodlandMansionPieces.MansionGrid grid) {
        for (int floor = 0; floor < 3; floor++) {
            int room = TWO_CELLS | (1200 + floor);
            grid.floorRooms[floor].set(GRID_X, GRID_Z, room);
            grid.floorRooms[floor].set(GRID_X, GRID_Z + 1, room | ORIGIN | DOOR);
        }
    }

    public static boolean occupiesReservedCells(BoundingBox box, BlockPos origin, Rotation rotation) {
        for (int z = GRID_Z; z <= GRID_Z + 1; z++) {
            var cell = cell(origin, rotation, z, 0);
            if (cell.getX() >= box.minX() && cell.getX() <= box.maxX()
                    && cell.getZ() >= box.minZ() && cell.getZ() <= box.maxZ()) return true;
        }
        return false;
    }

    /** The south end meets the z=5 corridor; all four walls are inside the estate. */
    public static void add(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces,
            BlockPos origin, Rotation rotation) {
        for (int floor = 0; floor < MansionUpperLayout.FLOOR_COUNT; floor++) {
            var position = cell(origin, rotation, GRID_Z, floor * 8);
            pieces.add(new WoodlandMansionPiece(manager, FRAME,
                    position.offset(new BlockPos(-1, 0, -1).rotate(rotation)), rotation));
            pieces.add(new WoodlandMansionPiece(manager, LIBRARY, position, rotation));
        }
    }

    public static boolean preservesAuthoredBlocks(String templateName) {
        return LIBRARY.equals(templateName) || FRAME.equals(templateName);
    }

    private static BlockPos cell(BlockPos origin, Rotation rotation, int z, int y) {
        return new BlockPos((GRID_X - 10) * 8, y, (z - 4) * 8).rotate(rotation).offset(origin);
    }
}
