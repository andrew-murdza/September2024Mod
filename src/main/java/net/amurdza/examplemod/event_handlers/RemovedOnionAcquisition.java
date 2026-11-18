package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Removes the dependency's onion offers without changing the other crop trades. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RemovedOnionAcquisition {
    private RemovedOnionAcquisition() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void villagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(RemovedOnionAcquisition::removeOnionTrades);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void wanderingTrades(WandererTradesEvent event) {
        removeOnionTrades(event.getGenericTrades());
        removeOnionTrades(event.getRareTrades());
    }

    public static void removeOnionTrades(List<VillagerTrades.ItemListing> trades) {
        trades.removeIf(listing -> {
            // Farmer's Delight uses this concrete, deterministic listing. Avoid invoking
            // arbitrary listings whose generation may require a live merchant or world.
            if (listing.getClass() != BasicItemListing.class) return false;
            var offer = listing.getOffer(null, RandomSource.create(0));
            return offer != null && (isOnion(offer.getBaseCostA()) || isOnion(offer.getCostB())
                    || isOnion(offer.getResult()));
        });
    }

    private static boolean isOnion(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("farmersdelight") && id.getPath().contains("onion");
    }
}
