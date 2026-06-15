package net.amurdza.examplemod.registry;

import com.github.alexthe666.alexsmobs.item.ItemModFishBucket;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.ModToolTiers;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AOEMod.MOD_ID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final RegistryObject<Item> BUCKET_END_FISH = ITEMS.register(
            "bucket_end_fish",
            () -> new ItemModFishBucket(ModEntities.END_FISH, Fluids.WATER, new Item.Properties())
    );

    public static final RegistryObject<Item> BUCKET_CUBOZOA = ITEMS.register(
            "bucket_cubozoa",
            () -> new ItemModFishBucket(ModEntities.CUBOZOA, Fluids.WATER, new Item.Properties())
    );

    public static final RegistryObject<Item> COOKED_PUFFERFISH = ITEMS.register(
            "cooked_pufferfish",
            () -> new Item(new Item.Properties().food(Foods.COOKED_SALMON))
    );

    public static final RegistryObject<Item> COOKED_TROPICAL_FISH = ITEMS.register(
            "cooked_tropical_fish",
            () -> new Item(new Item.Properties().food(Foods.COOKED_COD))
    );

    public static final RegistryObject<Item> RAW_SQUIRREL = ITEMS.register(
            "raw_squirrel",
            () -> new Item(new Item.Properties().food(Foods.RABBIT))
    );


    public static final RegistryObject<Item> COOKED_SQUIRREL = ITEMS.register(
            "cooked_squirrel",
            () -> new Item(new Item.Properties().food(Foods.COOKED_RABBIT))
    );

    public static final RegistryObject<Item> RAW_FOX = ITEMS.register(
            "raw_fox",
            () -> new Item(new Item.Properties().food(Foods.CHICKEN))
    );


    public static final RegistryObject<Item> COOKED_FOX = ITEMS.register(
            "cooked_fox",
            () -> new Item(new Item.Properties().food(Foods.COOKED_CHICKEN))
    );

    public static final RegistryObject<Item> RAW_MOOSE = ITEMS.register(
            "raw_moose",
            () -> new Item(new Item.Properties().food(Foods.BEEF))
    );


    public static final RegistryObject<Item> COOKED_MOOSE = ITEMS.register(
            "cooked_moose",
            () -> new Item(new Item.Properties().food(Foods.COOKED_BEEF))
    );

    public static final RegistryObject<Item> RAW_WARPED_TOAD = ITEMS.register(
            "raw_warped_toad",
            () -> new Item(new Item.Properties().food(Foods.CHICKEN))
    );


    public static final RegistryObject<Item> COOKED_WARPED_TOAD = ITEMS.register(
            "cooked_warped_toad",
            () -> new Item(new Item.Properties().food(Foods.COOKED_CHICKEN))
    );

    public static final RegistryObject<Item> RAW_SQUID = ITEMS.register(
            "raw_squid",
            () -> new Item(new Item.Properties().food(Foods.CHICKEN))
    );

    public static final RegistryObject<Item> RAW_GLOW_SQUID = ITEMS.register(
            "raw_glow_squid",
            () -> new Item(new Item.Properties().food(Foods.CHICKEN))
    );

    public static final RegistryObject<Item> COOKED_SQUID = ITEMS.register(
            "cooked_squid",
            () -> new Item(new Item.Properties().food(Foods.COOKED_CHICKEN))
    );

    public static final RegistryObject<Item> RAW_ARROW_SQUID = ITEMS.register(
            "raw_arrow_squid",
            () -> new Item(new Item.Properties().food(Foods.CHICKEN))
    );

    public static final RegistryObject<Item> COOKED_ARROW_SQUID = ITEMS.register(
            "cooked_arrow_squid",
            () -> new Item(new Item.Properties().food(Foods.COOKED_CHICKEN))
    );

    public static final RegistryObject<Item> NETHER_FUNGUS_STEW = ITEMS.register(
            "nether_fungus_stew",
            () -> new BowlFoodItem(new Item.Properties().food(Foods.MUSHROOM_STEW))
    );

    public static final RegistryObject<Item> CUBOZOA = ITEMS.register(
            "cubozoa",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationMod(0.3F)
                            .build()
            ))
    );

    public static final RegistryObject<Item> COOKED_CUBOZOA = ITEMS.register(
            "cooked_cubozoa",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(0.8F)
                            .build()
            ))
    );

    public static final RegistryObject<Item> END_FISH = ITEMS.register(
            "end_fish",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationMod(0.3F)
                            .build()
            ))
    );

    public static final RegistryObject<Item> COOKED_END_FISH = ITEMS.register(
            "cooked_end_fish",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(0.8F)
                            .build()
            ))
    );

    public static final RegistryObject<Item> CHERRIES = ITEMS.register(
            "cherries",
            () -> new Item(new Item.Properties().food(Foods.APPLE))
    );

    public static final RegistryObject<Item> PEAR = ITEMS.register(
            "pear",
            () -> new Item(new Item.Properties().food(Foods.APPLE))
    );

    public static final RegistryObject<Item> PLUM = ITEMS.register(
            "plum",
            () -> new Item(new Item.Properties().food(Foods.APPLE))
    );

    public static final RegistryObject<Item> ORANGE = ITEMS.register(
            "orange",
            () -> new Item(new Item.Properties().food(Foods.APPLE))
    );

    public static final RegistryObject<Item> MELON_PIE = ITEMS.register(
            "melon_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> GLOW_BERRY_PIE = ITEMS.register(
            "glow_berry_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> PEAR_PIE = ITEMS.register(
            "pear_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> CHERRY_PIE = ITEMS.register(
            "cherry_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> BANANA_PIE = ITEMS.register(
            "banana_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> PLUM_PIE = ITEMS.register(
            "plum_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> ORANGE_JUICE = ITEMS.register(
            "orange_juice",
            () -> new BowlFoodItem(new Item.Properties().stacksTo(1).food(Foods.MUSHROOM_STEW))
    );

    public static final RegistryObject<Item> CARROT_SEEDS = ITEMS.register(
            "carrot_seeds",
            () -> new ItemNameBlockItem(Blocks.CARROTS, new Item.Properties())
    );

    public static final RegistryObject<Item> POTATO_SEEDS = ITEMS.register(
            "potato_seeds",
            () -> new ItemNameBlockItem(Blocks.POTATOES, new Item.Properties())
    );

    public static final RegistryObject<Item> ONION_SEEDS = ITEMS.register(
            "onion_seeds",
            () -> new ItemNameBlockItem(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> RICE_SEEDS = ITEMS.register(
            "rice_seeds",
            () -> new ItemNameBlockItem(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> SOUL_BERRIES = ITEMS.register(
            "soul_berries",
            () -> new ItemNameBlockItem(
                    ModBlocks.SOUL_BERRY_BUSH.get(),
                    new Item.Properties().food(
                            new FoodProperties.Builder()
                                    .nutrition(2)
                                    .saturationMod(0.6F)
                                    .build()
                    )
            )
    );

    public static final RegistryObject<Item> CHORUS_BREAD = ITEMS.register(
            "chorus_bread",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.4F)
                            .build()
            ))
    );

    public static final RegistryObject<Item> SOUL_BERRY_COOKIE = ITEMS.register(
            "soul_berry_cookie",
            () -> new Item(new Item.Properties().food(Foods.COOKIE))
    );

    public static final RegistryObject<Item> BONE_ARROW = ITEMS.register(
            "bone_arrow",
            () -> new ArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_SWORD = ITEMS.register(
            "bone_sword",
            () -> new SwordItem(ModToolTiers.BONE, 3, -2.4F, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_SHOVEL = ITEMS.register(
            "bone_shovel",
            () -> new ShovelItem(ModToolTiers.BONE, 1.5F, -3.0F, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_PICKAXE = ITEMS.register(
            "bone_pickaxe",
            () -> new PickaxeItem(ModToolTiers.BONE, 1, -2.8F, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_AXE = ITEMS.register(
            "bone_axe",
            () -> new AxeItem(ModToolTiers.BONE, 7.0F, -3.2F, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_HOE = ITEMS.register(
            "bone_hoe",
            () -> new HoeItem(ModToolTiers.BONE, -1, -2.0F, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_SHEARS = ITEMS.register(
            "bone_shears",
            () -> new ShearsItem(new Item.Properties().durability(400))
    );
}
