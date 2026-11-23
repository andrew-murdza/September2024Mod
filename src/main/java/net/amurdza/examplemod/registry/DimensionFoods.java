package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Edible harvests explicitly requested by the dimension documents and absent from their source mods. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DimensionFoods {
    private static final Map<String, FoodProperties> FOODS = new LinkedHashMap<>();
    private static final FoodProperties RAW = new FoodProperties.Builder().nutrition(3).saturationMod(0.2F).build();
    private static final FoodProperties COOKED = new FoodProperties.Builder().nutrition(6).saturationMod(0.6F).build();

    static {
        add("plum", Foods.APPLE);
        add("raw_arthropleura", RAW);
        add("cooked_arthropleura", COOKED);
        for (String name : new String[]{"octopus",
                "arrow_squid", "vampire_squid",
                "triops", "blue_spotted_stingray", "umbrella_octopus"}) {
            add("raw_" + name, RAW);
            add("cooked_" + name, COOKED);
        }
        for (String name : new String[]{"tuna", "flying_fish", "crab_meat"}) {
            add("cooked_" + name, COOKED);
        }
        add("raw_squid", RAW);
        add("raw_glow_squid", RAW);
        // Legacy sulfur foods stay registered for saved stacks, but are excluded from catalogs and recipes.
        add("sulfur_end_fish", RAW);
        add("cooked_sulfur_end_fish", COOKED);
        // Starfish is a current retained seafood with its own cooking recipes.
        add("cooked_starfish", COOKED);
    }

    private DimensionFoods() { }

    private static void add(String name, FoodProperties food) {
        FOODS.put(name, food);
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> FOODS.forEach((name, food) ->
                helper.register(new ResourceLocation(AOEMod.MOD_ID, name), new Item(new Item.Properties().food(food)))));
    }

    @SubscribeEvent
    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        // Source catalogs enumerate registered foods; vanilla categories stay vanilla.
    }

    public static Item get(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(AOEMod.MOD_ID, name));
    }
}
