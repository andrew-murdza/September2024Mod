package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class StructureSpawnedMobData {
    private static final String STRUCTURE_SPAWNED = "aoemod.structureSpawned";
    private static final String HOME_X = "aoemod.structureHomeX";
    private static final String HOME_Y = "aoemod.structureHomeY";
    private static final String HOME_Z = "aoemod.structureHomeZ";
    public static final int HOME_RADIUS = 10;

    private StructureSpawnedMobData() {
    }

    public static void mark(Mob mob, BlockPos home) {
        CompoundTag data = mob.getPersistentData();
        data.putBoolean(STRUCTURE_SPAWNED, true);
        data.putInt(HOME_X, home.getX());
        data.putInt(HOME_Y, home.getY());
        data.putInt(HOME_Z, home.getZ());
        mob.setPersistenceRequired();

        if (mob instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.restrictTo(home, HOME_RADIUS);
        }
    }

    public static boolean isMarked(Mob mob) {
        return mob.getPersistentData().getBoolean(STRUCTURE_SPAWNED);
    }

    public static BlockPos home(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        return new BlockPos(data.getInt(HOME_X), data.getInt(HOME_Y), data.getInt(HOME_Z));
    }
}
