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

        registerVanillaMobs();
        registerModdedMobs();
        registerDropInfo();
        registerHostileDropInfo();
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


    private static void registerDropInfo() {
        addDropInfo(
                EntityType.COW,
                Items.BEEF,
                Items.COOKED_BEEF,
                Items.LEATHER,
                Items.BONE
        );

        addDropInfo(
                EntityType.MOOSHROOM,
                Items.BEEF,
                Items.COOKED_BEEF,
                Items.LEATHER,
                Items.BONE
        );

        addDropInfo(
                EntityType.PIG,
                Items.PORKCHOP,
                Items.COOKED_PORKCHOP,
                null,
                Items.BONE
        );

        addDropInfo(
                EntityType.SHEEP,
                Items.MUTTON,
                Items.COOKED_MUTTON,
                Items.WHITE_WOOL,
                Items.BONE
        );

        addDropInfo(
                EntityType.CHICKEN,
                Items.CHICKEN,
                Items.COOKED_CHICKEN,
                Items.FEATHER,
                Items.BONE
        );

        addDropInfo(
                EntityType.GOAT,
                ItemInit.RAW_GOAT.get(),
                ItemInit.COOKED_GOAT.get(),
                Items.WHITE_WOOL,
                Items.BONE
        );

        addDropInfo(
                EntityType.RABBIT,
                Items.RABBIT,
                Items.COOKED_RABBIT,
                Items.RABBIT_HIDE,
                Items.BONE
        );

        addDropInfo(
                EntityType.HOGLIN,
                Items.PORKCHOP,
                Items.COOKED_PORKCHOP,
                Items.LEATHER,
                Items.BONE
        );

        addDropInfo(
                EntityType.STRIDER,
                Items.MUTTON,
                Items.COOKED_MUTTON,
                Items.STRING,
                Items.BONE
        );

        addDropInfo(
                EntityType.COD,
                Items.COD,
                Items.COOKED_COD,
                null,
                Items.BONE_MEAL
        );

        addDropInfo(
                EntityType.SALMON,
                Items.SALMON,
                Items.COOKED_SALMON,
                null,
                Items.BONE_MEAL
        );

        addDropInfo(
                EntityType.TROPICAL_FISH,
                Items.TROPICAL_FISH,
                Items.TROPICAL_FISH,
                null,
                Items.BONE_MEAL
        );

        addDropInfo(
                EntityType.PUFFERFISH,
                Items.PUFFERFISH,
                ModItems.COOKED_PUFFERFISH.get(),
                null,
                Items.BONE_MEAL
        );

        addDropInfo(
                EntityType.FOX,
                ItemInit.RAW_FOX.get(),
                ItemInit.COOKED_FOX.get(),
                Items.ORANGE_WOOL,
                Items.BONE
        );

        addDropInfo(
                EntityType.HORSE,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.DONKEY,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.MULE,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.CAMEL,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.BEE,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.ALLAY,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.LLAMA,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.TRADER_LLAMA,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.POLAR_BEAR,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.WOLF,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.CAT,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.OCELOT,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.PANDA,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.DOLPHIN,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.TURTLE,
                null,
                null,
                null,
                Items.BONE
        );

        addDropInfo(
                EntityType.BAT,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.FROG,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.AXOLOTL,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.PARROT,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.SNIFFER,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.TADPOLE,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.VILLAGER,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.WANDERING_TRADER,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.PIGLIN,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.BEE,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.GLOW_SQUID,
                com.baisylia.culturaldelights.item.ModItems.GLOW_SQUID.get(),
                com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get(),
                Items.GLOW_INK_SAC,
                Items.BONE_MEAL
        );

        addDropInfo(
                EntityType.SQUID,
                com.baisylia.culturaldelights.item.ModItems.SQUID.get(),
                com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get(),
                Items.INK_SAC,
                Items.BONE_MEAL
        );
        addDropInfo(
                AMEntityRegistry.EMU.get(),
                Items.CHICKEN,
                Items.COOKED_CHICKEN,
                AMItemRegistry.EMU_FEATHER.get(),
                Items.BONE
        );
        addDropInfo(
                AMEntityRegistry.BISON.get(),
                Items.BEEF,
                Items.COOKED_BEEF,
                Items.BROWN_WOOL,
                Items.BONE
        );
        addDropInfo(
                AMEntityRegistry.GAZELLE.get(),
                Items.MUTTON,
                Items.COOKED_MUTTON,
                Items.LEATHER,
                Items.BONE
        );
        addDropInfo(
                AMEntityRegistry.MOOSE.get(),
                ItemInit.RAW_BEAR.get(),
                ItemInit.COOKED_BEAR.get(),
                Items.LEATHER,
                Items.BONE
        );
        addDropInfo(
                AMEntityRegistry.WARPED_TOAD.get(),
                ItemInit.RAW_SQUID.get(),
                ItemInit.COOKED_SQUID.get(),
                Items.SHROOMLIGHT,
                Items.BONE
        );
        addDropInfo(
                AMEntityRegistry.GORILLA.get(),
                null,
                null,
                null,
                null
        );
        addDropInfo(
                AMEntityRegistry.LOBSTER.get(),
                AMItemRegistry.LOBSTER_TAIL.get(),
                AMItemRegistry.COOKED_LOBSTER_TAIL.get(),
                null,
                Items.BONE_MEAL
        );
        addDropInfo(
                ModEntityTypes.SQUIRREL.get(),
                ItemInit.RAW_BAT.get(),
                ItemInit.COOKED_BAT.get(),
                Items.RABBIT_HIDE,
                Items.BONE
        );
        addDropInfo(
                NaturalistEntityTypes.DEER.get(),
                NaturalistItems.VENISON.get(),
                NaturalistItems.COOKED_VENISON.get(),
                Items.LEATHER,
                Items.BONE
        );
        addDropInfo(
                HybridAquaticEntityTypes.INSTANCE.getARROW_SQUID().get(),
                ItemInit.RAW_DOLPHIN.get(),
                ItemInit.COOKED_DOLPHIN.get(),
                Items.INK_SAC,
                Items.BONE_MEAL
        );
    }

    private static void registerHostileDropInfo() {
        // ------------------------------------------------------------
        // Undead / rotten flesh mobs
        // meatItem = rotten flesh
        // boneItem = bone because you said mobs that drop rotten flesh should also get bones
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.ZOMBIE,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                null,
                Items.BONE
        );

        addDropInfo(
                EntityType.ZOMBIE_VILLAGER,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                Items.EMERALD,
                Items.BONE
        );

        addDropInfo(
                EntityType.HUSK,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                Items.SAND,
                Items.BONE
        );

        addDropInfo(
                EntityType.DROWNED,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                Items.SEAGRASS,
                Items.BONE
        );

        addDropInfo(
                EntityType.ZOMBIFIED_PIGLIN,
                Items.ROTTEN_FLESH,
                null,
                Items.GOLD_NUGGET,
                Items.BONE
        );

        addDropInfo(
                EntityType.ZOGLIN,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                null,
                Items.BONE
        );

        addDropInfo(
                EntityType.ZOMBIE_HORSE,
                Items.ROTTEN_FLESH,
                Items.LEATHER,
                null,
                Items.BONE
        );

        // ------------------------------------------------------------
        // Skeleton-type mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.SKELETON,
                Items.ARROW,
                null,
                Items.BOW,
                Items.BONE
        );

        addDropInfo(
                EntityType.STRAY,
                Items.ARROW,
                null,
                Items.BOW,
                Items.BONE
        );

        addDropInfo(
                EntityType.WITHER_SKELETON,
                Items.COAL,
                null,
                Items.STONE_SWORD,
                Items.BONE
        );

        // Mostly unused vanilla entity, but it exists.
        addDropInfo(
                EntityType.SKELETON_HORSE,
                null,
                null,
                null,
                Items.BONE
        );

        // ------------------------------------------------------------
        // Spiders / arthropods
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.SPIDER,
                ItemInit.RAW_MONSTER_MEAT.get(),
                ItemInit.COOKED_MONSTER_MEAT.get(),
                Items.STRING,
                Items.SPIDER_EYE
        );

        addDropInfo(
                EntityType.CAVE_SPIDER,
                ItemInit.RAW_MONSTER_MEAT.get(),
                ItemInit.COOKED_MONSTER_MEAT.get(),
                Items.STRING,
                Items.SPIDER_EYE
        );

        addDropInfo(
                EntityType.SILVERFISH,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.ENDERMITE,
                null,
                null,
                null,
                null
        );

        // ------------------------------------------------------------
        // Creeper / explosive mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.CREEPER,
                null,
                null,
                Items.GUNPOWDER,
                null
        );

        // ------------------------------------------------------------
        // Slimes / magma
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.SLIME,
                null,
                null,
                Items.SLIME_BALL,
                null
        );

        addDropInfo(
                EntityType.MAGMA_CUBE,
                null,
                null,
                Items.MAGMA_CREAM,
                null
        );

        // ------------------------------------------------------------
        // Nether hostile mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.BLAZE,
                null,
                null,
                Items.GLOWSTONE_DUST,
                Items.BLAZE_ROD
        );

        addDropInfo(
                EntityType.GHAST,
                null,
                null,
                Items.GHAST_TEAR,
                Items.GUNPOWDER
        );

        addDropInfo(
                EntityType.PIGLIN,
                null,
                null,
                null,
                null
        );

        addDropInfo(
                EntityType.PIGLIN_BRUTE,
                null,
                null,
                Items.GOLD_INGOT,
                Items.GOLDEN_AXE
        );

        // ------------------------------------------------------------
        // End hostile mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.ENDERMAN,
                null,
                null,
                Items.ENDER_PEARL,
                null
        );

        addDropInfo(
                EntityType.SHULKER,
                null,
                null,
                Items.SHULKER_SHELL,
                Items.PURPLE_TERRACOTTA
        );

        // Ender dragon does not normally have a normal item drop.
        addDropInfo(
                EntityType.ENDER_DRAGON,
                null,
                null,
                null,
                null
        );

        // ------------------------------------------------------------
        // Illagers / raid mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.PILLAGER,
                null,
                null,
                Items.CROSSBOW,
                Items.EMERALD
        );

        addDropInfo(
                EntityType.VINDICATOR,
                null,
                null,
                Items.IRON_AXE,
                Items.EMERALD
        );

        addDropInfo(
                EntityType.EVOKER,
                null,
                null,
                Items.TOTEM_OF_UNDYING,
                Items.EMERALD
        );

        // Exists in vanilla, even though it is unused normally.
        addDropInfo(
                EntityType.ILLUSIONER,
                null,
                null,
                Items.BOW,
                Items.EMERALD
        );

        addDropInfo(
                EntityType.RAVAGER,
                Items.LEATHER,
                null,
                Items.SADDLE,
                Items.BONE
        );

        addDropInfo(
                EntityType.VEX,
                null,
                null,
                Items.IRON_SWORD,
                null
        );

        addDropInfo(
                EntityType.WITCH,
                Items.GUNPOWDER,
                null,
                Items.REDSTONE,
                Items.GLOWSTONE_DUST
        );

        // ------------------------------------------------------------
        // Ocean hostile mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.GUARDIAN,
                ItemInit.RAW_MONSTER_MEAT.get(),
                ItemInit.COOKED_MONSTER_MEAT.get(),
                Items.PRISMARINE_SHARD,
                Items.PRISMARINE_CRYSTALS
        );

        addDropInfo(
                EntityType.ELDER_GUARDIAN,
                ItemInit.RAW_MONSTER_MEAT.get(),
                ItemInit.COOKED_MONSTER_MEAT.get(),
                Items.PRISMARINE_SHARD,
                Items.PRISMARINE_CRYSTALS
        );

        // ------------------------------------------------------------
        // Flying / special hostile mobs
        // ------------------------------------------------------------

        addDropInfo(
                EntityType.PHANTOM,
                null,
                null,
                Items.PHANTOM_MEMBRANE,
                Items.BONE
        );

        addDropInfo(
                EntityType.WARDEN,
                Items.SCULK,
                null,
                Items.SCULK_CATALYST,
                Items.SCULK_SENSOR
        );

        addDropInfo(
                EntityType.WITHER,
                Items.SOUL_SAND,
                null,
                Items.NETHER_STAR,
                Items.WITHER_SKELETON_SKULL
        );

        // Mostly unused vanilla hostile entity.
        addDropInfo(
                EntityType.GIANT,
                Items.ROTTEN_FLESH,
                null,
                null,
                Items.BONE
        );
    }

    public static DropInfo dropInfo(Entity entity) {
        return DROP_INFO_BY_MOB.get(entity.getType());
    }

    private static void addToMap(
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map,
            Map<EntityType<?>, Float> defaultMap,
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

        defaultMap.put(mob, other);
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

    private static void registerVanillaMobs() {
        addAnimal(EntityType.COW);
        addAnimal(EntityType.PIG);
        addAnimal(EntityType.SHEEP);
        addAnimal(EntityType.CHICKEN);
        addAnimal(EntityType.RABBIT);

        addMonster(EntityType.ZOMBIE);
        addMonster(EntityType.SKELETON);
        addMonster(EntityType.CREEPER);
        addMonster(EntityType.SPIDER);

        addMushroomMob(EntityType.MOOSHROOM);
    }

    private static void registerModdedMobs() {
        // Example:
        // addAnimal(com.github.alexthe666.alexsmobs.entity.AMEntityRegistry.EMU.get());
    }

    private static void addAnimal(EntityType<?> mob) {
        // Animals:
        // tropical x1.5, savanna x3, mountains x0.5, desert x0.5, nether x0.5.
        addToMap(MOB_MEAT_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_MEAT_AMOUNT, mob,
                1.5F, 3.0F, 0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 1.0F
        );

        addToMap(MOB_SKIN_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_SKIN_AMOUNT, mob,
                1.5F, 3.0F, 0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 1.0F
        );

        addToMap(MOB_BONE_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_BONE_AMOUNT, mob,
                1.5F, 3.0F, 0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 1.0F
        );

        addToMap(MOB_GROWTH_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_GROWTH_CHANCE, mob,
                1.5F, 3.0F, 0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 1.0F
        );

        addToMap(MOB_TWINS_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_TWINS_CHANCE, mob,
                1.5F, 3.0F, 0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 1.0F
        );
    }

    private static void addMonster(EntityType<?> mob) {
        // Monsters:
        // tropical x0.4, savanna x0.7, mountains x2, desert/nether x3.
        addToMap(MOB_MEAT_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_MEAT_AMOUNT, mob,
                0.4F, 0.7F, 2.0F, 2.0F, 3.0F,
                3.0F, 3.0F, 3.0F, 3.0F, 3.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_SKIN_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_SKIN_AMOUNT, mob,
                0.4F, 0.7F, 2.0F, 2.0F, 3.0F,
                3.0F, 3.0F, 3.0F, 3.0F, 3.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_BONE_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_BONE_AMOUNT, mob,
                0.4F, 0.7F, 2.0F, 2.0F, 3.0F,
                3.0F, 3.0F, 3.0F, 3.0F, 3.0F,
                1.0F, 1.0F
        );
    }

    private static void addMushroomMob(EntityType<?> mob) {
        // Mushroom mobs:
        // tropical x2, mushroom caves x1.5, basalt deltas x0.6.
        addToMap(MOB_MEAT_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_MEAT_AMOUNT, mob,
                2.0F, 1.0F, 1.0F, 1.5F, 1.0F,
                1.0F, 0.6F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_SKIN_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_SKIN_AMOUNT, mob,
                2.0F, 1.0F, 1.0F, 1.5F, 1.0F,
                1.0F, 0.6F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_BONE_AMOUNT_BY_TAG_BY_MOB, DEFAULT_MOB_BONE_AMOUNT, mob,
                2.0F, 1.0F, 1.0F, 1.5F, 1.0F,
                1.0F, 0.6F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_GROWTH_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_GROWTH_CHANCE, mob,
                2.0F, 1.0F, 1.0F, 1.5F, 1.0F,
                1.0F, 0.6F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F
        );

        addToMap(MOB_TWINS_CHANCE_BY_TAG_BY_MOB, DEFAULT_MOB_TWINS_CHANCE, mob,
                2.0F, 1.0F, 1.0F, 1.5F, 1.0F,
                1.0F, 0.6F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F
        );
    }
}