package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.morallenplay.dropthemeat.init.ItemInit;
import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import com.starfish_studios.naturalist.core.registry.NaturalistItems;
import dev.hybridlabs.aquatic.entity.HybridAquaticEntityTypes;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import samebutdifferent.ecologics.registry.ModEntityTypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobConfig {
    private MobConfig() {}

    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_AGING_CHANCE_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<TagKey<Biome>, Map<EntityType<?>, Float>> MOB_BREEDING_CHANCE_BY_TAG_BY_MOB = new HashMap<>();

    public static final Map<Item, Map<TagKey<Biome>, Map<EntityType<?>, Float>>> MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB = new HashMap<>();
    public static final Map<EntityType<?>, MobInfo> MOB_INFO_BY_MOB = new HashMap<>();

    public static void init() {
        ensureAllTags(MOB_AGING_CHANCE_BY_TAG_BY_MOB);
        ensureAllTags(MOB_BREEDING_CHANCE_BY_TAG_BY_MOB);
        registerMobInfo();
    }

    private static void ensureAllTags(Map<TagKey<Biome>, Map<EntityType<?>, Float>> map) {
        ensureTag(map, ModTags.Biomes.tropicalBiomes);
        ensureTag(map, ModTags.Biomes.savannaBiomes);
        ensureTag(map, ModTags.Biomes.mountainBiomes);
        ensureTag(map, ModTags.Biomes.mushroomCaves);
        ensureTag(map, ModTags.Biomes.desertBiomes);
        ensureTag(map, ModTags.Biomes.deepDarkBiomes);
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

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float plains
    ) {
        return new MobBiomeMultipliers(
                tropical, savanna, mountains, mushroomCaves, desert, deepDark,
                basaltDeltas, crimsonForest, warpedForest, soulSandValley, plains,
                null, null
        );
    }

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float plains,
            Item item
    ) {
        return new MobBiomeMultipliers(
                tropical, savanna, mountains, mushroomCaves, desert, deepDark,
                basaltDeltas, crimsonForest, warpedForest, soulSandValley, plains,
                item, null
        );
    }

    private static MobBiomeMultipliers m(
            float tropical,
            float savanna,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float plains,
            Item rawItem,
            Item cookedItem
    ) {
        return new MobBiomeMultipliers(
                tropical, savanna, mountains, mushroomCaves, desert, deepDark,
                basaltDeltas, crimsonForest, warpedForest, soulSandValley, plains,
                rawItem, cookedItem
        );
    }

    private static void registerMobInfo() {
        // -------------------------------------------------------------------------
        // Mob info
        // -------------------------------------------------------------------------

        addMobInfo(EntityType.COW,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, Items.BEEF, Items.COOKED_BEEF),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.LEATHER));


        addMobInfo(EntityType.MOOSHROOM,
                m(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
                m(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
                m(0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, Items.BEEF, Items.COOKED_BEEF),
                m(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER));



        addMobInfo(EntityType.PIG,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, Items.PORKCHOP, Items.COOKED_PORKCHOP));

        addMobInfo(EntityType.SHEEP,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.MUTTON, Items.COOKED_MUTTON),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.WHITE_WOOL),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.BONE));

        addMobInfo(EntityType.CHICKEN,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.CHICKEN, Items.COOKED_CHICKEN),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.FEATHER),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.BONE));

        addMobInfo(EntityType.GOAT,
                m(0, 0, 0.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0),
                m(0, 0, 0.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0),
                m(0, 0, 1, 0.5F, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_GOAT.get(), ItemInit.COOKED_GOAT.get()),
                m(0, 0, 1.5F, 1.5F, 0, 0, 0, 0, 0, 0, 0, Items.WHITE_WOOL),
                m(0, 0, 0.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.RABBIT,
                m(3, 3, 0.8F, 0.8F, 0.4F, 0, 0, 0, 0, 0, 1),
                m(2.5F, 3, 1, 1, 1, 0, 0, 0, 0, 0, 1),
                m(1.25F, 1.25F, 0.4F, 0.4F, 0.2F, 0, 0, 0, 0, 0, 0.5F, Items.RABBIT, Items.COOKED_RABBIT),
                m(1.25F, 1.25F, 0.4F, 0.4F, 0.3F, 0, 0, 0, 0, 0, 0.5F, Items.RABBIT_HIDE),
                m(0.25F, 0.25F, 0.08F, 0.08F, 0.15F, 0, 0, 0, 0, 0, 0.1F, Items.RABBIT_FOOT),
                m(0.25F, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.HOGLIN,
                m(0, 0, 0, 0, 0, 0, 0.1F, 0.20F, 0, 0, 0),
                m(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0),
                m(0, 0, 0, 0, 0, 0, 1, 3, 0, 0, 0, Items.PORKCHOP, Items.COOKED_PORKCHOP),
                m(0, 0, 0, 0, 0, 0, 0.75F, 1, 0, 0, 0, Items.LEATHER));

        addMobInfo(EntityType.STRIDER,
                m(0, 0, 0, 0, 0, 0, 0.20F, 0.15F, 0.10F, 0, 0),
                m(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0),
                m(0, 0, 0, 0, 0, 0, 1, 0.5F, 0.35F, 0, 0, Items.MUTTON, Items.COOKED_MUTTON),
                m(0, 0, 0, 0, 0, 0, 3, 1.5F, 0.6F, 0, 0, Items.STRING));

        addMobInfo(EntityType.FOX,
                m(1, 3, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1, 1.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_FOX.get(), ItemInit.COOKED_FOX.get()),
                m(1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, Items.ORANGE_WOOL),
                m(0.25F, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.HORSE,
                m(1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0.8F),
                m(1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1));

        addMobInfo(EntityType.DONKEY,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1));

        addMobInfo(EntityType.MULE,
                m(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1));

        addMobInfo(EntityType.CAMEL,
                m(0, 0, 0, 0, 0.2F, 0, 0, 0, 0, 0, 0),
                m(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.BEE,
                m(1.5F, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0.5F),
                m(2F, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 1));

        addMobInfo(EntityType.ALLAY);

        addMobInfo(EntityType.LLAMA,
                m(0, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.TRADER_LLAMA,
                m(0, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.POLAR_BEAR,
                m(0, 0, 0.5F, 0.5F, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.WOLF,
                m(1, 2, 0.3F, 0.3F, 0, 0, 0, 0, 0, 0, 1),
                m(1, 2, 1, 1, 0, 0, 0, 0, 0, 0, 1));

        addMobInfo(EntityType.CAT,
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.OCELOT,
                m(2F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.PANDA,
                m(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.DOLPHIN);

        addMobInfo(EntityType.TURTLE,
                m(1.25F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.BAT);

        addMobInfo(EntityType.FROG,
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.0F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));


        addMobInfo(EntityType.AXOLOTL,
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.PARROT);

        addMobInfo(EntityType.SNIFFER,
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.TADPOLE,
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(EntityType.VILLAGER,
                m(2.5F, 2.5F, 0.75F, 0.75F, 0.4F, 0.4F, 0, 0.4F, 0.4F, 0.4F, 1),
                m(2.5F, 2.5F, 0.75F, 0.75F, 0.4F, 0.4F, 0, 0.4F, 0.4F, 0.4F, 1));


        addMobInfo(EntityType.WANDERING_TRADER);

        addMobInfo(EntityType.PIGLIN);

        addMobInfo(EntityType.COD,
                m(2F, 1, 2, 2, 0.5F, 0, 0, 0, 0, 0, 1, Items.COD, Items.COOKED_COD),
                m(0.2F, 0.05F, 0.15F, 0.15F, 0.01F, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.SALMON,
                m(2, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, Items.SALMON, Items.COOKED_SALMON),
                m(0.1F, 0.05F, 0.15F, 0.15F, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.TROPICAL_FISH,
                m(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.TROPICAL_FISH, Items.TROPICAL_FISH),
                m(0.2F, 0.05F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.PUFFERFISH,
                m(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.PUFFERFISH, ModItems.COOKED_PUFFERFISH.get()),
                m(0.15F, 0.05F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.GLOW_SQUID,
                m(0, 0, 2, 2, 0, 0.5F, 0, 0, 0, 0, 0, com.baisylia.culturaldelights.item.ModItems.GLOW_SQUID.get(), com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get()),
                m(0, 0, 3, 3, 0, 0.7F, 0, 0, 0, 0, 0, Items.GLOW_INK_SAC),
                m(0, 0, 0.2F, 0.2F, 0, 0.05F, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.SQUID,
                m(2, 1, 2, 2, 0, 0, 0, 0, 0, 0, 1, com.baisylia.culturaldelights.item.ModItems.SQUID.get(), com.baisylia.culturaldelights.item.ModItems.COOKED_SQUID.get()),
                m(3, 2, 3, 3, 0, 0, 0, 0, 0, 0, 1, Items.INK_SAC),
                m(0, 0, 0.2F, 0.2F, 0, 0, 0, 0, 0, 0, 0.05F, Items.BONE_MEAL));

        addMobInfo(AMEntityRegistry.EMU.get(),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.CHICKEN, Items.COOKED_CHICKEN),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, AMItemRegistry.EMU_FEATHER.get()),
                m(0, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.BISON.get(),
                m(0, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BEEF, Items.COOKED_BEEF),
                m(0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BROWN_WOOL),
                m(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.GAZELLE.get(),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.MUTTON, Items.COOKED_MUTTON),
                m(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER),
                m(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.MOOSE.get(),
                m(1.5F, 3, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.25F, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2.5F, 1, 0, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_BEAR.get(), ItemInit.COOKED_BEAR.get()),
                m(2, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER),
                m(1.5F, 2, 0.5F, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(AMEntityRegistry.WARPED_TOAD.get(),
                m(0, 0, 0, 0, 0, 0, 0, 0.2F, 0.4F, 0, 0),
                m(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0),
                m(0, 0, 0, 0, 0, 0, 0, 0.5F, 1, 0, 0, ItemInit.RAW_SQUID.get(), ItemInit.COOKED_SQUID.get()),
                m(0, 0, 0, 0, 0, 0, 0, 1.5F, 2.5F, 0, 0, Items.SHROOMLIGHT),
                m(0, 0, 0, 0, 0, 0, 0F, 0.2F, 0.3F, 0, 1, Items.BONE));

        addMobInfo(AMEntityRegistry.LOBSTER.get(),
                m(2, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, AMItemRegistry.LOBSTER_TAIL.get(), AMItemRegistry.COOKED_LOBSTER_TAIL.get()),
                m(0.25F, 0.05F, 0.25F, 0.25F, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(AMEntityRegistry.GORILLA.get(),
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        addMobInfo(ModEntityTypes.SQUIRREL.get(),
                m(3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(1.5F, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_BAT.get(), ItemInit.COOKED_BAT.get()),
                m(1.5F, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.RABBIT_HIDE),
                m(0.5F, 0.75F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(NaturalistEntityTypes.DEER.get(),
                m(2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                m(2, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, NaturalistItems.VENISON.get(), NaturalistItems.COOKED_VENISON.get()),
                m(1.5F, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.LEATHER),
                m(1, 1.5F, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(HybridAquaticEntityTypes.INSTANCE.getARROW_SQUID().get(),
                m(2, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_DOLPHIN.get(), ItemInit.COOKED_DOLPHIN.get()),
                m(2, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, Items.INK_SAC),
                m(0.5F, 0, 0.4F, 0.4F, 0, 0, 0, 0, 0, 0, 0, Items.BONE_MEAL));

        addMobInfo(EntityType.ZOMBIE,
                m(0, 0.8F, 1.5F, 1.5F, 2.5F, 1, 0, 0, 0, 0, 1, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0, 0.2F, 0.2F, 0.3F, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.ZOMBIE_VILLAGER,
                m(0, 0.8F, 1.5F, 1.5F, 2.5F, 1, 0, 0, 0, 0, 1, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0, 0.2F, 0.2F, 0.3F, 0, 0, 0, 0, 0, 0, Items.BONE),
                m(0, 0.2F, 0.7F, 0.7F, 0.4F, 0, 0, 0, 0, 0, 0.2F, Items.EMERALD),
                m(0, 0.2F, 0, 0, 0.4F, 0, 0, 0, 0, 0, 0, Items.GOLD_INGOT));

        addMobInfo(EntityType.HUSK,
                m(0, 0.8F, 1.5F, 1.5F, 2, 1, 0, 0, 0, 0, 1, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0, 0, 0, 0.3F, 0, 0, 0, 0, 0, 0, Items.BONE),
                m(0, 0, 0, 0, 0.4F, 0, 0, 0, 0, 0, 0.2F, Items.EMERALD),
                m(0, 0, 0, 0, 0.4F, 0, 0, 0, 0, 0, 0, Items.GOLD_INGOT));

        addMobInfo(EntityType.DROWNED,
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SEAGRASS),
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.BONE));

        addMobInfo(EntityType.ZOMBIFIED_PIGLIN,
                m(0, 0, 0, 0, 0.2F, 0.7F, 0, 0.5F, 2, 1.25F, 0, Items.ROTTEN_FLESH),
                m(0, 0, 0, 0, 0, 0, 0, 6, 9, 6, 0, Items.GOLD_NUGGET),
                m(0, 0, 0, 0, 0.2F, 0.6F, 0, 1, 1.5F, 1, 0, Items.BONE));

        addMobInfo(EntityType.ZOGLIN,
                m(0, 0, 0, 0, 0.35F, 0.6F, 0, 0.8F, 3, 1.7F, 0, Items.ROTTEN_FLESH),
                m(0, 0, 0, 0, 0.2F, 0.6F, 0, 1, 1.5F, 1, 0, Items.BONE));

        addMobInfo(EntityType.ZOMBIE_HORSE,
                m(0, 2, 1.5F, 1.5F, 3, 1, 1, 1, 1, 1, 1.5F, Items.ROTTEN_FLESH, Items.LEATHER),
                m(0, 0.3F, 0.2F, 0.2F, 0.1F, 0.05F, 0, 0, 0, 0, 0.1F, Items.BONE));

        //Up to here
        addMobInfo(EntityType.SKELETON,
                m(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, Items.ARROW),
                m(0, 0.015f, 0, 0, 0, 0, 0, 0, 0, 0, 0.03f, Items.BOW),
                m(0, 0.5f, 2, 1.25f, 2.25f, 1.0f, 2f, 1.5f, 1.25f, 3f, 1f, Items.BONE));

        addMobInfo(EntityType.STRAY,
                m(0, 0, 2.5f, 1.5f, 0, 0, 0, 0, 0, 0, 0, Items.BONE));

        addMobInfo(EntityType.WITHER_SKELETON,
                m(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, Items.COAL),
                m(0, 0, 0, 0, 0, 1, 1, 1, 1, 2.5f, 0, Items.BONE));

        addMobInfo(EntityType.SKELETON_HORSE,
                m(0, 2, 3, 3, 2, 2, 2, 2, 2, 3, 1.5f, Items.BONE));

        addMobInfo(EntityType.SPIDER,
                m(0.35F, 0.1F, 0.5f, 1f, 1.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.75f,
                        ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get()),
                m(3, 1.25F, 2, 1f, 3, 1f, 1f, 1f, 1f, 1f, 1f, Items.STRING),
                m(0.8F, 0.6F, 0.2F, 1, 3, 1, 1, 1, 1, 1, 1, Items.SPIDER_EYE));

        addMobInfo(EntityType.CAVE_SPIDER,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1,
                        ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get()),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.STRING),
                m(1.5F, 0.1F, 0.1F, 0.1F, 1f, 1f, 0.3f, 0.3f, 0.3f, 0.3f, 0.3f,
                        Items.SPIDER_EYE));

        addMobInfo(EntityType.SILVERFISH);

        addMobInfo(EntityType.ENDERMITE);

        addMobInfo(EntityType.CREEPER,
                m(0, 0, 0, 0, 5f, 1.5f, 2.5f, 4f, 5f, 2f, 0, Items.GUNPOWDER));

        addMobInfo(EntityType.SLIME,
                m(0.1f, 0.15F, 0.15F, 0.15F, 1, 0.7f, 0.1f, 0.1f, 0.1f,
                        0.1f, 0.1f, Items.SLIME_BALL));

        addMobInfo(EntityType.MAGMA_CUBE,
                m(0, 0, 0, 0, 0, 0, 1.5f, 0.7f, 0.2f, 0.1f, 0, Items.MAGMA_CREAM));

        addMobInfo(EntityType.BLAZE,
                m(0, 0, 0, 0, 0, 0, 2.0f, 1.25f, 0.2f, 0.25f, 0, Items.GLOWSTONE_DUST),
                m(0, 0, 0, 0, 0, 0, 1.5f, 0.6f, 0.2f, 0.1f, 0, Items.BLAZE_ROD));

        addMobInfo(EntityType.GHAST,
                m(0, 0, 0, 0, 0, 0.5f, 0.25f, 0.5f, 1f, 2f, 0, Items.GHAST_TEAR),
                m(0, 0, 0, 0, 0, 0.5f, 0.5f, 0.75f, 1.25f, 2.5f, 0, Items.GUNPOWDER));

        addMobInfo(EntityType.PIGLIN_BRUTE);

        addMobInfo(EntityType.ENDERMAN,
                m(0, 0, 0, 0, 0.65f, 1.5f, 0, 0, 0, 0.6f, 0, Items.ENDER_PEARL));

        addMobInfo(EntityType.SHULKER,
                m(0, 0, 0, 0, 1, 3, 0.1f, 0.1f, 0.1f, 0.1f, 0, Items.SHULKER_SHELL),
                m(0, 0, 0, 0, 3, 3, 1, 1, 1, 1.5f, 0, Items.PURPLE_TERRACOTTA));

        addMobInfo(EntityType.ENDER_DRAGON);

        addMobInfo(EntityType.PILLAGER,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.CROSSBOW),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.ARROW),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.EMERALD));

        addMobInfo(EntityType.VINDICATOR,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.IRON_AXE),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.EMERALD));

        addMobInfo(EntityType.EVOKER,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.TOTEM_OF_UNDYING),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.EMERALD));

        addMobInfo(EntityType.ILLUSIONER,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.BOW),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.EMERALD));

        addMobInfo(EntityType.RAVAGER,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.LEATHER),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SADDLE),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.BONE));

        addMobInfo(EntityType.VEX,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.IRON_SWORD));

        addMobInfo(EntityType.WITCH,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.GUNPOWDER),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.REDSTONE),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.GLOWSTONE_DUST));

        addMobInfo(EntityType.GUARDIAN,
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get()),
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, Items.PRISMARINE_SHARD),
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, Items.PRISMARINE_CRYSTALS));

        addMobInfo(EntityType.ELDER_GUARDIAN,
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, ItemInit.RAW_MONSTER_MEAT.get(), ItemInit.COOKED_MONSTER_MEAT.get()),
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, Items.PRISMARINE_SHARD),
                m(0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, Items.PRISMARINE_CRYSTALS));

        addMobInfo(EntityType.PHANTOM,
                m(0F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.PHANTOM_MEMBRANE),
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.BONE));

        addMobInfo(EntityType.WARDEN,
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SCULK),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SCULK_CATALYST),
                m(0.4F, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SCULK_SENSOR));

        addMobInfo(EntityType.WITHER,
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.SOUL_SAND),
                m(0F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.NETHER_STAR),
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.WITHER_SKELETON_SKULL));

        addMobInfo(EntityType.GIANT,
                m(0, 0.7F, 2, 1, 3, 1, 1, 1, 1, 1, 1, Items.ROTTEN_FLESH),
                m(0F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Items.BONE));
    }

    public static float mobGrowthChance(Entity entity) {
        return mobItemlessValue(entity, 0);
    }

    public static float mobTwinsChance(Entity entity) {
        return mobItemlessValue(entity, 1);
    }

    private static float mobItemlessValue(Entity entity, int itemlessIndex) {
        List<MobBiomeMultipliers> entries = MOB_INFO_BY_MOB.get(entity.getType()).drops();

        if (entries == null) {
            return -1F;
        }

        int found = 0;

        for (MobBiomeMultipliers entry : entries) {
            if (!entry.hasItem()) {
                if (found == itemlessIndex) {
                    return valueForBiome(entity, entry);
                }

                found++;
            }
        }

        return -1F;
    }

    public static float mobAmountForItem(Entity entity, Item item) {
        List<MobBiomeMultipliers> entries = MOB_INFO_BY_MOB.get(entity.getType()).drops();

        if (entries == null) {
            return -1F;
        }

        for (MobBiomeMultipliers entry : entries) {
            if (entry.hasItem(item)) {
                return valueForBiome(entity, entry);
            }
        }

        return -1F;
    }

    public static List<MobBiomeMultipliers> mobDropEntries(Entity entity) {
        List<MobBiomeMultipliers> entries = MOB_INFO_BY_MOB.get(entity.getType()).drops();

        if (entries == null) {
            return List.of();
        }

        return entries.stream()
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

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return entry.basaltDeltas();
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return entry.crimsonForest();
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return entry.warpedForest();
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return entry.soulSandValley();
        }

        if (biome.is(ModTags.Biomes.plainsBiomes)) {
            return entry.plains();
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
        put(map, ModTags.Biomes.mountainBiomes, mob, multipliers.mountains());
        put(map, ModTags.Biomes.mushroomCaves, mob, multipliers.mushroomCaves());
        put(map, ModTags.Biomes.desertBiomes, mob, multipliers.desert());
        put(map, ModTags.Biomes.deepDarkBiomes, mob, multipliers.deepDark());
        put(map, ModTags.Biomes.basaltDeltasBiomes, mob, multipliers.basaltDeltas());
        put(map, ModTags.Biomes.crimsonForestBiomes, mob, multipliers.crimsonForest());
        put(map, ModTags.Biomes.warpedForestBiomes, mob, multipliers.warpedForest());
        put(map, ModTags.Biomes.soulSandValleyBiomes, mob, multipliers.soulSandValley());
        put(map, ModTags.Biomes.plainsBiomes, mob, multipliers.plains());
    }

    private static void addDropMap(EntityType<?> mob, MobBiomeMultipliers multipliers) {
        if (multipliers.item() != null) {
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map = MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB.computeIfAbsent(multipliers.item(), item -> {
                Map<TagKey<Biome>, Map<EntityType<?>, Float>> newMap = new HashMap<>();
                ensureAllTags(newMap);
                return newMap;
            });
            addToMap(map, mob, multipliers);
        }

        if (multipliers.cookedItem() != null) {
            Map<TagKey<Biome>, Map<EntityType<?>, Float>> map = MOB_DROP_AMOUNT_BY_ITEM_BY_TAG_BY_MOB.computeIfAbsent(multipliers.cookedItem(), item -> {
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

        List<MobBiomeMultipliers> dropList = Arrays.asList(Arrays.copyOfRange(rows, dropStart, rows.length));
        MOB_INFO_BY_MOB.put(mob, new MobInfo(aging, breeding, dropList));

        for (MobBiomeMultipliers drop : dropList) {
            addDropMap(mob, drop);
        }
    }

    public record MobInfo(
            MobBiomeMultipliers aging,
            MobBiomeMultipliers breeding,
            List<MobBiomeMultipliers> drops
    ) {}

    public record MobBiomeMultipliers(
            float tropical,
            float savanna,
            float mountains,
            float mushroomCaves,
            float desert,
            float deepDark,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float plains,
            Item item,
            Item cookedItem
    ) {
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
    }
}
