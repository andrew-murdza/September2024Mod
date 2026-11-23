package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.world.level.block.state.BlockState;

/** Seals the first upper crop-room wall as well as V12's lower cellar walls. */
public final class MansionBasementLayoutV13 {
    private MansionBasementLayoutV13() { }

    public static BlockState cell(int x, int y, int z) {
        // Moving the upper rooms five blocks left their original front plane
        // outside V11's copied range. Replace terrain with the authored brick
        // wall while leaving the central landing, crops and lights untouched.
        if (z == 18 && Math.abs(x) > 7 && y >= -2 && y <= 6) {
            var wall = MansionBasementLayoutV10.cell(x >= 5 ? x - 1 : x, y, 13);
            if (wall != null) return wall;
        }
        return MansionBasementLayoutV12.cell(x, y, z);
    }
}
