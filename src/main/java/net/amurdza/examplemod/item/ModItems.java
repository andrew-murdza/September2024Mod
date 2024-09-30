package net.amurdza.examplemod.item;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS, AOEMod.MOD_ID);
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
    public static final RegistryObject<Item> GLOW_BERRIES= ITEMS.register("glow_berries",
            ()->new ItemNameBlockItem(ModBlocks.CAVE_VINES_HEAD.get(), (new Item.Properties()).food(Foods.GLOW_BERRIES)));
    public static final  RegistryObject<Item> BLUE_BERRIES= ITEMS.register("blue_berries",()->new ItemNameBlockItem(ModBlocks.BLUE_BERRY_BUSH.get(),
            (new Item.Properties()).food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.6F).build())));
}
