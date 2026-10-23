package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.BlockHelper;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.event_handlers.BlockGrowthRate;
import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import vectorwing.farmersdelight.common.Configuration;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantGameplayTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    private PlantGameplayTests() { }

    @GameTest(template = TEMPLATE)
    public static void unconfiguredDimensionPreservesGrowthAndDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        helper.assertTrue(BlockConfig.blockGrowthChance(level, pos, wheat) < 0,
                "Overworld wheat must retain its original growth decision");
        BlockEvent.CropGrowEvent.Pre growth = new BlockEvent.CropGrowEvent.Pre(level, pos, wheat);
        BlockGrowthRate.blockGrowthPre(growth);
        helper.assertTrue(growth.getResult() == Event.Result.DEFAULT,
                "Unconfigured growth event must not be denied or forced");
        BlockEvent.BreakEvent breaking = new BlockEvent.BreakEvent(level, pos, wheat, helper.makeMockSurvivalPlayer());
        CustomBlockDrops.addConfiguredDrops(breaking);
        helper.assertFalse(breaking.isCanceled(), "An absent drop entry must leave normal breaking intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dimensionRatesAndSnowySeedPool(GameTestHelper helper) {
        ServerLevel tropical = dimension(helper, GameplayDimensions.TROPICAL);
        ServerLevel plains = dimension(helper, GameplayDimensions.PLAINS);
        ServerLevel snowy = dimension(helper, GameplayDimensions.SNOWY);
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        float tropicalRate = BlockConfig.blockGrowthChance(tropical, BlockPos.ZERO, wheat);
        helper.assertTrue(tropicalRate > BlockConfig.blockGrowthChance(plains, BlockPos.ZERO, wheat),
                "Tropical wheat should grow faster than plains wheat");
        helper.assertTrue(tropicalRate == BlockConfig.blockGrowthChance(tropical, new BlockPos(100, 200, -100), wheat),
                "Growth must depend on dimension, not the biome at the sample position");
        helper.assertTrue(BlockConfig.blockGrowthChance(snowy, BlockPos.ZERO, wheat) < 0,
                "No configured wheat rate must preserve the default instead of forcing zero");
        for (Block block : new Block[]{Blocks.GRASS, Blocks.FERN, Blocks.LARGE_FERN}) {
            var entries = BlockConfig.blockDropEntries(snowy, BlockPos.ZERO, block.defaultBlockState());
            helper.assertFalse(entries.isEmpty(), "Snowy seed pool missing for " + block);
            helper.assertTrue(entries.stream().allMatch(entry -> entry.item() == Items.BEETROOT_SEEDS),
                    "Snowy grass and ferns must only select beetroot seeds");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cropDropsUseTheirActualAgeProperties(GameTestHelper helper) {
        ServerLevel tropical = dimension(helper, GameplayDimensions.TROPICAL);
        BlockState cocoa = Blocks.COCOA.defaultBlockState().setValue(BlockStateProperties.AGE_2, 2);
        BlockState rice = vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get()
                .defaultBlockState().setValue(BlockStateProperties.AGE_3, 3);
        for (BlockState crop : new BlockState[]{cocoa, rice}) {
            var entries = BlockConfig.blockDropEntries(tropical, BlockPos.ZERO, crop);
            helper.assertFalse(entries.isEmpty(), "Missing crop drops for " + crop.getBlock());
            helper.assertTrue(entries.stream().anyMatch(entry -> BlockConfig.blockDropAmount(crop, entry) > 1),
                    "Mature cocoa and rice should use their mature yield without invalid-property errors");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void richFarmlandHydratesWithoutFertilizing(GameTestHelper helper) {
        BlockPos soilPos = new BlockPos(1, 1, 1);
        BlockState farmland = vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get()
                .defaultBlockState().setValue(FarmBlock.MOISTURE, 0);
        helper.setBlock(soilPos, farmland);
        helper.setBlock(soilPos.above(), Blocks.WHEAT.defaultBlockState());
        helper.setBlock(soilPos.offset(10, 0, 0), Blocks.WATER);
        double oldChance = Configuration.RICH_SOIL_BOOST_CHANCE.get();
        try {
            Configuration.RICH_SOIL_BOOST_CHANCE.set(1.0D);
            BlockPos absolute = helper.absolutePos(soilPos);
            for (int i = 0; i < 20; i++) {
                helper.getLevel().getBlockState(absolute).randomTick(helper.getLevel(), absolute, RandomSource.create(i));
            }
            helper.assertTrue(helper.getBlockState(soilPos).getValue(FarmBlock.MOISTURE) == 7,
                    "Rich farmland should hydrate from water beyond the original four-block range");
            helper.assertTrue(helper.getBlockState(soilPos.above()).getValue(BlockStateProperties.AGE_7) == 0,
                    "Rich farmland must not automatically fertilize its crop even with boost chance forced to one");
        } finally {
            Configuration.RICH_SOIL_BOOST_CHANCE.set(oldChance);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void substrateRestrictionsUseTheBiomeAbove(GameTestHelper helper) {
        ServerLevel snowy = dimension(helper, GameplayDimensions.SNOWY);
        ServerLevel nether = dimension(helper, GameplayDimensions.NETHER);
        BlockPos floor = new BlockPos(0, 80, 0);
        var snowyFloor = biomeBoundary(snowy, floor, "snowy_forest", "mushroom_caves");
        var wrongSnowyFloor = biomeBoundary(snowy, floor, "mushroom_caves", "snowy_forest");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(snowyFloor, floor, Blocks.MYCELIUM.defaultBlockState()),
                "Mycelium must accept mushroom caves above even if the floor itself has a different biome");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(wrongSnowyFloor, floor, Blocks.MYCELIUM.defaultBlockState()),
                "Mycelium must reject a mushroom-cave floor with a different biome above");
        var crimsonAbove = biomeBoundary(nether, floor, "warped_forest", "crimson_forest");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(crimsonAbove, floor, Blocks.CRIMSON_NYLIUM.defaultBlockState()),
                "Crimson nylium must accept crimson forest above");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(crimsonAbove, floor, Blocks.WARPED_NYLIUM.defaultBlockState()),
                "Warped nylium must not use the floor biome when crimson forest is above");
        helper.assertTrue(BiomePlacementRestrictions.allowedMask(
                vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get()) == BiomePlacementRestrictions.TROPICAL,
                "Rich farmland belongs to tropical; ordinary farmland has separate restrictions");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void giantMushroomHabitat(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 80, 0);
        ServerLevel snowy = dimension(helper, GameplayDimensions.SNOWY);
        helper.assertTrue(BlockHelper.canGrowGiantMushroom(dimension(helper, GameplayDimensions.TROPICAL), pos),
                "Tropical dimension should permit giant mushrooms");
        helper.assertTrue(BlockHelper.canGrowGiantMushroom(biomeBoundary(snowy, pos, "mushroom_caves", "snowy_forest"), pos),
                "Mushroom caves should permit giant mushrooms");
        helper.assertFalse(BlockHelper.canGrowGiantMushroom(biomeBoundary(snowy, pos, "snowy_forest", "mushroom_caves"), pos),
                "Snowy forest alone should not permit giant mushrooms");
        helper.assertTrue(BlockHelper.canGrowGiantMushroom(helper.getLevel(), pos),
                "Unconfigured worlds must retain their original giant mushroom availability");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deepDarkBonemealWinsOverTheDesertTag(GameTestHelper helper) throws ReflectiveOperationException {
        ServerLevel desert = dimension(helper, GameplayDimensions.DESERT);
        BlockPos floor = new BlockPos(0, 80, 0);
        LevelReader deepDark = biomeBoundary(desert, floor, "deep_dark", "deep_dark");
        helper.assertTrue(deepDark.getBiome(floor).is(ModTags.Biomes.desertBiomes)
                        && deepDark.getBiome(floor).is(ModTags.Biomes.deepDarkBiomes),
                "Regression fixture must use the loaded deep-dark biome's overlapping desert/deep-dark tags");
        var eligible = BoneMeal.class.getDeclaredMethod("isBiomeSurfaceBonemealTarget", LevelReader.class, BlockPos.class, BlockState.class);
        eligible.setAccessible(true);
        helper.assertTrue((boolean) eligible.invoke(null, deepDark, floor, Blocks.SCULK.defaultBlockState()),
                "Deep-dark sculk must remain a bonemeal target even though deep dark also belongs to desert_biomes");
        helper.assertFalse((boolean) eligible.invoke(null, deepDark, floor, Blocks.SAND.defaultBlockState()),
                "Deep dark must use its sculk floor rule instead of the broad desert sand rule");
        var selectFeatures = BoneMeal.class.getDeclaredMethod("biomeBonemealFeaturesAt", LevelReader.class, BlockPos.class);
        selectFeatures.setAccessible(true);
        Object features = selectFeatures.invoke(null, deepDark, floor.above());
        helper.assertTrue(features != null, "Deep-dark bonemeal must have a feature palette");
        String[][] expected = {{"floor", "deep_dark/floor/grass"}, {"fullLiquid", "deep_dark/water/water_plants_full"},
                {"shallowLiquid", "deep_dark/water/water_plants_shallow_full"}};
        var registry = desert.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (String[] entry : expected) {
            var accessor = features.getClass().getDeclaredMethod(entry[0]);
            accessor.setAccessible(true);
            ResourceKey<?> key = (ResourceKey<?>) accessor.invoke(features);
            helper.assertTrue(key.location().equals(AOEMod.makeID(entry[1])),
                    "Deep dark selected the wrong " + entry[0] + " bonemeal palette: " + key.location());
            helper.assertTrue(registry.containsKey(key.location()), "Selected bonemeal palette must exist: " + key.location());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftingDependenciesRestrictFinishedBlocks(GameTestHelper helper) {
        ServerLevel tropical = dimension(helper, GameplayDimensions.TROPICAL);
        ServerLevel savanna = dimension(helper, GameplayDimensions.SAVANNA);
        ServerLevel plains = dimension(helper, GameplayDimensions.PLAINS);
        ServerLevel snowy = dimension(helper, GameplayDimensions.SNOWY);
        ServerLevel desert = dimension(helper, GameplayDimensions.DESERT);
        ServerLevel nether = dimension(helper, GameplayDimensions.NETHER);
        BlockPos pos = new BlockPos(0, 80, 0);
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(tropical, pos, Blocks.TORCH.defaultBlockState()),
                "Charcoal should make torches available in tropical");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(nether, pos, Blocks.TORCH.defaultBlockState()),
                "Imported coal and Nether sticks should make torches available in Nether");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(tropical, pos, Blocks.IRON_TRAPDOOR.defaultBlockState()),
                "Tropical has no iron for iron trapdoors");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(savanna, pos, Blocks.IRON_TRAPDOOR.defaultBlockState()),
                "Savanna has no iron for iron trapdoors");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(nether, pos, Blocks.IRON_TRAPDOOR.defaultBlockState()),
                "Iron is unavailable in desert and therefore cannot transfer to Nether");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(plains, pos, Blocks.IRON_TRAPDOOR.defaultBlockState()),
                "Plains iron should permit iron trapdoors");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(snowy, pos, Blocks.IRON_TRAPDOOR.defaultBlockState()),
                "Snowy iron should permit iron trapdoors");
        List<Block> spruceProducts = new ArrayList<>(List.of(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS,
                Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR));
        for (String path : new String[]{"hollow_spruce_log", "vertical_spruce_planks"}) {
            ResourceLocation id = new ResourceLocation("quark", path);
            if (ForgeRegistries.BLOCKS.containsKey(id)) spruceProducts.add(ForgeRegistries.BLOCKS.getValue(id));
        }
        for (Block block : spruceProducts) {
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(snowy, pos, block.defaultBlockState()),
                    "Snowy spruce wood should permit " + block);
            for (ServerLevel other : new ServerLevel[]{tropical, savanna, plains, desert, nether}) {
                helper.assertFalse(BiomePlacementRestrictions.isAllowed(other, pos, block.defaultBlockState()),
                        "Spruce building products must remain snowy-only: " + block + " in " + other.dimension().location());
            }
        }
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (!ForgeRegistries.BLOCKS.getKey(block).getPath().equals("copper_trapdoor")) continue;
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(desert, pos, block.defaultBlockState()),
                    "Desert copper should permit the backported copper trapdoor");
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(nether, pos, block.defaultBlockState()),
                    "Transferred copper products should permit the backported copper trapdoor in Nether");
        }
        for (Block block : new Block[]{Blocks.COCOA, Blocks.CAVE_VINES}) {
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(tropical, pos, block.defaultBlockState()),
                    "Harvested " + block + " must remain available despite optional compression/decompression recipes");
        }
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(desert, pos, Blocks.CACTUS.defaultBlockState()),
                "Desert cactus must seed its compression cycle");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(tropical, pos, Blocks.CACTUS.defaultBlockState()),
                "The documented prickly-pear support cactus is desert-only");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(plains, pos, Blocks.SUGAR_CANE.defaultBlockState()),
                "Naturally harvested sugar cane must seed its optional storage-block cycle");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(snowy, pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                "Snowy sweet berries must seed their sack/unpacking cycle");
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(tropical, pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                "The dimension documents supersede the old tropical sweet-berry placement rule");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(biomeBoundary(nether, pos, "soul_sand_valley", "soul_sand_valley"),
                        pos, Blocks.NETHER_WART.defaultBlockState()),
                "Natural Nether wart must seed its storage-sack cycle in its required biome");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void campfiresDoNotDamagePlayers(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block block : new Block[]{Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE}) {
            BlockState state = block.defaultBlockState().setValue(CampfireBlock.LIT, true);
            float health = player.getHealth();
            state.entityInside(helper.getLevel(), pos, player);
            helper.assertTrue(player.getHealth() == health, "Lit " + block + " must not damage a player");
        }
        helper.succeed();
    }

    private static ServerLevel dimension(GameTestHelper helper, ResourceKey<Level> key) {
        ServerLevel level = helper.getLevel().getServer().getLevel(key);
        helper.assertTrue(level != null, "Required gameplay dimension is not loaded: " + key.location());
        return level;
    }

    private static ServerLevelAccessor biomeBoundary(ServerLevel level, BlockPos floor, String here, String above) {
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> hereBiome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(here)));
        Holder<Biome> aboveBiome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(above)));
        return (ServerLevelAccessor) Proxy.newProxyInstance(PlantGameplayTests.class.getClassLoader(),
                new Class<?>[]{ServerLevelAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "getBiome" -> floor.above().equals(args[0]) ? aboveBiome : hereBiome;
                    case "toString" -> "Test biome boundary";
                    default -> throw new UnsupportedOperationException("Unexpected world access in placement rule: " + method.getName());
                });
    }
}
