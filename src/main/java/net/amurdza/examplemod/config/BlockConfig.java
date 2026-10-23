package net.amurdza.examplemod.config;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import dev.xkmc.fruitsdelight.init.food.FruitType;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.mcreator.nourishednether.init.NourishedNetherModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

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

    public static final Map<ResourceKey<Level>, Map<Block, Float>> BLOCK_GROWTH_CHANCE_BY_DIMENSION = new HashMap<>();

    public static final Map<Block, BlockInfo> BLOCK_INFO_BY_BLOCK = new HashMap<>();

    private static final Set<Block> BLOCKS_WITH_GROWTH_CONFIG = new HashSet<>();

    public static void init() {
        BLOCK_INFO_BY_BLOCK.clear();
        registerBlockInfo();
        DocumentedContentBalance.registerCropDrops();
    }

    public static float blockGrowthChance(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (!hasGrowthConfig(block)) {
            return -1.0F;
        }

        return BLOCK_GROWTH_CHANCE_BY_DIMENSION
                .getOrDefault(level.dimension(), Map.of())
                .getOrDefault(block, -1.0F);
    }

    public static boolean hasGrowthConfig(Block block) {
        return BLOCKS_WITH_GROWTH_CONFIG.contains(block);
    }

    static void clearGrowthConfig() {
        BLOCKS_WITH_GROWTH_CONFIG.clear();
        BLOCK_GROWTH_CHANCE_BY_DIMENSION.clear();
    }

    public static List<BlockLootEntry> blockDropEntries(Level level, BlockPos pos, BlockState state) {
        BlockInfo info = BLOCK_INFO_BY_BLOCK.get(state.getBlock());

        if (info == null || info.drops().isEmpty()) {
            return List.of();
        }

        return info.drops().stream()
                .filter(entry -> level.dimension().equals(entry.dimension()))
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

    }

    static void addLoot(Block block, BlockLootEntry... drops) {
        addBlockInfo(block, drops);
    }

    static void addToMap(ResourceKey<Level> dimension, float value, Block block) {
        if (value == 0.0F) {
            return;
        }

        BLOCKS_WITH_GROWTH_CONFIG.add(block);
        BLOCK_GROWTH_CHANCE_BY_DIMENSION
                .computeIfAbsent(dimension, tag -> new HashMap<>())
                .put(block, value);
    }

    private static BlockLootEntry drop(ResourceKey<Level> dimension, Item item) {
        return new BlockLootEntry(dimension, item, state -> 1.0F, false);
    }

    private static BlockLootEntry drop(ResourceKey<Level> dimension, Item item, float amount) {
        return new BlockLootEntry(dimension, item, state -> amount, false);
    }

    private static BlockLootEntry drop(ResourceKey<Level> dimension, Item item, float amount, boolean affectedByFortune) {
        return new BlockLootEntry(dimension, item, state -> amount, affectedByFortune);
    }

    private static BlockLootEntry drop(ResourceKey<Level> dimension, Item item, Function<BlockState, Float> dropAmount) {
        return new BlockLootEntry(dimension, item, dropAmount, false);
    }

    private static void registerBlockInfo() {
        registerVanillaOres();
        registerVanillaLeaves();
        registerVanillaCrops();
        registerVanillaPlants();
    }

    private static void registerVanillaOres() {
        // Coal retains its distinct dimension-dependent yield.
        addOre(Blocks.COAL_ORE, GameplayDimensions.SNOWY, Items.COAL, 3);
        addOre(Blocks.COAL_ORE, GameplayDimensions.PLAINS, Items.COAL, 1);
        addOreDesert(Blocks.COAL_ORE, Items.COAL, 1.5f);

        addOre(Blocks.DEEPSLATE_COAL_ORE, GameplayDimensions.SNOWY, Items.COAL, 3);
        addOre(Blocks.DEEPSLATE_COAL_ORE, GameplayDimensions.PLAINS, Items.COAL, 1);
        addOreDesert(Blocks.DEEPSLATE_COAL_ORE, Items.COAL, 1.5f);

        // Copper
        addOre(Blocks.COPPER_ORE, GameplayDimensions.SNOWY, Items.RAW_COPPER, 3);
        addOre(Blocks.COPPER_ORE, GameplayDimensions.PLAINS, Items.RAW_COPPER, 3);
        addOreDesert(Blocks.COPPER_ORE, Items.RAW_COPPER, 3);

        addOre(Blocks.DEEPSLATE_COPPER_ORE, GameplayDimensions.SNOWY, Items.RAW_COPPER, 3);
        addOre(Blocks.DEEPSLATE_COPPER_ORE, GameplayDimensions.PLAINS, Items.RAW_COPPER, 3);
        addOreDesert(Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER, 3);

        // Gold
        addOre(Blocks.GOLD_ORE, GameplayDimensions.SNOWY, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.GOLD_ORE, GameplayDimensions.PLAINS, Items.RAW_GOLD, 1);
        addOreDesert(Blocks.GOLD_ORE, Items.RAW_GOLD, 3);

        addOre(Blocks.DEEPSLATE_GOLD_ORE, GameplayDimensions.SNOWY, Items.RAW_GOLD, 2.5F);
        addOre(Blocks.DEEPSLATE_GOLD_ORE, GameplayDimensions.PLAINS, Items.RAW_GOLD, 1.0F);
        addOreDesert(Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD, 3);

        // Iron: mountains and plains only.
        addOre(Blocks.IRON_ORE, GameplayDimensions.SNOWY, Items.RAW_IRON, 3);
        addOre(Blocks.IRON_ORE, GameplayDimensions.PLAINS, Items.RAW_IRON, 1);
        addOre(Blocks.DEEPSLATE_IRON_ORE, GameplayDimensions.SNOWY, Items.RAW_IRON, 3);
        addOre(Blocks.DEEPSLATE_IRON_ORE, GameplayDimensions.PLAINS, Items.RAW_IRON, 1);

        // Lapis: mountains only.
        addOre(Blocks.LAPIS_ORE, GameplayDimensions.SNOWY, Items.LAPIS_LAZULI, 26);
        addOre(Blocks.DEEPSLATE_LAPIS_ORE, GameplayDimensions.SNOWY, Items.LAPIS_LAZULI, 26);

        // Diamond: mountains only.
        addOre(Blocks.DIAMOND_ORE, GameplayDimensions.SNOWY, Items.DIAMOND, 4);
        addOre(Blocks.DEEPSLATE_DIAMOND_ORE, GameplayDimensions.SNOWY, Items.DIAMOND, 4);

        // Emerald: mountains only.
        addOre(Blocks.EMERALD_ORE, GameplayDimensions.SNOWY, Items.EMERALD, 4);
        addOre(Blocks.DEEPSLATE_EMERALD_ORE, GameplayDimensions.SNOWY, Items.EMERALD, 4);

        // Redstone ore belongs to the desert gameplay dimension.
        addOreDesert(Blocks.REDSTONE_ORE, Items.REDSTONE, 13.5f);
        addOreDesert(Blocks.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE, 13.5f);

        // Nether ore rates apply throughout the Nether gameplay dimension.
        addOre(Blocks.NETHER_QUARTZ_ORE, GameplayDimensions.NETHER, Items.QUARTZ, 1.25f);
        addOre(ModBlocks.BLACKSTONE_QUARTZ_ORE.get(), GameplayDimensions.NETHER, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.BASALT_QUARTZ_ORE.get(), GameplayDimensions.NETHER, Items.QUARTZ, 2.5f);
        addOre(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), GameplayDimensions.NETHER, NourishedNetherModItems.SOUL_QUARTZ.get(), 1);
        addOre(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), GameplayDimensions.NETHER, NourishedNetherModItems.SOUL_QUARTZ.get(), 1);

        addOre(Blocks.NETHER_GOLD_ORE, GameplayDimensions.NETHER, Items.GOLD_INGOT, 1.5f);
        addOre(ModBlocks.BLACKSTONE_GOLD_ORE.get(), GameplayDimensions.NETHER, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.BASALT_GOLD_ORE.get(), GameplayDimensions.NETHER, Items.GOLD_INGOT, 4);
        addOre(ModBlocks.SOUL_SOIL_GOLD_ORE.get(), GameplayDimensions.NETHER, Items.GOLD_INGOT, 1);
        addOre(ModBlocks.SOUL_SAND_GOLD_ORE.get(), GameplayDimensions.NETHER, Items.GOLD_INGOT, 1);
    }

    private static void registerVanillaLeaves() {
        addLoot(Blocks.SPRUCE_LEAVES,
                leaf(GameplayDimensions.SNOWY, Items.SPRUCE_SAPLING),
                stick(GameplayDimensions.SNOWY)
        );

        addLoot(Blocks.ACACIA_LEAVES,
                leaf(GameplayDimensions.SAVANNA, Items.ACACIA_SAPLING),
                stick(GameplayDimensions.SAVANNA)
        );

        addLoot(Blocks.BIRCH_LEAVES,
                leaf(GameplayDimensions.PLAINS, Items.BIRCH_SAPLING),
                stick(GameplayDimensions.PLAINS)
        );

        addLoot(Blocks.OAK_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.OAK_SAPLING),
                leaf(GameplayDimensions.PLAINS, Items.OAK_SAPLING),
                stick(GameplayDimensions.TROPICAL),
                stick(GameplayDimensions.PLAINS),
                fruit(GameplayDimensions.TROPICAL,Items.APPLE),
                fruit(GameplayDimensions.PLAINS,Items.APPLE)
        );

        addLoot(Blocks.DARK_OAK_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.DARK_OAK_SAPLING),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, externalItem("aoemod", "plum", externalItem("environmental", "plum", Items.APPLE)))
        );

        addLoot(Blocks.CHERRY_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.CHERRY_SAPLING),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, externalItem("hearthandharvest", "cherry", Items.APPLE))
        );

        addLoot(Blocks.MANGROVE_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.MANGROVE_PROPAGULE),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, externalItem("fruitsdelight", "orange", Items.APPLE))
        );

        addLoot(Blocks.AZALEA_LEAVES,
                leaf(GameplayDimensions.TROPICAL, ModBlocks.AZALEA_SAPLING.get().asItem()),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, externalItem("fruitsdelight", "pear", Items.APPLE))
        );

        addLoot(Blocks.FLOWERING_AZALEA_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.FLOWERING_AZALEA),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, externalItem("fruitsdelight", "pear", Items.APPLE))
        );

        addLoot(Blocks.JUNGLE_LEAVES,
                leaf(GameplayDimensions.TROPICAL, Items.JUNGLE_SAPLING),
                stick(GameplayDimensions.TROPICAL),
                fruit(GameplayDimensions.TROPICAL, AMItemRegistry.BANANA.get())
        );

        addLoot(Blocks.WARPED_WART_BLOCK,
                drop(GameplayDimensions.NETHER, Items.WARPED_FUNGUS, 0.12f));
        addLoot(Blocks.NETHER_WART_BLOCK,
                drop(GameplayDimensions.NETHER, Items.CRIMSON_FUNGUS, 0.12f));

        addLoot(Blocks.RED_MUSHROOM_BLOCK,
                drop(GameplayDimensions.TROPICAL, Items.RED_MUSHROOM,1.5f),
                drop(GameplayDimensions.SNOWY, Items.RED_MUSHROOM,2f)
        );

        addLoot(Blocks.BROWN_MUSHROOM_BLOCK,
                drop(GameplayDimensions.TROPICAL, Items.BROWN_MUSHROOM,0.5f),
                drop(GameplayDimensions.SNOWY, Items.BROWN_MUSHROOM,0.75f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_block,
                drop(GameplayDimensions.SNOWY, GlimmeringWealdModule.glow_shroom.asItem(),0.75f)
        );

        addLoot(GlimmeringWealdModule.glow_shroom_stem,
                drop(GameplayDimensions.SNOWY, GlimmeringWealdModule.glow_shroom.asItem(),0.75f)
        );
    }

    private static BlockLootEntry leaf(ResourceKey<Level> dimension, Item saplingItem) {
        float val = 0.05f;
        if(dimension == GameplayDimensions.TROPICAL){
            val = 0.20f;
        }
        else if(dimension == GameplayDimensions.SAVANNA){
            val = 0.075f;
        }
        return drop(dimension, saplingItem, val);
    }

    private static BlockLootEntry stick(ResourceKey<Level> dimension) {
        float val = 0.02f;
        if(dimension == GameplayDimensions.TROPICAL){
            val = 0.08f;
        }
        else if(dimension == GameplayDimensions.SAVANNA){
            val = 0.03f;
        }
        return drop(dimension, Items.STICK, val);
    }

    private static BlockLootEntry fruit(ResourceKey<Level> dimension, Item fruit) {
        float val = 0;
        if(dimension == GameplayDimensions.TROPICAL){
            val = 0.02f;
        }
        else if(dimension == GameplayDimensions.PLAINS){
            val = 0.005f;
        }
        return drop(dimension, fruit, val);
    }

    private static Item externalItem(String namespace, String path, Item fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
        return item == null || item == Items.AIR ? fallback : item;
    }

    private static Block externalBlock(String namespace, String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
        return block == null ? Blocks.AIR : block;
    }
    
    private static void registerVanillaCrops() {
        addLoot(Blocks.WHEAT,
                drop(GameplayDimensions.TROPICAL, Items.WHEAT, matureAge7Amount(3,0)),
                drop(GameplayDimensions.TROPICAL, Items.WHEAT_SEEDS, matureAge7Amount(5)),
                drop(GameplayDimensions.SAVANNA, Items.WHEAT, matureAge7Amount(1.5f,0)),
                drop(GameplayDimensions.SAVANNA, Items.WHEAT_SEEDS, matureAge7Amount(3.5F)),
                drop(GameplayDimensions.PLAINS, Items.WHEAT, matureAge7Amount(1.0F,0)),
                drop(GameplayDimensions.PLAINS, Items.WHEAT_SEEDS, matureAge7Amount(2.5F))
        );

        addLoot(Blocks.CARROTS,
                drop(GameplayDimensions.TROPICAL, Items.CARROT, matureAge7Amount(8.5f)),
                drop(GameplayDimensions.SAVANNA, Items.CARROT, matureAge7Amount(5F)),
                drop(GameplayDimensions.PLAINS, Items.CARROT, matureAge7Amount(3.5F))
        );

        addLoot(Blocks.POTATOES,
                drop(GameplayDimensions.TROPICAL, Items.POTATO, matureAge7Amount(8.5f)),
                drop(GameplayDimensions.SAVANNA, Items.POTATO, matureAge7Amount(5F)),
                drop(GameplayDimensions.PLAINS, Items.POTATO, matureAge7Amount(3.5F)),
                drop(GameplayDimensions.PLAINS, Items.POISONOUS_POTATO, matureAge7Amount(0.02F,0))
        );

        addLoot(Blocks.BEETROOTS,
                drop(GameplayDimensions.TROPICAL, Items.BEETROOT, matureAge3Amount(3,0)),
                drop(GameplayDimensions.TROPICAL, Items.BEETROOT_SEEDS, matureAge3Amount(5)),
                drop(GameplayDimensions.SNOWY, Items.BEETROOT, matureAge3Amount(1,0)),
                drop(GameplayDimensions.SNOWY, Items.BEETROOT_SEEDS, matureAge3Amount(1.5F))
        );

        addLoot(Blocks.MELON_STEM,
                drop(GameplayDimensions.TROPICAL, Items.MELON_SEEDS, matureAge7Amount(5))
        );

        addLoot(Blocks.PUMPKIN_STEM,
                drop(GameplayDimensions.TROPICAL, Items.PUMPKIN_SEEDS, matureAge7Amount(5)),
                drop(GameplayDimensions.SAVANNA, Items.PUMPKIN_SEEDS, matureAge7Amount(3.5f)),
                drop(GameplayDimensions.PLAINS, Items.PUMPKIN_SEEDS, matureAge7Amount(2.5f))
        );

        addLoot(Blocks.MELON,
                drop(GameplayDimensions.TROPICAL, Items.MELON)
        );

        addLoot(Blocks.PUMPKIN,
                drop(GameplayDimensions.TROPICAL, Items.PUMPKIN),
                drop(GameplayDimensions.SAVANNA, Items.PUMPKIN),
                drop(GameplayDimensions.PLAINS, Items.PUMPKIN)
        );

        addLoot(Blocks.COCOA,
                drop(GameplayDimensions.TROPICAL, Items.COCOA_BEANS, matureAge2Amount(7))
        );

        addLoot(Blocks.SWEET_BERRY_BUSH,
                berryAmount(GameplayDimensions.TROPICAL,true),
                berryAmount(GameplayDimensions.SNOWY,true),
                berryAmount(GameplayDimensions.TROPICAL,false),
                berryAmount(GameplayDimensions.SNOWY,false)
        );

        addLoot(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get(),
                berryAmount(GameplayDimensions.TROPICAL,false),
                berryAmount(GameplayDimensions.SNOWY,false)
        );

        addLoot(Blocks.NETHER_WART,
                drop(GameplayDimensions.NETHER, Items.NETHER_WART, matureAge3Amount(3))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.ONION.get(), matureAge7Amount(8.5f))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.RICE.get(), matureAge3Amount(7))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.TOMATO.get(), matureAge7Amount(3,0)),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.TOMATO_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE.get(), matureAge7Amount(3,0)),
                drop(GameplayDimensions.TROPICAL, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE_SEEDS.get(), matureAge7Amount(5))
        );

        addLoot(samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get(),
                drop(GameplayDimensions.TROPICAL, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(3,0)),
                drop(GameplayDimensions.DESERT, samebutdifferent.ecologics.registry.ModItems.PRICKLY_PEAR.get(),
                        matureAge3Amount(1,0))
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
        return state -> state.getValue(BlockStateProperties.AGE_2) >= 2 ? amountWhenMature : 1.0F;
    }

    private static void registerVanillaPlants() {
        addSeeds(Blocks.GRASS,List.of(GameplayDimensions.TROPICAL,GameplayDimensions.SAVANNA,GameplayDimensions.PLAINS,GameplayDimensions.SNOWY));
        addSeeds(Blocks.TALL_GRASS,List.of(GameplayDimensions.TROPICAL,GameplayDimensions.SAVANNA,GameplayDimensions.PLAINS));
        addSeeds(Blocks.FERN,List.of(GameplayDimensions.TROPICAL,GameplayDimensions.SNOWY));
        addSeeds(Blocks.LARGE_FERN,List.of(GameplayDimensions.TROPICAL,GameplayDimensions.SNOWY));
        addSeeds(ModBlocks.DESERT_GRASS.get(), GameplayDimensions.DESERT);
        addSeeds(ModBlocks.DESERT_TALL_GRASS.get(), GameplayDimensions.DESERT);

        addCaveVines(Blocks.CAVE_VINES_PLANT);
        addCaveVines(Blocks.CAVE_VINES);

        addLoot(Blocks.DEAD_BUSH, drop(GameplayDimensions.DESERT, Items.STICK, 1F));

    }

    private static void addCaveVines(Block block){
        addLoot(block,
                drop(GameplayDimensions.TROPICAL, Items.GLOW_BERRIES, state -> state.getValue(BlockStateProperties.BERRIES) ? 1f + BlockBehaviorConfig.DIMENSION_TO_GLOW_BERRY_AMOUNTS.get(GameplayDimensions.TROPICAL) : 1f)
        );
    }

    private static void addSeeds(Block block, List<ResourceKey<Level>> dimensions){
        for(ResourceKey<Level> dimension: dimensions){
            addSeeds(block,dimension);
        }
    }

    private static void addSeeds(Block block, ResourceKey<Level> dimension){
        float mult = block instanceof DoublePlantBlock || block == ModBlocks.DESERT_TALL_GRASS.get() ? 2 : 1;

        if(dimension.equals(GameplayDimensions.TROPICAL)){
            float val = 0.05f * mult;
            addLoot(block,
                    seedDrop(dimension, Items.WHEAT_SEEDS, val),
                    seedDrop(dimension, Items.BEETROOT_SEEDS, val),
                    seedDrop(dimension, Items.PUMPKIN_SEEDS, val),
                    seedDrop(dimension, ModItems.POTATO_SEEDS.get(), val),
                    seedDrop(dimension, ModItems.CARROT_SEEDS.get(), val),
                    seedDrop(dimension, Items.MELON_SEEDS, val),
                    seedDrop(dimension, ModItems.RICE_SEEDS.get(), val),
                    seedDrop(dimension, vectorwing.farmersdelight.common.registry.ModItems.CABBAGE_SEEDS.get(), val),
                    seedDrop(dimension, vectorwing.farmersdelight.common.registry.ModItems.TOMATO_SEEDS.get(), val),
                    seedDrop(dimension, ModItems.ONION_SEEDS.get(), val)
            );
        }
        else if (dimension.equals(GameplayDimensions.SAVANNA)){
            addLoot(block,
                    seedDrop(dimension, Items.WHEAT_SEEDS, 0.25f * mult),
                    seedDrop(dimension, Items.PUMPKIN_SEEDS, 0.015f * mult),
                    seedDrop(dimension, Items.POTATO, 0.03f * mult),
                    seedDrop(dimension, Items.CARROT, 0.03f * mult)
            );
        }
        else if (dimension.equals(GameplayDimensions.PLAINS)){
            addLoot(block,
                    seedDrop(dimension, Items.WHEAT_SEEDS, 0.0625f * mult),
                    seedDrop(dimension, Items.PUMPKIN_SEEDS, 0.0125f * mult),
                    seedDrop(dimension, ModItems.CARROT_SEEDS.get(), 0.025f * mult),
                    seedDrop(dimension, ModItems.POTATO_SEEDS.get(), 0.025f * mult)
            );
        }
        else if (dimension.equals(GameplayDimensions.SNOWY)){
            addLoot(block,
                    seedDrop(dimension, Items.BEETROOT_SEEDS, 0.05f * mult));
        }
        else if (dimension.equals(GameplayDimensions.DESERT)){
            addLoot(block, seedDrop(dimension, Items.WHEAT_SEEDS, 0.08f * mult));
        }
    }

    private static void addOre(
            Block block,
            ResourceKey<Level> dimension,
            Item item,
            float amount
    ) {
        addLoot(block, drop(dimension, item, amount, true));
    }

    private static void addOreDesert(
            Block block,
            Item item,
            float amount
    ) {
        addOre(block, GameplayDimensions.DESERT, item, amount);
    }

    private static BlockLootEntry berryAmount(ResourceKey<Level> dimension, boolean blue) {
        return drop(dimension, blue ? FruitType.BLUEBERRY.getFruit(): Items.SWEET_BERRIES,
                state -> {
                    int age = state.getValue(SweetBerryBushBlock.AGE);
                    if(age<2){
                        return 1f;
                    }
                    if(age==2){
                        return 1 + BlockBehaviorConfig.DIMENSION_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS.get(dimension);
                    }
                    return 1 + BlockBehaviorConfig.DIMENSION_TO_MATURE_SWEET_BERRY_AMOUNTS.get(dimension);
        });
    }

    private static BlockLootEntry seedDrop(ResourceKey<Level> dimension, Item item, float amount) {
        return new BlockLootEntry(dimension, item, state -> amount, false, true);
    }

    public record BlockInfo(
            List<BlockLootEntry> drops
    ) {}

    public record BlockLootEntry(
            ResourceKey<Level> dimension,
            Item item,
            Function<BlockState, Float> dropAmount,
            boolean affectedByFortune,
            boolean groupedSeedRoll
    ) {
        public BlockLootEntry(
                ResourceKey<Level> dimension,
                Item item,
                Function<BlockState, Float> dropAmount,
                boolean affectedByFortune
        ) {
            this(dimension, item, dropAmount, affectedByFortune, false);
        }

        public BlockLootEntry {
            if (dropAmount == null) {
                dropAmount = state -> 1.0F;
            }
        }
    }
}
