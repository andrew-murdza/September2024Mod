package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.Set;

/** Intentionally removed food stacks disappear without interrupting existing-save loading. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RetiredFoods {
    private static final Set<String> REMOVED_FOODS = Set.of(
            "onion_seeds", "raw_anglerfish", "cooked_anglerfish", "raw_warped_toad", "cooked_warped_toad", "cooked_pufferfish", "cooked_tropical_fish",
            "raw_mahi_mahi", "cooked_mahi_mahi", "raw_rubber_belly_glider", "cooked_rubber_belly_glider", "cooked_carp");

    private RetiredFoods() { }

    @SubscribeEvent
    public static void discardRemovedFoods(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(Registries.ITEM, AOEMod.MOD_ID)) {
            if (mapping.getKey().getPath().equals("bull_pepper_juice")) mapping.remap(net.amurdza.examplemod.registry.ModItems.BULLET_PEPPER_SOUP.get());
            else if (REMOVED_FOODS.contains(mapping.getKey().getPath())) mapping.ignore();
        }
    }
}
