package net.amurdza.examplemod.config;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Initial economy tuning for the content selected by the six dimension documents. */
public final class DocumentedContentBalance {
    private DocumentedContentBalance() { }

    public static void registerGrowth() {
        growth(GameplayDimensions.TROPICAL, 1.0F, "pineapple_delight:pineapple_crop",
                "hearthandharvest:corn_stalk", "hearthandharvest:cotton", "legumedelight:beans",
                "legumedelight:budding_beans", "legumedelight:beans_on_rope");
        growth(GameplayDimensions.TROPICAL, 0.55F, "upgrade_aquatic:mulberry_vine");
        growth(GameplayDimensions.SAVANNA, 0.5F, "vintagedelight:oat_crop", "farmersdelight:cabbages");
        growth(GameplayDimensions.PLAINS, 0.34F, "farmersdelight:cabbages");
        growth(GameplayDimensions.DESERT, 0.2F, "vintagedelight:peanut_crop");
        growth(GameplayDimensions.DESERT, 0.15F, "collectorsreap:pink_dragon_fruits",
                "collectorsreap:budding_pink_dragon_fruits");
        growth(GameplayDimensions.DESERT, 0.28F, "hybrid_aquatic:bull_kelp", "hybrid_aquatic:bull_kelp_plant");
        growth(GameplayDimensions.SNOWY, 0.6F, "twilightforest:mushgloom");
        growth(GameplayDimensions.SNOWY, 0.14F, "minecraft:poplar_sapling");
        growth(GameplayDimensions.NETHER, 0.1F, "mynethersdelight:powdery_cane", "mynethersdelight:powdery_cannon");
    }

    private static void growth(ResourceKey<Level> dimension, float rate, String... ids) {
        for (String id : ids) {
            Block block = block(id);
            if (block != null) BlockConfig.addToMap(dimension, rate, block);
        }
    }

    public static void registerCropDrops() {
        crop("hearthandharvest:cotton", GameplayDimensions.TROPICAL,
                "hearthandharvest:cotton", 3, "hearthandharvest:cotton_seeds", 2);
        crop("vintagedelight:oat_crop", GameplayDimensions.SAVANNA,
                "vintagedelight:raw_oats", 3, "vintagedelight:oat_seeds", 2);
        crop("vintagedelight:peanut_crop", GameplayDimensions.DESERT,
                "vintagedelight:peanut", 3, "vintagedelight:peanut", 1);
        crop("farmersdelight:cabbages", GameplayDimensions.SAVANNA,
                "farmersdelight:cabbage", 2, "farmersdelight:cabbage_seeds", 3);
        crop("farmersdelight:cabbages", GameplayDimensions.PLAINS,
                "farmersdelight:cabbage", 1, "farmersdelight:cabbage_seeds", 2);
        // Multi-block corn, beans, fruit bushes and pineapple retain their native harvesting
        // and seed returns. Their growth receives the regional rate without replacing loot.
    }

    private static void crop(String blockId, ResourceKey<Level> dimension, String produceId,
                             float yield, String seedId, float seedYield) {
        Block block = block(blockId);
        Item produce = item(produceId);
        Item seed = item(seedId);
        if (!(block instanceof CropBlock) || produce == null || seed == null) return;
        BlockConfig.addLoot(block,
                new BlockConfig.BlockLootEntry(dimension, produce, state -> mature(state) ? yield : 0, false),
                new BlockConfig.BlockLootEntry(dimension, seed, state -> mature(state) ? seedYield : 1, false));
    }

    public static boolean mature(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                return state.getValue(age).equals(age.getPossibleValues().stream().max(Integer::compareTo).orElse(0));
            }
        }
        return false;
    }

    public static void registerMobs() {
        // Columns follow MobConfig: tropical, savanna, plains, snow, desert, deep dark,
        // soul sand valley, warped forest, crimson forest, basalt deltas.
        livestock("biodiverse:capybara", 0, 1.5F, 1.25F, "minecraft:beef", "minecraft:cooked_beef", 3, 0.5F);
        livestock("biodiverse:cassowary", 0, 1.5F, 1.25F,
                "bountiful_critters:pheasant", "bountiful_critters:cooked_pheasant", 2, 0);
        livestock("aoemod:arthropleura", 0, 1, 1.25F,
                "untamedwilds:food_pachyderm_raw", "untamedwilds:food_pachyderm_cooked", 4, 0);
        livestock("biodiverse:wildebeest", 1, 2, 1.5F, "minecraft:beef", "minecraft:cooked_beef", 5, 2);
        livestock("biodiverse:oryx", 1, 2, 1.5F, "wildernature:venison", "wildernature:cooked_venison", 4, 1);
        livestock("alexsmobs:bison", 1, 2, 1.5F, "wildernature:bison_meat", "wildernature:cooked_bison_meat", 6, 2);
        livestock("alexsmobs:emu", 1, 2, 1.5F, "wildernature:cassowary_meat", "wildernature:cooked_cassowary_meat", 3, 0);
        livestock("alexsmobs:kangaroo", 1, 2, 1.5F, "alexsmobs:kangaroo_meat", "alexsmobs:cooked_kangaroo_meat", 3, 1);
        livestock("twilightforest:boar", 1, 2, 1.5F, "minecraft:porkchop", "minecraft:cooked_porkchop", 4, 0);
        livestock("environmental:reindeer", 3, 0.8F, 1, "biodiverse:raw_venison", "biodiverse:cooked_venison", 3, 1);
        livestock("biodiverse:reindeer", 3, 0.8F, 1, "biodiverse:raw_venison", "biodiverse:cooked_venison", 3, 1);
        livestock("alexsmobs:jerboa", 4, 0.6F, 1, "untamedwilds:food_turtle_raw", "untamedwilds:food_turtle_cooked", 1, 0);
        livestock("outer_end:stalker", 5, 0.5F, 1, "outer_end:stalker_meat", "outer_end:cooked_stalker_meat", 2, 0);

        // Decorative and working animals gain no meat/resource reward by default.
        working("wildernature:flamingo", 0, 1.25F);
        working("alexsmobs:anaconda", 0, 0.75F);
        working("alexsmobs:elephant", 0, 0.5F);
        working("alexsmobs:laviathan", 9, 0.4F);

        fish("angling:mahi_mahi", "aoemod:raw_mahi_mahi", "aoemod:cooked_mahi_mahi", 3, 0);
        fish("angling:anomalocaris", "aoemod:raw_anomalocaris", "aoemod:cooked_anomalocaris", 2, 0);
        fish("finsandtails:rubber_belly_glider", "aoemod:raw_rubber_belly_glider", "aoemod:cooked_rubber_belly_glider", 3, 0);
        fish("alexsmobs:flying_fish", "aoemod:raw_flying_fish", "aoemod:cooked_flying_fish", 2, 3);
        fish("angling:anglerfish", "aoemod:raw_anglerfish", "aoemod:cooked_anglerfish", 2, 3);
        fish("aquatica:isopod", "aoemod:raw_isopod", "aoemod:cooked_isopod", 2, 3);
        fish("alexsmobs:triops", "aoemod:raw_triops", "aoemod:cooked_triops", 1, 4);
        fish("ambientadditions:blue_spotted_stingray", "aoemod:raw_blue_spotted_stingray", "aoemod:cooked_blue_spotted_stingray", 1, 4);
        fish("upgrade_aquatic:lionfish", "upgrade_aquatic:lionfish", "upgrade_aquatic:cooked_lionfish", 2, 0);
        fish("hybrid_aquatic:lightfoot_crab", "hybrid_aquatic:lightfoot_crab_claw", "hybrid_aquatic:cooked_crab", 2, 0);
        fish("hybrid_aquatic:octopus", "aoemod:raw_octopus", "aoemod:cooked_octopus", 3, 0);
        fish("hybrid_aquatic:trout", "hybrid_aquatic:trout", "aoemod:cooked_trout", 2, 0);
        fish("collectorsreap:tiger_prawn", "collectorsreap:tiger_prawn", "aoemod:cooked_giant_river_prawn", 2, 0);
        fish("upgrade_aquatic:perch", "upgrade_aquatic:perch", "upgrade_aquatic:cooked_perch", 1, 1, 2);
        fish("hybrid_aquatic:carp", "hybrid_aquatic:carp", "aoemod:cooked_carp", 1.5F, 1, 2);
        fish("finsandtails:swamp_mucker", "finsandtails:swamp_mucker", "aoemod:cooked_swamp_mucker", 1.5F, 1, 2);
        fish("bountiful_critters:krill", "bountiful_critters:raw_krill", "bountiful_critters:fried_krill", 2, 3);
        fish("hybrid_aquatic:arrow_squid", "aoemod:raw_arrow_squid", "aoemod:cooked_arrow_squid", 3, 3);
        fish("hybrid_aquatic:vampire_squid", "aoemod:raw_vampire_squid", "aoemod:cooked_vampire_squid", 3, 3);
        fish("hybrid_aquatic:needlefish", "hybrid_aquatic:needlefish", "aoemod:cooked_needlefish", 2, 3);
        fish("aoemod:swordfish", "aoemod:raw_swordfish", "aoemod:cooked_swordfish", 3, 3);
        fish("hybrid_aquatic:tuna", "hybrid_aquatic:tuna", "aoemod:cooked_tuna", 3, 3);
        fish("hybrid_aquatic:dragonfish", "hybrid_aquatic:dragonfish", "aoemod:cooked_dragonfish", 2, 3);
        fish("alexscaves:lanternfish", "alexscaves:lanternfish", "alexscaves:cooked_lanternfish", 2, 3);
        fish("alexscaves:tripodfish", "alexscaves:tripodfish", "alexscaves:cooked_tripodfish", 2, 3);
        fish("hybrid_aquatic:spider_crab", "aoemod:raw_spider_crab_leg", "aoemod:cooked_spider_crab_leg", 3, 3);
        fish("finsandtails:golden_river_ray", "finsandtails:raw_golden_river_ray_wing", "finsandtails:golden_river_ray_wing_fillet", 1, 4);
        fish("hybrid_aquatic:hound_shark", "aoemod:raw_hound_shark", "aoemod:cooked_hound_shark", 2, 4);
        fish("ambientadditions:shame_faced_crab", "aoemod:raw_shame_faced_crab", "aoemod:cooked_shame_faced_crab", 1, 4);
    }

    private static void working(String id, int region, float aging) {
        EntityType<?> type = entity(id);
        if (type != null && !MobConfig.MOB_INFO_BY_MOB.containsKey(type)) {
            MobConfig.addMobInfo(type, row(region, aging, null, null), row(region, 1, null, null));
        }
    }

    private static void livestock(String id, int region, float aging, float offspring,
                                  String rawId, String cookedId, float meat, float leather) {
        EntityType<?> type = entity(id);
        Item raw = item(rawId);
        if (type == null || raw == null) return;
        List<MobConfig.MobBiomeMultipliers> rows = new ArrayList<>();
        rows.add(row(region, aging, null, null));
        rows.add(row(region, offspring, null, null));
        rows.add(row(region, meat, raw, item(cookedId)));
        if (leather > 0) rows.add(row(region, leather, Items.LEATHER, null));
        MobConfig.addMobInfo(type, rows.toArray(MobConfig.MobBiomeMultipliers[]::new));
    }

    private static void fish(String id, String rawId, String cookedId, float yield, int... regions) {
        EntityType<?> type = entity(id);
        Item raw = item(rawId);
        if (type == null || raw == null) return;
        float[] yields = new float[10];
        for (int region : regions) yields[region] = yield;
        MobConfig.addMobInfo(type, row(yields, raw, item(cookedId)));
    }

    private static MobConfig.MobBiomeMultipliers row(int region, float value, Item raw, Item cooked) {
        float[] values = new float[10];
        values[region] = value;
        return row(values, raw, cooked);
    }

    private static MobConfig.MobBiomeMultipliers row(float[] v, Item raw, Item cooked) {
        return new MobConfig.MobBiomeMultipliers(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], raw, cooked);
    }

    private static EntityType<?> entity(String id) {
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
    }

    private static Block block(String id) {
        Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return value == Blocks.AIR ? null : value;
    }

    private static Item item(String id) {
        if (id == null) return null;
        Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return value == Items.AIR ? null : value;
    }
}
