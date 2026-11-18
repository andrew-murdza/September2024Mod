package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;

/** Large aquatic animals receive one natural spawning pass for every four small-fish passes. */
public final class AquaticSpawnAttempts {
    public static final int LARGE_WATER_INTERVAL = 4;

    private AquaticSpawnAttempts() { }

    public static boolean allows(LevelReader level, MobCategory category, ChunkPos chunk, long gameTime) {
        if (category != MobCategory.WATER_CREATURE && category != net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_CREATURE
                || !GameplayDimensions.isCustom(level)) return true;
        // Offset neighbouring chunks so the reduced workload is spread across ticks.
        return Math.floorMod(gameTime + chunk.x + (long) chunk.z, LARGE_WATER_INTERVAL) == 0;
    }
}
