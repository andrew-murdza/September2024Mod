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
        for (String name : new String[]{"mahi_mahi", "anomalocaris", "octopus", "rubber_belly_glider",
                "flying_fish", "arrow_squid", "vampire_squid", "anglerfish", "spider_crab_leg",
                "isopod", "triops", "blue_spotted_stingray", "hound_shark", "shame_faced_crab"}) {
            add("raw_" + name, RAW);
            add("cooked_" + name, COOKED);
        }
        for (String name : new String[]{"trout", "carp", "swamp_mucker", "needlefish", "tuna",
                "dragonfish", "giant_river_prawn"}) add("cooked_" + name, COOKED);
        add("sulfur_end_fish", RAW);
        add("cooked_sulfur_end_fish", COOKED);
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
        if (event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)) FOODS.keySet().forEach(name -> event.accept(get(name)));
    }

    public static Item get(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(AOEMod.MOD_ID, name));
    }
}
