package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Lit room thresholds and stair approaches, with the old oven hood removed. */
public final class MansionBasementLayoutV8 {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private MansionBasementLayoutV8() { }

    public static BlockState cell(int x, int y, int z) {
        // Copy the seven lights on the stone flight in the November 17 spawn
        // example. The ceiling lights travel with the lowered soffit.
        if (Math.abs(x) == 2 && y == 3 && z == 8) return torch(Direction.SOUTH);
        if (Math.abs(x) == 1 && y == 5 && z == 6) return torch(x < 0 ? Direction.EAST : Direction.WEST);
        if (Math.abs(x) <= 1 && y == 6 && z == 6) return torch(Direction.NORTH);
        if (z == 0 && Math.abs(x) <= 1 && y == 11) return AIR;
        if (z >= 1 && z <= 7 && Math.abs(x) <= 3) {
            if (y == 15) return Blocks.BIRCH_PLANKS.defaultBlockState();
            // Moving the descending soffit three down and three toward the
            // landing exposes the first three columns to the raised room roof.
            if (z <= 3 && Math.abs(x) <= 1 && y > 8 - z && y < 15) return AIR;
            if (y == 14) return Blocks.OAK_PLANKS.defaultBlockState();
        }
        if (z >= 8 && z <= 10 && Math.abs(x) <= 1 && y >= 13 - z && y <= 6)
            return Blocks.STONE_BRICKS.defaultBlockState();
        if (y == 3) {
            BlockState torch = entranceTorch(x, z);
            if (torch != null) return torch;
        }

        // The ovens were moved into the rear wall in V7. Their old lintel
        // still projected a full block into the room above the empty column.
        if (Math.abs(x) == 29 && y >= 3 && y <= 5 && z >= 14 && z <= 72
                && (z - 14) % 12 <= 10) {
            var room = MansionBasementLayoutV5.room(x >= 0, (z - 14) / 12, true);
            if (room == MansionBasementLayoutV5.Room.KITCHEN || room == MansionBasementLayoutV5.Room.SMITH) return AIR;
        }
        return MansionBasementLayoutV7.cell(x, y, z);
    }

    private static BlockState entranceTorch(int x, int z) {
        int side = Math.abs(x);
        boolean east = x >= 0;
        if (z >= 14 && z <= 72) {
            int across = (z - 14) % 12;
            if (across == 3 || across == 7) {
                // Both faces of each hall/room and room/room dividing wall.
                if (side == 5 || side == 17) return torch(east ? Direction.WEST : Direction.EAST);
                if (side == 7 || side == 19) return torch(east ? Direction.EAST : Direction.WEST);
            }
        }
        boolean outer = side >= 19;
        int depth = side - (outer ? 19 : 7);
        if (depth != 3 && depth != 7) return null;
        for (int row = 0; row < 4; row++) {
            int wallZ = 25 + row * 12;
            if (z != wallZ - 1 && z != wallZ + 1) continue;
            if (MansionBasementLayoutV5.sideEntrance(MansionBasementLayoutV5.room(east, row, outer))
                    && MansionBasementLayoutV5.sideEntrance(MansionBasementLayoutV5.room(east, row + 1, outer)))
                return torch(z < wallZ ? Direction.NORTH : Direction.SOUTH);
        }
        return null;
    }

    private static BlockState torch(Direction facing) {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }
}
