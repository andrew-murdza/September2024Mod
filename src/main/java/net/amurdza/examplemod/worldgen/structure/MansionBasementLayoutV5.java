package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

import java.util.List;

/** Five rows of four authored 11x11 rooms around an 11-wide hall: a 61x61 cellar. */
public final class MansionBasementLayoutV5 {
    public enum Room { WHEAT, CARROT, POTATO, BEET, CABBAGE, MUSHROOM, CANE, PUMPKIN,
        KITCHEN, GRANARY, SMITH, CHICKEN, SHEEP, COW, PIG }
    // Rows run from the entrance toward the back. Each pair is inner, outer.
    private static final Room[][] WEST = {{Room.CARROT, Room.MUSHROOM}, {Room.BEET, Room.CANE},
            {Room.CABBAGE, Room.PUMPKIN}, {Room.WHEAT, Room.KITCHEN}, {Room.POTATO, Room.GRANARY}};
    private static final Room[][] EAST = {{Room.CARROT, Room.COW}, {Room.BEET, Room.CHICKEN},
            {Room.CABBAGE, Room.PIG}, {Room.WHEAT, Room.SHEEP}, {Room.POTATO, Room.SMITH}};
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState(), AIR = Blocks.AIR.defaultBlockState();
    private MansionBasementLayoutV5() { }

    public static Room room(boolean east, int row, boolean outer) { return (east ? EAST : WEST)[row][outer ? 1 : 0]; }
    public static boolean pen(Room room) { return room == Room.CHICKEN || room == Room.SHEEP || room == Room.COW || room == Room.PIG; }
    public static BlockPos roomCenter(boolean east, int row, boolean outer) {
        return new BlockPos((east ? 1 : -1) * (outer ? 24 : 12), 1, 19 + row * 12);
    }
    public static Room roomAt(int x, int z) {
        if (Math.abs(x) < 7 || Math.abs(x) > 29 || z < 14 || z > 72 || (z - 14) % 12 == 11) return null;
        if (Math.abs(x) == 18) return null;
        return room(x >= 0, (z - 14) / 12, Math.abs(x) >= 19);
    }

    public static BlockState cell(int x, int y, int z) {
        int side = Math.abs(x);
        if (z <= 13 && side <= 6) return stairwell(x, y, z);
        if (x <= -30 && x >= -47 && z >= 13 && z <= 49) {
            if (x == -30 && (y == -2 || y == 6)) return BRICK;
            int row = Math.min(2, Math.max(0, (z - 14) / 12));
            return mine(-x - 30, y, z - (14 + row * 12));
        }
        if (side > 30 || z < 13 || z > 73 || y < -2 || y > 6) return null;
        if (y == -2 || y == 6) return BRICK;
        if (z == 13 || z == 73) return BRICK;
        int row = (z - 14) / 12, across = (z - 14) % 12;
        if (side <= 5) {
            if (y <= 0) return BRICK;
            if ((x == -3 || x == 0 || x == 3) && (z - 16) % 4 == 0) {
                if (y == 4) return lantern();
                if (y == 5) return Blocks.CHAIN.defaultBlockState();
            }
            return AIR;
        }
        boolean outer = side >= 19;
        int depth = side - (outer ? 19 : 7);
        Room room = room(x >= 0, row, outer);
        if (y == -1) {
            return side != 6 && side != 18 && side != 30 && across <= 10
                    && authored(room, depth, y, across).is(Blocks.SAND) ? Blocks.SAND.defaultBlockState() : BRICK;
        }
        if (side == 6 || side == 18 || side == 30) {
            if (y == 0) return BRICK;
            Room near = room(x >= 0, row, side == 30);
            Room far = side == 18 ? room(x >= 0, row, true) : null;
            if (side < 30 && across >= 4 && across <= 6 && y <= 3) {
                return y == 1 && (pen(near) || far != null && pen(far))
                        ? gate(x >= 0 ? Direction.WEST : Direction.EAST) : AIR;
            }
            BlockState decoration = wall(near, side == 6 ? -1 : 11, y, across);
            return x < 0 ? decoration.mirror(Mirror.FRONT_BACK) : decoration;
        }
        if (across == 11) {
            if (y == 0) return BRICK;
            Room next = room(x >= 0, row + 1, outer);
            if (depth >= 4 && depth <= 6 && y <= 3 && sideEntrance(room) && sideEntrance(next))
                return y == 1 && (pen(room) || pen(next)) ? gate(Direction.NORTH) : AIR;
            BlockState decoration = wall(room, depth, y, 11);
            if (decoration == BRICK) decoration = wall(next, depth, y, -1);
            return x < 0 ? decoration.mirror(Mirror.FRONT_BACK) : decoration;
        }
        // Side door approaches displace furniture only where a neighboring room exists.
        boolean frontDoor = row > 0 && sideEntrance(room) && sideEntrance(room(x >= 0, row - 1, outer));
        boolean backDoor = row < 4 && sideEntrance(room) && sideEntrance(room(x >= 0, row + 1, outer));
        if (across >= 4 && across <= 6 && y >= 1 && y <= 3 && (depth == 0 || depth == 10 && !outer)) return AIR;
        if (depth >= 4 && depth <= 6 && y >= 1 && y <= 3
                && (across == 0 && frontDoor || across == 10 && backDoor)) return AIR;
        if (y == 4 || y == 5) {
            if ((depth == 2 || depth == 8) && (across == 2 || across == 8) || depth == 5 && across == 5)
                return y == 4 ? lantern() : Blocks.CHAIN.defaultBlockState();
        }
        BlockState result = authored(room, depth, y, across);
        if (y == 0 && pen(room) && result.is(Blocks.STONE_BRICKS)
                && !cell(x, 1, z).is(Blocks.HAY_BLOCK)) result = Blocks.GRASS_BLOCK.defaultBlockState();
        if (result.is(Blocks.LANTERN) || result.is(Blocks.CHAIN)) result = AIR;
        if (y == 1 && crop(room) != null && authored(room, depth, 0, across).is(Blocks.FARMLAND)) {
            result = crop(room).getStateForAge(crop(room).getMaxAge());
        }
        if (result.is(Blocks.CHEST)) result = normalizeChest(room, depth, y, across, result, frontDoor, backDoor, outer);
        return x < 0 ? result.mirror(Mirror.FRONT_BACK) : result;
    }

    public static boolean sideEntrance(Room room) {
        return room != Room.SMITH && room != Room.KITCHEN && room != Room.GRANARY;
    }

    private static BlockState authored(Room room, int depth, int y, int across) {
        return AuthoredMansionRooms.state(pen(room) ? "COOP" : room.name(), depth, y, across);
    }

    private static BlockState wall(Room room, int depth, int y, int across) {
        BlockState state = authored(room, depth, y, across);
        return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) ? state : BRICK;
    }

    private static CropBlock crop(Room room) {
        return switch (room) {
            case WHEAT -> (CropBlock) Blocks.WHEAT;
            case CARROT -> (CropBlock) Blocks.CARROTS;
            case POTATO -> (CropBlock) Blocks.POTATOES;
            case BEET -> (CropBlock) Blocks.BEETROOTS;
            case CABBAGE -> (CropBlock) net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("farmersdelight:cabbages"));
            default -> null;
        };
    }

    private static BlockState normalizeChest(Room room, int depth, int y, int across, BlockState state,
                                             boolean frontDoor, boolean backDoor, boolean outer) {
        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) return state;
        Direction facing = state.getValue(ChestBlock.FACING);
        Direction partner = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        int d = depth + partner.getStepX(), a = across + partner.getStepZ();
        boolean doorway = d >= 4 && d <= 6 && (a == 0 && frontDoor || a == 10 && backDoor)
                || a >= 4 && a <= 6 && (d == 0 || d == 10 && !outer);
        return doorway || !authored(room, d, y, a).is(Blocks.CHEST)
                ? state.setValue(ChestBlock.TYPE, ChestType.SINGLE) : state;
    }

    private static BlockState stairwell(int x, int y, int z) {
        int side = Math.abs(x);
        if (z == 9 && side >= 4 && side <= 6 && y >= -2 && y <= 6) return BRICK;
        if (z < 0 || z > 13 || side > (z < 10 ? 3 : 6)) return null;
        if (z == 13 && y >= 3 && y <= 6) return BRICK;
        int step = Math.max(0, 10 - z), ceiling = z < 10 ? step + 5 : 6;
        if (y < step - 2 || y > ceiling || y >= 10 && z >= 4 || side == 3 && z < 10 && y >= 10) return null;
        if (z == 13) {
            if (x == 0 && (y == 1 || y == 2)) return Blocks.IRON_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            return BRICK;
        }
        if (y < step || side == (z < 10 ? 3 : 6) && y < 10 || y == ceiling && ceiling < 10) return BRICK;
        if (y == step) return z < 10 ? Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH) : BRICK;
        if (z == 11 && side == 3 && (y == 4 || y == 5))
            return y == 4 ? lantern() : Blocks.CHAIN.defaultBlockState();
        if (z == 6 && side == 2 && y == 8)
            return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, x < 0 ? Direction.EAST : Direction.WEST);
        return AIR;
    }

    private static BlockState mine(int depth, int y, int across) {
        if (depth < 0 || depth > 17 || across < -1 || across > 11 || y < -1 || y > 5) return null;
        if (y == -1 || y == 5 || across == -1 || across == 11 || depth == 17) return BRICK;
        if (y == 0 || y == 4) return Blocks.STONE.defaultBlockState();
        if (across == 2 || across == 8) {
            if (y == 2 && (depth == 5 || depth == 10 || depth == 15))
                return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
            return AIR;
        }
        return depth == 0 ? BRICK : Blocks.STONE.defaultBlockState();
    }

    private static BlockState gate(Direction facing) { return Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing); }
    private static BlockState lantern() { return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true); }
    public record Animal(String id, BlockPos pos) { }
    public static List<Animal> animals() {
        var result = new java.util.ArrayList<Animal>();
        for (int row = 0; row < 4; row++) {
            Room room = room(true, row, true);
            String id = "minecraft:" + room.name().toLowerCase(java.util.Locale.ROOT);
            var center = roomCenter(true, row, true);
            for (int dx : new int[]{-2, 1}) for (int dz : new int[]{-2, 1}) result.add(new Animal(id, center.offset(dx, 0, dz)));
        }
        return List.copyOf(result);
    }
}
