package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Outer-wall mine lights and regional ore pockets; V13's masonry stays intact. */
public final class MansionBasementLayoutV14 {
    private static final Block[] ORES = {Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE};

    private MansionBasementLayoutV14() { }

    public static boolean isOre(BlockState state) {
        return state != null && (state.is(Blocks.COAL_ORE) || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.GOLD_ORE) || state.is(Blocks.COPPER_ORE));
    }

    public static BlockState cell(int x, int y, int z) {
        var previous = MansionBasementLayoutV13.cell(x, y, z);
        if (previous == null || x < -48 || x > -30) return previous;
        boolean lower = y < -1;
        int mineY = y - (lower ? MansionBasementLayoutV11.LOWER_FLOOR : 0);
        int mineZ = z - (lower ? MansionBasementLayoutV11.SECOND_ROOM_SHIFT : MansionBasementLayoutV11.FIRST_ROOM_SHIFT);
        if (mineZ < 13 || mineZ > 49 || mineY < -1 || mineY > 5) return previous;
        int row = Math.min(2, Math.max(0, (mineZ - 14) / 12));
        int across = mineZ - (14 + row * 12), depth = -x - 30;
        if (previous.is(Blocks.WALL_TORCH)) {
            // Each pair of tunnels faces away from the stone between them.
            return previous.setValue(WallTorchBlock.FACING, across == 2 ? Direction.SOUTH : Direction.NORTH);
        }
        // Never replace the brick shell, tunnel air, furnishings or lights.
        if (!previous.is(Blocks.STONE)) return previous;
        for (int vein = 0; vein < ORES.length; vein++) for (int outerWall : new int[]{1, 9}) {
            int distance = Math.abs(depth - (4 + vein * 4)) + Math.abs(mineY - 2) + Math.abs(across - outerWall);
            if (distance <= 1) return ORES[Math.floorMod(vein + row + (lower ? 1 : 0), ORES.length)].defaultBlockState();
        }
        return previous;
    }
}
