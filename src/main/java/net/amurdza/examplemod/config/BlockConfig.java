package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import dev.xkmc.fruitsdelight.init.food.FruitType;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.mcreator.nourishednether.init.NourishedNetherModItems;
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

    private static final Set<Block> BLOCKS_WITH_GROWTH_CONFIG = new HashSet<>();

    public static void init() {
        ensureAllGrowthTags();
        ensureAllLootTags();
        registerBlockInfo();
    }

    public static float blockGrowthChance(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (!hasGrowthConfig(block)) {
            return 0.0F;
        }

        Holder<Biome> biome = level.getBiome(pos);
        float total = 0.0F;

        for (Map.Entry<TagKey<Biome>, Map<Block, Float>> entry : BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG.entrySet()) {
            if (biome.is(entry.getKey())) {
                total += entry.getValue().getOrDefault(block, 0.0F);
            }
        }

        return total;
    }

    public static boolean hasGrowthConfig(Block block) {
        return BLOCKS_WITH_GROWTH_CONFIG.contains(block);
    }

    static void clearGrowthConfig() {
        BLOCKS_WITH_GROWTH_CONFIG.clear();
        BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG.values().forEach(Map::clear);
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
            BlockLootEntry... drops
    ) {
        BlockInfo oldInfo = BLOCK_INFO_BY_BLOCK.get(block);

        List<BlockLootEntry> dropList = new ArrayList<>();

        if (oldInfo != null) {
            dropList.addAll(oldInfo.drops());
        }

        dropList.addAll(Arrays.asList(drops));

        BLOCK_INFO_BY_BLOCK.put(block, new BlockInfo(List.copyOf(dropList)));

        for (BlockLootEntry drop : drops) {
            addDropMap(block, drop);
        }
    }

    private static void addLoot(Block block, BlockLootEntry... drops) {
        addBlockInfo(block, drops);
    }

    static void addToMap(TagKey<Biome> biomeTag, float value, Block block) {
        if (value == 0.0F) {
            return;
        }

        BLOCKS_WITH_GROWTH_CONFIG.add(block);
        BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG
                .computeIfAbsent(biomeTag, tag -> new HashMap<>())
                .merge(block, value, Float::sum);
    }

    static void replaceInMap(TagKey<Biome> biomeTag, float value, Block block) {
        if (value == 0.0F) {
            return;
        }

        BLOCKS_WITH_GROWTH_CONFIG.add(block);
        BLOCK_GROWTH_CHANCE_BY_BLOCK_BY_TAG
                .computeIfAbsent(biomeTag, tag -> new HashMap<>())
                .put(block, value);
    }

    private static void addDropMap(Block block, BlockLootEntry entry) {
        BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG
                .computeIfAbsent(entry.biomeTag(), tag -> new HashMap<>())
                .computeIfAbsent(entry.item(), item -> new HashMap<>())
                .computeIfAbsent(block, b -> new ArrayList<>())
                .add(entry);
    }

    private static void ensureAllGrowthTags() {
        ensureGrowthTag(ModTags.Biomes.aoeJungleBiomes);
        ensureGrowthTag(ModTags.Biomes.tropicalBiomes);
        ensureGrowthTag(ModTags.Biomes.savannaBiomes);
        ensureGrowthTag(ModTags.Biomes.mountainBiomes);
        ensureGrowthTag(ModTags.Biomes.desertBiomes);
        ensureGrowthTag(ModTags.Biomes.deepDarkBiomes);
        ensureGrowthTag(ModTags.Biomes.netherBiomes);
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
        registerVanillaOres();
        registerVanillaLeaves();
        registerVanillaCrops();
        registerVanillaPlants();
    }

    private static void registerVanillaOres() {
        // Coal: separate because you said coal is the exception.
        addOre(Blocks.COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 3);
        addOre(Blocks.COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1);
        addOreDesertAndDeepDark(Blocks.COAL_ORE, Items.COAL, 1.5f);

        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.mountainBiomes, Items.COAL, 3);
        addOre(Blocks.DEEPSLATE_COAL_ORE, ModTags.Biomes.plainsBiomes, Items.COAL, 1);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COAL_ORE, Items.COAL, 1.5f);

        // Copper
        addOre(Blocks.COPPER_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_COPPER, 3);
        addOre(Blocks.COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3);
        addOreDesertAndDeepDark(Blocks.COPPER_ORE, Items.RAW_COPPER, 3);

        addOre(Blocks.DEEPSLATE_COPPER_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_COPPER, 3);
        addOre(Blocks.DEEPSLATE_COPPER_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_COPPER, 3);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3);

        // Gold
        addOre(Blocks.GOLD_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1);
        addOreDesertAndDeepDark(Blocks.GOLD_ORE, Items.RAW_GOLD, 3);

        addOre(Blocks.DEEPSLATE_GOLD_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.DEEPSLATE_GOLD_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_GOLD, 1.0F);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 3);

        // Iron: mountains and plains only.
        addOre(Blocks.IRON_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_IRON, 3);
        addOre(Blocks.IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1);
        addOre(Blocks.DEEPSLATE_IRON_ORE, ModTags.Biomes.mountainBiomes, Items.RAW_IRON, 3);
        addOre(Blocks.DEEPSLATE_IRON_ORE, ModTags.Biomes.plainsBiomes, Items.RAW_IRON, 1);

        // Lapis: mountains only.
        addOre(Blocks.LAPIS_ORE, ModTags.Biomes.mountainBiomes, Items.LAPIS_LAZULI, 26);
        addOre(Blocks.DEEPSLATE_LAPIS_ORE, ModTags.Biomes.mountainBiomes, Items.LAPIS_LAZULI, 26);

        // Diamond: mountains only.
        addOre(Blocks.DIAMOND_ORE, ModTags.Biomes.mountainBiomes, Items.DIAMOND, 4);
        addOre(Blocks.DEEPSLATE_DIAMOND_ORE, ModTags.Biomes.mountainBiomes, Items.DIAMOND, 4);

        // Emerald: mountains only.
        addOre(Blocks.EMERALD_ORE, ModTags.Biomes.mountainBiomes, Items.EMERALD, 4);
        addOre(Blocks.DEEPSLATE_EMERALD_ORE, ModTags.Biomes.mountainBiomes, Items.EMERALD, 4);

        // Redstone: desert and deep dark only.
        addOreDesertAndDeepDark(Blocks.REDSTONE_ORE, Items.REDSTONE, 13.5f);
        addOreDesertAndDeepDark(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE, 13.5f);

        // Nether ores: each nether biome gets its own separate call.
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.crimsonForestBiomes, Items.QUARTZ, 1.25f);
        addOre(Blocks.NETHER_QUARTZ_ORE, ModTags.Biomes.warpedForestBiomes, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, NourishedNetherModItems.SOUL_QUARTZ.get(), 1);
        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, NourishedNetherModItems.SOUL_QUARTZ.get(), 1);

        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.crimsonForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(Blocks.NETHER_GOLD_ORE, ModTags.Biomes.warpedForestBiomes, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), ModTags.Biomes.basaltDeltasBiomes, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), ModTags.Biomes.soulSandValleyBiomes, Items.GOLD_INGOT, 1);
    }

    private static void registerVanillaLeaves() {
        addLoot(Blocks.SPRUCE_LEAVES,
                leaf(ModTags.Biomes.mountainBiomes, Items.SPRUCE_SAPLING),
                stick(ModTags.Biomes.mountainBiomes)
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
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "mangosteen", Items.APPLE))
        );

        addLoot(Blocks.CHERRY_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.CHERRY_SAPLING),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "cherries", externalItem("fruitsdelight", "cherry", Items.APPLE)))
        );

        addLoot(Blocks.MANGROVE_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, Items.MANGROVE_PROPAGULE),
                stick(ModTags.Biomes.tropicalBiomes),
                fruit(ModTags.Biomes.tropicalBiomes, externalItem("fruitsdelight", "orange", Items.APPLE))
        );

        addLoot(Blocks.AZALEA_LEAVES,
                leaf(ModTags.Biomes.tropicalBiomes, ModBlocks.AZALEA_SAPLING.get().asItem()),
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
                drop(ModTags.Biomes.mountainBiomes, Items.RED_MUSHROOM,2f)
        );

        addLoot(Blocks.BROWN_MUSHROOM_BLOCK,
                drop(ModTags.Biomes.tropicalBiomes, Items.BROWN_MUSHROOM,0.5f),
                drop(ModTags.Biomes.mountainBiomes, Items.BROWN_MUSHROOM,0.75f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_block,
                drop(ModTags.Biomes.mountainBiomes, GlimmeringWealdModule.glow_shroom.asItem(),0.75f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_stem,
                drop(ModTags.Biomes.mountainBiomes, GlimmeringWealdModule.glow_shroom.asItem(),0.75f)
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

    private static Block externalBlock(String namespace, String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
        return block == null ? Blocks.AIR : block;
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
                drop(ModTags.Biomes.mountainBiomes, Items.BEETROOT_SEEDS, matureAge3Amount(1.5F))
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

        addLoot(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get(),
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
        addSeeds(Blocks.GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes,ModTags.Biomes.mountainBiomes));
        addSeeds(Blocks.TALL_GRASS,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.savannaBiomes,ModTags.Biomes.plainsBiomes));
        addSeeds(Blocks.FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes));
        addSeeds(Blocks.LARGE_FERN,List.of(ModTags.Biomes.tropicalBiomes,ModTags.Biomes.mountainBiomes));
        addSeeds(ModBlocks.DESERT_GRASS.get(), ModTags.Biomes.desertBiomes);
        addSeeds(ModBlocks.DESERT_TALL_GRASS.get(), ModTags.Biomes.desertBiomes);

        addCaveVines(Blocks.CAVE_VINES_PLANT);
        addCaveVines(Blocks.CAVE_VINES);

        addLoot(Blocks.DEAD_BUSH, drop(ModTags.Biomes.desertBiomes, Items.STICK, 1F));

        Block soulWeeds = externalBlock("nourished_nether", "soul_weeds");
        if (soulWeeds != Blocks.AIR) {
            addLoot(soulWeeds, drop(ModTags.Biomes.soulSandValleyBiomes, ModItems.ASHEN_WHEAT_SEEDS.get()));
        }
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

    private static void addOreDesertAndDeepDark(
            Block block,
            Item item,
            float amount
    ) {
        addOre(block, ModTags.Biomes.desertBiomes, item, amount);
        addOre(block, ModTags.Biomes.deepDarkBiomes, item, amount);
    }

    private static BlockLootEntry berryAmount(TagKey<Biome> biome, boolean blue) {
        return drop(biome, blue ? FruitType.BLUEBERRY.getFruit(): Items.SWEET_BERRIES,
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
            List<BlockLootEntry> drops
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
