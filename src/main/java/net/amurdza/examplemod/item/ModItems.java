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

public class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS, AOEMod.MOD_ID);
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
    public static final RegistryObject<Item> GLOW_BERRIES= ITEMS.register("glow_berries",
            ()->new ItemNameBlockItem(ModBlocks.CAVE_VINES.get(), (new Item.Properties()).food(Foods.GLOW_BERRIES)));
    public static final RegistryObject<Item> GRAPES= ITEMS.register("grapes",
            ()->new ItemNameBlockItem(ModBlocks.GRAPE_VINE.get(), (new Item.Properties()).food(Foods.SWEET_BERRIES)));
    public static final RegistryObject<Item> WARPED_CARROT= ITEMS.register("warped_carrot",
            ()->new ItemNameBlockItem(ModBlocks.WARPED_CARROTS.get(), (new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> CRIMSON_POTATO= ITEMS.register("crimson_potato",
            ()->new ItemNameBlockItem(ModBlocks.CRIMSON_POTATOES.get(), (new Item.Properties()).food(Foods.POTATO)));
    public static final RegistryObject<Item> BAKED_CRIMSON_POTATO= ITEMS.register("baked_crimson_potato",
            ()->new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> SOUL_BEET= ITEMS.register("soul_beet",
            ()->new Item((new Item.Properties()).food(Foods.BEETROOT)));
    public static final RegistryObject<Item> SOUL_BEET_SEEDS= ITEMS.register("soul_beet_seeds",
            ()->new ItemNameBlockItem(ModBlocks.SOUL_BEETS.get(), (new Item.Properties())));
    public static final RegistryObject<Item> SOUL_BEET_SOUP= ITEMS.register("soul_beet_soup",
            ()->new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> CRYO_ROOT_SOUP= ITEMS.register("cryo_root_soup",
            ()->new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> ASHEN_BREAD = ITEMS.register("ashen_bread",
            () -> new Item(new Item.Properties().food(Foods.BREAD)));

    public static final RegistryObject<Item> DEVILS_HOLE_PUPFUSH = ITEMS.register("devils_hole_pupfush",
            () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> COOKED_DEVILS_HOLE_PUPFUSH = ITEMS.register("cooked_devils_hole_pupfush",
            () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).build())));

    public static final RegistryObject<Item> TRIOPS = ITEMS.register("triops",
            () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> COOKED_TRIOPS = ITEMS.register("cooked_triops",
            () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).build())));


    public final static RegistryObject<Item> BUCKET_END_FISH = ITEMS.register("bucket_end_fish",()->new ItemModFishBucket(ModEntities.END_FISH, Fluids.WATER, new Item.Properties()));
    public final static RegistryObject<Item> BUCKET_CUBOZOA = ITEMS.register("bucket_cubozoa",()->new ItemModFishBucket(ModEntities.CUBOZOA, Fluids.WATER, new Item.Properties()));


    public static final RegistryObject<Item> COOKED_DRAGONFISH = ITEMS.register("cooked_dragonfish", () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> COOKED_TUNA = ITEMS.register("cooked_tuna", () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> COOKED_KOI = ITEMS.register("cooked_koi", () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build())));

    public static final RegistryObject<Item> CRAB_LEGS = ITEMS.register("crab_legs", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_CRAB_LEGS = ITEMS.register("cooked_crab_legs", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
     public static final RegistryObject<Item> CUBOZOA = ITEMS.register("cubozoa", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_CUBOZOA = ITEMS.register("cooked_cubozoa", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> CUTTLEFISH = ITEMS.register("cuttlefish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_CUTTLEFISH = ITEMS.register("cooked_cuttlefish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> END_FISH = ITEMS.register("end_fish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_END_FISH = ITEMS.register("cooked_end_fish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> FIREWORK_JELLYFISH = ITEMS.register("firework_jellyfish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_FIREWORK_JELLYFISH = ITEMS.register("cooked_firework_jellyfish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> ISOPOD = ITEMS.register("isopod", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_ISOPOD = ITEMS.register("cooked_isopod", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> NETHER_CRAB_CLAW = ITEMS.register("nether_crab_claw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_NETHER_CRAB_CLAW = ITEMS.register("cooked_nether_crab_claw", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> SHRIMP = ITEMS.register("shrimp", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_SHRIMP = ITEMS.register("cooked_shrimp", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> SQUAT_LOBSTER = ITEMS.register("squat_lobster", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_SQUAT_LOBSTER = ITEMS.register("cooked_squat_lobster", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> VAMPIRE_SQUID = ITEMS.register("vampire_squid", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_VAMPIRE_SQUID = ITEMS.register("cooked_vampire_squid", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> WIZARD_JELLYFISH = ITEMS.register("wizard_jellyfish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> COOKED_WIZARD_JELLYFISH = ITEMS.register("cooked_wizard_jellyfish", () -> new Item(new Item.Properties().food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).build())));

    public static final RegistryObject<Item> ARROW_SQUID = ITEMS.register("arrow_squid",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> WARPED_ARROW_SQUID = ITEMS.register("warped_arrow_squid",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> COOKED_ARROW_SQUID = ITEMS.register("cooked_arrow_squid",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build())));

    public static final RegistryObject<Item> FRILLED_SHARK = ITEMS.register("frilled_shark",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> COOKED_FRILLED_SHARK = ITEMS.register("cooked_frilled_shark",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.7F).build())));

    public static final RegistryObject<Item> HOUND_SHARK = ITEMS.register("hound_shark",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).build())));

    public static final RegistryObject<Item> COOKED_HOUND_SHARK = ITEMS.register("cooked_hound_shark",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.7F).build())));

    public static final RegistryObject<Item> COOKED_LOST_TENTACLE = ITEMS.register("cooked_lost_tentacle",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.7F).build())));

    public static final RegistryObject<Item> BEANS = ITEMS.register("beans",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(1).saturationMod(0.2F).build())));

    public static final RegistryObject<Item> BEAN_SEEDS = ITEMS.register("bean_seeds",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BEAN_SOUP = ITEMS.register("bean_soup",
            () -> new Item((new Item.Properties()).food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).build())));


    public static final  RegistryObject<Item> JUNGLE_SWEET_BERRIES= ITEMS.register("jungle_sweet_berries",()->new ItemNameBlockItem(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get(),
            (new Item.Properties()).food(Foods.SWEET_BERRIES)));
    public static final  RegistryObject<Item> SOUL_BERRIES= ITEMS.register("soul_berries",()->new ItemNameBlockItem(ModBlocks.SOUL_BERRY_BUSH.get(),
            (new Item.Properties()).food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.6F).build())));
    public static final  RegistryObject<Item> ASHEN_WHEAT= ITEMS.register("ashen_wheat",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> ASHEN_WHEAT_SEEDS= ITEMS.register("ashen_wheat_seeds",
            ()->new ItemNameBlockItem(ModBlocks.ASHEN_WHEAT.get(), (new Item.Properties())));

    public static final RegistryObject<Item> BONE_ARROW = ITEMS.register("bone_arrow", ()->new ArrowItem(new Item.Properties()));
    public static final RegistryObject<Item> BONE_SWORD = ITEMS.register("bone_sword", ()->new SwordItem(ModToolTiers.BONE, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_SHOVEL = ITEMS.register("bone_shovel", ()->new ShovelItem(ModToolTiers.BONE, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_PICKAXE = ITEMS.register("bone_pickaxe", ()->new PickaxeItem(ModToolTiers.BONE, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_AXE = ITEMS.register("bone_axe", ()->new AxeItem(ModToolTiers.BONE, 7.0F, -3.2F, new Item.Properties()));
    public static final RegistryObject<Item> BONE_HOE = ITEMS.register("bone_hoe", ()->new HoeItem(ModToolTiers.BONE, -1, -2.0F, new Item.Properties()));
}
