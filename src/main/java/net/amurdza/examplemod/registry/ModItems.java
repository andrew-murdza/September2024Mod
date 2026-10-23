package net.amurdza.examplemod.registry;

import com.github.alexthe666.alexsmobs.item.ItemModFishBucket;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.CopperArmorMaterial;
import net.amurdza.examplemod.item.BoneBucketItem;
import net.amurdza.examplemod.item.CropSeedItem;
import net.amurdza.examplemod.item.MaceItem;
import net.amurdza.examplemod.item.MaterialShieldItem;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.item.SpearItem;
import net.amurdza.examplemod.item.StackableBowlFoodItem;
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

    public static final RegistryObject<Item> BONE_BUCKET = ITEMS.register("bone_bucket",
            () -> new BoneBucketItem(Fluids.EMPTY, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BONE_WATER_BUCKET = ITEMS.register("bone_water_bucket",
            () -> new BoneBucketItem(Fluids.WATER, new Item.Properties().stacksTo(1).craftRemainder(BONE_BUCKET.get())));
    public static final RegistryObject<Item> BONE_LAVA_BUCKET = ITEMS.register("bone_lava_bucket",
            () -> new BoneBucketItem(Fluids.LAVA, new Item.Properties().stacksTo(1).craftRemainder(BONE_BUCKET.get())));
    public static final RegistryObject<Item> BOSS_AXE = ITEMS.register("boss_axe",
            () -> new AxeItem(Tiers.IRON, 6.0F, -3.1F, new Item.Properties()));

    private static final FoodProperties RAW_FISH_FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.3F)
            .build();

    private static final FoodProperties COOKED_FISH_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationMod(0.8F)
            .build();

    private static final FoodProperties RAW_COD_PLUS_ONE = rawCodPlusOne();

    private static final FoodProperties FARMERS_DELIGHT_COOKIE = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .fast()
            .build();

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static FoodProperties rawCodPlusOne() {
        int nutrition = Foods.COD.getNutrition() + 1;
        float saturation = Foods.COD.getNutrition() * Foods.COD.getSaturationModifier() * 2.0F + 1.0F;
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation / (nutrition * 2.0F))
                .build();
    }

    public static Item endFishForVariant(int variant, boolean cooked) {
        if (variant >= 5 && variant <= 7) {
            return DimensionFoods.get(cooked ? "cooked_sulfur_end_fish" : "sulfur_end_fish");
        }
        return cooked ? COOKED_END_FISH.get() : END_FISH.get();
    }

    public static Item netherFishForVariantIndex(int variantIndex, boolean cooked) {
        return NETHER_FISH.get();
    }

    public static final RegistryObject<Item> BUCKET_END_FISH = ITEMS.register(
            "bucket_end_fish",
            () -> new ItemModFishBucket(ModEntities.END_FISH, Fluids.WATER, new Item.Properties())
    );

    public static final RegistryObject<Item> BUCKET_CUBOZOA = ITEMS.register(
            "bucket_cubozoa",
            () -> new ItemModFishBucket(ModEntities.CUBOZOA, Fluids.LAVA, new Item.Properties())
    );

    public static final RegistryObject<Item> BUCKET_NETHER_FISH = ITEMS.register(
            "bucket_nether_fish",
            () -> new ItemModFishBucket(ModEntities.NETHER_FISH, Fluids.LAVA, new Item.Properties())
    );

    public static final RegistryObject<Item> COOKED_PUFFERFISH = ITEMS.register(
            "cooked_pufferfish",
            () -> new Item(new Item.Properties().food(Foods.COOKED_SALMON))
    );

    public static final RegistryObject<Item> COOKED_TROPICAL_FISH = ITEMS.register(
            "cooked_tropical_fish",
            () -> new Item(new Item.Properties().food(Foods.COOKED_COD))
    );

    public static final RegistryObject<Item> END_FISH = ITEMS.register(
            "end_fish",
            () -> new Item(new Item.Properties().food(RAW_FISH_FOOD))
    );
    public static final RegistryObject<Item> COOKED_END_FISH = ITEMS.register(
            "cooked_end_fish",
            () -> new Item(new Item.Properties().food(COOKED_FISH_FOOD))
    );
    public static final RegistryObject<Item> NETHER_FISH = ITEMS.register(
            "nether_fish",
            () -> new Item(new Item.Properties().food(Foods.COD))
    );

    public static final RegistryObject<Item> RAW_SWORDFISH = ITEMS.register("raw_swordfish",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3).saturationMod(0.2F).build())));

    public static final RegistryObject<Item> COOKED_SWORDFISH = ITEMS.register("cooked_swordfish",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(7).saturationMod(0.8F).build())));

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

    public static final RegistryObject<Item> ONION_SEEDS = ITEMS.register(
            "onion_seeds",
            () -> new CropSeedItem(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> RICE_SEEDS = ITEMS.register(
            "rice_seeds",
            () -> new CropSeedItem(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), new Item.Properties())
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

    public static final RegistryObject<Item> SOUL_BERRY_COOKIE = ITEMS.register(
            "soul_berry_cookie",
            () -> new Item(new Item.Properties().food(Foods.COOKIE))
    );

    public static final RegistryObject<Item> BLUEBERRY_COOKIE = ITEMS.register(
            "blueberry_cookie",
            () -> new Item(new Item.Properties().food(FARMERS_DELIGHT_COOKIE))
    );

    public static final RegistryObject<Item> SEARED_COD_ROLL = ITEMS.register(
            "seared_cod_roll",
            () -> new Item(new Item.Properties().food(RAW_COD_PLUS_ONE))
    );
    public static final RegistryObject<Item> NETHER_FISH_ROLL = ITEMS.register(
            "nether_fish_roll",
            () -> new Item(new Item.Properties().food(RAW_COD_PLUS_ONE))
    );

    public static final RegistryObject<Item> GLOWSHROOM_STEW = ITEMS.register(
            "glowshroom_stew",
            () -> new BowlFoodItem(new Item.Properties().stacksTo(1).food(Foods.MUSHROOM_STEW))
    );

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

    public static final RegistryObject<Item> COPPER_SWORD = ITEMS.register(
            "copper_sword", () -> new SwordItem(ModToolTiers.COPPER, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_SHOVEL = ITEMS.register(
            "copper_shovel", () -> new ShovelItem(ModToolTiers.COPPER, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_PICKAXE = ITEMS.register(
            "copper_pickaxe", () -> new PickaxeItem(ModToolTiers.COPPER, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_AXE = ITEMS.register(
            "copper_axe", () -> new AxeItem(ModToolTiers.COPPER, 7.0F, -3.2F, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_HOE = ITEMS.register(
            "copper_hoe", () -> new HoeItem(ModToolTiers.COPPER, -1, -2.0F, new Item.Properties()));

    public static final RegistryObject<Item> COPPER_HELMET = ITEMS.register(
            "copper_helmet", () -> new ArmorItem(CopperArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_CHESTPLATE = ITEMS.register(
            "copper_chestplate", () -> new ArmorItem(CopperArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_LEGGINGS = ITEMS.register(
            "copper_leggings", () -> new ArmorItem(CopperArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_BOOTS = ITEMS.register(
            "copper_boots", () -> new ArmorItem(CopperArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> BONE_SPEAR = ITEMS.register("bone_spear",
            () -> new SpearItem(ModToolTiers.BONE, 0.85F, 0.82F, 0.65F, 12.0F, 8.25F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_SPEAR = ITEMS.register("wooden_spear",
            () -> new SpearItem(Tiers.WOOD, 0.65F, 0.70F, 0.75F, 14.0F, 10.0F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> STONE_SPEAR = ITEMS.register("stone_spear",
            () -> new SpearItem(Tiers.STONE, 0.75F, 0.82F, 0.70F, 13.0F, 9.0F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> COPPER_SPEAR = ITEMS.register("copper_spear",
            () -> new SpearItem(ModToolTiers.COPPER, 0.85F, 0.82F, 0.65F, 12.0F, 8.25F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> IRON_SPEAR = ITEMS.register("iron_spear",
            () -> new SpearItem(Tiers.IRON, 0.95F, 0.95F, 0.60F, 11.0F, 6.75F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_SPEAR = ITEMS.register("golden_spear",
            () -> new SpearItem(Tiers.GOLD, 0.95F, 0.70F, 0.70F, 13.0F, 8.50F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_SPEAR = ITEMS.register("diamond_spear",
            () -> new SpearItem(Tiers.DIAMOND, 1.05F, 1.075F, 0.50F, 10.0F, 6.50F, 4.6F, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_SPEAR = ITEMS.register("netherite_spear",
            () -> new SpearItem(Tiers.IRON, 0.95F, 0.95F, 0.60F, 11.0F, 6.75F, 4.6F,
                    new Item.Properties().fireResistant()));

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
