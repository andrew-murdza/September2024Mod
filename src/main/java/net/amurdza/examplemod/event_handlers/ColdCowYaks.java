package net.amurdza.examplemod.event_handlers;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariants;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/** Recognition of legacy cow-based yaks; all current mount behavior lives in YakEntity. */
public final class ColdCowYaks {
    private ColdCowYaks() { }
    @SuppressWarnings("unchecked")
    public static void makeCold(Cow cow) {
        if (cow instanceof VariantDataHolder<?> holder) {
            ((VariantDataHolder<CowVariant>) holder).setVariantData(CowVariants.REGISTRIES.getOrThrow(CowVariants.COLD));
        }
    }
    public static boolean isColdCow(Cow cow) {
        return cow.getType() == EntityType.COW && cow instanceof VariantDataHolder<?> holder
                && holder.getVariantData().orElse(null) == CowVariants.REGISTRIES.getOrThrow(CowVariants.COLD);
    }
}
