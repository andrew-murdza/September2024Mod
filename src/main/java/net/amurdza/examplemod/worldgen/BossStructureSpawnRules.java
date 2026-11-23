package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

/** Ambient spawns never refill an authored boss roster, including after it has been defeated. */
public final class BossStructureSpawnRules {
    private BossStructureSpawnRules() { }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, EntityType<?> type) {
        if (SavannaCampSpawnRules.blocked(level, pos, type)) return true;
        return !BossStructureBounds.get(level.getLevel()).nearby(new AABB(pos)).isEmpty();
    }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, Mob mob) {
        return blocked(level, pos, mob.getType());
    }
}
