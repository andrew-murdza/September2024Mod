package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Twelve-wide cellar aisle; east rooms move one block without altering their authored contents. */
public final class MansionBasementLayoutV10 {
    private MansionBasementLayoutV10() { }

    public static int previousX(int x, int z) { return z >= 14 && x >= 5 ? x - 1 : x; }

    public static BlockState cell(int x, int y, int z) {
        if (z == 13 && (x == 0 || x == 1) && (y == 1 || y == 2)) {
            return Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(DoorBlock.HINGE, x == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT)
                    .setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        if (z >= 14 && z <= 72 && x >= -5 && x <= 6 && (y == 4 || y == 5)) {
            if ((x == -4 || x == -1 || x == 2 || x == 5) && (z - 15) % 4 == 0)
                return y == 4 ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)
                        : Blocks.CHAIN.defaultBlockState();
            return Blocks.AIR.defaultBlockState();
        }
        // The back wall spans the added aisle column too.
        return MansionBasementLayoutV9.cell(z == 73 && x >= 6 ? x - 1 : previousX(x, z), y, z);
    }
}
