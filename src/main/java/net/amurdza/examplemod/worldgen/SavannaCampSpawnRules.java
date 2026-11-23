package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;

/** All camp spawns are authored once, regardless of ambient mob category. */
public final class SavannaCampSpawnRules {
    private SavannaCampSpawnRules() { }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, EntityType<?> type) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        WorldGenLevel world = level instanceof WorldGenLevel generation ? generation : level.getLevel();
        return StructurePlantExclusion.excludes(world, pos);
    }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, Mob mob) {
        return blocked(level, pos, mob.getType());
    }
}
