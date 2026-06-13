package net.amurdza.examplemod.config;

import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BlockConfig {
    private BlockConfig() {}

    public static final Map<TagKey<Biome>, Map<Block, Float>> BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG = new HashMap<>();

    public static final Map<TagKey<Biome>, Map<Item, Map<Block, List<BlockLootEntry>>>> BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG = new HashMap<>();

    public static final Map<Block, BlockInfo> BLOCK_INFO_BY_BLOCK = new HashMap<>();

    public static void init() {
        ensureAllGrowthTags(BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG);
        ensureAllLootTags(BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG);
        registerBlockInfo();
    }

    public static float blockGrowthChance(Level level, BlockPos pos, BlockState state) {
        BlockInfo info = BLOCK_INFO_BY_BLOCK.get(state.getBlock());

        if (info == null || info.growth().isEmpty()) {
            return 0.0F;
        }

        Holder<Biome> biome = level.getBiome(pos);
        float total = 0.0F;

        for (BlockGrowthEntry entry : info.growth()) {
            if (biome.is(entry.biomeTag())) {
                total += entry.value();
            }
        }

        return total;
    }

    public static List<BlockLootEntry> blockDropEntries(Level level, BlockPos pos, BlockState state) {
        BlockInfo info = BLOCK_INFO_BY_BLOCK.get(state.getBlock());

        if (info == null || info.drops().isEmpty()) {
            return List.of();
        }

        Holder<Biome> biome = level.getBiome(pos);

        return info.drops().stream()
                .filter(entry -> biome.is(entry.biomeTag()))
                .toList();
    }

    public static float blockDropAmount(BlockState state, BlockLootEntry entry) {
        return entry.dropAmount().apply(state);
    }

    private static void addBlockInfo(
            Block block,
            List<BlockGrowthEntry> growthRows,
            BlockLootEntry... drops
    ) {
        BlockInfo oldInfo = BLOCK_INFO_BY_BLOCK.get(block);

        List<BlockGrowthEntry> growthList = new ArrayList<>();
        List<BlockLootEntry> dropList = new ArrayList<>();

        if (oldInfo != null) {
            growthList.addAll(oldInfo.growth());
            dropList.addAll(oldInfo.drops());
        }

        growthList.addAll(growthRows);
        dropList.addAll(Arrays.asList(drops));

        BLOCK_INFO_BY_BLOCK.put(block, new BlockInfo(List.copyOf(growthList), List.copyOf(dropList)));

        for (BlockGrowthEntry growth : growthRows) {
            addGrowthMap(block, growth);
        }

        for (BlockLootEntry drop : drops) {
            addDropMap(block, drop);
        }
    }

    private static void addGrowth(Block block, BlockGrowthEntry... growthRows) {
        addBlockInfo(block, List.of(growthRows));
    }

    private static void addLoot(Block block, BlockLootEntry... drops) {
        addBlockInfo(block, List.of(), drops);
    }

    private static void addGrowthMap(Block block, BlockGrowthEntry entry) {
        if (entry.value() == 0.0F) {
            return;
        }

        BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG
                .computeIfAbsent(entry.biomeTag(), tag -> new HashMap<>())
                .merge(block, entry.value(), Float::sum);
    }

    private static void addDropMap(Block block, BlockLootEntry entry) {
        BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG
                .computeIfAbsent(entry.biomeTag(), tag -> new HashMap<>())
                .computeIfAbsent(entry.item(), item -> new HashMap<>())
                .computeIfAbsent(block, b -> new ArrayList<>())
                .add(entry);
    }

    private static void ensureAllGrowthTags(Map<TagKey<Biome>, Map<Block, Float>> map) {
        ensureGrowthTag(map, ModTags.Biomes.tropicalBiomes);
        ensureGrowthTag(map, ModTags.Biomes.savannaBiomes);
        ensureGrowthTag(map, ModTags.Biomes.mountainBiomes);
        ensureGrowthTag(map, ModTags.Biomes.mushroomCaves);
        ensureGrowthTag(map, ModTags.Biomes.desertBiomes);
        ensureGrowthTag(map, ModTags.Biomes.deepDarkBiomes);
        ensureGrowthTag(map, ModTags.Biomes.basaltDeltasBiomes);
        ensureGrowthTag(map, ModTags.Biomes.crimsonForestBiomes);
        ensureGrowthTag(map, ModTags.Biomes.warpedForestBiomes);
        ensureGrowthTag(map, ModTags.Biomes.soulSandValleyBiomes);
        ensureGrowthTag(map, ModTags.Biomes.plainsBiomes);
    }

    private static void ensureAllLootTags(Map<TagKey<Biome>, Map<Item, Map<Block, List<BlockLootEntry>>>> map) {
        ensureLootTag(map, ModTags.Biomes.tropicalBiomes);
        ensureLootTag(map, ModTags.Biomes.savannaBiomes);
        ensureLootTag(map, ModTags.Biomes.mountainBiomes);
        ensureLootTag(map, ModTags.Biomes.mushroomCaves);
        ensureLootTag(map, ModTags.Biomes.desertBiomes);
        ensureLootTag(map, ModTags.Biomes.deepDarkBiomes);
        ensureLootTag(map, ModTags.Biomes.basaltDeltasBiomes);
        ensureLootTag(map, ModTags.Biomes.crimsonForestBiomes);
        ensureLootTag(map, ModTags.Biomes.warpedForestBiomes);
        ensureLootTag(map, ModTags.Biomes.soulSandValleyBiomes);
        ensureLootTag(map, ModTags.Biomes.plainsBiomes);
    }

    private static void ensureGrowthTag(Map<TagKey<Biome>, Map<Block, Float>> map, TagKey<Biome> tag) {
        map.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static void ensureLootTag(Map<TagKey<Biome>, Map<Item, Map<Block, List<BlockLootEntry>>>> map, TagKey<Biome> tag) {
        map.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static BlockGrowthEntry g(TagKey<Biome> biomeTag, float value) {
        return new BlockGrowthEntry(biomeTag, value);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item) {
        return new BlockLootEntry(biomeTag, item, state -> 1.0F, false);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item, boolean affectedByFortune) {
        return new BlockLootEntry(biomeTag, item, state -> 1.0F, affectedByFortune);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item, float amount) {
        return new BlockLootEntry(biomeTag, item, state -> amount, false);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item, float amount, boolean affectedByFortune) {
        return new BlockLootEntry(biomeTag, item, state -> amount, affectedByFortune);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item, Function<BlockState, Float> dropAmount) {
        return new BlockLootEntry(biomeTag, item, dropAmount, false);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item, Function<BlockState, Float> dropAmount, boolean affectedByFortune) {
        return new BlockLootEntry(biomeTag, item, dropAmount, affectedByFortune);
    }

    private static void registerBlockInfo() {
        registerVanillaGrowthBlocks();
        registerVanillaOres();
        registerVanillaLeaves();
        registerVanillaCrops();
        registerVanillaPlants();
    }

    private static void registerVanillaGrowthBlocks() {
        addGrowth(Blocks.KELP,
                g(ModTags.Biomes.tropicalBiomes, 0.7F),
                g(ModTags.Biomes.savannaBiomes, 0.28F),
                g(ModTags.Biomes.mountainBiomes, 0.7F),
                g(ModTags.Biomes.mushroomCaves, 0.7F)
        );

        addGrowth(Blocks.KELP_PLANT,
                g(ModTags.Biomes.tropicalBiomes, 0.7F),
                g(ModTags.Biomes.savannaBiomes, 0.28F),
                g(ModTags.Biomes.mountainBiomes, 0.7F),
                g(ModTags.Biomes.mushroomCaves, 0.7F)
        );

        addGrowth(Blocks.WEEPING_VINES,
                g(ModTags.Biomes.crimsonForestBiomes, 0.1F)
        );

        addGrowth(Blocks.WEEPING_VINES_PLANT,
                g(ModTags.Biomes.crimsonForestBiomes, 0.1F)
        );

        addGrowth(Blocks.TWISTING_VINES,
                g(ModTags.Biomes.warpedForestBiomes, 0.1F)
        );

        addGrowth(Blocks.TWISTING_VINES_PLANT,
                g(ModTags.Biomes.warpedForestBiomes, 0.1F)
        );

        addGrowth(Blocks.NETHER_WART,
                g(ModTags.Biomes.soulSandValleyBiomes, 0.1F)
        );

        addGrowth(Blocks.BUDDING_AMETHYST,
                g(ModTags.Biomes.mountainBiomes, 0.6F),
                g(ModTags.Biomes.mushroomCaves, 0.6F)
        );

        addGrowth(Blocks.BROWN_MUSHROOM,
                g(ModTags.Biomes.tropicalBiomes, 0.6F),
                g(ModTags.Biomes.savannaBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.6F),
                g(ModTags.Biomes.plainsBiomes, 0.2F)
        );

        addGrowth(Blocks.RED_MUSHROOM,
                g(ModTags.Biomes.tropicalBiomes, 0.6F),
                g(ModTags.Biomes.savannaBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.6F),
                g(ModTags.Biomes.plainsBiomes, 0.2F)
        );

        addGrowth(Blocks.CRIMSON_FUNGUS,
                g(ModTags.Biomes.crimsonForestBiomes, 0.07F)
        );

        addGrowth(Blocks.WARPED_FUNGUS,
                g(ModTags.Biomes.warpedForestBiomes, 0.07F)
        );

        addGrowth(Blocks.CHORUS_FLOWER,
                g(ModTags.Biomes.soulSandValleyBiomes, 0.1F)
        );

        addGrowth(Blocks.POINTED_DRIPSTONE,
                g(ModTags.Biomes.mountainBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.2F)
        );

        addGrowth(Blocks.SWEET_BERRY_BUSH,
                g(ModTags.Biomes.tropicalBiomes, 0.6F),
                g(ModTags.Biomes.mountainBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.2F)
        );

        addGrowth(Blocks.COCOA,
                g(ModTags.Biomes.tropicalBiomes, 0.6F)
        );

        addGrowth(Blocks.CACTUS,
                g(ModTags.Biomes.tropicalBiomes, 8.0F),
                g(ModTags.Biomes.desertBiomes, 4.0F)
        );

        addGrowth(Blocks.MELON_STEM,
                g(ModTags.Biomes.tropicalBiomes, 1.0F)
        );

        addGrowth(Blocks.PUMPKIN_STEM,
                g(ModTags.Biomes.tropicalBiomes, 1.0F),
                g(ModTags.Biomes.savannaBiomes, 0.5F),
                g(ModTags.Biomes.plainsBiomes, 0.34F)
        );

        addGrowth(Blocks.BEETROOTS,
                g(ModTags.Biomes.tropicalBiomes, 0.5F),
                g(ModTags.Biomes.mountainBiomes, 0.06F)
        );

        addGrowth(Blocks.WHEAT,
                g(ModTags.Biomes.tropicalBiomes, 1.0F),
                g(ModTags.Biomes.savannaBiomes, 0.5F),
                g(ModTags.Biomes.plainsBiomes, 0.34F)
        );

        addGrowth(Blocks.CARROTS,
                g(ModTags.Biomes.tropicalBiomes, 1.0F),
                g(ModTags.Biomes.savannaBiomes, 0.5F),
                g(ModTags.Biomes.plainsBiomes, 0.34F)
        );

        addGrowth(Blocks.POTATOES,
                g(ModTags.Biomes.tropicalBiomes, 1.0F),
                g(ModTags.Biomes.savannaBiomes, 0.5F),
                g(ModTags.Biomes.plainsBiomes, 0.34F)
        );

        addGrowth(Blocks.CAVE_VINES,
                g(ModTags.Biomes.tropicalBiomes, 0.55F)
        );

        addGrowth(Blocks.CAVE_VINES_PLANT,
                g(ModTags.Biomes.tropicalBiomes, 0.55F)
        );

        addGrowth(Blocks.VINE,
                g(ModTags.Biomes.tropicalBiomes, 0.75F)
        );

        addGrowth(Blocks.TURTLE_EGG,
                g(ModTags.Biomes.tropicalBiomes, 0.2F)
        );

        addGrowth(Blocks.SEA_PICKLE,
                g(ModTags.Biomes.tropicalBiomes, 1.0F),
                g(ModTags.Biomes.savannaBiomes, 0.4F),
                g(ModTags.Biomes.mountainBiomes, 0.7F)
        );

        addGrowth(Blocks.BAMBOO,
                g(ModTags.Biomes.tropicalBiomes, 1.0F)
        );

        addGrowth(Blocks.BAMBOO_SAPLING,
                g(ModTags.Biomes.tropicalBiomes, 1.0F)
        );

        addGrowth(Blocks.SPRUCE_SAPLING,
                g(ModTags.Biomes.mountainBiomes, 0.14F)
        );

        addGrowth(Blocks.ACACIA_SAPLING,
                g(ModTags.Biomes.savannaBiomes, 0.22F)
        );

        addGrowth(Blocks.OAK_SAPLING,
                g(ModTags.Biomes.tropicalBiomes, 0.43F),
                g(ModTags.Biomes.plainsBiomes, 0.14F)
        );

        addGrowth(Blocks.BIRCH_SAPLING,
                g(ModTags.Biomes.plainsBiomes, 0.14F)
        );

        addGrowth(Blocks.CHERRY_SAPLING,
                g(ModTags.Biomes.tropicalBiomes, 0.43F)
        );

        addGrowth(Blocks.MANGROVE_PROPAGULE,
                g(ModTags.Biomes.tropicalBiomes, 0.43F)
        );

        addGrowth(Blocks.DARK_OAK_SAPLING,
                g(ModTags.Biomes.tropicalBiomes, 0.14F)
        );

        addGrowth(Blocks.JUNGLE_SAPLING,
                g(ModTags.Biomes.tropicalBiomes, 0.56F)
        );
    }

    private static void registerVanillaOres() {
        // Coal: separate because you said coal is the exception.
        addOre(Blocks.COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 1.0F, true);
        addOre(Blocks.COAL_ORE, ModTags.Biomes.mushroomCaves, Items.COAL, 1.0F, true);
        addOre(Blocks.COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1.0F, true);
        addOreDesertAndDeepDark(Blocks.COAL_ORE, Items.COAL, 1.0F, true);

        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 1.0F, true);
        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.mushroomCaves, Items.COAL, 1.0F, true);
        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1.0F, true);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COAL_ORE, Items.COAL, 1.0F, true);

        // Copper
        addOreMountainsAndMushroomCaves(Blocks.COPPER_ORE, Items.RAW_COPPER, 3.0F, true);
        addOre(Blocks.COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3.0F, true);
        addOreDesertAndDeepDark(Blocks.COPPER_ORE, Items.RAW_COPPER, 3.0F, true);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3.0F, true);
        addOre(Blocks.DEEPSLATE_COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3.0F, true);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3.0F, true);

        // Gold
        addOreMountainsAndMushroomCaves(Blocks.GOLD_ORE, Items.RAW_GOLD, 1.0F, false);
        addOre(Blocks.GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1.0F, false);
        addOreDesertAndDeepDark(Blocks.GOLD_ORE, Items.RAW_GOLD, 1.0F, false);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 1.0F, false);
        addOre(Blocks.DEEPSLATE_GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1.0F, false);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 1.0F, false);

        // Iron: mountains, mushroom caves, plains only.
        addOreMountainsAndMushroomCaves(Blocks.IRON_ORE, Items.RAW_IRON, 1.0F, false);
        addOre(Blocks.IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1.0F, false);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_IRON_ORE, Items.RAW_IRON, 1.0F, false);
        addOre(Blocks.DEEPSLATE_IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1.0F, false);

        // Lapis: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.LAPIS_ORE, Items.LAPIS_LAZULI, 6.5F, true);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_LAPIS_ORE, Items.LAPIS_LAZULI, 6.5F, true);

        // Diamond: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.DIAMOND_ORE, Items.DIAMOND, 1.0F, true);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND, 1.0F, true);

        // Emerald: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.EMERALD_ORE, Items.EMERALD, 1.0F, true);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_EMERALD_ORE, Items.EMERALD, 1.0F, true);

        // Redstone: desert and deep dark only.
        addOreDesertAndDeepDark(Blocks.REDSTONE_ORE, Items.REDSTONE, 4.5F, true);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE, 4.5F, true);

        // Nether ores: each nether biome gets its own separate call.
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 1.0F, true);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.crimsonForestBiomes, Items.QUARTZ, 1.0F, true);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.warpedForestBiomes, Items.QUARTZ, 1.0F, true);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.soulSandValleyBiomes, Items.QUARTZ, 1.0F, true);

        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_NUGGET, 4F, true);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.crimsonForestBiomes, Items.GOLD_NUGGET, 1.5F, true);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5F, true);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1.5F, true);

        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_NUGGET, 4F, true);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_NUGGET, 1.5F, true);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5F, true);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1.5F, true);

        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_NUGGET, 4F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_NUGGET, 1.5F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1.5F, true);

        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_NUGGET, 4F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_NUGGET, 1.5F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5F, true);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1.5F, true);

        
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.basaltDeltasBiomes, Items.ANCIENT_DEBRIS, 1.0F, false);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.crimsonForestBiomes, Items.ANCIENT_DEBRIS, 1.0F, false);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.warpedForestBiomes, Items.ANCIENT_DEBRIS, 1.0F, false);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.soulSandValleyBiomes, Items.ANCIENT_DEBRIS, 1.0F, false);
    }

    private static void registerVanillaLeaves() {
        addLoot(Blocks.SPRUCE_LEAVES,
                leaf(ModTags.Biomes.mountainBiomes, Items.SPRUCE_SAPLING),
                leaf(ModTags.Biomes.mushroomCaves, Items.SPRUCE_SAPLING),
                stick(ModTags.Biomes.mountainBiomes),
                stick(ModTags.Biomes.mushroomCaves)
        );

        addLoot(Blocks.ACACIA_LEAVES,
                leaf(ModTags.Biomes.savannaBiomes, Items.ACACIA_SAPLING),
                stick(ModTags.Biomes.savannaBiomes)
        );

        addLoot(Blocks.BIRCH_LEAVES,
                leaf(ModTags.Biomes.plainsBiomes, Items.BIRCH_SAPLING),
                stick(ModTags.Biomes.plainsBiomes)
        );

        addLoot(Blocks.OAK_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.OAK_SAPLING),
                leaf(ModTags.Biomes.plainsBiomes, Items.OAK_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                stick(ModTags.Biomes.plainsBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE),
                fruit(ModTags.Biomes.plainsBiomes,Items.APPLE)
        );

        addLoot(Blocks.DARK_OAK_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.DARK_OAK_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.CHERRY_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.CHERRY_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.MANGROVE_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.MANGROVE_PROPAGULE),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.AZALEA_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.AZALEA),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.FLOWERING_AZALEA_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.FLOWERING_AZALEA),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.JUNGLE_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.JUNGLE_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes,Items.APPLE)
        );

        addLoot(Blocks.WARPED_WART_BLOCK,
                drop(ModTags.Biomes.desertBiomes, Items.WARPED_FUNGUS,0.015f),
                drop(ModTags.Biomes.deepDarkBiomes, Items.WARPED_FUNGUS,0.03f),
                drop(ModTags.Biomes.basaltDeltasBiomes, Items.WARPED_FUNGUS,0.02f),
                drop(ModTags.Biomes.crimsonForestBiomes, Items.WARPED_FUNGUS,0.02f),
                drop(ModTags.Biomes.warpedForestBiomes, Items.WARPED_FUNGUS,0.12f),
                drop(ModTags.Biomes.soulSandValleyBiomes, Items.WARPED_FUNGUS,0.06f)
        );

        addLoot(Blocks.NETHER_WART_BLOCK,
                drop(ModTags.Biomes.desertBiomes, Items.CRIMSON_FUNGUS,0.015f),
                drop(ModTags.Biomes.deepDarkBiomes, Items.CRIMSON_FUNGUS,0.03f),
                drop(ModTags.Biomes.basaltDeltasBiomes, Items.CRIMSON_FUNGUS,0.06f),
                drop(ModTags.Biomes.crimsonForestBiomes, Items.CRIMSON_FUNGUS,0.12f),
                drop(ModTags.Biomes.warpedForestBiomes, Items.CRIMSON_FUNGUS,0.02f),
                drop(ModTags.Biomes.soulSandValleyBiomes, Items.CRIMSON_FUNGUS,0.06f)
        );
    }

    private static BlockLootEntry leaf(TagKey<Biome> biome, Item saplingItem) {
        float val = 0.05f;
        if(biome == ModTags.Biomes.tropicalBiomes){
            val = 0.20f;
        }
        else if(biome == ModTags.Biomes.savannaBiomes){
            val = 0.075f;
        }
        return drop(biome, saplingItem, val);
    }

    private static BlockLootEntry stick(TagKey<Biome> biome) {
        float val = 0.02f;
        if(biome == ModTags.Biomes.tropicalBiomes){
            val = 0.08f;
        }
        else if(biome == ModTags.Biomes.savannaBiomes){
            val = 0.03f;
        }
        return drop(biome, Items.STICK, val);
    }

    private static BlockLootEntry fruit(TagKey<Biome> biome, Item fruit) {
        float val = 0;
        if(biome == ModTags.Biomes.tropicalBiomes){
            val = 0.02f;
        }
        else if(biome == ModTags.Biomes.plainsBiomes){
            val = 0.005f;
        }
        return drop(biome, fruit, val);
    }
    
    private static void registerVanillaCrops() {
        addLoot(Blocks.WHEAT,
                drop(ModTags.Biomes.tropicalBiomes, Items.WHEAT, matureAge7Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, Items.WHEAT_SEEDS, matureAge7Amount(5)),
                drop(ModTags.Biomes.savannaBiomes, Items.WHEAT, matureAge7Amount(1.5f,0)),
                drop(ModTags.Biomes.savannaBiomes, Items.WHEAT_SEEDS, matureAge7Amount(3.5F)),
                drop(ModTags.Biomes.plainsBiomes, Items.WHEAT, matureAge7Amount(1.0F,0)),
                drop(ModTags.Biomes.plainsBiomes, Items.WHEAT_SEEDS, matureAge7Amount(2.5F))
        );

        addLoot(Blocks.CARROTS,
                drop(ModTags.Biomes.tropicalBiomes, Items.CARROT, matureAge7Amount(8.5f)),
                drop(ModTags.Biomes.savannaBiomes, Items.CARROT, matureAge7Amount(5F)),
                drop(ModTags.Biomes.plainsBiomes, Items.CARROT, matureAge7Amount(3.5F))
        );

        addLoot(Blocks.POTATOES,
                drop(ModTags.Biomes.tropicalBiomes, Items.POTATO, matureAge7Amount(8.5f)),
                drop(ModTags.Biomes.savannaBiomes, Items.POTATO, matureAge7Amount(5F)),
                drop(ModTags.Biomes.plainsBiomes, Items.POTATO, matureAge7Amount(3.5F)),
                drop(ModTags.Biomes.plainsBiomes, Items.POISONOUS_POTATO, matureAge7Amount(0.02F,0))
        );

        addLoot(Blocks.BEETROOTS,
                drop(ModTags.Biomes.tropicalBiomes, Items.BEETROOT, matureAge3Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, Items.BEETROOT_SEEDS, matureAge3Amount(5)),
                drop(ModTags.Biomes.mountainBiomes, Items.BEETROOT, matureAge3Amount(1,0)),
                drop(ModTags.Biomes.mushroomCaves, Items.BEETROOT_SEEDS, matureAge3Amount(1.5F))
        );

        addLoot(Blocks.MELON_STEM,
                drop(ModTags.Biomes.tropicalBiomes, Items.MELON_SEEDS, matureAge7Amount(5))
        );

        addLoot(Blocks.PUMPKIN_STEM,
                drop(ModTags.Biomes.tropicalBiomes, Items.PUMPKIN_SEEDS, matureAge7Amount(5)),
                drop(ModTags.Biomes.savannaBiomes, Items.PUMPKIN_SEEDS, matureAge7Amount(3.5f)),
                drop(ModTags.Biomes.plainsBiomes, Items.PUMPKIN_SEEDS, matureAge7Amount(2.5f))
        );

        addLoot(Blocks.MELON,
                drop(ModTags.Biomes.tropicalBiomes, Items.MELON)
        );

        addLoot(Blocks.PUMPKIN,
                drop(ModTags.Biomes.tropicalBiomes, Items.PUMPKIN),
                drop(ModTags.Biomes.savannaBiomes, Items.PUMPKIN),
                drop(ModTags.Biomes.plainsBiomes, Items.PUMPKIN)
        );

        addLoot(Blocks.COCOA,
                drop(ModTags.Biomes.tropicalBiomes, Items.COCOA_BEANS, matureAge2Amount(7))
        );

        addLoot(Blocks.NETHER_WART,
                drop(ModTags.Biomes.soulSandValleyBiomes, Items.NETHER_WART, matureAge3Amount(3))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.ONION.get(), matureAge7Amount(8.5f))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.RICE.get(), matureAge7Amount(7))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.TOMATO.get(), matureAge7Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.TOMATO_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.CABBAGE.get(), matureAge7Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, ModItems.CABBAGE_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get(),
                drop(ModTags.Biomes.tropicalBiomes, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(1,0))
        );
    }

    private static Function<BlockState, Float> matureAge7Amount(float amountWhenMature) {
        return matureAge7Amount(amountWhenMature,1f);
    }

    private static Function<BlockState, Float> matureAge7Amount(float amountWhenMature, float nonMatureAmount) {
        return state -> state.getValue(BlockStateProperties.AGE_7) >= 7 ? amountWhenMature : 0.0F;
    }

    private static Function<BlockState, Float> matureAge3Amount(float amountWhenMature) {
        return matureAge3Amount(amountWhenMature,1f);
    }


    private static Function<BlockState, Float> matureAge3Amount(float amountWhenMature, float nonMatureAmount) {
        return state -> state.getValue(BlockStateProperties.AGE_3) >= 3 ? amountWhenMature : nonMatureAmount;
    }

    private static Function<BlockState, Float> matureAge2Amount(float amountWhenMature) {
        return matureAge3Amount(amountWhenMature,1f);
    }

    private static void registerVanillaPlants() {
        addSeeds(Blocks.GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(Blocks.TALL_GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes));
        addSeeds(Blocks.FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(Blocks.LARGE_FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(ModBlocks.DESERT_GRASS.get(), ModTags.Biomes.desertBiomes);
        addSeeds(ModBlocks.DESERT_TALL_GRASS.get(), ModTags.Biomes.desertBiomes);

        addLoot(Blocks.GRASS,
                plantSeed(ModTags.Biomes.tropicalBiomes),
                plantSeed(ModTags.Biomes.savannaBiomes),
                plantSeed(ModTags.Biomes.plainsBiomes),
                plantSeed(ModTags.Biomes.mountainBiomes),
                plantSeed(ModTags.Biomes.mushroomCaves)
        );

        addLoot(Blocks.TALL_GRASS,
                plantSeed(ModTags.Biomes.tropicalBiomes),
                plantSeed(ModTags.Biomes.savannaBiomes),
                plantSeed(ModTags.Biomes.plainsBiomes),
                plantSeed(ModTags.Biomes.mountainBiomes),
                plantSeed(ModTags.Biomes.mushroomCaves)
        );

        addLoot(Blocks.FERN,
                drop(ModTags.Biomes.mountainBiomes, Items.FERN),
                drop(ModTags.Biomes.mushroomCaves, Items.FERN),
                drop(ModTags.Biomes.plainsBiomes, Items.FERN)
        );

        addLoot(Blocks.LARGE_FERN,
                drop(ModTags.Biomes.mountainBiomes, Items.LARGE_FERN),
                drop(ModTags.Biomes.mushroomCaves, Items.LARGE_FERN),
                drop(ModTags.Biomes.plainsBiomes, Items.LARGE_FERN)
        );

        addLoot(Blocks.SWEET_BERRY_BUSH,
                sweetBerryAmount(ModTags.Biomes.tropicalBiomes),
                sweetBerryAmount(ModTags.Biomes.mountainBiomes),
                sweetBerryAmount(ModTags.Biomes.mushroomCaves)
        );

        addCaveVines(Blocks.CAVE_VINES_PLANT);
        addCaveVines(Blocks.CAVE_VINES);

        addLoot(Blocks.DEAD_BUSH,
                drop(ModTags.Biomes.desertBiomes, Items.STICK, 1F)
        );
    }

    private static void addCaveVines(Block block){
        addLoot(block,
                drop(ModTags.Biomes.tropicalBiomes, Items.GLOW_BERRIES, state -> state.getValue(BlockStateProperties.BERRIES) ? 1f + BlockBehaviorConfig.BIOME_TO_GLOW_BERRY_AMOUNTS.get(ModTags.Biomes.tropicalBiomes) : 1f)
        );
    }

    private static void addSeeds(Block block, List<TagKey<Biome>> biomes){
        for(TagKey<Biome> biome: biomes){
            addSeeds(block,biome);
        }
    }

    private static void addSeeds(Block block, TagKey<Biome> biome){
        float mult = block instanceof DoublePlantBlock || block == ModBlocks.DESERT_TALL_GRASS.get() ? 2 : 1;
        if(biome.equals(ModTags.Biomes.tropicalBiomes)){
            float val = 0.1f * mult;
            addLoot(block,
                    drop(biome,Items.WHEAT_SEEDS,val),
                    drop(biome,Items.BEETROOT_SEEDS,val),
                    drop(biome,Items.PUMPKIN_SEEDS,val),
                    drop(biome,Items.POTATO,val),
                    drop(biome,Items.CARROT,val),
                    drop(biome,Items.MELON_SEEDS,val),
                    drop(biome, ModItems.RICE.get(),val),
                    drop(biome, ModItems.CABBAGE_SEEDS.get(),val),
                    drop(biome, ModItems.TOMATO_SEEDS.get(),val),
                    drop(biome, ModItems.ONION.get(),val)
            );
        }
        else if (biome.equals(ModTags.Biomes.savannaBiomes)){
            addLoot(block,
                    drop(biome,Items.WHEAT_SEEDS,0.35f * mult),
                    drop(biome,Items.PUMPKIN_SEEDS,0.05f * mult),
                    drop(biome,Items.POTATO,0.05f * mult),
                    drop(biome,Items.CARROT,0.05f * mult)
            );
        }
        else if (biome.equals(ModTags.Biomes.plainsBiomes)){
            addLoot(block,
                    drop(biome,Items.WHEAT_SEEDS,0.125f * mult),
                    drop(biome,Items.PUMPKIN_SEEDS,0.025f * mult),
                    drop(biome,Items.POTATO,0.05f * mult),
                    drop(biome,Items.CARROT,0.05f * mult)
            );
        }
        else if (biome.equals(ModTags.Biomes.mountainBiomes)){
            addLoot(block,drop(biome,Items.BEETROOT_SEEDS,0.1f));
        }
        else if (biome.equals(ModTags.Biomes.mushroomCaves)){
            addLoot(block,drop(biome,Items.BEETROOT_SEEDS,0.1f));
        }
        else if (biome.equals(ModTags.Biomes.desertBiomes)){
            addLoot(block,drop(biome,Items.WHEAT_SEEDS,0.05f));
        }
    }

    private static void addOre(
            Block block,
            TagKey<Biome> biomeTag,
            Item item,
            float amount,
            boolean affectedByFortune
    ) {
        addLoot(block, drop(biomeTag, item, amount, affectedByFortune));
    }

    private static void addOreMountainsAndMushroomCaves(
            Block block,
            Item item,
            float amount,
            boolean affectedByFortune
    ) {
        addOre(block, ModTags.Biomes.mountainBiomes, item, amount, affectedByFortune);
        addOre(block, ModTags.Biomes.mushroomCaves, item, amount, affectedByFortune);
    }

    private static void addOreDesertAndDeepDark(
            Block block,
            Item item,
            float amount,
            boolean affectedByFortune
    ) {
        addOre(block, ModTags.Biomes.desertBiomes, item, amount, affectedByFortune);
        addOre(block, ModTags.Biomes.deepDarkBiomes, item, amount, affectedByFortune);
    }

    private static BlockLootEntry plantSeed(TagKey<Biome> biomeTag) {
        return drop(biomeTag, Items.WHEAT_SEEDS, 0.125F);
    }

    private static BlockLootEntry sweetBerryAmount(TagKey<Biome> biome) {
        return drop(ModTags.Biomes.tropicalBiomes, Items.SWEET_BERRIES,
                state -> {
                    int age = state.getValue(SweetBerryBushBlock.AGE);
                    if(age<2){
                        return 1f;
                    }
                    if(age==2){
                        return 1 + BlockBehaviorConfig.BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.get(biome);
                    }
                    return 1 + BlockBehaviorConfig.BIOME_TO_MATURE_BERRY_AMOUNTS.get(biome);
        });
    }

    public record BlockInfo(
            List<BlockGrowthEntry> growth,
            List<BlockLootEntry> drops
    ) {}

    public record BlockGrowthEntry(
            TagKey<Biome> biomeTag,
            float value
    ) {}

    public record BlockLootEntry(
            TagKey<Biome> biomeTag,
            Item item,
            Function<BlockState, Float> dropAmount,
            boolean affectedByFortune
    ) {
        public BlockLootEntry {
            if (dropAmount == null) {
                dropAmount = state -> 1.0F;
            }
        }
    }
}