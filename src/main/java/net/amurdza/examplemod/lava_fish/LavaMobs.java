package net.amurdza.examplemod.lava_fish;

import codyhuh.unusualfishmod.core.registry.UFEntities;
import com.legacy.blue_skies.registries.SkiesEntityTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class LavaMobs {
    public static boolean isLavaMob(EntityType<?> type) {
        return type == UFEntities.SEA_SPIDER.get() || type == UFEntities.CRIMSONSHELL_SQUID.get()
                || type==UFEntities.PINKFIN.get() || type==UFEntities.AMBER_GOBY.get()
                || type == SkiesEntityTypes.HORIZOFIN_TUNID || type == SkiesEntityTypes.CHARSCALE_MOKI;
    }

    public static boolean isLavaMob(Object entity) {
        return isLavaMob(((Entity)entity).getType());
    }
}
