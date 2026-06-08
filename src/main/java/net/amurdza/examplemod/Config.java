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
import net.minecraft.tags.BiomeTags;
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
    public static final double BAND_WIDTH = 640;

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
    public static final int MAX_MUSHROOMS_FOR_GROWTH = 5;
    public static final double PLACE_CHORUS_FLOWER_CHANCE = 0.1;
    public static final HashMap<ResourceKey<Biome>,Integer>DIFF_MAP =new HashMap<>();
    public static final float PLAYER_DEFENSE_FACTOR = 1.05F;
    public static final float MONSTER_DEFENSE_FACTOR = 1.1F;
    public static final float PLAYER_ATTACK_FACTOR = 1.05F;
    public static final float MONSTER_ATTACK_FACTOR = 1.05F;
    public static final int FARMLAND_DISTANCE = 14;
    public static final double GLOW_LICHEN_TRUNK_CHANCE = 0.1;
    public static final double VINE_TRUNK_CHANCE = 0.5;
    public static final float PRICKLY_PEAR_CHANCE = 1.0F;
    public static final int MYCELIUM_SPREAD_NUM_TRIES = 4;
    public static final int GRASS_SPREAD_NUM_TRIES = 4;
    public static List<Item> BLACKLISTED_USE_ITEMS=List.of();
    public static final ForgeConfigSpec SPEC;


    /**
     * Each biome tag has two multipliers:
     * - plant: general plants (grass, flowers, crops, etc.)
     * - mushroom: anything we treat as "mushroom category" (e.g., MYCELIUM blocks and MushroomBlock instances)
     */
    private static final Map<TagKey<Biome>, BiomeBonemealMultipliers> DEFAULTS = new HashMap<>();

    public static Map<TagKey<Biome>, Float> FEATHER_RATES = new HashMap<>();



    public static final Map<TagKey<Biome>, BiomeBonemealMultipliers> BIOME_TAG_TO_BONEMEAL_MULTIPLIERS = new LinkedHashMap<>();

    // Backing config values (so Forge can serialize them)
    private static final Map<TagKey<Biome>, ForgeConfigSpec.DoubleValue> PLANT_MULT_VALUES = new LinkedHashMap<>();
    private static final Map<TagKey<Biome>, ForgeConfigSpec.DoubleValue> MUSHROOM_MULT_VALUES = new LinkedHashMap<>();

    static {
        FEATHER_RATES.put(ModTags.Biomes.tropicalBiomes, 2f);
        FEATHER_RATES.put(ModTags.Biomes.savannaBiomes, 4f);

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

        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.deepDarkBiomes,
                0.5,  // plant
                1   // mushroom
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

    public static class BlockInfoObject{
        public int i;
        public double d;
        public BlockInfoObject(int i, double d){
            this.i=i;
            this.d=d;
        }
    }
}
