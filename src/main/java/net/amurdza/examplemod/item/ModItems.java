package net.amurdza.examplemod.item;

import com.github.alexthe666.alexsmobs.item.ItemModFishBucket;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.entity.ModEntities;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS, AOEMod.MOD_ID);
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }

    public final static RegistryObject<Item> BUCKET_END_FISH = ITEMS.register("bucket_end_fish",()->new ItemModFishBucket(ModEntities.END_FISH, Fluids.WATER, new Item.Properties()));
    public final static RegistryObject<Item> BUCKET_CUBOZOA = ITEMS.register("bucket_cubozoa",()->new ItemModFishBucket(ModEntities.CUBOZOA, Fluids.WATER, new Item.Properties()));


    public static final RegistryObject<Item> COOKED_PUFFERFISH = ITEMS.register
            ("cooked_pufferfish", () -> new Item((new Item.Properties()).food(Foods.COOKED_SALMON)));
    public static final RegistryObject<Item> COOKED_TUNA = ITEMS.register("cooked_tuna", () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> CUBOZOA = ITEMS.register("cubozoa", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_CUBOZOA = ITEMS.register("cooked_cubozoa", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> END_FISH = ITEMS.register("end_fish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_END_FISH = ITEMS.register("cooked_end_fish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> NETHER_CRAB_CLAW = ITEMS.register("nether_crab_claw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_NETHER_CRAB_CLAW = ITEMS.register("cooked_nether_crab_claw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));

    public static final  RegistryObject<Item> SOUL_BERRIES= ITEMS.register("soul_berries",()->new ItemNameBlockItem(ModBlocks.SOUL_BERRY_BUSH.get(),
            (new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.6F).build())));

    public static final  RegistryObject<Item> SOUL_BERRY_COOKIE= ITEMS.register("soul_berry_cookie",() ->
            new Item((new Item.Properties()).food(Foods.COOKIE)));

    public static final  RegistryObject<Item> GLOW_BERRY_JUICE= ITEMS.register("glow_berry_juice",() ->
            new Item((new Item.Properties()).food(Foods.COOKIE)));

    public static final RegistryObject<Item> BONE_ARROW = ITEMS.register("bone_arrow", ()->new ArrowItem(new Item.Properties()));
    public static final RegistryObject<Item> BONE_SWORD = ITEMS.register("bone_sword", ()->new SwordItem(ModToolTiers.BONE, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_SHOVEL = ITEMS.register("bone_shovel", ()->new ShovelItem(ModToolTiers.BONE, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_PICKAXE = ITEMS.register("bone_pickaxe", ()->new PickaxeItem(ModToolTiers.BONE, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_AXE = ITEMS.register("bone_axe", ()->new AxeItem(ModToolTiers.BONE, 7.0F, -3.2F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_HOE = ITEMS.register("bone_hoe", ()->new HoeItem(ModToolTiers.BONE, -1, -2.0F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_SHEARS = ITEMS.register("bone_shears", ()->new ShearsItem((new Item.Properties()).durability(400)));

}
