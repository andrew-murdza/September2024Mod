package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.morallenplay.dropthemeat.init.ItemInit;
import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import com.starfish_studios.naturalist.core.registry.NaturalistItems;
import dev.hybridlabs.aquatic.entity.HybridAquaticEntityTypes;
import net.amurdza.examplemod.registry.ModEntities;
import org.violetmoon.quark.content.mobs.module.WraithModule;
import samebutdifferent.ecologics.registry.ModBlocks;
import samebutdifferent.ecologics.registry.ModEntityTypes;
import net.mcreator.borninchaosv.init.BornInChaosV1ModEntities;

import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobConfig {
    private MobConfig() {}

    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_AGING_CHANCE_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_BREEDING_CHANCE_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>,Float> BIOME_TO_NUGGETS_FROM_ARMOR = new HashMap<>();

    public static final Map<Item, Map<TagKey<Biome>, Map<EntityType<?>, Float>>> MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<EntityType<?>, MobInfo> MOB_INFO_BY_MOB = new HashMap<>();

    public static void init() {
        ensureAllTags(MOB_AGING_CHANCE_BY_TAG_BY_MOB);
        ensureAllTags(MOB_BREEDING_CHANCE_BY_TAG_BY_MOB);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.tropicalBiomes,0f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.savannaBiomes,0f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.plainsBiomes,1f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.mountainBiomes,7f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.mushroomCaves,7f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.desertBiomes,3f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.deepDarkBiomes,3f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.soulSandValleyBiomes,1f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.warpedForestBiomes,3f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.crimsonForestBiomes,1f);
        BIOME_TO_NUGGETS_FROM_ARMOR.put(ModTags.Biomes.basaltDeltasBiomes,1f);
        registerMobInfo();
    }

    private static void ensureAllTags(Map<TagKey<Biome>, Map<EntityType<?>, Float>> map) {
        ensureTag(map, ModTags.Biomes.tropicalBiomes);
        ensureTag(map, ModTags.Biomes.savannaBiomes);
        ensureTag(map, ModTags.Biomes.plainsBiomes);
        ensureTag(map, ModTags.Biomes.mountainBiomes);
        ensureTag(map, ModTags.Biomes.mushroomCaves);
        ensureTag(map, ModTags.Biomes.desertBiomes);
        ensureTag(map, ModTags.Biomes.deepDarkBiomes);
        ensureTag(map, ModTags.Biomes.soulSandValleyBiomes);
        ensureTag(map, ModTags.Biomes.warpedForestBiomes);
        ensureTag(map, ModTags.Biomes.crimsonForestBiomes);
        ensureTag(map, ModTags.Biomes.basaltDeltasBiomes);
    }

    private static void ensureTag(Map<TagKey<Biome>, Map<EntityType<?>, Float>> map, TagKey<Biome> tag) {
        map.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static void put(
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map,
            TagKey<Biome> tag,
            EntityType<?> mob,
            float value
    ) {
        ensureTag(map, tag);
        map.get(tag).put(mob, value);
    }

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float plains,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas
    ) {
        return m(
                tropical, savanna, plains, mountains, mushroomCaves, desert, deepDark,
                soulSandValley, warpedForest, crimsonForest, basaltDeltas,
                null, null
        );
    }

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float plains,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas,
            Item item
    ) {
        return m(
                tropical, savanna, plains, mountains, mushroomCaves, desert, deepDark,
                soulSandValley, warpedForest, crimsonForest, basaltDeltas,
                item, null
        );
    }

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float plains,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas,
            Item rawItem,
            Item cookedItem
    ) {
        return new MobBiomeMultipliers(
                tropical, savanna, plains, mountains, mushroomCaves, desert, deepDark,
                soulSandValley, warpedForest, crimsonForest, basaltDeltas,
                rawItem, cookedItem
        );
    }

    private static MobBiomeMultipliers m(TagKey<Biome> tag, float value) {
        return m(tag, value, null, null);
    }

    private static MobBiomeMultipliers m(TagKey<Biome> tag, float value, Item item) {
        return m(tag, value, item, null);
    }

    private static MobBiomeMultipliers m(TagKey<Biome> tag, float value, Item rawItem, Item cookedItem) {
        float tropical = 0F;
        float savanna = 0F;
        float plains = 0F;
        float mountains = 0F;
        float mushroomCaves = 0F;
        float desert = 0F;
        float deepDark = 0F;
        float soulSandValley = 0F;
        float warpedForest = 0F;
        float crimsonForest = 0F;
        float basaltDeltas = 0F;

        if (tag == ModTags.Biomes.tropicalBiomes) {
            tropical = value;
        } else if (tag == ModTags.Biomes.savannaBiomes) {
            savanna = value;
        } else if (tag == ModTags.Biomes.plainsBiomes) {
            plains = value;
        } else if (tag == ModTags.Biomes.mountainBiomes) {
            mountains = value;
        } else if (tag == ModTags.Biomes.mushroomCaves) {
            mushroomCaves = value;
        } else if (tag == ModTags.Biomes.desertBiomes) {
            desert = value;
        } else if (tag == ModTags.Biomes.deepDarkBiomes) {
            deepDark = value;
        } else if (tag == ModTags.Biomes.soulSandValleyBiomes) {
            soulSandValley = value;
        } else if (tag == ModTags.Biomes.warpedForestBiomes) {
            warpedForest = value;
        } else if (tag == ModTags.Biomes.crimsonForestBiomes) {
            crimsonForest = value;
        } else if (tag == ModTags.Biomes.basaltDeltasBiomes) {
            basaltDeltas = value;
        }

        return m(
                tropical, savanna, plains, mountains, mushroomCaves, desert, deepDark,
                soulSandValley, warpedForest, crimsonForest, basaltDeltas,
                rawItem, cookedItem
        );
    }

    private static MobBiomeMultipliers mountainAndCaveM(float value) {
        return mountainAndCaveM(value, value, null, null);
    }

    private static MobBiomeMultipliers mountainAndCaveM(float value, Item item) {
        return mountainAndCaveM(value, value, item, null);
    }

    private static MobBiomeMultipliers mountainAndCaveM(float value, Item rawItem, Item cookedItem) {
        return mountainAndCaveM(value, value, rawItem, cookedItem);
    }

    private static MobBiomeMultipliers mountainAndCaveM(float mountains, float mushroomCaves, Item item) {
        return mountainAndCaveM(mountains, mushroomCaves, item, null);
    }

    private static MobBiomeMultipliers mountainAndCaveM(float mountains, float mushroomCaves, Item rawItem, Item cookedItem) {
        return m(0, 0, 0, mountains, mushroomCaves, 0, 0, 0, 0, 0, 0, rawItem, cookedItem);
    }

    private static MobBiomeMultipliers desertM(
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas
    ) {
        return desertM(desert, deepDark, soulSandValley, warpedForest, crimsonForest, basaltDeltas, null, null);
    }

    private static MobBiomeMultipliers desertM(
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas,
            Item item
    ) {
        return desertM(desert, deepDark, soulSandValley, warpedForest, crimsonForest, basaltDeltas, item, null);
    }

    private static MobBiomeMultipliers desertM(
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas,
            Item rawItem,
            Item cookedItem
    ) {
        return m(0, 0, 0, 0, 0, desert, deepDark, soulSandValley, warpedForest, crimsonForest, basaltDeltas, rawItem, cookedItem);
    }

    private static void registerMobInfo() {
        // -------------------------------------------------------------------------
        // Mob info
        // -------------------------------------------------------------------------

        addMobInfo(EntityType.COW,
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.plainsBiomes, 2, Items.BEEF, Items.COOKED_BEEF),
                m(ModTags.Biomes.savannaBiomes, 6, Items.BEEF, Items.COOKED_BEEF),
                m(ModTags.Biomes.plainsBiomes, 1, Items.LEATHER),
                m(ModTags.Biomes.savannaBiomes, 3, Items.LEATHER));


        addMobInfo(EntityType.MOOSHROOM,
                m(ModTags.Biomes.mountainBiomes, 1),
                m(ModTags.Biomes.mountainBiomes, 1),
                m(ModTags.Biomes.mountainBiomes, 2, Items.BEEF, Items.COOKED_BEEF),
                m(ModTags.Biomes.mountainBiomes, 1, Items.LEATHER));



        addMobInfo(EntityType.PIG,
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.plainsBiomes, 2, Items.PORKCHOP, Items.COOKED_PORKCHOP),
                m(ModTags.Biomes.savannaBiomes, 5, Items.PORKCHOP, Items.COOKED_PORKCHOP),
                m(ModTags.Biomes.savannaBiomes, 1.75f, Items.BONE));

        addMobInfo(EntityType.SHEEP,
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.plainsBiomes, 1, Items.MUTTON, Items.COOKED_MUTTON),
                m(ModTags.Biomes.plainsBiomes, 3, Items.MUTTON, Items.COOKED_MUTTON),
                m(ModTags.Biomes.plainsBiomes, 1, Items.WHITE_WOOL),
                m(ModTags.Biomes.savannaBiomes, 3, Items.WHITE_WOOL),
                m(ModTags.Biomes.plainsBiomes, 0.5f, Items.BONE),
                m(ModTags.Biomes.savannaBiomes, 1.25f, Items.BONE));

        addMobInfo(EntityType.CHICKEN,
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.rainforestBiomes, 2),
                m(ModTags.Biomes.rainforestBiomes, 2),
                m(ModTags.Biomes.plainsBiomes, 1, Items.CHICKEN, Items.COOKED_CHICKEN),
                m(ModTags.Biomes.plainsBiomes, 2.5f, Items.CHICKEN, Items.COOKED_CHICKEN),
                m(ModTags.Biomes.plainsBiomes, 1, Items.FEATHER),
                m(ModTags.Biomes.savannaBiomes, 3, Items.FEATHER),
                m(ModTags.Biomes.rainforestBiomes, 2.5f, Items.FEATHER),
                m(ModTags.Biomes.savannaBiomes, 1.5f, Items.BONE),
                m(ModTags.Biomes.rainforestBiomes, 0.6f, Items.BONE));

        addMobInfo(EntityType.GOAT,
                mountainAndCaveM(0.5F),
                mountainAndCaveM(0.5F),
                mountainAndCaveM(1, 0.5F, Items.MUTTON, Items.MUTTON),
                mountainAndCaveM(1.5F, Items.WHITE_WOOL),
                mountainAndCaveM(0.5F, Items.BONE));

        addMobInfo(EntityType.RABBIT,
                m(3, 3, 1, 0.8F, 0.8F, 0.4F, 0, 0, 0, 0, 0),
                m(2.5F, 3, 1, 1, 1, 1, 0, 0, 0, 0, 0),
                m(1.25F, 1.25F, 0.5F, 0.4F, 0.4F, 0.2F, 0, 0, 0, 0, 0, Items.RABBIT, Items.COOKED_RABBIT),
                m(1.25F, 1.25F, 0.5F, 0.4F, 0.4F, 0.3F, 0, 0, 0, 0, 0, Items.RABBIT_HIDE),
                m(0.25F, 0.25F, 0.1F, 0.08F, 0.08F, 0.15F, 0, 0, 0, 0, 0, Items.RABBIT_FOOT),
                m(0.25F, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.HOGLIN,
                desertM(0, 0, 0, 0, 0.20F, 0.1F),
                desertM(0, 0, 0, 0, 1, 1),
                desertM(0, 0, 0, 0, 3, 1, Items.PORKCHOP, Items.COOKED_PORKCHOP),
                desertM(0, 0, 0, 0, 1, 0.75F, Items.LEATHER));

        addMobInfo(EntityType.STRIDER,
                desertM(0, 0, 0, 0.10F, 0.15F, 0.20F),
                desertM(0, 0, 0, 1, 1, 1),
                desertM(0, 0, 0, 0.35F, 0.5F, 1, Items.MUTTON, Items.COOKED_MUTTON),
                desertM(0, 0, 0, 0.6F, 1.5F, 3, Items.STRING));

        addMobInfo(EntityType.FOX,
                m(1, 3, 0, 0.5F, 0, 0, 0, 0, 0, 0, 0),
                m(1.5f, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.HORSE,
                m(1, 3, 0.8F, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.DONKEY,
                m(ModTags.Biomes.plainsBiomes, 1),
                m(ModTags.Biomes.plainsBiomes, 1));

        addMobInfo(EntityType.MULE,
                m(ModTags.Biomes.plainsBiomes, 1));

        addMobInfo(EntityType.CAMEL,
                m(ModTags.Biomes.desertBiomes, 0.2F),
                m(ModTags.Biomes.desertBiomes, 1));

        addMobInfo(EntityType.BEE,
                m(1.5F, 1.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0),
                m(2F, 1.5F, 1, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.ALLAY);

        addMobInfo(EntityType.LLAMA,
                m(ModTags.Biomes.savannaBiomes, 1.5F),
                m(ModTags.Biomes.savannaBiomes, 2));

        addMobInfo(EntityType.TRADER_LLAMA,
                m(ModTags.Biomes.savannaBiomes, 1.5F),
                m(ModTags.Biomes.savannaBiomes, 2));

        addMobInfo(EntityType.POLAR_BEAR,
                mountainAndCaveM(0.5F),
                mountainAndCaveM(1));

        addMobInfo(EntityType.WOLF,
                m(1, 2, 1, 0.3F, 0.3F, 0, 0, 0, 0, 0, 0),
                m(1, 2, 1, 1, 1, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.CAT,
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.OCELOT,
                m(ModTags.Biomes.tropicalBiomes, 2F),
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(EntityType.PANDA,
                m(ModTags.Biomes.tropicalBiomes, 1),
                m(ModTags.Biomes.tropicalBiomes, 1));

        addMobInfo(EntityType.DOLPHIN);

        addMobInfo(EntityType.TURTLE,
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1));

        addMobInfo(EntityType.BAT);

        addMobInfo(EntityType.FROG,
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1.0F),
                m(ModTags.Biomes.rainforestBiomes, 1.0F, Items.SLIME_BALL));


        addMobInfo(EntityType.AXOLOTL,
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(EntityType.PARROT);

        addMobInfo(EntityType.SNIFFER,
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(EntityType.TADPOLE,
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(EntityType.VILLAGER,
                m(2.5F, 2.5F, 1, 0.75F, 0.75F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F),
                m(2.5F, 2.5F, 1, 0.75F, 0.75F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F));


        addMobInfo(EntityType.WANDERING_TRADER);

        addMobInfo(EntityType.PIGLIN);

        addMobInfo(EntityType.COD,
                m(2F, 1, 1, 2, 2, 0.5F, 0, 0, 0, 0, 0, Items.COD, Items.COOKED_COD),
                m(0.2F, 0.05F, 0, 0.15F, 0.15F, 0.01F, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.SALMON,
                m(2, 1, 0, 2, 2, 0, 0, 0, 0, 0, 0, Items.SALMON, Items.COOKED_SALMON),
                m(0.1F, 0.05F, 0, 0.15F, 0.15F, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));


        addMobInfo(ModEntities.END_FISH.get(),
                m(2, 0, 0, 2, 2, 1, 0, 0, 0, 0, 0, ModItems.END_FISH.get(), ModItems.COOKED_END_FISH.get()),
                m(0.1F, 0.05F, 0, 0.15F, 0.15F, 0.05f, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(ModEntities.CUBOZOA.get(),
                desertM(0.5F, 0.5F, 1F, 1F, 1F, 1F, Items.SLIME_BALL));


        addMobInfo(EntityType.TROPICAL_FISH,
                m(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.TROPICAL_FISH, Items.TROPICAL_FISH),
                m(0.2F, 0.05F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.PUFFERFISH,
                m(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.PUFFERFISH, ModItems.COOKED_PUFFERFISH.get()),
                m(0.15F, 0.05F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.GLOW_SQUID,
                m(0, 0, 0, 2, 2, 0, 0.5F, 0, 0, 0, 0, com.ncpbails.culturaldelights.item.ModItems.GLOW_SQUID.get(), ModItems.COOKED_GLOW_SQUID.get()),
                m(0, 0, 0, 3, 3, 0, 0.7F, 0, 0, 0, 0, Items.GLOW_INK_SAC),
                m(0, 0, 0, 0.2F, 0.2F, 0, 0.05F, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.SQUID,
                m(2, 1, 1, 2, 2, 0, 0, 0, 0, 0, 0, com.ncpbails.culturaldelights.item.ModItems.SQUID.get(), com.ncpbails.culturaldelights.item.ModItems.COOKED_SQUID.get()),
                m(3, 2, 1, 3, 3, 0, 0, 0, 0, 0, 0, Items.INK_SAC),
                m(0, 0, 0.05F, 0.2F, 0.2F, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(AMEntityRegistry.EMU.get(),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3, NaturalistItems.DUCK.get(), NaturalistItems.COOKED_DUCK.get()),
                m(ModTags.Biomes.savannaBiomes, 3, AMItemRegistry.EMU_FEATHER.get()),
                m(ModTags.Biomes.savannaBiomes, 0.5F, Items.BONE));

        addMobInfo(AMEntityRegistry.BISON.get(),
                m(ModTags.Biomes.savannaBiomes, 1.5F),
                m(ModTags.Biomes.savannaBiomes, 1),
                m(ModTags.Biomes.savannaBiomes, 8, ItemInit.RAW_BEAR.get(), ItemInit.COOKED_BEAR.get()),
                m(ModTags.Biomes.savannaBiomes, 7, Items.BROWN_WOOL),
                m(ModTags.Biomes.savannaBiomes, 2, Items.BONE));

        addMobInfo(AMEntityRegistry.GAZELLE.get(),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3, ItemInit.RAW_GOAT.get(), ItemInit.COOKED_GOAT.get()),
                m(ModTags.Biomes.savannaBiomes, 3, Items.LEATHER),
                m(ModTags.Biomes.savannaBiomes, 1, Items.BONE));

        addMobInfo(AMEntityRegistry.MOOSE.get(),
                m(1.5F, 3, 0, 0.5F, 0, 0, 0, 0, 0, 0, 0),
                m(1.25F, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2.5F, 0, 1, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_LLAMA.get(), ItemInit.COOKED_LLAMA.get()),
                m(2, 3, 0, 2, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER),
                m(1.5F, 2, 0, 0.5F, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.WARPED_TOAD.get(),
                desertM(0, 0, 0, 0.4F, 0.2F, 0),
                desertM(0, 0, 1, 1, 1, 0),
                desertM(0, 0, 0, 1, 0.5F, 0, ItemInit.RAW_SQUID.get(), ItemInit.COOKED_SQUID.get()),
                desertM(0, 0, 0, 2.5F, 1.5F, 0, Items.SHROOMLIGHT),
                m(0, 0, 1, 0, 0, 0, 0, 0, 0.3F, 0.2F, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.LOBSTER.get(),
                m(2, 1, 0, 2, 2, 0, 0, 0, 0, 0, 0, AMItemRegistry.LOBSTER_TAIL.get(), AMItemRegistry.COOKED_LOBSTER_TAIL.get()),
                m(0.25F, 0.05F, 0, 0.25F, 0.25F, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(AMEntityRegistry.GORILLA.get(),
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(ModEntityTypes.SQUIRREL.get(),
                m(3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_RED_MEAT.get(), ItemInit.COOKED_RED_MEAT.get()),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.RABBIT_HIDE),
                m(0.5F, 0.75F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(NaturalistEntityTypes.DEER.get(),
                m(2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(2, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, NaturalistItems.VENISON.get(), NaturalistItems.COOKED_VENISON.get()),
                m(1.5F, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER),
                m(1, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(NaturalistEntityTypes.BOAR.get(),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 3),
                m(ModTags.Biomes.savannaBiomes, 5, ItemInit.RAW_TURTLE.get(), ItemInit.COOKED_TURTLE.get()),
                m(ModTags.Biomes.savannaBiomes, 1.75f, Items.BONE));

        addMobInfo(HybridAquaticEntityTypes.INSTANCE.getARROW_SQUID().get(),
                m(2, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, ItemInit.RAW_WOLF.get(), ItemInit.COOKED_WOLF.get()),
                m(2, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, Items.INK_SAC),
                m(0.5F, 0, 0, 0.4F, 0.4F, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.ZOMBIE,
                m(ModTags.Biomes.savannaBiomes,1,Items.ROTTEN_FLESH,Items.LEATHER),
                m(ModTags.Biomes.savannaBiomes,0.25f,Items.BONE));

        addMobInfo(EntityType.ZOMBIE_VILLAGER,
                m(ModTags.Biomes.plainsBiomes, 2, Items.ROTTEN_FLESH, Items.LEATHER),
                m(ModTags.Biomes.plainsBiomes, 0.3F, Items.BONE),
                m(ModTags.Biomes.plainsBiomes, 0.15F, Items.GOLD_INGOT));

        addMobInfo(EntityType.HUSK,
                desertM(2, 1, 0.5f, 0.5f, 0.5f, 0.5f, Items.ROTTEN_FLESH, Items.LEATHER),
                desertM(0.3F, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f, Items.BONE),
                desertM(0.4F, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f, Items.GOLD_INGOT));

        addMobInfo(EntityType.DROWNED,
                mountainAndCaveM(2, Items.ROTTEN_FLESH, Items.LEATHER),
                mountainAndCaveM(1, Items.SEAGRASS),
                mountainAndCaveM(1, Items.KELP),
                mountainAndCaveM(1, Items.BONE));

        addMobInfo(EntityType.ZOMBIFIED_PIGLIN,
                desertM(0.2F, 0.7F, 1.25F, 2, 0.5F, 0, Items.ROTTEN_FLESH),
                desertM(0, 0, 6, 9, 6, 0, Items.GOLD_NUGGET),
                desertM(0.2F, 0.6F, 1, 1.5F, 1, 0, Items.BONE));

        addMobInfo(EntityType.ZOGLIN,
                desertM(0.35F, 0.6F, 1.7F, 3, 0.8F, 0, Items.ROTTEN_FLESH),
                desertM(0.2F, 0.6F, 1, 1.5F, 1, 0, Items.BONE));

        addMobInfo(EntityType.ZOMBIE_HORSE,
                m(ModTags.Biomes.savannaBiomes, 2, Items.ROTTEN_FLESH, Items.LEATHER),
                m(ModTags.Biomes.savannaBiomes, 0.3F, Items.BONE));

        //Up to here
        addMobInfo(EntityType.SKELETON,
                m(ModTags.Biomes.savannaBiomes, 0.5f, Items.ARROW),
                m(ModTags.Biomes.savannaBiomes, 0.2f, Items.BONE),
                m(ModTags.Biomes.savannaBiomes, 0.015f, Items.BOW));

        addMobInfo(EntityType.STRAY,
                mountainAndCaveM(2.5f, 1.5f, Items.BONE));

        addMobInfo(ModEntities.BREEZE.get(),
                mountainAndCaveM(2.5f, 2.5f, Items.BLUE_ICE),
                mountainAndCaveM(2.5f, 2.5f, Items.SNOWBALL));

        addMobInfo(EntityType.WITHER_SKELETON,
                desertM(0.25f, 0.5f, 2f, 1, 1, 2f, Items.COAL),
                desertM(0.25f, 0.5f, 2f, 1, 1, 2f, Items.BONE));

        addMobInfo(EntityType.SKELETON_HORSE,
                m(ModTags.Biomes.savannaBiomes, 2, Items.BONE));

        addMobInfo(EntityType.SPIDER,
                m(ModTags.Biomes.tropicalBiomes, 3, Items.STRING),
                m(ModTags.Biomes.tropicalBiomes, 0.8F, Items.SPIDER_EYE));

        addMobInfo(EntityType.CAVE_SPIDER,
                m(ModTags.Biomes.tropicalBiomes, 4, Items.STRING),
                m(ModTags.Biomes.tropicalBiomes, 1F, Items.SPIDER_EYE),
                m(ModTags.Biomes.tropicalBiomes, 0.1f, Items.EMERALD));

        addMobInfo(ModEntities.BOGGED.get(),
                m(ModTags.Biomes.tropicalBiomes, 2, Items.ARROW),
                m(ModTags.Biomes.tropicalBiomes, 0.1f, Items.BOW),
                m(ModTags.Biomes.tropicalBiomes, 0.1f, Items.EMERALD));

        addMobInfo(ModEntities.SPIDER_QUEEN.get(),
                m(ModTags.Biomes.tropicalBiomes, 16, Items.WHITE_WOOL),
                m(ModTags.Biomes.tropicalBiomes, 10F, Items.SPIDER_EYE),
                m(ModTags.Biomes.tropicalBiomes, 5, Items.BONE),
                m(ModTags.Biomes.tropicalBiomes, 12f, Items.EMERALD));

        addMobInfo(AMEntityRegistry.CENTIPEDE_HEAD.get(),
                m(ModTags.Biomes.tropicalBiomes, 2F, Items.BONE),
                m(ModTags.Biomes.tropicalBiomes, 0.2f, Items.EMERALD));

        addMobInfo(EntityType.SILVERFISH);

        addMobInfo(EntityType.ENDERMITE);

        addMobInfo(ModEntities.NAUTILUS.get(),
                m(ModTags.Biomes.tropicalBiomes, 1.5F),
                m(ModTags.Biomes.tropicalBiomes, 1.5F));

        addMobInfo(ModEntities.PARCHED.get(),
                desertM(3f, 2f, 2.5f, 1.25f, 1.25f, 0.75f, Items.BONE),
                desertM(3f, 2f, 2.5f, 1.25f, 1.25f, 0.75f, Items.SAND),
                desertM(1.5f, 1f, 1.25f, 0.625f, 0.625f, 0.4f, Items.RED_SAND)
        );

        addMobInfo(ModEntities.CAMEL_HUSK.get(),
                desertM(2f, 1.5f, 2f, 1f, 1f, 0.75f, Items.BONE),
                desertM(3f, 2f, 2.5f, 1.25f, 1.25f, 0.75f, Items.ROTTEN_FLESH));

        addMobInfo(ModEntities.CAMEL_HUSK_JOCKEY.get(),
                desertM(2f, 1.5f, 2f, 1f, 1f, 0.75f, Items.BONE),
                desertM(3f, 2f, 2.5f, 1.25f, 1.25f, 0.75f, Items.ROTTEN_FLESH),
                desertM(0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, Items.EMERALD));

        addMobInfo(ModEntities.CREAKING.get(),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, Items.OAK_LOG),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, Items.MANGROVE_LOG),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, Items.CHERRY_LOG),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, Items.DARK_OAK_LOG),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, Items.JUNGLE_LOG),
                m(ModTags.Biomes.tropicalBiomes, 0.05F, ModBlocks.AZALEA_LOG.get().asItem()),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.OAK_SAPLING),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.MANGROVE_PROPAGULE),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.CHERRY_SAPLING),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.DARK_OAK_SAPLING),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.JUNGLE_SAPLING),
                m(ModTags.Biomes.tropicalBiomes, 0.45F, Items.AZALEA));

        addMobInfo(ModEntities.ZOMBIE_HORSEMAN.get(),
                m(ModTags.Biomes.savannaBiomes,3,Items.ROTTEN_FLESH,Items.LEATHER),
                m(ModTags.Biomes.savannaBiomes,2,Items.BONE),
                m(ModTags.Biomes.savannaBiomes,0.1f,Items.EMERALD));

        addMobInfo(ModEntities.SKELETON_HORSEMAN.get(),
                m(ModTags.Biomes.savannaBiomes,0.5f,Items.BOW),
                m(ModTags.Biomes.savannaBiomes,1.5f,Items.ARROW),
                m(ModTags.Biomes.savannaBiomes,3,Items.BONE),
                m(ModTags.Biomes.savannaBiomes,0.25f,Items.EMERALD));

        addMobInfo(ModEntities.ARCHLICH.get(),
                mountainAndCaveM(3,Items.BONE),
                mountainAndCaveM(3,Items.DIAMOND),
                mountainAndCaveM(30,Items.LAPIS_LAZULI),
                mountainAndCaveM(24,Items.EMERALD)
        );

        addMobInfo(ModEntities.ILLAGER_LORD.get(),
                m(ModTags.Biomes.plainsBiomes,12,Items.IRON_INGOT),
                m(ModTags.Biomes.plainsBiomes,20,Items.EMERALD));



        addMobInfo(EntityType.CREEPER,
                desertM(3f, 1.5f, 2f, 5f, 4f, 2.5f, Items.GUNPOWDER));

        addMobInfo(EntityType.SLIME,
                m(ModTags.Biomes.tropicalBiomes, 0.1f, Items.SLIME_BALL));

        addMobInfo(EntityType.MAGMA_CUBE,
                desertM(0, 0, 0.1f, 0.2f, 0.7f, 1.5f, Items.MAGMA_CREAM));

        addMobInfo(EntityType.BLAZE,
                desertM(0, 0, 0.25f, 0.2f, 1.25f, 2.0f, Items.GLOWSTONE_DUST),
                desertM(0, 0, 0.1f, 0.2f, 0.6f, 1.5f, Items.BLAZE_ROD));

        addMobInfo(EntityType.GHAST,
                desertM(0, 0.5f, 2f, 1f, 0.5f, 0.25f, Items.GHAST_TEAR),
                desertM(0, 0.5f, 2.5f, 1.25f, 0.75f, 0.5f, Items.GUNPOWDER));

        addMobInfo(EntityType.PIGLIN_BRUTE);

        addMobInfo(EntityType.ENDERMAN,
                desertM(0.65f, 1.5f, 0.6f, 0.3f, 0.2f, 0.1f, Items.ENDER_PEARL));

        addMobInfo(EntityType.SHULKER,
                desertM(1, 3, 0.1f, 0.1f, 0.1f, 0.1f, Items.SHULKER_SHELL),
                desertM(3, 3, 1.5f, 1, 1, 1, Items.PURPLE_TERRACOTTA));

        addMobInfo(EntityType.ENDER_DRAGON);

        addMobInfo(EntityType.PILLAGER,
                m(ModTags.Biomes.plainsBiomes, 0.08f, Items.CROSSBOW),
                m(ModTags.Biomes.plainsBiomes, 0.5f, Items.ARROW));

        addMobInfo(EntityType.VINDICATOR,
                m(ModTags.Biomes.plainsBiomes, 0.4f, Items.IRON_INGOT),
                m(ModTags.Biomes.plainsBiomes, 0.2f, Items.EMERALD));

        addMobInfo(EntityType.EVOKER,
                m(ModTags.Biomes.plainsBiomes, 1, Items.EMERALD));

        addMobInfo(EntityType.ILLUSIONER,
                m(ModTags.Biomes.plainsBiomes, 24, Items.EMERALD),
                m(ModTags.Biomes.plainsBiomes, 1, Items.BOW),
                m(ModTags.Biomes.plainsBiomes, 10, Items.ARROW));

        addMobInfo(EntityType.RAVAGER,
                m(ModTags.Biomes.plainsBiomes, 1.25f, Items.LEATHER),
                m(ModTags.Biomes.plainsBiomes, 0.5f, Items.SADDLE),
                m(ModTags.Biomes.plainsBiomes, 1f, Items.BONE));

        addMobInfo(EntityType.VEX);

        addMobInfo(EntityType.WITCH,
                m(ModTags.Biomes.plainsBiomes, 0.2f, Items.SUGAR),
                m(ModTags.Biomes.plainsBiomes, 0.2f, Items.RED_MUSHROOM),
                m(ModTags.Biomes.plainsBiomes, 0.2f, Items.BROWN_MUSHROOM),
                m(ModTags.Biomes.plainsBiomes, 1, Items.STICK));

        addMobInfo(EntityType.GUARDIAN,
                mountainAndCaveM(3, Items.PRISMARINE_SHARD),
                mountainAndCaveM(3, Items.PRISMARINE_CRYSTALS));

        addMobInfo(EntityType.ELDER_GUARDIAN,
                mountainAndCaveM(12, Items.PRISMARINE_SHARD),
                mountainAndCaveM(12, Items.PRISMARINE_CRYSTALS));

        addMobInfo(EntityType.PHANTOM,
                desertM(0.5f, 1, 2, 1, 1, 0.5f, Items.PHANTOM_MEMBRANE),
                desertM(0.5f, 0.5f, 1, 0.5f, 0.5f, 0.25f, Items.BONE));

        addMobInfo(EntityType.WARDEN,
                desertM(2, 4, 2, 1, 0.5f, 0.25f, Items.SCULK),
                desertM(0.5f, 1, 0.5f, 0.25f, 0.125f, 0.0625f, Items.SCULK_CATALYST),
                desertM(0.5f, 1, 0.5f, 0.25f, 0.125f, 0.0625f, Items.SCULK_SENSOR));

        addMobInfo(EntityType.WITHER,
                desertM(3, 3, 6, 1.5f, 1, 0.25f, Items.SOUL_SAND),//WRONG
                desertM(1, 1, 2, 0.5f, 0.3f, 0.15f, Items.NETHER_STAR),//WRONG
                desertM(1.5f, 3, 6, 3, 1.5f, 0.75f, Items.WITHER_SKELETON_SKULL),//WRONG
                desertM(32f, 32f, 32f, 32f, 32f, 32f, Items.EMERALD));

        addMobInfo(EntityType.GIANT,
                m(0, 5, 15, 25, 5, 15, 5, 0, 0, 0, 0, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0, 6, 10, 2, 6, 2, 0, 0, 0, 0, Items.BONE));

        addMobInfo(BornInChaosV1ModEntities.SENOR_PUMPKIN.get(),
                m(ModTags.Biomes.savannaBiomes, 4, vectorwing.farmersdelight.common.registry.ModItems.PUMPKIN_SLICE.get()),
                m(ModTags.Biomes.savannaBiomes, 0.3f, Items.BONE));

        addMobInfo(BornInChaosV1ModEntities.SIR_PUMPKINHEAD.get(),
                m(ModTags.Biomes.savannaBiomes, 1, Items.PUMPKIN),
                m(ModTags.Biomes.savannaBiomes, 2, Items.BONE),
                m(ModTags.Biomes.savannaBiomes, 0.4f, Items.EMERALD));

        addMobInfo(BornInChaosV1ModEntities.LORD_PUMPKINHEAD.get(),
                m(ModTags.Biomes.savannaBiomes, 1, Items.PUMPKIN),
                m(ModTags.Biomes.savannaBiomes, 5, Items.BONE),
                m(ModTags.Biomes.savannaBiomes, 16, Items.EMERALD));

        addMobInfo(WraithModule.wraithType,
                desertM(0.5f, 1f, 2, 1.5f, 1f, 0.5f, Items.GHAST_TEAR));

        addMobInfo(IafEntityRegistry.TROLL.get(),
                mountainAndCaveM(2, Items.IRON_INGOT),
                mountainAndCaveM(1, Items.SPRUCE_LOG),
                mountainAndCaveM(2, Items.BONE),
                mountainAndCaveM(0.2f, Items.EMERALD));

        addMobInfo(IafEntityRegistry.DREAD_KNIGHT.get(),
                mountainAndCaveM(3.5f, Items.IRON_INGOT),
                mountainAndCaveM(1.5f, Items.BONE),
                mountainAndCaveM(0.2f, Items.EMERALD));

        addMobInfo(IafEntityRegistry.DREAD_LICH.get(),
                mountainAndCaveM(0.5f, Items.DIAMOND),
                mountainAndCaveM(1f, Items.BONE),
                mountainAndCaveM(0.5f, Items.EMERALD));
    }

    public static float mobGrowthChance(Entity entity) {
        MobInfo info = MOB_INFO_BY_MOB.get(entity.getType());

        if (info == null || info.aging() == null) {
            return -1F;
        }

        return valueForBiome(entity, info.aging());
    }

    public static float mobTwinsChance(Entity entity) {
        MobInfo info = MOB_INFO_BY_MOB.get(entity.getType());

        if (info == null || info.breeding() == null) {
            return -1F;
        }

        return valueForBiome(entity, info.breeding());
    }

    public static float mobAmountForItem(Entity entity, Item item) {
        MobInfo info = MOB_INFO_BY_MOB.get(entity.getType());

        if (info == null) {
            return -1F;
        }

        for (MobBiomeMultipliers entry : info.drops()) {
            if (entry.hasItem(item)) {
                return valueForBiome(entity, entry);
            }
        }

        return -1F;
    }

    public static List<MobBiomeMultipliers> mobDropEntries(Entity entity) {
        MobInfo info = MOB_INFO_BY_MOB.get(entity.getType());

        if (info == null) {
            return List.of();
        }

        return info.drops().stream()
                .filter(MobBiomeMultipliers::hasItem)
                .toList();
    }

    public static float mobAmount(Entity entity, MobBiomeMultipliers entry) {
        return valueForBiome(entity, entry);
    }

    private static float valueForBiome(Entity entity, MobBiomeMultipliers entry) {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return entry.tropical();
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)) {
            return entry.savanna();
        }

        if (biome.is(ModTags.Biomes.plainsBiomes)) {
            return entry.plains();
        }

        if (biome.is(ModTags.Biomes.mountainBiomes)) {
            return entry.mountains();
        }

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return entry.mushroomCaves();
        }

        if (biome.is(ModTags.Biomes.desertBiomes)) {
            return entry.desert();
        }

        if (biome.is(ModTags.Biomes.deepDarkBiomes)) {
            return entry.deepDark();
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return entry.soulSandValley();
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return entry.warpedForest();
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return entry.crimsonForest();
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return entry.basaltDeltas();
        }

        return 0.0F;
    }

    private static void addToMap(
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map,
            EntityType<?> mob,
            MobBiomeMultipliers multipliers
    ) {
        put(map, ModTags.Biomes.tropicalBiomes, mob, multipliers.tropical());
        put(map, ModTags.Biomes.savannaBiomes, mob, multipliers.savanna());
        put(map, ModTags.Biomes.plainsBiomes, mob, multipliers.plains());
        put(map, ModTags.Biomes.mountainBiomes, mob, multipliers.mountains());
        put(map, ModTags.Biomes.mushroomCaves, mob, multipliers.mushroomCaves());
        put(map, ModTags.Biomes.desertBiomes, mob, multipliers.desert());
        put(map, ModTags.Biomes.deepDarkBiomes, mob, multipliers.deepDark());
        put(map, ModTags.Biomes.soulSandValleyBiomes, mob, multipliers.soulSandValley());
        put(map, ModTags.Biomes.warpedForestBiomes, mob, multipliers.warpedForest());
        put(map, ModTags.Biomes.crimsonForestBiomes, mob, multipliers.crimsonForest());
        put(map, ModTags.Biomes.basaltDeltasBiomes, mob, multipliers.basaltDeltas());
    }

    private static void addDropMap(EntityType<?> mob, MobBiomeMultipliers multipliers) {
        if (multipliers.item() != null) {
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map =
                    MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB.computeIfAbsent(multipliers.item(), item -> {
                        Map<TagKey<Biome>, Map<EntityType<?>, Float>> newMap = new HashMap<>();
                        ensureAllTags(newMap);
                        return newMap;
                    });

            addToMap(map, mob, multipliers);
        }

        if (multipliers.cookedItem() != null) {
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map =
                    MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB.computeIfAbsent(multipliers.cookedItem(), item -> {
                        Map<TagKey<Biome>, Map<EntityType<?>, Float>> newMap = new HashMap<>();
                        ensureAllTags(newMap);
                        return newMap;
                    });

            addToMap(map, mob, multipliers);
        }
    }

    private static void addMobInfo(
            EntityType<?> mob,
            MobBiomeMultipliers... rows
    ) {
        MobBiomeMultipliers aging = null;
        MobBiomeMultipliers breeding = null;
        int dropStart = 0;

        if (rows.length > 0 && !rows[0].isDropRow()) {
            aging = rows[0];
            addToMap(MOB_AGING_CHANCE_BY_TAG_BY_MOB, mob, aging);
            dropStart = 1;
        }

        if (rows.length > 1 && !rows[1].isDropRow()) {
            breeding = rows[1];
            addToMap(MOB_BREEDING_CHANCE_BY_TAG_BY_MOB, mob, breeding);
            dropStart = 2;
        }

        boolean affectedByLooting = dropsAffectedByLooting(mob);

        List<MobBiomeMultipliers> dropList = Arrays.stream(Arrays.copyOfRange(rows, dropStart, rows.length))
                .map(drop -> drop.withAffectedByLooting(affectedByLooting))
                .toList();

        MOB_INFO_BY_MOB.put(mob, new MobInfo(aging, breeding, dropList));

        for (MobBiomeMultipliers drop : dropList) {
            addDropMap(mob, drop);
        }
    }

    private static boolean dropsAffectedByLooting(EntityType<?> mob) {
        if (mob == EntityType.GUARDIAN || mob == EntityType.ELDER_GUARDIAN || mob == EntityType.HOGLIN) {
            return false;
        }

        return mob.getCategory() == MobCategory.MONSTER;
    }


    public record MobInfo(
            MobBiomeMultipliers aging,
            MobBiomeMultipliers breeding,
            List<MobBiomeMultipliers> drops
    ) {}

    public record MobBiomeMultipliers(
            float tropical,
            float savanna,
            float plains,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float soulSandValley,
            float warpedForest,
            float crimsonForest,
            float basaltDeltas,
            Item item,
            Item cookedItem,
            boolean affectedByLooting
    ) {
        public MobBiomeMultipliers(
                float tropical,
                float savanna,
                float plains,
                float mountains,
                float mushroomCaves,
                float desert,
                float deepDark,
                float soulSandValley,
                float warpedForest,
                float crimsonForest,
                float basaltDeltas,
                Item item,
                Item cookedItem
        ) {
            this(
                    tropical,
                    savanna,
                    plains,
                    mountains,
                    mushroomCaves,
                    desert,
                    deepDark,
                    soulSandValley,
                    warpedForest,
                    crimsonForest,
                    basaltDeltas,
                    item,
                    cookedItem,
                    false
            );
        }

        public boolean hasItem() {
            return item != null;
        }

        public boolean hasItem(Item item) {
            return this.item == item || this.cookedItem == item;
        }

        public Item selectedItem(LivingEntity entity) {
            if (entity.isOnFire() && cookedItem != null) {
                return cookedItem;
            }

            return item;
        }

        public boolean isDropRow() {
            return item != null || cookedItem != null;
        }

        public MobBiomeMultipliers withAffectedByLooting(boolean affectedByLooting) {
            return new MobBiomeMultipliers(
                    tropical,
                    savanna,
                    plains,
                    mountains,
                    mushroomCaves,
                    desert,
                    deepDark,
                    soulSandValley,
                    warpedForest,
                    crimsonForest,
                    basaltDeltas,
                    item,
                    cookedItem,
                    affectedByLooting
            );
        }
    }
}
