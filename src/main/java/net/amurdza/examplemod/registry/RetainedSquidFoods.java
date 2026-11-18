package net.amurdza.examplemod.registry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Cultural Delights 0.16.1's three squid foods, without its other items or world generation. */
public final class RetainedSquidFoods {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "culturaldelights");
    public static final RegistryObject<Item> SQUID = ITEMS.register("squid", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).build())));
    public static final RegistryObject<Item> GLOW_SQUID = ITEMS.register("glow_squid", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F)
                    .effect(() -> new MobEffectInstance(MobEffects.GLOWING, 40, 1), 1.0F).alwaysEat().build())));
    public static final RegistryObject<Item> COOKED_SQUID = ITEMS.register("cooked_squid", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.8F).build())));

    private RetainedSquidFoods() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
