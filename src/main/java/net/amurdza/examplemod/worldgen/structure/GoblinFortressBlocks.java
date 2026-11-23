package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Exact, scoped authored writes let the fortress keep its requested farms and underground habitats. */
public final class GoblinFortressBlocks {
    private record Write(WorldGenLevel level, BlockPos pos, BlockState state) { }
    private static final ThreadLocal<Write> CURRENT = new ThreadLocal<>();

    private GoblinFortressBlocks() { }

    public static boolean isAuthored(LevelReader level, BlockPos pos, BlockState state) {
        var write = CURRENT.get();
        return write != null && write.level == level && write.pos.equals(pos) && write.state == state;
    }

    public static boolean set(WorldGenLevel level, BlockPos pos, BlockState state) {
        var previous = CURRENT.get();
        CURRENT.set(new Write(level, pos.immutable(), state));
        try { return level.setBlock(pos, state, 2); }
        finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
