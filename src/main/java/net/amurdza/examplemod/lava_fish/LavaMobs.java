package net.amurdza.examplemod.lava_fish;

import com.legacy.blue_skies.registries.SkiesEntityTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class LavaMobs {
    public static boolean isLavaMob(EntityType<?> type) {
        return type == SkiesEntityTypes.MUNICIPAL_MONKFISH;
    }

    public static boolean isLavaMob(Object entity) {
        return isLavaMob(((Entity)entity).getType());
    }
}
