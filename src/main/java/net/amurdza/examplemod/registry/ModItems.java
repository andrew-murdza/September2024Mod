package net.amurdza.examplemod.registry;

import com.github.alexthe666.alexsmobs.item.ItemModFishBucket;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.CropSeedItem;
import net.amurdza.examplemod.item.MaceItem;
import net.amurdza.examplemod.item.MaterialShieldItem;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.item.SpearItem;
import net.amurdza.examplemod.item.StackableBowlFoodItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AOEMod.MOD_ID);

    // Retired item ID retained so inventories from older worlds can still load.
    public static final RegistryObject<Item> MANSION_KEY = ITEMS.register("mansion_key", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOSS_AXE = ITEMS.register("boss_axe",
            () -> new AxeItem(Tiers.IRON, 6.0F, -3.1F, new Item.Properties()));

    public static final RegistryObject<Item> GOBLIN_KNIGHT_SPAWN_EGG = ITEMS.register("spawn_egg_goblin_knight",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(twilightforest.init.TFEntities.UPPER_GOBLIN_KNIGHT,
                    0x5A6B36, 0xA0A6AB, new Item.Properties()));

    // Orcz registers these group selectors and spell-effect mobs without eggs of its own.
    public static final RegistryObject<Item> ORCZ_SAVAGE_ORC_SPAWN_EGG = orczSpawnEgg("savageorc", 0x456336, 0x6A4C31);
    public static final RegistryObject<Item> ORCZ_SAVAGE_GOBLIN_SPAWN_EGG = orczSpawnEgg("savagegoblin", 0x669141, 0x413222);
    public static final RegistryObject<Item> ORCZ_IRON_SAVAGE_ORC_SPAWN_EGG = orczSpawnEgg("savageorciron", 0x456336, 0xA8A8A8);
    public static final RegistryObject<Item> ORCZ_DECAY_STRIKE_SPAWN_EGG = orczSpawnEgg("decaystrike", 0x253B22, 0xA1C94A);
    public static final RegistryObject<Item> ORCZ_CUSTOM_DECAY_STRIKE_SPAWN_EGG = orczSpawnEgg("decaystrikecustom", 0x253B22, 0x7964A8);
    public static final RegistryObject<Item> ORCZ_FLAME_DECAY_STRIKE_SPAWN_EGG = orczSpawnEgg("decaystrikeflame", 0x253B22, 0xDF7429);

    private static RegistryObject<Item> orczSpawnEgg(String entity, int primary, int secondary) {
        return ITEMS.register("spawn_egg_orcz_" + entity, () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                () -> (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>)
                        ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("orcz", entity)),
                primary, secondary, new Item.Properties()));
    }

    private static final FoodProperties RAW_FISH_FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.3F)
            .build();

    private static final FoodProperties COOKED_FISH_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationMod(0.8F)
            .build();

    private static final FoodProperties FARMERS_DELIGHT_COOKIE = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .fast()
            .build();

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static Item endFishForVariant(int variant, boolean cooked) {
        // Legacy sulfur variants must not reintroduce their retired food items.
        return cooked ? COOKED_END_FISH.get() : END_FISH.get();
    }

    public static final RegistryObject<Item> BUCKET_END_FISH = ITEMS.register(
            "bucket_end_fish",
            () -> new ItemModFishBucket(ModEntities.END_FISH, Fluids.WATER, new Item.Properties())
    );

    public static final RegistryObject<Item> BUCKET_CUBOZOA = ITEMS.register(
            "bucket_cubozoa",
            () -> new ItemModFishBucket(ModEntities.CUBOZOA, Fluids.LAVA, new Item.Properties())
    );


    public static final RegistryObject<Item> END_FISH = ITEMS.register(
            "end_fish",
            () -> new Item(new Item.Properties().food(RAW_FISH_FOOD))
    );
    public static final RegistryObject<Item> COOKED_END_FISH = ITEMS.register(
            "cooked_end_fish",
            () -> new Item(new Item.Properties().food(COOKED_FISH_FOOD))
    );

    public static final RegistryObject<Item> COOKED_GLOW_SQUID = ITEMS.register(
            "cooked_glow_squid",
            () -> new Item(new Item.Properties().food(Foods.COOKED_COD))
    );

    public static final RegistryObject<Item> COOKED_SQUID = ITEMS.register(
            "cooked_squid",
            () -> new Item(new Item.Properties().food(Foods.COOKED_COD))
    );

    public static final RegistryObject<Item> MELON_PIE = ITEMS.register(
            "melon_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> GLOWBERRY_PIE = ITEMS.register(
            "glowberry_pie",
            () -> new Item(new Item.Properties().food(Foods.PUMPKIN_PIE))
    );

    public static final RegistryObject<Item> ORANGE_PIE = ITEMS.register(
            "orange_pie",
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

    public static final RegistryObject<Item> CARROT_SEEDS = ITEMS.register(
            "carrot_seeds",
            () -> new CropSeedItem(Blocks.CARROTS, new Item.Properties())
    );

    public static final RegistryObject<Item> POTATO_SEEDS = ITEMS.register(
            "potato_seeds",
            () -> new CropSeedItem(Blocks.POTATOES, new Item.Properties())
    );

    public static final RegistryObject<Item> BEAN_SEEDS = ITEMS.register(
            "bean_seeds",
            () -> new CropSeedItem(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    new net.minecraft.resources.ResourceLocation("legumedelight", "budding_beans")), new Item.Properties())
    );

    public static final RegistryObject<Item> RICE_SEEDS = ITEMS.register(
            "rice_seeds",
            () -> new CropSeedItem(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> SOUL_BEETROOT = ITEMS.register(
            "soul_beetroot",
            () -> new Item(new Item.Properties().food(Foods.BEETROOT))
    );

    public static final RegistryObject<Item> SOUL_BEETROOT_SEEDS = ITEMS.register(
            "soul_beetroot_seeds",
            () -> new ItemNameBlockItem(ModBlocks.SOUL_BEETROOTS.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> SOUL_BEETROOT_SOUP = ITEMS.register(
            "soul_beetroot_soup",
            () -> new StackableBowlFoodItem(new Item.Properties().food(Foods.BEETROOT_SOUP))
    );


    public static final RegistryObject<Item> BLUEBERRY_COOKIE = ITEMS.register(
            "blueberry_cookie",
            () -> new Item(new Item.Properties().food(FARMERS_DELIGHT_COOKIE))
    );


    public static final RegistryObject<Item> GLOWSHROOM_STEW = bowlFood("glowshroom_stew", Foods.MUSHROOM_STEW);
    public static final RegistryObject<Item> MUSHGLOOM_STEW = bowlFood("mushgloom_stew", Foods.MUSHROOM_STEW);
    public static final RegistryObject<Item> BULLET_PEPPER_SOUP = bowlFood("bullet_pepper_soup", Foods.MUSHROOM_STEW);
    public static final RegistryObject<Item> CARROT_SOUP = bowlFood("carrot_soup", Foods.BEETROOT_SOUP);

    private static RegistryObject<Item> bowlFood(String name, FoodProperties food) {
        return ITEMS.register(name, () -> new BowlFoodItem(new Item.Properties().stacksTo(1).food(food)));
    }

    public static final RegistryObject<Item> HONEY_BOWL = ITEMS.register(
            "honey_bowl",
            () -> new BowlFoodItem(new Item.Properties().stacksTo(1).food(
                    new FoodProperties.Builder().nutrition(6).saturationMod(0.1F).build()))
    );

    public static final RegistryObject<Item> BONE_ARROW = ITEMS.register(
            "bone_arrow",
            () -> new ArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> BREEZE_ROD = ITEMS.register(
            "breeze_rod",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> MACE = ITEMS.register(
            "mace",
            () -> new MaceItem(new Item.Properties().durability(500))
    );

    public static final RegistryObject<Item> WOODEN_SHIELD = materialShield(
            "wooden_shield", "shield_wooden", 336, () -> Ingredient.of(ItemTags.PLANKS), false, 1.0F
    );
    public static final RegistryObject<Item> COPPER_SHIELD = materialShield(
            "copper_shield", "shield_copper", 448, () -> Ingredient.of(Items.COPPER_INGOT), false, 1.0F
    );
    public static final RegistryObject<Item> IRON_SHIELD = materialShield(
            "iron_shield", "shield_iron", 560, () -> Ingredient.of(Items.IRON_INGOT), false, 1.0F
    );
    public static final RegistryObject<Item> GOLDEN_SHIELD = materialShield(
            "golden_shield", "shield_gold", 280, () -> Ingredient.of(Items.GOLD_INGOT), false, 1.0F
    );
    public static final RegistryObject<Item> DIAMOND_SHIELD = materialShield(
            "diamond_shield", "shield_diamond", 896, () -> Ingredient.of(Items.DIAMOND), false, 1.0F
    );
    public static final RegistryObject<Item> NETHERITE_SHIELD = materialShield(
            "netherite_shield", "shield_netherite", 504, () -> Ingredient.of(Items.NETHERITE_INGOT), true, 1.0F
    );
    public static final RegistryObject<Item> BOSS_SHIELD = materialShield(
            "boss_shield", "boss_shield", 1120, () -> Ingredient.of(Items.IRON_INGOT), false, 1.2F
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

    public static final RegistryObject<Item> COPPER_SWORD = nativeItem("copper_sword");
    public static final RegistryObject<Item> COPPER_SHOVEL = nativeItem("copper_shovel");
    public static final RegistryObject<Item> COPPER_PICKAXE = nativeItem("copper_pickaxe");
    public static final RegistryObject<Item> COPPER_AXE = nativeItem("copper_axe");
    public static final RegistryObject<Item> COPPER_HOE = nativeItem("copper_hoe");

    public static final RegistryObject<Item> COPPER_HELMET = nativeItem("copper_helmet");
    public static final RegistryObject<Item> COPPER_CHESTPLATE = nativeItem("copper_chestplate");
    public static final RegistryObject<Item> COPPER_LEGGINGS = nativeItem("copper_leggings");
    public static final RegistryObject<Item> COPPER_BOOTS = nativeItem("copper_boots");

    public static final RegistryObject<Item> BONE_SPEAR = ITEMS.register("bone_spear",
            () -> new SpearItem(ModToolTiers.BONE, 0.85F, 0.82F, 0.65F, 12.0F, 8.25F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_SPEAR = nativeItem("wooden_spear");
    public static final RegistryObject<Item> STONE_SPEAR = nativeItem("stone_spear");
    public static final RegistryObject<Item> COPPER_SPEAR = nativeItem("copper_spear");
    public static final RegistryObject<Item> IRON_SPEAR = nativeItem("iron_spear");
    public static final RegistryObject<Item> GOLDEN_SPEAR = nativeItem("golden_spear");
    public static final RegistryObject<Item> DIAMOND_SPEAR = nativeItem("diamond_spear");
    public static final RegistryObject<Item> NETHERITE_SPEAR = nativeItem("netherite_spear");

    /** References the single VanillaBackport registration without registering an AoE copy. */
    private static RegistryObject<Item> nativeItem(String path) {
        return RegistryObject.create(new ResourceLocation("minecraft", path), ForgeRegistries.ITEMS);
    }

    private static RegistryObject<Item> materialShield(
            String name,
            String texture,
            int durability,
            java.util.function.Supplier<net.minecraft.world.item.crafting.Ingredient> repairIngredient,
            boolean fireResistant,
            float modelScale
    ) {
        Item.Properties properties = new Item.Properties().durability(durability);
        if (fireResistant) {
            properties.fireResistant();
        }
        return ITEMS.register(
                name,
                () -> new MaterialShieldItem(
                        properties,
                        AOEMod.makeID("textures/entity/" + texture + ".png"),
                        repairIngredient,
                        modelScale
                )
        );
    }
}
