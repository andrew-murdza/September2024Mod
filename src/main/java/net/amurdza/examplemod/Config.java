package net.amurdza.examplemod;

import com.belgieyt.trailsandtalesplus.Objects.TTBlockRegistry;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntitySiren;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.teamabnormals.upgrade_aquatic.core.registry.UABlocks;
import net.amurdza.examplemod.block.ModBlocks;
import net.brnbrd.delightful.common.block.DelightfulBlocks;
import net.mehvahdjukaar.hauntedharvest.reg.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    public static final double LLAMA_HEALTH = 40;
    public static final double HORSE_HEALTH = 40;
    public static final double HORSE_SPEED = 0.4;
    public static final double HORSE_JUMP_STRENGTH = 1.1;
    public static final double DONKEY_HEALTH = 40;
    public static final double DONKEY_SPEED = 0.31;
    public static final double DONKEY_JUMP_STRENGTH = 1.0;
    public static final int GLOW_BERRY_HARVEST_AMOUNT = 2;
    public static final int SWEET_BERRIES_PARTIALLY_GROWN = 2;
    public static final int SWEET_BERRIES_FULLY_GROWN = 5;
    public static final int WOOL_FROM_SHEAR = 3;
    public static final int CHANCE_OF_TALL_SEAGRASS_BONEMEAL = 4;
    public static final int MAX_MUSHROOMS_FOR_GROWTH = 5;
    public static final int MYCELIUM_GRASS_SPREAD_NUM_TRIES = 8;
    public static final int GLOW_LICHEN_TRIES = 4;
    public static final double PLACE_CHORUS_FLOWER_CHANCE = 0.1;
    public static final double CHORUS_FLOWER_GROW_CHANCE = 0.16;
    public static final int MULBERRY_HARVEST_AMOUNT_IN_SPECIAL_BIOME = 4;
    public static final int NUM_GRAPES = 3;
    public static final double PUPA_CHANCE = 0.2;
    public static final int ANT_FOOD_COUNT = 4;
    public static final HashMap<ResourceKey<Biome>,Integer>DIFF_MAP =new HashMap<>();
    public static final float PLAYER_DEFENSE_FACTOR = 1.05F;
    public static final float MONSTER_DEFENSE_FACTOR = 1.1F;
    public static final float PLAYER_ATTACK_FACTOR = 1.05F;
    public static final float MONSTER_ATTACK_FACTOR = 1.05F;
    public static List<Item> BLACKLISTED_USE_ITEMS=List.of();
    public static final int BLOOD_MOON_FREQUENCY = 7;
    public static final float BLOOD_MOON_SPAWN_CAP_MULTIPLIER = 3.0f;

    public static List<? extends String> TWIN_MOBS=List.of("minecraft:axolotl","minecraft:bee","minecraft:camel","minecraft:cat","minecraft:chicken","minecraft:cow","minecraft:donkey","minecraft:frog","minecraft:horse","minecraft:mooshroom","minecraft:mule","minecraft:ocelot","minecraft:parrot","minecraft:pig","minecraft:rabbit","minecraft:sheep","minecraft:sniffer","minecraft:tadpole","minecraft:turtle","minecraft:fox","minecraft:goat","minecraft:llama","minecraft:panda","minecraft:polar_bear","minecraft:wolf","minecraft:strider","minecraft:hoglin","minecraft:hoglin","alexsmobs:banana_slug","alexsmobs:blue_jay","alexsmobs:cockroach","alexsmobs:crow","alexsmobs:endergrade","alexsmobs:flutter","alexsmobs:fly","alexsmobs:gazelle","alexsmobs:hummingbird","alexsmobs:jerboa","alexsmobs:laviathan","alexsmobs:maned_wolf","alexsmobs:mimic_octopus","alexsmobs:mudskipper","alexsmobs:mungus","alexsmobs:Potoo","alexsmobs:rain_frog","alexsmobs:road_runner","alexsmobs:seagull","alexsmobs:seal","alexsmobs:sugar_glider","alexsmobs:terrapin","alexsmobs:toucan","alexsmobs:triops","alexsmobs:anteater","alexsmobs:bald_eagle","alexsmobs:bison","alexsmobs:caiman","alexsmobs:capuchin_monkey","alexsmobs:cosmaw","alexsmobs:elephant","alexsmobs:emu","alexsmobs:gelada_monkey","alexsmobs:gorilla","alexsmobs:kangaroo","alexsmobs:mantis_shrimp","alexsmobs:racoon","alexsmobs:snow_leopard","alexsmobs:tarantula_hawk","alexsmobs:tasmanian_devil","alexsmobs:warped_toad","alexsmobs:alligator_snapping_turtle","alexsmobs:grizzly_bear","alexsmobs:platypus","alexsmobs:rattlesnake","alexsmobs:rhinoceros","alexsmobs:skunk","alexsmobs:anaconda","alexsmobs:crocodile","alexsmobs:froststalker","alexsmobs:komodo_dragon","alexsmobs:tiger","alexsmobs:tusklin","iceandfire:hippocampus","iceandfire:hippogryphs","iceandfire:amphithere","samurai_dynasty:twotailed","quark:crab","quark:foxhound","quark:toretoise","ecoologics:coconut_crab");
    public static List<? extends String> BLACKLISTED_MOBS=List.of();
    public static List<? extends String> PARTIALLY_INFERTILE_MOBS=List.of();
    public static List<? extends Double> TWIN_CHANCES= NonNullList.withSize(TWIN_MOBS.size(),1D);
    public static List<? extends Double> INFERTILE_CHANCES=List.of(0.8D);
    public static List<? extends String> SLOWER_GROWTH_MOBS=List.of();
    public static List<? extends String> FASTER_GROWTH_MOBS=TWIN_MOBS;

    //applies for recovering from breeding, babies growing, eggs, wool growing, etc.
    public static List<? extends Double> SLOWER_GROWTH_CHANCES=NonNullList.withSize(SLOWER_GROWTH_MOBS.size(),0.25D);
    public static List<? extends Integer> FASTER_GROWTH_AMOUNT= NonNullList.withSize(FASTER_GROWTH_MOBS.size(),3);
    public static int MAX_LEAVES_DISTANCE=8;

    public static HashMap<Block,BlockInfoObject>BLOCK_GROWTH_DATA=new HashMap<>();
    public static HashMap<Class<? extends LivingEntity>,List<ShedInfoObject>> SHED_DATA=new HashMap<>();
    private static void addToMap(Block block, int i, double d){
        BLOCK_GROWTH_DATA.put(block,new BlockInfoObject(i,d));
    }
    private static void addToMap(Block block, int i){
        addToMap(block,i,1D);
    }
    private static void addToMap(Block block, double d){
        addToMap(block,1,d);
    }
    private static void addToMap(Block block){
        addToMap(block,1,1D);
    }
    private static void addToMap(Class<? extends LivingEntity> entity, Item item, double d){
        ShedInfoObject info=new ShedInfoObject(item,d);
        if(SHED_DATA.containsKey(entity)){
            SHED_DATA.get(entity).add(info);
        }
        else{
            SHED_DATA.put(entity,new ArrayList<>(List.of(info)));
        }
    }
    private static void addToMap(Class<? extends LivingEntity> entity, double d, Item... items){
        for(Item item: items){
            addToMap(entity,item,d);
        }
    }
    private static void addToMap(Class<? extends LivingEntity> entity, Item... items){
        addToMap(entity,0.1D,items);
    }

    public static class BlockInfoObject{
        public int i;
        public double d;
        public BlockInfoObject(int i, double d){
            this.i=i;
            this.d=d;
        }
    }
    public static class ShedInfoObject{
        public Item item;
        public double d;
        public ShedInfoObject(Item item, double d){
            this.item=item;
            this.d=d;
        }
    }

    @SubscribeEvent
    static void onLoad(final FMLCommonSetupEvent event)
    {
        addToMap(Blocks.BAMBOO_SAPLING);
        addToMap(Blocks.BAMBOO);
        addToMap(Blocks.BEETROOTS, 0.45D);
        addToMap(Blocks.CACTUS, 4);
        addToMap(Blocks.CARROTS);
        addToMap(Blocks.CAVE_VINES);
        addToMap(Blocks.CAVE_VINES_PLANT);
        addToMap(Blocks.COCOA);
        addToMap(Blocks.MELON_STEM);
        addToMap(Blocks.PITCHER_CROP);
        addToMap(Blocks.POTATOES);
        addToMap(Blocks.PUMPKIN_STEM);
        addToMap(Blocks.SUGAR_CANE, 4);
        addToMap(Blocks.SWEET_BERRY_BUSH);
        addToMap(Blocks.VINE);
        addToMap(Blocks.WHEAT);
        addToMap(Blocks.VINE);
        addToMap(UABlocks.MULBERRY_VINE.get());
        addToMap(ModBlocks.BLUE_BERRY_BUSH.get());
        addToMap(ModBlocks.GRAPE_VINE.get());
        addToMap(DelightfulBlocks.CANTALOUPE_PLANT.get(), 0.25D);
        addToMap(net.ribs.vintagedelight.block.ModBlocks.OAT_CROP.get());
        addToMap(net.ribs.vintagedelight.block.ModBlocks.PEANUT_CROP.get());
        addToMap(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get());
        addToMap(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get());
        addToMap(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get());
        addToMap(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get());
        addToMap(TTBlockRegistry.CHERRY_VINE.get());
        addToMap(samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get());
        addToMap(ModRegistry.CORN_BASE.get());
        addToMap(ModRegistry.CORN_MIDDLE.get());
        addToMap(ModRegistry.CORN_TOP.get());

        addToMap(Blocks.POINTED_DRIPSTONE,0.1);
        addToMap(Blocks.RED_MUSHROOM,0.5);
        addToMap(Blocks.BROWN_MUSHROOM,0.5);
        addToMap(Blocks.SMALL_AMETHYST_BUD,0.5);
        addToMap(Blocks.MEDIUM_AMETHYST_BUD,0.5);
        addToMap(Blocks.LARGE_AMETHYST_BUD,0.5);
        addToMap(Blocks.TURTLE_EGG,0.2);
        addToMap(UABlocks.PICKERELWEED.get(), 0.12D);
        addToMap(Blocks.NETHER_WART,0.25D);
        addToMap(Blocks.TWISTING_VINES,0.4D);
        addToMap(Blocks.WEEPING_VINES,0.4D);
        addToMap(Blocks.KELP,0.6D);

        addToMap(EntityEmu.class, AMItemRegistry.EMU_FEATHER.get(),AMItemRegistry.EMU_EGG.get());
        addToMap(Chicken.class, Items.FEATHER, Items.EGG);
        addToMap(EntitySiren.class, 0, IafItemRegistry.SIREN_TEAR.get(),IafItemRegistry.SHINY_SCALES.get());
        addToMap(EntitySeaSerpent.class,0,IafItemRegistry.SERPENT_FANG.get());
        addToMap(EntityPixie.class,0,IafItemRegistry.PIXIE_DUST.get(),IafItemRegistry.PIXIE_WINGS.get());
        addToMap(EntityPixie.class,0,IafItemRegistry.PIXIE_WINGS.get());
        addToMap(EntityAnaconda.class,AMItemRegistry.SHED_SNAKE_SKIN.get());
        addToMap(EntityGrizzlyBear.class,AMItemRegistry.BEAR_FUR.get());
        addToMap(EntityMoose.class,AMItemRegistry.MOOSE_ANTLER.get());
        addToMap(EntityCrocodile.class,AMItemRegistry.CROCODILE_SCUTE.get());
        addToMap(EntityAlligatorSnappingTurtle.class,0,AMItemRegistry.SPIKED_SCUTE.get());


        List<Block> saplings=List.of(samebutdifferent.ecologics.registry.ModBlocks.COCONUT_SEEDLING.get(), Blocks.SPRUCE_SAPLING,Blocks.ACACIA_SAPLING,Blocks.CHERRY_SAPLING,Blocks.BIRCH_SAPLING,Blocks.DARK_OAK_SAPLING,Blocks.JUNGLE_SAPLING,Blocks.OAK_SAPLING,Blocks.MANGROVE_PROPAGULE);
        for(Block block: saplings){
            addToMap(block,0.45D);
        }

    }
}
