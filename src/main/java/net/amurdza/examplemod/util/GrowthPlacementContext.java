package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** A scoped distinction between natural chorus growth and a player planting a new flower. */
public final class GrowthPlacementContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private record Tree(ResourceKey<Level> dimension, BlockPos origin) { }
    private static final ThreadLocal<Tree> TREE = new ThreadLocal<>();
    private GrowthPlacementContext() { }
    public static boolean isGrowing() { return DEPTH.get() > 0; }

    /** Only an atomic tree attempt may authorize blocks above its native biome. */
    public static void runTree(LevelReader level, BlockPos origin, Runnable growth) {
        Tree previous = TREE.get();
        TREE.set(new Tree(GameplayDimensions.dimension(level), origin.immutable()));
        try { run(growth); }
        finally {
            if (previous == null) TREE.remove();
            else TREE.set(previous);
        }
    }

    public static boolean allowsCanopyExtension(LevelReader level, BlockPos pos, BlockState state) {
        Tree tree = TREE.get();
        return tree != null && tree.dimension() != null
                && tree.dimension().equals(GameplayDimensions.dimension(level))
                && TreeGrowthPlacementRules.allowsExtension(level, tree.origin(), pos, state);
    }
    public static void run(Runnable growth) {
        int previous = DEPTH.get();
        DEPTH.set(previous + 1);
        try { growth.run(); }
        finally {
            if (previous == 0) DEPTH.remove();
            else DEPTH.set(previous);
        }
    }
}
