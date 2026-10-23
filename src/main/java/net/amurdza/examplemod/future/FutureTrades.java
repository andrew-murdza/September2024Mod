package net.amurdza.examplemod.future;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renewable acquisition for requested plants whose generated features are deliberately excluded. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FutureTrades {
    private FutureTrades() { }

    @SubscribeEvent
    public static void addRequestedPlantTrades(WandererTradesEvent event) {
        for (String name : new String[]{"wildflowers", "shelf_mushroom", "poplar_sapling"}) {
            var item = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", name));
            event.getGenericTrades().add((trader, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, name.equals("poplar_sapling") ? 5 : 1),
                    new ItemStack(item), 8, 1, 0.05F));
        }
    }
}
