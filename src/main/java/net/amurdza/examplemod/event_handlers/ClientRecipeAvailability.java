package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRecipeAvailability {
    private ClientRecipeAvailability() { }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        DimensionMaterialAvailability.clearCache();
    }
}
