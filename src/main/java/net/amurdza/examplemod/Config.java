package net.amurdza.examplemod;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntitySiren;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;

import java.util.*;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    public static final Function<RandomSource, Integer> shearAmount=randomSource->2*(1+randomSource.nextInt(3));
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
//    public static final int CHANCE_OF_TALL_SEAGRASS_BONEMEAL = 4;
    public static final int MAX_MUSHROOMS_FOR_GROWTH = 5;
//    public static final int MYCELIUM_GRASS_SPREAD_NUM_TRIES = 8;
    public static final int GLOW_LICHEN_TRIES = 4;
    public static final double PLACE_CHORUS_FLOWER_CHANCE = 0.1;
    public static final double CHORUS_FLOWER_GROW_CHANCE = 0.16;
    public static final int MULBERRY_HARVEST_AMOUNT_IN_SPECIAL_BIOME = 4;
    public static final int NUM_GRAPES = 3;
    public static final double PUPA_CHANCE = 0.2;
    public static final int ANT_FOOD_COUNT = 4;
    public static final int LEAVES_SHED_CHANCE = 800;
    public static final HashMap<ResourceKey<Biome>,Integer>DIFF_MAP =new HashMap<>();
    public static final float PLAYER_DEFENSE_FACTOR = 1.05F;
    public static final float MONSTER_DEFENSE_FACTOR = 1.1F;
    public static final float PLAYER_ATTACK_FACTOR = 1.05F;
    public static final float MONSTER_ATTACK_FACTOR = 1.05F;
    public static final int FARMLAND_DISTANCE = 14;
//    public static final float PROPEL_GROWTH_CHANCE = 0.5F;
    public static final float PITAYA_GROWTH_CHANCE = 0.5F;
    public static final double GLOW_LICHEN_TRUNK_CHANCE = 0.1;
    public static final double VINE_TRUNK_CHANCE = 0.5;
    public static final double FRUIT_LEAVES_CHANCE = 0.5;
    public static final float PRICKLY_PEAR_CHANCE = 1F;
    public static List<Item> BLACKLISTED_USE_ITEMS=List.of();
//    public static final int BLOOD_MOON_FREQUENCY = 7;
//    public static final float BLOOD_MOON_SPAWN_CAP_MULTIPLIER = 3.0f;
//    public static int MAX_BEANSTALK_Y=320;


    public static final ForgeConfigSpec SPEC;

    // Existing stuff you already have:
    // public static final Set<Item> BLACKLISTED_USE_ITEMS = ...
    // etc.

    /**
     * Each biome tag has two multipliers:
     * - plant: general plants (grass, flowers, crops, etc.)
     * - mushroom: anything we treat as "mushroom category" (e.g., MYCELIUM blocks and MushroomBlock instances)
     */
    private static final Map<TagKey<Biome>, BiomeBonemealMultipliers> DEFAULTS = new HashMap<>();

    public static final Map<TagKey<Biome>, BiomeBonemealMultipliers> BIOME_TAG_TO_BONEMEAL_MULTIPLIERS = new LinkedHashMap<>();

    // Backing config values (so Forge can serialize them)
    private static final Map<TagKey<Biome>, ForgeConfigSpec.DoubleValue> PLANT_MULT_VALUES = new LinkedHashMap<>();
    private static final Map<TagKey<Biome>, ForgeConfigSpec.DoubleValue> MUSHROOM_MULT_VALUES = new LinkedHashMap<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("bonemeal");
        builder.comment("Bonemeal multipliers by biome tag. Each tag has separate plant and mushroom multipliers.");

        // Tropical / Jungle: *4
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.tropicalBiomes,
                4.0,  // plant
                4.0   // mushroom
        );

        // Savanna: *2
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.savannaBiomes,
                2.0,  // plant
                2.0   // mushroom
        );

        // Mountains: plant *0.5, but "mushroom category" (MYCELIUM + MushroomBlock) *1.5
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.mountainBiomes,
                0.5,  // plant
                1.5   // mushroom
        );

        // Desert: plant *0.5, but mushroom category *1
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.desertBiomes,
                0.5,  // plant
                1.0   // mushroom
        );

        // Nether: plant *0.5, but mushroom category *1
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.netherBiomes,
                0.5,  // plant
                1.0   // mushroom
        );

        builder.pop();

        SPEC = builder.build();
    }

    private static void defineBiomeTagMultipliers(
            ForgeConfigSpec.Builder builder,
            TagKey<Biome> tag,
            double defaultPlant,
            double defaultMushroom
    ) {
        String baseKey = tag.location().getPath(); // e.g. "tropical_biomes"
        builder.push(baseKey);

        ForgeConfigSpec.DoubleValue plant = builder
                .comment("Multiplier for plants in biomes with tag '" + tag.location() + "'.")
                .defineInRange("plant", defaultPlant, 0.0, 64.0);

        ForgeConfigSpec.DoubleValue mushroom = builder
                .comment("Multiplier for mushroom-category blocks (MYCELIUM + MushroomBlock) in biomes with tag '" + tag.location() + "'.")
                .defineInRange("mushroom", defaultMushroom, 0.0, 64.0);

        builder.pop();

        PLANT_MULT_VALUES.put(tag, plant);
        MUSHROOM_MULT_VALUES.put(tag, mushroom);
        DEFAULTS.put(tag, new BiomeBonemealMultipliers((float) defaultPlant, (float) defaultMushroom));
    }

    /**
     * Call this after config reload if you support live reload.
     * (E.g., from ModConfigEvent.Reloading)
     */
    public static void rebuildBiomeMultiplierCache() {
        BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.clear();

        for (TagKey<Biome> tag : PLANT_MULT_VALUES.keySet()) {
            BiomeBonemealMultipliers def = DEFAULTS.get(tag);

            float plantFallback = (def != null) ? def.plant() : 1.0f;
            float mushroomFallback = (def != null) ? def.mushroom() : 1.0f;

            double plant = safeGet(PLANT_MULT_VALUES.get(tag), plantFallback);
            double mushroom = safeGet(MUSHROOM_MULT_VALUES.get(tag), mushroomFallback);

            BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.put(
                    tag,
                    new BiomeBonemealMultipliers((float) plant, (float) mushroom)
            );
        }
    }
    private static float safeGet(ForgeConfigSpec.DoubleValue v, float fallback) {
        try {
            return (float)(double)v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public record BiomeBonemealMultipliers(float plant, float mushroom) {}






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
//    private static void addToMap(Block block, int i){
//        addToMap(block,i,1D);
//    }
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

    private static float getOrDefault(ForgeConfigSpec.DoubleValue v, float fallback) {
        try {
            return v == null ? fallback : (float)(double)v.get();
        } catch (IllegalStateException e) {
            // config not loaded yet
            return fallback;
        }
    }

    @SubscribeEvent
    static void onLoad(final FMLCommonSetupEvent event)
    {
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


        List<Block> saplings=List.of(samebutdifferent.ecologics.registry.ModBlocks.COCONUT_SEEDLING.get(), Blocks.SPRUCE_SAPLING,Blocks.ACACIA_SAPLING,Blocks.CHERRY_SAPLING,Blocks.BIRCH_SAPLING,Blocks.DARK_OAK_SAPLING,Blocks.JUNGLE_SAPLING,Blocks.OAK_SAPLING,Blocks.MANGROVE_PROPAGULE, Objects.requireNonNull(BWGWood.BAOBAB.sapling()).getBlock());
        for(Block block: saplings){
            addToMap(block,0.45D);
        }

    }
}
