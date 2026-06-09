package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.morallenplay.dropthemeat.init.ItemInit;
import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import com.starfish_studios.naturalist.core.registry.NaturalistItems;
import dev.hybridlabs.aquatic.entity.HybridAquaticEntityTypes;
import net.amurdza.examplemod.event_handlers.DropInfo;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import samebutdifferent.ecologics.registry.ModEntityTypes;

import java.util.HashMap;
import java.util.Map;

public final class MobConfig {
    private MobConfig() {}

    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_GROWTH_CHANCE_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_TWINS_CHANCE_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_MEAT_AMOUNT_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_SKIN_AMOUNT_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_BONE_AMOUNT_BY_TAG_BY_MOB = new HashMap<>();

    public static final Map<EntityType<?>, Float> DEFAULT_MOB_GROWTH_CHANCE = new HashMap<>();
    public static final Map<EntityType<?>, Float> DEFAULT_MOB_TWINS_CHANCE = new HashMap<>();
    public static final Map<EntityType<?>, Float> DEFAULT_MOB_MEAT_AMOUNT = new HashMap<>();
    public static final Map<EntityType<?>, Float> DEFAULT_MOB_SKIN_AMOUNT = new HashMap<>();
    public static final Map<EntityType<?>, Float> DEFAULT_MOB_BONE_AMOUNT = new HashMap<>();
    public static final Map<EntityType<?>, DropInfo> DROP_INFO_BY_MOB = new HashMap<>();

    public static void init() {
        ensureAllTags(MOB_GROWTH_CHANCE_BY_TAG_BY_MOB);
        ensureAllTags(MOB_TWINS_CHANCE_BY_TAG_BY_MOB);
        ensureAllTags(MOB_MEAT_AMOUNT_BY_TAG_BY_MOB);
        ensureAllTags(MOB_SKIN_AMOUNT_BY_TAG_BY_MOB);
        ensureAllTags(MOB_BONE_AMOUNT_BY_TAG_BY_MOB);
        registerMobInfo();
    }

    private static void ensureAllTags(Map<TagKey<Biome>, Map<EntityType<?>, Float>> map) {
        ensureTag(map, ModTags.Biomes.tropicalBiomes);
        ensureTag(map, ModTags.Biomes.savannaBiomes);
        ensureTag(map, ModTags.Biomes.mountainBiomes);
        ensureTag(map, ModTags.Biomes.mushroomCaves);
        ensureTag(map, ModTags.Biomes.desertBiomes);
        ensureTag(map, ModTags.Biomes.deepDarkBiomes);
        ensureTag(map, ModTags.Biomes.netherBiomes);
        ensureTag(map, ModTags.Biomes.basaltDeltasBiomes);
        ensureTag(map, ModTags.Biomes.crimsonForestBiomes);
        ensureTag(map, ModTags.Biomes.warpedForestBiomes);
        ensureTag(map, ModTags.Biomes.soulSandValleyBiomes);
        ensureTag(map, ModTags.Biomes.plainsBiomes);
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

    private static void addDropInfo(
            EntityType<?> mob,
            Item meatItem,
            Item cookedMeatItem,
            Item skinItem,
            Item boneItem
    ) {
        DROP_INFO_BY_MOB.put(mob, new DropInfo(meatItem, cookedMeatItem, skinItem, boneItem));
    }


    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float mountains,
            float desert,
            float nether,
            float mushroomCaves,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float water,
            float other
    ) {
        return new MobBiomeMultipliers(
                tropical, savanna, mountains, desert, nether,
                mushroomCaves, basaltDeltas, crimsonForest, warpedForest, soulSandValley,
                water, other
        );
    }

    private static MobBiomeMultipliers allZero=m(0F, 0F, 0F, 0F, 0F,
            0F, 0F, 0F, 0F, 0F, 0F, 0F);

    private static void addNullDropMobInfo(
            EntityType<?> mob,
            MobBiomeMultipliers growth,
            MobBiomeMultipliers twins
    ) {
        addMobInfo(
                mob,
                null,
                null,
                null,
                null,
                allZero,
                allZero,
                allZero,
                growth,
                twins
        );
    }

    private static void registerMobInfo() {
        // -------------------------------------------------------------------------
        // Passive / animal mobs
        // -------------------------------------------------------------------------

        addMobInfo(EntityType.COW, Items.BEEF, Items.COOKED_BEEF, Items.LEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.MOOSHROOM, Items.BEEF, Items.COOKED_BEEF, Items.LEATHER, Items.BONE,
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.PIG, Items.PORKCHOP, Items.COOKED_PORKCHOP, null, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.SHEEP, Items.MUTTON, Items.COOKED_MUTTON, Items.WHITE_WOOL, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.CHICKEN, Items.CHICKEN, Items.COOKED_CHICKEN, Items.FEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.GOAT, ItemInit.RAW_GOAT.get(), ItemInit.COOKED_GOAT.get(), Items.WHITE_WOOL, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.RABBIT, Items.RABBIT, Items.COOKED_RABBIT, Items.RABBIT_HIDE, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.HOGLIN, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.LEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.STRIDER, Items.MUTTON, Items.COOKED_MUTTON, Items.STRING, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(EntityType.FOX, ItemInit.RAW_FOX.get(), ItemInit.COOKED_FOX.get(), Items.ORANGE_WOOL, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.HORSE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.DONKEY,
                 m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.MULE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.CAMEL,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.BEE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.ALLAY,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.LLAMA,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.TRADER_LLAMA,
               m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.POLAR_BEAR,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.WOLF,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.CAT,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.OCELOT,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.PANDA,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.DOLPHIN,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.TURTLE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.BAT,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.FROG,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.AXOLOTL,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.PARROT,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.SNIFFER,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.TADPOLE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.VILLAGER,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.WANDERING_TRADER,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.PIGLIN,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        // -------------------------------------------------------------------------
        // Water mobs
        // -------------------------------------------------------------------------

        addMobInfo(EntityType.COD, Items.COD, Items.COOKED_COD, null, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SALMON, Items.SALMON, Items.COOKED_SALMON, null, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.TROPICAL_FISH, Items.TROPICAL_FISH, Items.TROPICAL_FISH, null, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.PUFFERFISH, Items.PUFFERFISH, ModItems.COOKED_PUFFERFISH.get(), null, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.GLOW_SQUID, com.baisylia.culturaldelights.item.ModItems.GLOW_SQUID.get(), com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get(), Items.GLOW_INK_SAC, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SQUID, com.baisylia.culturaldelights.item.ModItems.SQUID.get(), com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get(), Items.INK_SAC, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        // -------------------------------------------------------------------------
        // Modded passive / animal mobs
        // -------------------------------------------------------------------------

        addMobInfo(AMEntityRegistry.EMU.get(), Items.CHICKEN, Items.COOKED_CHICKEN, AMItemRegistry.EMU_FEATHER.get(), Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(AMEntityRegistry.BISON.get(), Items.BEEF, Items.COOKED_BEEF, Items.BROWN_WOOL, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(AMEntityRegistry.GAZELLE.get(), Items.MUTTON, Items.COOKED_MUTTON, Items.LEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(AMEntityRegistry.MOOSE.get(), ItemInit.RAW_BEAR.get(), ItemInit.COOKED_BEAR.get(), Items.LEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(AMEntityRegistry.WARPED_TOAD.get(), ItemInit.RAW_SQUID.get(), ItemInit.COOKED_SQUID.get(), Items.SHROOMLIGHT, Items.BONE,
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(2.0F, 1.0F, 0.5F, 0.5F, 0.5F, 1.5F, 0.6F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(AMEntityRegistry.LOBSTER.get(), AMItemRegistry.LOBSTER_TAIL.get(), AMItemRegistry.COOKED_LOBSTER_TAIL.get(), null, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addNullDropMobInfo(AMEntityRegistry.GORILLA.get(),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(ModEntityTypes.SQUIRREL.get(), ItemInit.RAW_BAT.get(), ItemInit.COOKED_BAT.get(), Items.RABBIT_HIDE, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(NaturalistEntityTypes.DEER.get(), NaturalistItems.VENISON.get(), NaturalistItems.COOKED_VENISON.get(), Items.LEATHER, Items.BONE,
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.3F, 0.5F, 0.5F, 0.3F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F),
                m(1.5F, 3.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 1.0F, 1.0F));

        addMobInfo(HybridAquaticEntityTypes.INSTANCE.getARROW_SQUID().get(), ItemInit.RAW_DOLPHIN.get(), ItemInit.COOKED_DOLPHIN.get(), Items.INK_SAC, Items.BONE_MEAL,
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        // -------------------------------------------------------------------------
        // Hostile mobs
        // -------------------------------------------------------------------------

        addMobInfo(EntityType.ZOMBIE, Items.ROTTEN_FLESH, Items.LEATHER, null, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ZOMBIE_VILLAGER, Items.ROTTEN_FLESH, Items.LEATHER, Items.EMERALD, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.HUSK, Items.ROTTEN_FLESH, Items.LEATHER, Items.SAND, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.DROWNED, Items.ROTTEN_FLESH, Items.LEATHER, Items.SEAGRASS, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ZOMBIFIED_PIGLIN, Items.ROTTEN_FLESH, null, Items.GOLD_NUGGET, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ZOGLIN, Items.ROTTEN_FLESH, Items.LEATHER, null, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ZOMBIE_HORSE, Items.ROTTEN_FLESH, Items.LEATHER, null, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SKELETON, Items.ARROW, null, Items.BOW, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.STRAY, Items.ARROW, null, Items.BOW, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.WITHER_SKELETON, Items.COAL, null, Items.STONE_SWORD, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SKELETON_HORSE, null, null, null, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SPIDER, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get(), Items.STRING, Items.SPIDER_EYE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.CAVE_SPIDER, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get(), Items.STRING, Items.SPIDER_EYE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.SILVERFISH,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.ENDERMITE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.CREEPER, null, null, Items.GUNPOWDER, null,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SLIME, null, null, Items.SLIME_BALL, null,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.MAGMA_CUBE, null, null, Items.MAGMA_CREAM, null,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.BLAZE, null, null, Items.GLOWSTONE_DUST, Items.BLAZE_ROD,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.GHAST, null, null, Items.GHAST_TEAR, Items.GUNPOWDER,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.PIGLIN_BRUTE, null, null, Items.GOLD_INGOT, Items.GOLDEN_AXE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ENDERMAN, null, null, Items.ENDER_PEARL, null,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.SHULKER, null, null, Items.SHULKER_SHELL, Items.PURPLE_TERRACOTTA,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addNullDropMobInfo(EntityType.ENDER_DRAGON,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.PILLAGER, null, null, Items.CROSSBOW, Items.EMERALD,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.VINDICATOR, null, null, Items.IRON_AXE, Items.EMERALD,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.EVOKER, null, null, Items.TOTEM_OF_UNDYING, Items.EMERALD,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ILLUSIONER, null, null, Items.BOW, Items.EMERALD,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.RAVAGER, Items.LEATHER, null, Items.SADDLE, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.VEX, null, null, Items.IRON_SWORD, null,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.WITCH, Items.GUNPOWDER, null, Items.REDSTONE, Items.GLOWSTONE_DUST,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.GUARDIAN, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get(), Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.ELDER_GUARDIAN, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get(), Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.PHANTOM, null, null, Items.PHANTOM_MEMBRANE, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.WARDEN, Items.SCULK, null, Items.SCULK_CATALYST, Items.SCULK_SENSOR,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.WITHER, Items.SOUL_SAND, null, Items.NETHER_STAR, Items.WITHER_SKELETON_SKULL,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));

        addMobInfo(EntityType.GIANT, Items.ROTTEN_FLESH, null, null, Items.BONE,
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                m(0.4F, 0.7F, 2.0F, 3.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
    }

    public static DropInfo dropInfo(Entity entity) {
        return DROP_INFO_BY_MOB.get(entity.getType());
    }

    private static void addToMap(
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map,
            EntityType<?> mob,
            float tropical,
            float savanna,
            float mountains,
            float mountainCaves,
            float desert,
            float deepDark,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float plains,
            float other
    ) {
        put(map, ModTags.Biomes.tropicalBiomes, mob, tropical);
        put(map, ModTags.Biomes.savannaBiomes, mob, savanna);
        put(map, ModTags.Biomes.mountainBiomes, mob, mountains);
        put(map, ModTags.Biomes.mushroomCaves, mob, mountainCaves);
        put(map, ModTags.Biomes.desertBiomes, mob, desert);
        put(map, ModTags.Biomes.deepDarkBiomes, mob, deepDark);
        put(map, ModTags.Biomes.basaltDeltasBiomes, mob, basaltDeltas);
        put(map, ModTags.Biomes.crimsonForestBiomes, mob, crimsonForest);
        put(map, ModTags.Biomes.warpedForestBiomes, mob, warpedForest);
        put(map, ModTags.Biomes.soulSandValleyBiomes, mob, soulSandValley);
        put(map, ModTags.Biomes.plainsBiomes, mob, plains);
    }

    public static float mobGrowthChance(Entity entity) {
        return getMultiplier(entity, MOB_GROWTH_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_GROWTH_CHANCE);
    }

    public static float mobTwinsChance(Entity entity) {
        return getMultiplier(entity, MOB_TWINS_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_TWINS_CHANCE);
    }

    public static float mobMeatAmount(Entity entity) {
        return getMultiplier(entity, MOB_MEAT_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_MEAT_AMOUNT);
    }

    public static float mobSkinAmount(Entity entity) {
        return getMultiplier(entity, MOB_SKIN_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_SKIN_AMOUNT);
    }

    public static float mobBoneAmount(Entity entity) {
        return getMultiplier(entity, MOB_BONE_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_BONE_AMOUNT);
    }

    private static float getMultiplier(
            Entity entity,
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map,
            Map<EntityType<?>, Float> defaultMap
    ) {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        EntityType<?> mob = entity.getType();

        float result = 1.0F;
        boolean found = false;

        for (Map.Entry<TagKey<Biome>, Map<EntityType<?>, Float>> entry : map.entrySet()) {
            if (biome.is(entry.getKey())) {
                Float value = entry.getValue().get(mob);
                if (value != null) {
                    result *= value;
                    found = true;
                }
            }
        }

        if (!found) {
            return defaultMap.getOrDefault(mob, 1.0F);
        }

        return result;
    }


    private record MobBiomeMultipliers(
            float tropical,
            float savanna,
            float mountains,
            float desert,
            float nether,

            float mushroomCaves,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,

            float water,
            float other
    ) {}

    private static void addMobInfo(
            EntityType<?> mob,
            Item meatItem,
            Item cookedMeatItem,
            Item skinItem,
            Item boneItem,

            MobBiomeMultipliers meat,
            MobBiomeMultipliers skin,
            MobBiomeMultipliers bone,
            MobBiomeMultipliers growth,
            MobBiomeMultipliers twins
    ) {
        addDropInfo(mob, meatItem, cookedMeatItem, skinItem, boneItem);

        addToMap(MOB_MEAT_AMOUNT_BY_TAG_BY_MOB, mob,
                meat.tropical(), meat.savanna(), meat.mountains(), meat.desert(), meat.nether(),
                meat.mushroomCaves(), meat.basaltDeltas(), meat.crimsonForest(), meat.warpedForest(), meat.soulSandValley(),
                meat.water(), meat.other()
        );

        addToMap(MOB_SKIN_AMOUNT_BY_TAG_BY_MOB, mob,
                skin.tropical(), skin.savanna(), skin.mountains(), skin.desert(), skin.nether(),
                skin.mushroomCaves(), skin.basaltDeltas(), skin.crimsonForest(), skin.warpedForest(), skin.soulSandValley(),
                skin.water(), skin.other()
        );

        addToMap(MOB_BONE_AMOUNT_BY_TAG_BY_MOB, mob,
                bone.tropical(), bone.savanna(), bone.mountains(), bone.desert(), bone.nether(),
                bone.mushroomCaves(), bone.basaltDeltas(), bone.crimsonForest(), bone.warpedForest(), bone.soulSandValley(),
                bone.water(), bone.other()
        );

        addToMap(MOB_GROWTH_CHANCE_BY_TAG_BY_MOB, mob,
                growth.tropical(), growth.savanna(), growth.mountains(), growth.desert(), growth.nether(),
                growth.mushroomCaves(), growth.basaltDeltas(), growth.crimsonForest(), growth.warpedForest(), growth.soulSandValley(),
                growth.water(), growth.other()
        );

        addToMap(MOB_TWINS_CHANCE_BY_TAG_BY_MOB, mob,
                twins.tropical(), twins.savanna(), twins.mountains(), twins.desert(), twins.nether(),
                twins.mushroomCaves(), twins.basaltDeltas(), twins.crimsonForest(), twins.warpedForest(), twins.soulSandValley(),
                twins.water(), twins.other()
        );
    }
}