package net.amurdza.examplemod.lava_fish;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class LavaMobs {
    public static boolean isLavaMob(EntityType<?> type) {
        return false;
    }

    public static boolean isLavaMob(Object entity) {
        return isLavaMob(((Entity)entity).getType());
    }
}
