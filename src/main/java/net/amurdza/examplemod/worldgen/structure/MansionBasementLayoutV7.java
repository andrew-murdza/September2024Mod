package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Raised cellar with a single-cell stair, a wider landing, and wall-mounted workstations. */
public final class MansionBasementLayoutV7 {
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private MansionBasementLayoutV7() { }

    public static BlockState cell(int x, int y, int z) {
        if (z <= 13 && Math.abs(x) <= 7) return stairwell(x, y, z);

        // The 59-block hall has a one-block margin at each end of the array.
        if (Math.abs(x) <= 5 && z >= 14 && z <= 72 && (y == 4 || y == 5)) {
            if ((x == -3 || x == 0 || x == 3) && (z - 15) % 4 == 0)
                return y == 4 ? lantern() : Blocks.CHAIN.defaultBlockState();
            return AIR;
        }

        // The captures included their rear ovens inside the room. Move those
        // ovens into the shared shell, keeping all eleven interior columns.
        int side = Math.abs(x);
        if (side >= 19 && side <= 30 && z >= 14 && z <= 72 && (z - 14) % 12 <= 10) {
            var room = MansionBasementLayoutV5.room(x >= 0, (z - 14) / 12, true);
            if (room == MansionBasementLayoutV5.Room.SMITH || room == MansionBasementLayoutV5.Room.KITCHEN) {
                int depth = side - 19, across = (z - 14) % 12;
                if ((y == 1 || y == 2) && depth == 11) {
                    var oven = AuthoredMansionRooms.state(room.name(), 10, y, across);
                    return x < 0 ? oven.mirror(Mirror.FRONT_BACK) : oven;
                }
                if ((y == 1 || y == 2) && depth == 10) return AIR;
                if (room == MansionBasementLayoutV5.Room.KITCHEN && depth == 8
                        && (across == 0 || across == 10) && y == 1) return Blocks.CRAFTING_TABLE.defaultBlockState();
            }
        }
        return MansionBasementLayoutV6.cell(x, y, z);
    }

    private static BlockState stairwell(int x, int y, int z) {
        int side = Math.abs(x);
        if (z < 0 || z > 13 || y < -2 || y > 14) return null;
        if (z >= 8) {
            if (y > 6) return null;
            if (y <= 0 || y == 6 || side == 7) return BRICK;
            if (z == 13) {
                if (x == 0 && (y == 1 || y == 2)) return Blocks.IRON_DOOR.defaultBlockState()
                        .setValue(DoorBlock.FACING, Direction.SOUTH)
                        .setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                return BRICK;
            }
            if (z == 10 && (x == -4 || x == 4) && (y == 4 || y == 5))
                return y == 4 ? lantern() : Blocks.CHAIN.defaultBlockState();
            return AIR;
        }
        // Close the shoulders of the broadened landing beside the last tread.
        if (z == 7 && side >= 4) return y <= 6 ? BRICK : null;
        if (side > 4) return null;
        if (z == 0) {
            if (y >= 7) {
                if (side > 1 || y > 10) return null;
                return y == 7 ? Blocks.BIRCH_PLANKS.defaultBlockState() : AIR;
            }
            return BRICK;
        }
        if (side == 4) return y < 7 ? BRICK : null;
        int step = 8 - z;
        if (y == 14) return Blocks.BIRCH_PLANKS.defaultBlockState();
        if (side <= 1) {
            if (y == step) return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            if (y > step && y <= step + 4) return AIR;
        }
        return y >= 7 ? Blocks.OAK_PLANKS.defaultBlockState() : BRICK;
    }

    private static BlockState lantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }
}
