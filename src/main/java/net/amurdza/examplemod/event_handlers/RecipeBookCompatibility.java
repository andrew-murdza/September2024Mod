package net.amurdza.examplemod.event_handlers;

import alabaster.hearthandharvest.common.HHRecipeBookTypes;
import net.amurdza.examplemod.AOEMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import vectorwing.farmersdelight.FarmersDelight;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RecipeBookCompatibility {
    private RecipeBookCompatibility() { }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Hearth & Harvest 1.4.1 otherwise creates these types only when its client
            // categories or server menus are used. RecipeBookSettings sends two booleans
            // per enum value without a count, so both physical sides must initialize them
            // before any player connects. Preserve the existing upstream enum instances
            // and the order established by Farmer's Delight's common mod constructor.
            Objects.requireNonNull(FarmersDelight.RECIPE_TYPE_COOKING);
            Objects.requireNonNull(HHRecipeBookTypes.AGING);
            Objects.requireNonNull(HHRecipeBookTypes.FERMENTING);
        });
    }
}
