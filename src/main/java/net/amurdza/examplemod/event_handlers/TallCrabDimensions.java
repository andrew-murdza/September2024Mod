package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Match collision and eye height to the two enlarged, raised crab models. */
// Retained for the historical geometry audit; native crab dimensions are now used.
public final class TallCrabDimensions {
    private TallCrabDimensions() { }

    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (!AquaticMobSizes.isTallCrab(event.getEntity())) return;
        // The event also fires during construction, before synchronized age/size
        // data exists. getNewSize is freshly calculated from getDimensions(pose).
        // getOriginalSize is the previous, already enlarged collision box; using
        // it here would multiply the size again on every refresh.
        float inheritedScale = event.getNewSize().width / event.getEntity().getType().getDimensions().width;
        event.setNewSize(EntityDimensions.scalable(AquaticMobSizes.tallCrabWidth(event.getEntity()),
                AquaticMobSizes.tallCrabHeight(event.getEntity())).scale(inheritedScale));
        event.setNewEyeHeight(AquaticMobSizes.tallCrabHeight(event.getEntity()) * inheritedScale * 0.94F);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (AquaticMobSizes.isTallCrab(event.getEntity())) event.getEntity().refreshDimensions();
    }
}
