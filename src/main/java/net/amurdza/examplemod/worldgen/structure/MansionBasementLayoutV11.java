package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

/** Two copies of the cellar, reached through a five-block landing and a continuing central flight. */
public final class MansionBasementLayoutV11 {
    public static final int LOWER_FLOOR = -7;
    public static final int FIRST_ROOM_SHIFT = 5;
    public static final int SECOND_ROOM_SHIFT = 12;
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final List<BlockPos> DOORS = List.of(new BlockPos(-4, 1, 18), new BlockPos(4, 1, 18), new BlockPos(0, -6, 25));

    private MansionBasementLayoutV11() { }

    public static List<BlockPos> doors() { return DOORS; }

    public static boolean isDoor(int x, int y, int z) {
        return DOORS.stream().anyMatch(door -> x == door.getX() && z == door.getZ()
                && (y == door.getY() || y == door.getY() + 1));
    }

    public static MansionBasementLayoutV5.Room roomAt(int x, int y, int z) {
        int oldZ = z - (y < 0 ? SECOND_ROOM_SHIFT : FIRST_ROOM_SHIFT);
        return MansionBasementLayoutV5.roomAt(MansionBasementLayoutV10.previousX(x, oldZ), oldZ);
    }

    public static BlockState cell(int x, int y, int z) {
        BlockState result = rawCell(x, y, z);
        return result != null && result.is(Blocks.BIRCH_PLANKS) ? Blocks.OAK_PLANKS.defaultBlockState() : result;
    }

    private static BlockState rawCell(int x, int y, int z) {
        int side = Math.abs(x);
        // The front buried shoulders share the top stair's stone course. The
        // rear wall (Z8) remains wood, as do all the courses above this one.
        if (y == 7 && z == 0 && side >= 2 && side <= 3) return BRICK;
        if (z <= 7 && side <= 7) {
            var state = MansionBasementLayoutV9.cell(x, y, z);
            if (y == 7 && state != null && (state.is(Blocks.OAK_PLANKS) || state.is(Blocks.BIRCH_PLANKS))) return BRICK;
            return state;
        }

        // All switches attach to the outer jamb of their door. The central
        // lower entrance has a pair on each face to remain mirror-symmetric.
        if (y == 2 && side == 5 && (z == 17 || z == 19)) return lever(z < 18 ? Direction.NORTH : Direction.SOUTH);
        if (y == -5 && side == 1 && (z == 24 || z == 26)) return lever(z < 25 ? Direction.NORTH : Direction.SOUTH);

        // Six lights over the enlarged first patio, three over the lower one.
        if ((x == -4 || x == 0 || x == 4) && (z == 10 || z == 15) && (y == 4 || y == 5))
            return y == 4 ? lantern() : Blocks.CHAIN.defaultBlockState();
        if ((x == -4 || x == 0 || x == 4) && z == 22 && (y == -3 || y == -2))
            return y == -3 ? lantern() : Blocks.CHAIN.defaultBlockState();
        if (side == 2 && y == 3 && z == 8) return torch(Direction.SOUTH);
        if (side == 1 && y == -2 && z == 18) return torch(x < 0 ? Direction.EAST : Direction.WEST);
        if (side == 2 && y == -4 && z == 20) return torch(Direction.SOUTH);

        // The second flight begins only after Z8..12's five flat blocks.
        // Its upper portion is open to the patio ceiling, keeping the three
        // central lanterns clear. It passes below the first door wall at Z18.
        if (z >= 13 && z <= 19 && side <= 2 && y >= -9) {
            int tread = 13 - z;
            if (side == 2 && y <= 0) return BRICK;
            if (side <= 1) {
                if (y < tread) return BRICK;
                if (y == tread) return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
                int ceiling = z <= 17 ? 6 : 18 - z;
                if (y < ceiling) return AIR;
                if (y == ceiling) return BRICK;
            }
        }

        if (side <= 7 && z >= 8 && z <= 18 && y >= -2 && y <= 6) {
            if (y <= 0 || y == 6 || side == 7) return BRICK;
            if (z == 18) {
                if (side == 4 && (y == 1 || y == 2)) return door(x < 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT, y == 1);
                return BRICK;
            }
            return AIR;
        }
        if (side <= 7 && z >= 20 && z <= 25 && y >= -9 && y <= -1) {
            if (y <= -7 || y == -1 || side == 7) return BRICK;
            if (z == 25) {
                if (x == 0 && (y == -6 || y == -5)) return door(DoorHingeSide.LEFT, y == -6);
                return BRICK;
            }
            return AIR;
        }

        // The lower room's headroom takes priority over the upper floor's
        // original two-course foundation. Its ceiling remains one block below
        // the upper floor, matching the existing main-floor/cellar separation.
        var lower = z >= 26 ? MansionBasementLayoutV10.cell(x, y - LOWER_FLOOR, z - SECOND_ROOM_SHIFT) : null;
        if (y <= -1 && lower != null) return lower;
        return z >= 19 ? MansionBasementLayoutV10.cell(x, y, z - FIRST_ROOM_SHIFT) : null;
    }

    private static BlockState door(DoorHingeSide hinge, boolean lower) {
        return Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HINGE, hinge).setValue(DoorBlock.HALF, lower ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
    }

    private static BlockState lever(Direction facing) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, facing);
    }

    private static BlockState lantern() { return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true); }
    private static BlockState torch(Direction facing) { return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing); }
}
