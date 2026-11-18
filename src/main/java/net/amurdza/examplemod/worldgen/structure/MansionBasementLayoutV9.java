package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Restores the authored stair ceiling; the roof adjustment belongs to the mansion exterior. */
public final class MansionBasementLayoutV9 {
    private MansionBasementLayoutV9() { }

    public static BlockState cell(int x, int y, int z) {
        if (z <= 13 && Math.abs(x) <= 7) {
            if (Math.abs(x) == 2 && y == 3 && z == 8) return torch(Direction.SOUTH);
            if (Math.abs(x) == 1 && y == 5 && z == 6) return torch(x < 0 ? Direction.EAST : Direction.WEST);
            if (Math.abs(x) <= 1 && y == 9 && z == 3) return torch(Direction.NORTH);
            if (z == 0 && Math.abs(x) <= 1 && y == 11) return Blocks.AIR.defaultBlockState();
            return MansionBasementLayoutV7.cell(x, y, z);
        }
        return MansionBasementLayoutV8.cell(x, y, z);
    }

    private static BlockState torch(Direction facing) {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }
}
