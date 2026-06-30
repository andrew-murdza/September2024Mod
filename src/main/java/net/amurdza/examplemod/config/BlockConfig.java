package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import dev.xkmc.fruitsdelight.init.food.FruitType;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.*;
import java.util.function.Function;

public final class BlockConfig {
    private BlockConfig() {}

    public static final Map<TagKey<Biome>, Map<Block, Float>> BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG = new HashMap<>();

    public static final Map<TagKey<Biome>, Map<Item, Map<Block, List<BlockLootEntry>>>> BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG = new HashMap<>();

    public static final Map<Block, BlockInfo> BLOCK_INFO_BY_BLOCK = new HashMap<>();

    public static void init() {
        ensureAllGrowthTags();
        ensureAllLootTags();
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

    private static void ensureAllGrowthTags() {
        ensureGrowthTag(ModTags.Biomes.tropicalBiomes);
        ensureGrowthTag(ModTags.Biomes.savannaBiomes);
        ensureGrowthTag(ModTags.Biomes.mountainBiomes);
        ensureGrowthTag(ModTags.Biomes.mushroomCaves);
        ensureGrowthTag(ModTags.Biomes.desertBiomes);
        ensureGrowthTag(ModTags.Biomes.deepDarkBiomes);
        ensureGrowthTag(ModTags.Biomes.basaltDeltasBiomes);
        ensureGrowthTag(ModTags.Biomes.crimsonForestBiomes);
        ensureGrowthTag(ModTags.Biomes.warpedForestBiomes);
        ensureGrowthTag(ModTags.Biomes.soulSandValleyBiomes);
        ensureGrowthTag(ModTags.Biomes.plainsBiomes);
    }

    private static void ensureAllLootTags() {
        ensureLootTag(ModTags.Biomes.tropicalBiomes);
        ensureLootTag(ModTags.Biomes.savannaBiomes);
        ensureLootTag(ModTags.Biomes.mountainBiomes);
        ensureLootTag(ModTags.Biomes.mushroomCaves);
        ensureLootTag(ModTags.Biomes.desertBiomes);
        ensureLootTag(ModTags.Biomes.deepDarkBiomes);
        ensureLootTag(ModTags.Biomes.basaltDeltasBiomes);
        ensureLootTag(ModTags.Biomes.crimsonForestBiomes);
        ensureLootTag(ModTags.Biomes.warpedForestBiomes);
        ensureLootTag(ModTags.Biomes.soulSandValleyBiomes);
        ensureLootTag(ModTags.Biomes.plainsBiomes);
    }

    private static void ensureGrowthTag(TagKey<Biome> tag) {
        BlockConfig.BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static void ensureLootTag(TagKey<Biome> tag) {
        BlockConfig.BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static BlockGrowthEntry g(TagKey<Biome> biomeTag, float value) {
        return new BlockGrowthEntry(biomeTag, value);
    }

    private static BlockLootEntry drop(TagKey<Biome> biomeTag, Item item) {
        return new BlockLootEntry(biomeTag, item, state -> 1.0F, false);
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
                g(ModTags.Biomes.mountainBiomes, 0.2F),
                g(ModTags.Biomes.desertBiomes, 0.1F),
                g(ModTags.Biomes.plainsBiomes, 0.2F)
        );

        addGrowth(Blocks.RED_MUSHROOM,
                g(ModTags.Biomes.tropicalBiomes, 0.6F),
                g(ModTags.Biomes.savannaBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.6F),
                g(ModTags.Biomes.mountainBiomes, 0.2F),
                g(ModTags.Biomes.desertBiomes, 0.1F),
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

        addGrowth(ModBlocks.BLUE_BERRY_BUSH.get(),
                g(ModTags.Biomes.tropicalBiomes, 0.6F),
                g(ModTags.Biomes.mountainBiomes, 0.2F),
                g(ModTags.Biomes.mushroomCaves, 0.2F)
        );

        addGrowth(ModBlocks.ASHEN_WHEAT_CROP.get(),
                g(ModTags.Biomes.soulSandValleyBiomes, 0.15F),
                g(ModTags.Biomes.desertBiomes, 0.05F),
                g(ModTags.Biomes.deepDarkBiomes, 0.05F),
                g(ModTags.Biomes.warpedForestBiomes, 0.10F),
                g(ModTags.Biomes.crimsonForestBiomes, 0.10F),
                g(ModTags.Biomes.basaltDeltasBiomes, 0.05F)
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

        addGrowth(ModBlocks.PALE_OAK_SAPLING.get(),
                g(ModTags.Biomes.soulSandValleyBiomes, 0.10F),
                g(ModTags.Biomes.deepDarkBiomes, 0.02F),
                g(ModTags.Biomes.warpedForestBiomes, 0.05F),
                g(ModTags.Biomes.crimsonForestBiomes, 0.05F),
                g(ModTags.Biomes.basaltDeltasBiomes, 0.02F)
        );

        addGrowth(GlimmeringWealdModule.glow_shroom,
                g(ModTags.Biomes.mushroomCaves, 0.6F),
                g(ModTags.Biomes.mountainBiomes, 0.2F)
        );
    }

    private static void registerVanillaOres() {
        // Coal: separate because you said coal is the exception.
        addOre(Blocks.COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 2.5F);
        addOre(Blocks.COAL_ORE, ModTags.Biomes.mushroomCaves, Items.COAL, 4);
        addOre(Blocks.COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1);
        addOreDesertAndDeepDark(Blocks.COAL_ORE, Items.COAL, 1.5f);

        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 2.5f);
        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.mushroomCaves, Items.COAL, 2.5f);
        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COAL_ORE, Items.COAL, 1.5f);

        // Copper
        addOreMountainsAndMushroomCaves(Blocks.COPPER_ORE, Items.RAW_COPPER, 3);
        addOre(Blocks.COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3);
        addOreDesertAndDeepDark(Blocks.COPPER_ORE, Items.RAW_COPPER, 3);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3);
        addOre(Blocks.DEEPSLATE_COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3);

        // Gold
        addOreMountainsAndMushroomCaves(Blocks.GOLD_ORE, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1);
        addOreDesertAndDeepDark(Blocks.GOLD_ORE, Items.RAW_GOLD, 3);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.DEEPSLATE_GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1.0F);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 3);

        // Iron: mountains, mushroom caves, plains only.
        addOreMountainsAndMushroomCaves(Blocks.IRON_ORE, Items.RAW_IRON, 3);
        addOre(Blocks.IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1);

        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_IRON_ORE, Items.RAW_IRON, 1);
        addOre(Blocks.DEEPSLATE_IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1);

        // Lapis: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.LAPIS_ORE, Items.LAPIS_LAZULI, 26);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_LAPIS_ORE, Items.LAPIS_LAZULI, 26);

        // Diamond: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.DIAMOND_ORE, Items.DIAMOND, 4);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND, 4);

        // Emerald: mountains and mushroom caves only.
        addOreMountainsAndMushroomCaves(Blocks.EMERALD_ORE, Items.EMERALD, 4);
        addOreMountainsAndMushroomCaves(Blocks.DEEPSLATE_EMERALD_ORE, Items.EMERALD, 4);

        // Redstone: desert and deep dark only.
        addOreDesertAndDeepDark(Blocks.REDSTONE_ORE, Items.REDSTONE, 13.5f);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE, 13.5f);

        // Nether ores: each nether biome gets its own separate call.
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 1.25f);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.crimsonForestBiomes, Items.QUARTZ, 1.25f);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.warpedForestBiomes, Items.QUARTZ, 1.25f);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.soulSandValleyBiomes, Items.QUARTZ, 1);

        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.QUARTZ, 1);

        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.QUARTZ, 1);

        Item soulQuartz = externalItem("nourished_nether", "soul_quartz", Items.QUARTZ);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), ModTags.Biomes.crimsonForestBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), ModTags.Biomes.warpedForestBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, soulQuartz, 1);

        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), ModTags.Biomes.crimsonForestBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), ModTags.Biomes.warpedForestBiomes, soulQuartz, 1);
        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, soulQuartz, 1);

        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);

        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);

        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);

        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);

        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);

        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.basaltDeltasBiomes, Items.NETHERITE_SCRAP, 2);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.crimsonForestBiomes, Items.NETHERITE_SCRAP, 1);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.warpedForestBiomes, Items.NETHERITE_SCRAP, 1);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.soulSandValleyBiomes, Items.NETHERITE_SCRAP, 1);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.deepDarkBiomes, Items.NETHERITE_SCRAP, 0.5F);
        addOre(Blocks.ANCIENT_DEBRIS, ModTags.Biomes.desertBiomes, Items.NETHERITE_SCRAP, 0.5F);
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
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "plum", Items.APPLE))
        );

        addLoot(Blocks.CHERRY_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.CHERRY_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "cherries", externalItem("fruitsdelight", "cherry", Items.APPLE)))
        );

        addLoot(Blocks.MANGROVE_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.MANGROVE_PROPAGULE),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "mango", Items.APPLE))
        );

        addLoot(Blocks.AZALEA_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.AZALEA),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "pear", Items.APPLE))
        );

        addLoot(Blocks.FLOWERING_AZALEA_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.FLOWERING_AZALEA),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "pear", Items.APPLE))
        );

        addLoot(Blocks.JUNGLE_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.JUNGLE_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, AMItemRegistry.BANANA.get())
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

        addLoot(Blocks.RED_MUSHROOM_BLOCK,
                drop(ModTags.Biomes.tropicalBiomes, Items.RED_MUSHROOM,1.5f),
                drop(ModTags.Biomes.mushroomCaves, Items.RED_MUSHROOM,2f),
                drop(ModTags.Biomes.mountainBiomes, Items.RED_MUSHROOM,1f)
        );

        addLoot(Blocks.BROWN_MUSHROOM_BLOCK,
                drop(ModTags.Biomes.tropicalBiomes, Items.BROWN_MUSHROOM,0.5f),
                drop(ModTags.Biomes.mushroomCaves, Items.BROWN_MUSHROOM,0.75f),
                drop(ModTags.Biomes.mountainBiomes, Items.BROWN_MUSHROOM,0.375f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_block,
                drop(ModTags.Biomes.mushroomCaves, GlimmeringWealdModule.glow_shroom.asItem(),0.75f),
                drop(ModTags.Biomes.mountainBiomes, GlimmeringWealdModule.glow_shroom.asItem(),0.375f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_stem,
                drop(ModTags.Biomes.mushroomCaves, GlimmeringWealdModule.glow_shroom.asItem(),0.75f),
                drop(ModTags.Biomes.mountainBiomes, GlimmeringWealdModule.glow_shroom.asItem(),0.375f)
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

    private static Item externalItem(String namespace, String path, Item fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
        return item == null ? fallback : item;
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

        addLoot(Blocks.SWEET_BERRY_BUSH,
                berryAmount(ModTags.Biomes.tropicalBiomes,true),
                berryAmount(ModTags.Biomes.mountainBiomes,true),
                berryAmount(ModTags.Biomes.tropicalBiomes,false),
                berryAmount(ModTags.Biomes.mountainBiomes,false)
        );

        addLoot(Blocks.NETHER_WART,
                drop(ModTags.Biomes.soulSandValleyBiomes, Items.NETHER_WART, matureAge3Amount(3))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.ONION.get(), matureAge7Amount(8.5f))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.RICE.get(), matureAge7Amount(7))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.TOMATO.get(), matureAge7Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.TOMATO_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE.get(), matureAge7Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get(),
                drop(ModTags.Biomes.tropicalBiomes, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(3,0)),
                drop(ModTags.Biomes.tropicalBiomes, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(1,0))
        );

        addLoot(ModBlocks.ASHEN_WHEAT_CROP.get(),
                drop(ModTags.Biomes.soulSandValleyBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.soulSandValleyBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.5f)),
                drop(ModTags.Biomes.deepDarkBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.deepDarkBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.15f)),
                drop(ModTags.Biomes.desertBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.desertBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.1f)),
                drop(ModTags.Biomes.warpedForestBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.warpedForestBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.25f)),
                drop(ModTags.Biomes.crimsonForestBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.crimsonForestBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.25f)),
                drop(ModTags.Biomes.basaltDeltasBiomes, ModItems.ASHEN_WHEAT.get(), matureAge7Amount(1,0)),
                drop(ModTags.Biomes.basaltDeltasBiomes, ModItems.ASHEN_WHEAT_SEEDS.get(), matureAge7Amount(1.15f))
        );
    }

    private static Function<BlockState, Float> matureAge7Amount(float amountWhenMature) {
        return matureAge7Amount(amountWhenMature,1f);
    }

    private static Function<BlockState, Float> matureAge7Amount(float amountWhenMature, float nonMatureAmount) {
        return state -> state.getValue(BlockStateProperties.AGE_7) >= 7 ? amountWhenMature : nonMatureAmount;
    }

    private static Function<BlockState, Float> matureAge3Amount(float amountWhenMature) {
        return matureAge3Amount(amountWhenMature,1f);
    }


    private static Function<BlockState, Float> matureAge3Amount(float amountWhenMature, float nonMatureAmount) {
        return state -> state.getValue(BlockStateProperties.AGE_3) >= 3 ? amountWhenMature : nonMatureAmount;
    }

    private static Function<BlockState, Float> matureAge2Amount(float amountWhenMature) {
        return matureAge3Amount(amountWhenMature,1);
    }

    private static void registerVanillaPlants() {
        addSeeds(Blocks.GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(Blocks.TALL_GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes));
        addSeeds(Blocks.FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(Blocks.LARGE_FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes,ModTags.Biomes.mushroomCaves));
        addSeeds(ModBlocks.DESERT_GRASS.get(), ModTags.Biomes.desertBiomes);
        addSeeds(ModBlocks.DESERT_TALL_GRASS.get(), ModTags.Biomes.desertBiomes);

        addCaveVines(Blocks.CAVE_VINES_PLANT);
        addCaveVines(Blocks.CAVE_VINES);

        addLoot(Blocks.DEAD_BUSH, drop(ModTags.Biomes.desertBiomes, Items.STICK, 1F));
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
            float val = 0.05f * mult;
            addLoot(block,
                    seedDrop(biome, Items.WHEAT_SEEDS, val),
                    seedDrop(biome, Items.BEETROOT_SEEDS, val),
                    seedDrop(biome, Items.PUMPKIN_SEEDS, val),
                    seedDrop(biome, ModItems.POTATO_SEEDS.get(), val),
                    seedDrop(biome, ModItems.CARROT_SEEDS.get(), val),
                    seedDrop(biome, Items.MELON_SEEDS, val),
                    seedDrop(biome, ModItems.RICE_SEEDS.get(), val),
                    seedDrop(biome, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE_SEEDS.get(), val),
                    seedDrop(biome, vectorwing.farmersdelight.common.registry.ModItems.TOMATO_SEEDS.get(), val),
                    seedDrop(biome, ModItems.ONION_SEEDS.get(), val)
            );
        }
        else if (biome.equals(ModTags.Biomes.savannaBiomes)){
            addLoot(block,
                    seedDrop(biome, Items.WHEAT_SEEDS, 0.25f * mult),
                    seedDrop(biome, Items.PUMPKIN_SEEDS, 0.015f * mult),
                    seedDrop(biome, Items.POTATO, 0.03f * mult),
                    seedDrop(biome, Items.CARROT, 0.03f * mult)
            );
        }
        else if (biome.equals(ModTags.Biomes.plainsBiomes)){
            addLoot(block,
                    seedDrop(biome, Items.WHEAT_SEEDS, 0.0625f * mult),
                    seedDrop(biome, Items.PUMPKIN_SEEDS, 0.0125f * mult),
                    seedDrop(biome, Items.POTATO, 0.025f * mult),
                    seedDrop(biome, Items.CARROT, 0.025f * mult)
            );
        }
        else if (biome.equals(ModTags.Biomes.mountainBiomes)){
            addLoot(block, seedDrop(biome, Items.BEETROOT_SEEDS, 0.1f * mult));
        }
        else if (biome.equals(ModTags.Biomes.mushroomCaves)){
            addLoot(block, seedDrop(biome, Items.BEETROOT_SEEDS, 0.1f * mult));
        }
        else if (biome.equals(ModTags.Biomes.desertBiomes)){
            addLoot(block, seedDrop(biome, Items.WHEAT_SEEDS, 0.05f * mult));
        }
    }

    private static void addOre(
            Block block,
            TagKey<Biome> biomeTag,
            Item item,
            float amount
    ) {
        addLoot(block, drop(biomeTag, item, amount, true));
    }

    private static void addOreMountainsAndMushroomCaves(
            Block block,
            Item item,
            float amount
    ) {
        addOre(block, ModTags.Biomes.mountainBiomes, item, amount);
        addOre(block, ModTags.Biomes.mushroomCaves, item, amount);
    }

    private static void addOreDesertAndDeepDark(
            Block block,
            Item item,
            float amount
    ) {
        addOre(block, ModTags.Biomes.desertBiomes, item, amount);
        addOre(block, ModTags.Biomes.deepDarkBiomes, item, amount);
    }

    private static BlockLootEntry berryAmount(TagKey<Biome> biome, boolean blue) {
        return drop(ModTags.Biomes.tropicalBiomes, blue ? FruitType.BLUEBERRY.getFruit(): Items.SWEET_BERRIES,
                state -> {
                    int age = state.getValue(SweetBerryBushBlock.AGE);
                    if(age<2){
                        return 1f;
                    }
                    if(age==2){
                        return 1 + BlockBehaviorConfig.BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.get(biome);
                    }
                    return 1 + BlockBehaviorConfig.BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS.get(biome);
        });
    }

    private static BlockLootEntry seedDrop(TagKey<Biome> biomeTag, Item item, float amount) {
        return new BlockLootEntry(biomeTag, item, state -> amount, false, true);
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
            boolean affectedByFortune,
            boolean groupedSeedRoll
    ) {
        public BlockLootEntry(
                TagKey<Biome> biomeTag,
                Item item,
                Function<BlockState, Float> dropAmount,
                boolean affectedByFortune
        ) {
            this(biomeTag, item, dropAmount, affectedByFortune, false);
        }

        public BlockLootEntry {
            if (dropAmount == null) {
                dropAmount = state -> 1.0F;
            }
        }
    }
}
