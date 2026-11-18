package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** The first November 14 blueprint, retained for saved and partially generated basements. */
final class MansionBasementLayoutV2 {
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private enum Room { WHEAT, CARROT, POTATO, BEET, CABBAGE, KITCHEN, GRANARY, PUMPKIN, CANE, COOP, MUSHROOM }
    // In each pair the first room opens onto the central aisle, and the second lies beyond it.
    private static final Room[][] WEST = {{Room.WHEAT, Room.PUMPKIN}, {Room.CARROT, Room.CABBAGE},
            {Room.BEET, Room.MUSHROOM}, {Room.POTATO, Room.CANE}};
    private static final Room[][] EAST = {{Room.CABBAGE, Room.COOP}, {Room.BEET, Room.GRANARY},
            {Room.WHEAT, Room.POTATO}, {Room.CARROT, Room.KITCHEN}};
    static BlockState cell(int x, int y, int z, Block cabbage) {
        int side = x < 0 ? -1 - x : x;
        // A continuous slab sits beneath every channel and the sand-lined cane pond.
        if (y == -2 && side <= 29 && z >= 0 && z <= 65 && !(z < 10 && side <= 1)) return BRICK;
        if (z < 14) return stairwell(x, y, z, side);
        if (z > 65) return null;
        if (z >= 52) return smith(x, y, z);
        int row = Math.min(3, (z - 14) / 9);
        int across = z - (15 + 9 * row);
        boolean back = side >= 17;
        int depth = back ? side - 17 : side - 5;
        Room room = (x < 0 ? WEST : EAST)[row][back ? 1 : 0];
        boolean hasMine = back && (room == Room.MUSHROOM || room == Room.CABBAGE || room == Room.POTATO);
        if (hasMine && side >= 28 && side <= 44 && (across >= 0 && across <= 2 || across >= 5 && across <= 7))
            return mine(side, y, across);
        if (side > 29 || y < -2 || y > 6) return null;
        if (y == -2 || y == -1 || y == 6) {
            if (y == -1 && room == Room.CANE && across >= 1 && across <= 6 && depth >= 1 && depth <= 9)
                return Blocks.SAND.defaultBlockState();
            return BRICK;
        }
        if (side <= 2) {
            if (y == 4 && (z - 18) % 9 == 0 && (x == -2 || x == 1)) return hangingLamp();
            if (y == 5 && (z - 18) % 9 == 0 && (x == -2 || x == 1)) return Blocks.CHAIN.defaultBlockState();
            return y == 0 ? BRICK : AIR;
        }
        // Adjacent rooms share one wall. The outer rectangular shell remains two courses thick.
        if (across < 0 || across > 7) {
            if (room == Room.KITCHEN && (across == -1 || across == 8) && depth >= 0 && depth <= 11 && y == 2)
                return facing(Blocks.FURNACE, across < 0 ? Direction.SOUTH : Direction.NORTH);
            return BRICK;
        }
        boolean doorway = across >= 2 && across <= 5 && y >= 1 && y <= 3;
        if (side == 3 || side == 4 || side == 16) {
            Room behind = (x < 0 ? WEST : EAST)[row][1];
            if (behind == Room.COOP && side == 16 && doorway && y == 1)
                return facing(Blocks.OAK_FENCE_GATE, Direction.WEST);
            return doorway ? AIR : BRICK;
        }
        if (back && (depth == 11 || depth == 12)) {
            if (room == Room.KITCHEN && depth == 11 && (across == 0 || across == 7) && y == 1)
                return Blocks.CRAFTING_TABLE.defaultBlockState();
            if (room == Room.KITCHEN && depth == 11 && across >= 1 && across <= 6 && (y == 1 || y == 2))
                return facing(Blocks.FURNACE, Direction.WEST);
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
        // The stair roof ends two blocks below the basement roof. Close that
        // vertical seam so natural lake water above the stairs cannot enter
        // the hall over its locked doorway.
        if (z == 13 && y >= 3 && y <= 6) return BRICK;
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
            if (y == 0) return (depth == 10 ? Blocks.STONE_BRICKS
                    : across == 0 || across == 7 ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState();
            if (y == 1 && depth == 10) return Blocks.HAY_BLOCK.defaultBlockState();
        }
        if (room == Room.KITCHEN || room == Room.GRANARY || room == Room.COOP
                || depth < 1 || depth > 9 || across < 1 || across > 6) return y == 0 ? BRICK : AIR;
        if (room == Room.MUSHROOM) {
            if (depth == 5) return y == 0 ? Blocks.WATER.defaultBlockState() : AIR;
            if (y == 0) return Blocks.PODZOL.defaultBlockState();
            if (y == 1 && (depth + across) % 3 == 0) return (depth % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
            return AIR;
        }
        if (room == Room.CANE) {
            if (y == 0) return depth % 3 == 0 ? Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true) : Blocks.WATER.defaultBlockState();
            return depth % 3 == 0 && (y == 1 || y == 2) ? Blocks.SUGAR_CANE.defaultBlockState() : AIR;
        }
        if (depth == 5) return y == 0 ? Blocks.WATER.defaultBlockState() : AIR;
        if (y == 0) return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        if (y != 1) return AIR;
        if (room == Room.PUMPKIN) {
            int strip = depth < 5 ? depth - 1 : depth - 6;
            boolean stem = depth < 5 ? strip % 2 == 0 : strip % 2 == 1;
            return stem ? facing(Blocks.ATTACHED_PUMPKIN_STEM, depth < 5 ? out : in) : Blocks.PUMPKIN.defaultBlockState();
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
        if (y <= 0 || y == 6 || x == -6 || x == 5 || z == 65) return BRICK;
        if (z == 52) return x >= -2 && x <= 1 && y <= 3 ? AIR : BRICK;
        if (z == 64) {
            boolean center = x >= -3 && x <= 2;
            return (center && (y == 1 || y == 2) || (x == -4 || x == 3) && y == 2)
                    ? facing(Blocks.BLAST_FURNACE, Direction.NORTH) : BRICK;
        }
        if (x == -5 || x == 4)
            return z >= 57 && y == 2 ? facing(Blocks.BLAST_FURNACE, x < 0 ? Direction.EAST : Direction.WEST) : BRICK;
        if (x == -4 || x == 3) {
            if (z >= 57 && y == 1) return facing(Blocks.ANVIL, Direction.NORTH);
            if (z <= 56 && y == 1) return chest(x < 0 ? Direction.EAST : Direction.WEST,
                    (z - 53) % 2 == 0 ? Direction.SOUTH : Direction.NORTH);
        }
        if ((x == -2 || x == 1) && (z == 56 || z == 60) && y == 5) return hangingLamp();
        return AIR;
    }

    private static BlockState mine(int side, int y, int across) {
        if (y < 0 || y > 4) return null;
        int center = across <= 2 ? 1 : 6;
        BlockState shell = side <= 29 ? BRICK : Blocks.STONE.defaultBlockState();
        if (y == 0 || y == 4 || across != center || side == 44) return shell;
        if (y == 2 && (side == 33 || side == 38 || side == 43))
            return facing(Blocks.WALL_TORCH, across <= 2 ? Direction.SOUTH : Direction.NORTH);
        return AIR;
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
