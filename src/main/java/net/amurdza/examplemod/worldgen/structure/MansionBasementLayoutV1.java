package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** The saved November 13 blueprint; partially generated old basements keep their original cells. */
final class MansionBasementLayoutV1 {
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private enum Room { WHEAT, CARROT, POTATO, BEET, CABBAGE, KITCHEN, GRANARY, PUMPKIN, CANE, COOP, MUSHROOM }
    // In each pair the first room opens onto the central aisle, and the second lies beyond it.
    private static final Room[][] WEST = {{Room.WHEAT, Room.PUMPKIN}, {Room.CARROT, Room.CABBAGE},
            {Room.BEET, Room.MUSHROOM}, {Room.POTATO, Room.CANE}};
    private static final Room[][] EAST = {{Room.CABBAGE, Room.COOP}, {Room.KITCHEN, Room.GRANARY},
            {Room.WHEAT, Room.POTATO}, {Room.CARROT, Room.BEET}};
    static BlockState cell(int x, int y, int z, Block cabbage) {
        int side = x < 0 ? -1 - x : x;
        if (y == -2 && side <= 32 && z >= 0 && z <= 67 && (z < 14 || z >= 54) && !(z < 10 && side <= 1)) return BRICK;
        if (z < 14) return stairwell(x, y, z, side);
        if (side > 32 || z > 67) return null;
        if (z >= 54) return smith(x, y, z);
        int row = (z - 14) / 10;
        int across = (z - 14) % 10 - 1; // eight clear blocks, then two courses of dividing wall
        boolean back = side >= 19;
        int depth = back ? side - 20 : side - 5;
        Room room = (x < 0 ? WEST : EAST)[row][back ? 1 : 0];
        boolean hasShaft = back && (room == Room.MUSHROOM || room == Room.CABBAGE || room == Room.POTATO);
        if (hasShaft && depth >= 10 && depth <= 12 && (across >= 0 && across <= 2 || across >= 5 && across <= 7) && y <= 0) {
            return shaft(depth, y, across);
        }
        if (y < -2 || y > 6) return null;
        if (y == -2 || y == -1 || y == 6) {
            if (y == -1 && room == Room.CANE && across >= 1 && across <= 6 && depth >= 1 && depth <= 9) return Blocks.SAND.defaultBlockState();
            return BRICK;
        }
        if (side <= 2) {
            if (z == 53 && (x < -2 || x > 1)) return BRICK;
            if (y == 4 && z % 10 == 8 && (x == -2 || x == 1)) return hangingLamp();
            if (y == 5 && z % 10 == 8 && (x == -2 || x == 1)) return Blocks.CHAIN.defaultBlockState();
            return y == 0 ? BRICK : AIR;
        }
        // A transverse gallery links the back rooms through adjacent farms, preserving the solid kitchen wall.
        if (side == 18) return y == 0 || z == 14 || z == 53 ? BRICK : AIR;
        if (across < 0 || across > 7) return BRICK;
        boolean doorway = across >= 2 && across <= 5 && y >= 1 && y <= 3;
        if (side == 3 || side == 4 || side == 19) {
            if (room == Room.COOP && side == 19 && doorway && y == 1) {
                return across == 3 || across == 4 ? facing(Blocks.OAK_FENCE_GATE, Direction.WEST)
                        : Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            }
            return doorway ? AIR : BRICK;
        }
        if (depth == 11 || depth == 12) {
            if (room == Room.KITCHEN && depth == 11 && (across == 0 || across == 7) && y == 1) return Blocks.CRAFTING_TABLE.defaultBlockState();
            if (room == Room.KITCHEN && depth == 11 && across >= 1 && across <= 6 && (y == 1 || y == 2)) {
                return facing(Blocks.FURNACE, Direction.WEST);
            }
            if (!back && room != Room.KITCHEN && doorway) return AIR;
            return BRICK;
        }
        if (depth < 0 || depth > 10) return BRICK;
        if ((depth == 2 || depth == 8) && (across == 2 || across == 5)) {
            if (y == 5) return Blocks.CHAIN.defaultBlockState();
            if (y == 4) return hangingLamp();
        }
        return furnishings(room, x >= 0, depth, y, across, cabbage);
    }

    private static BlockState stairwell(int x, int y, int z, int side) {
        if (z < 0 || z > 13 || side > (z < 10 ? 2 : 3)) return null;
        int step = Math.max(0, 10 - z);
        if (y < step - 2) return null;
        // Keep ground-floor headroom open on every side of the first steps so the corridor remains traversable.
        int ceiling = step + 4;
        if (y > ceiling || y >= 10 && z >= 4 || side == 2 && z < 10 && y >= 10) return null;
        if (z == 13) {
            if ((x == -1 || x == 0) && (y == 1 || y == 2)) {
                return facing(Blocks.IRON_DOOR, Direction.SOUTH)
                        .setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.HINGE, x == -1 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            }
            return BRICK;
        }
        if (y < step || side == (z < 10 ? 2 : 3) && y < 10 || y == ceiling && ceiling < 10) return BRICK;
        if (y == step) return z < 10 ? facing(Blocks.STONE_BRICK_STAIRS, Direction.NORTH) : BRICK;
        return AIR;
    }

    private static BlockState furnishings(Room room, boolean east, int depth, int y, int across, Block cabbage) {
        Direction in = east ? Direction.WEST : Direction.EAST;
        Direction out = in.getOpposite();
        if (room == Room.GRANARY && y == 1) {
            if (depth == 10 && across >= 1 && across <= 6) return chest(in, (across % 2 == 1) ? Direction.SOUTH : Direction.NORTH);
            if ((across == 0 || across == 7) && depth < 10) return chest(across == 0 ? Direction.SOUTH : Direction.NORTH, depth % 2 == 0 ? out : in);
        }
        if (room == Room.KITCHEN && y == 1 && (across == 0 || across == 7)) return Blocks.CRAFTING_TABLE.defaultBlockState();
        if (room == Room.COOP) {
            if (y == 0) return (depth < 10 && (across == 0 || across == 7) ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState();
            if (y == 1 && depth == 10) return Blocks.HAY_BLOCK.defaultBlockState();
        }
        if (room == Room.KITCHEN || room == Room.GRANARY || room == Room.COOP
                || depth < 1 || depth > 9 || across < 1 || across > 6) return y == 0 ? BRICK : AIR;
        if (room == Room.MUSHROOM) {
            if (y == 0) return Blocks.PODZOL.defaultBlockState();
            if (y == 1 && (depth + across) % 3 == 0) return (depth % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
            return AIR;
        }
        if (room == Room.CANE) {
            if (y == 0) return (depth % 3 == 0 ? Blocks.SAND : Blocks.WATER).defaultBlockState();
            return depth % 3 == 0 && (y == 1 || y == 2) ? Blocks.SUGAR_CANE.defaultBlockState() : AIR;
        }
        if (depth == 5) return y == 0 ? Blocks.WATER.defaultBlockState() : AIR;
        if (y == 0) return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        if (y != 1) return AIR;
        if (room == Room.PUMPKIN) {
            int strip = depth < 5 ? depth - 1 : depth - 6;
            return strip % 2 == 0 ? facing(Blocks.ATTACHED_PUMPKIN_STEM, out) : Blocks.PUMPKIN.defaultBlockState();
        }
        Block crop = switch (room) {
            case WHEAT -> Blocks.WHEAT;
            case CARROT -> Blocks.CARROTS;
            case POTATO -> Blocks.POTATOES;
            case BEET -> Blocks.BEETROOTS;
            case CABBAGE -> cabbage;
            default -> null;
        };
        return crop instanceof CropBlock plant ? plant.getStateForAge(plant.getMaxAge()) : AIR;
    }

    private static BlockState smith(int x, int y, int z) {
        if (x < -6 || x > 5 || y < -2 || y > 6) return null;
        if (y <= 0 || y == 6 || x == -6 || x == 5 || z == 67) return BRICK;
        if (z == 54) return x >= -2 && x <= 1 && y <= 3 ? AIR : BRICK;
        if (z == 66) {
            boolean center = x >= -3 && x <= 2;
            return (center && (y == 1 || y == 2) || (x == -4 || x == 3) && y == 2)
                    ? facing(Blocks.BLAST_FURNACE, Direction.NORTH) : BRICK;
        }
        if (x == -5 || x == 4) {
            return z >= 61 && y == 2 ? facing(Blocks.BLAST_FURNACE, x < 0 ? Direction.EAST : Direction.WEST) : BRICK;
        }
        if (x == -4 || x == 3) {
            if (z >= 61 && y == 1) return facing(Blocks.ANVIL, Direction.NORTH);
            if (z <= 60 && y == 1) return chest(x < 0 ? Direction.EAST : Direction.WEST,
                    (z - 55) % 2 == 0 ? Direction.SOUTH : Direction.NORTH);
        }
        if ((x == -2 || x == 1) && (z == 58 || z == 62) && y == 5) return hangingLamp();
        return AIR;
    }

    private static BlockState shaft(int depth, int y, int across) {
        int center = across <= 2 ? 1 : 6;
        if (y < -16) return null;
        if (y == -16) return Blocks.STONE.defaultBlockState();
        if (depth == 10 && across == center) return facing(Blocks.LADDER, Direction.SOUTH);
        if (depth == 11 && across == center && y < -1) {
            if (y != -3 && y != -8 && y != -13) return AIR;
            return facing(Blocks.WALL_TORCH, across <= 2 ? Direction.SOUTH : Direction.NORTH);
        }
        return y == 0 || y == -1 ? BRICK : Blocks.STONE.defaultBlockState();
    }

    private static BlockState chest(Direction facing, Direction partner) {
        return facing(Blocks.CHEST, facing).setValue(ChestBlock.TYPE,
                partner == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT);
    }
    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
    }
    private static BlockState hangingLamp() { return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true); }
}
