package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Completes the lower cellar's front walls without changing either floor's rooms or entrances. */
public final class MansionBasementLayoutV12 {
    private MansionBasementLayoutV12() { }

    public static BlockState cell(int x, int y, int z) {
        int side = Math.abs(x);
        // The continuing flight ends at Z19. Close the shoulders beside its
        // three-wide opening before the lower patio starts at Z20.
        if (z == 19 && side >= 2 && side <= 7 && y >= -9 && y <= -1)
            return Blocks.STONE_BRICKS.defaultBlockState();

        // V11 copied rooms starting at old Z14, leaving their old Z13 front
        // wall as natural terrain. Keep the patio's own door wall, and copy
        // the missing room/mine wall on either side of it. The east rooms
        // retain V10's one-block shift for the widened central aisle.
        if (z == 25 && side > 7 && y >= -9 && y <= -1) {
            var wall = MansionBasementLayoutV10.cell(x >= 5 ? x - 1 : x,
                    y - MansionBasementLayoutV11.LOWER_FLOOR, 13);
            if (wall != null) return wall;
        }
        return MansionBasementLayoutV11.cell(x, y, z);
    }
}
