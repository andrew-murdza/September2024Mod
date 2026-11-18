package net.amurdza.examplemod.block;

import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Wooden wall using vanilla post, connection, collision and stacking behavior. */
public final class AcaciaPalisadeBlock extends WallBlock {
    public AcaciaPalisadeBlock(Properties properties) { super(properties); }

    /** Retained for old templates; vanilla low/tall sides must stay intact. */
    public static BlockState normalConnections(BlockState state) { return state; }
}
