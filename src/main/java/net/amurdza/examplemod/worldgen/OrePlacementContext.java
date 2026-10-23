package net.amurdza.examplemod.worldgen;

import net.minecraft.world.level.WorldGenLevel;

import java.util.function.BooleanSupplier;

/** OreFeature's direct chunk-section writes still need the level for per-block habitat checks. */
public final class OrePlacementContext {
    private static final ThreadLocal<WorldGenLevel> LEVEL = new ThreadLocal<>();

    private OrePlacementContext() { }

    public static WorldGenLevel level() { return LEVEL.get(); }

    public static boolean run(WorldGenLevel level, BooleanSupplier placement) {
        WorldGenLevel previous = LEVEL.get();
        LEVEL.set(level);
        try { return placement.getAsBoolean(); }
        finally {
            if (previous == null) LEVEL.remove();
            else LEVEL.set(previous);
        }
    }
}
