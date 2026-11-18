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
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) net.amurdza.examplemod.util.ObtainableContent.rebuild(
                event.getRecipeManager(), connection.registryAccess());
    }

    @SubscribeEvent
    public static void onTagsUpdated(net.minecraftforge.event.TagsUpdatedEvent event) {
        if (event.getUpdateCause() != net.minecraftforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) return;
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        // Dedicated-server login sends recipes before tags. Recompute once the
        // actual dimension-source tags arrive, rather than retaining that partial catalog.
        if (connection != null) net.amurdza.examplemod.util.ObtainableContent.rebuild(
                connection.getRecipeManager(), event.getRegistryAccess());
    }
}
