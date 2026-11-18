package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/** Revised mine thresholds and backing, retaining the authored V5 room arrangement. */
public final class MansionBasementLayoutV6 {
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();

    private MansionBasementLayoutV6() { }

    public static BlockState cell(int x, int y, int z) {
        if (x <= -30 && x >= -48 && z >= 13 && z <= 49 && y >= -1 && y <= 5) {
            int row = Math.min(2, Math.max(0, (z - 14) / 12));
            int across = z - (14 + row * 12), depth = -x - 30;
            if (across < -1 || across > 11) return null;
            if (y == -1 || y == 5 || across == -1 || across == 11 || depth == 18) return BRICK;
            // The room-facing floor and lintel are continuous brick rows. This
            // also completes the marked middle column without lining the ore.
            if (depth == 0 && (y == 0 || y == 4)) return BRICK;
            // One last mineable stone column now precedes the farther backing.
            if (depth == 17 || y == 0 || y == 4) return STONE;
            if (across == 2 || across == 8) {
                if (y == 2 && (depth == 5 || depth == 10 || depth == 15))
                    return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
                return Blocks.AIR.defaultBlockState();
            }
            return depth == 0 ? BRICK : STONE;
        }
        return MansionBasementLayoutV5.cell(x, y, z);
    }
}
