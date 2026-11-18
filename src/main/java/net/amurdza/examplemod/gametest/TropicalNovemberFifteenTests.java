package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.*;
import net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalNovemberFifteenTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void restoredIslandsKeepCherryRainforestAndOriginalFlowerFaunaAllocations(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var source = tropical.getChunkSource().getGenerator().getBiomeSource();
        var sampler = tropical.getChunkSource().randomState().sampler();
        var nativeLayout = DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var nativeIsland = nativeLayout.island(0, 0);
        helper.assertTrue(source.getNoiseBiome((int) nativeIsland.x() >> 2, 20, (int) nativeIsland.z() >> 2, sampler)
                .is(new ResourceLocation("aoemod:cherry_grove")), "New generation must select the restored cherry center");
        helper.assertTrue(TropicalLayout.COLUMN_SPACING == 920 && TropicalLayout.CHERRY_RADIUS == 138
                && TropicalLayout.FOREST_RADIUS == 270 && TropicalLayout.MANGROVE_LAND_RADIUS == 300
                && TropicalLayout.ISLAND_RADIUS == 360, "Restore the complete original island dimensions");
        for (long seed : new long[]{0, 1, -1, 7331, 841237, -92674112}) {
            var layout = new TropicalLayout(seed);
            var island = layout.island(0, 0);
            var plan = TropicalIslandPlan.forIsland(layout, island);
            Set<String> forest = new HashSet<>(), grove = new HashSet<>();
            plan.rainforest().forEach(c -> forest.add(plan.rainforestSpecies(c.x(), c.z())));
            plan.cherry().forEach(c -> { grove.add(plan.cherryCrop(c.x(), c.z())); grove.add(plan.cherryFlower(c.x(), c.z())); });
            helper.assertTrue(forest.containsAll(Set.of("environmental:blue_delphinium", "environmental:purple_delphinium")),
                    "Restored rainforest must allocate both delphiniums");
            helper.assertTrue(grove.containsAll(Set.of("biomesoplenty:pink_daffodil", "biomeswevegone:japanese_orchid",
                    "minecraft:allium", "minecraft:peony", "minecraft:lilac", "minecraft:pink_petals",
                    "farmersdelight:wild_carrots", "legumedelight:wild_beans")),
                    "The independent cherry biome must recover its entire flora");
            for (int angle = 0; angle < 16; angle++) for (double radius : new double[]{0,
                    TropicalLayout.CHERRY_RADIUS - .1, TropicalLayout.CHERRY_RADIUS + .1,
                    269.9, 270.1, 299.9, 300.1, 359.9, 360.1}) {
                var at = layout.sample(island.x() + radius * Math.cos(angle * Math.PI / 8),
                        island.z() + radius * Math.sin(angle * Math.PI / 8), island);
                helper.assertTrue(at.biome() == (radius < TropicalLayout.CHERRY_RADIUS ? 3 : radius < 270 ? 2 : radius < 360 ? 1 : 0),
                        "Restored circular habitats disagree at radius " + radius);
                if (radius > 270 && radius < 300) helper.assertTrue(Math.round(at.height()) == 63, "Dry mangrove remains Y63");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bambooPatchesContainTenStemsAndStopBelowLeavesOrSolidCeilings(GameTestHelper helper) {
        for (String habitat : List.of("cherry_grove", "rainforest", "mangrove_swamp")) for (int clearance : new int[]{2, 9, 28, 0}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) {
                blocks.put(new BlockPos(x, 101, z), Blocks.FERN.defaultBlockState());
                if (clearance > 0) blocks.put(new BlockPos(x, 101 + clearance, z),
                        ((x + z) % 2 == 0 ? Blocks.JUNGLE_LEAVES : Blocks.STONE).defaultBlockState());
            }
            var original = Map.copyOf(blocks);
            var world = TropicalTreeShapeTests.fixture(helper, blocks, habitat, 0, true);
            int count = TropicalBamboo.placePatch(world, 0, 0, RandomSource.create(771));
            if (!habitat.equals("cherry_grove")) {
                helper.assertTrue(count == 0 && blocks.equals(original), "Bamboo must remain exclusive to cherry groves");
                continue;
            }
            helper.assertTrue(count == 10, "Eligible patches must commit all ten stems");
            long roots = blocks.entrySet().stream().filter(e -> e.getKey().getY() == 101 && e.getValue().is(Blocks.BAMBOO)).count();
            helper.assertTrue(roots == 10, "Each patch needs ten distinct root columns");
            for (var entry : blocks.entrySet()) {
                if (!entry.getValue().is(Blocks.BAMBOO)) continue;
                BlockPos at = entry.getKey();
                helper.assertTrue(at.getX() >= 0 && at.getX() < 6 && at.getZ() >= 0 && at.getZ() < 6,
                        "Every stem must stay inside the same six-by-six patch");
                if (clearance > 0) helper.assertTrue(at.getY() < 101 + clearance, "Bamboo overwrote its canopy stop");
            }
            if (clearance > 0) {
                helper.assertTrue(blocks.entrySet().stream().filter(e -> e.getKey().getY() == 100 + clearance
                        && e.getValue().is(Blocks.BAMBOO)).count() == 10, "Every stem must reach immediately below the canopy");
                original.forEach((pos, state) -> {
                    if (pos.getY() > 101) helper.assertTrue(blocks.get(pos).equals(state), "Solid/leaves ceiling changed");
                });
            }
            helper.assertFalse(blocks.values().stream().anyMatch(s -> s.is(Blocks.PODZOL)), "Bamboo must not make podzol");
            for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++)
                helper.assertTrue(world.getBlockState(new BlockPos(x, 100, z)).is(Blocks.MOSS_BLOCK), "Existing soil changed");
        }
        Map<BlockPos, BlockState> blocked = new HashMap<>();
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) blocked.put(new BlockPos(x, 101, z), Blocks.BLUE_ORCHID.defaultBlockState());
        for (int x = 0; x < 9; x++) blocked.put(new BlockPos(x % 6, 101, x / 6), Blocks.AIR.defaultBlockState());
        var before = Map.copyOf(blocked);
        helper.assertTrue(TropicalBamboo.placePatch(TropicalTreeShapeTests.fixture(helper, blocked, "cherry_grove", 0, true),
                        0, 0, RandomSource.create(18)) == 0 && blocked.equals(before),
                "A blocked patch with only nine clear columns must roll back without consuming flowers");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeMangroveRootsReplaceFlowersButFloraCannotReplaceRoots(GameTestHelper helper) throws Exception {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var tree = tropical.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:rainforest/tree/natural_mangrove"));
        for (String crop : List.of("farmersdelight:wild_cabbages", "farmersdelight:wild_rice", "hearthandharvest:wild_cotton")) {
            var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(crop));
            helper.assertFalse(TropicalPlantIntegrity.flowerRootMayReplace(block.defaultBlockState()),
                    "Shared flower tags must not erase the reserved wild-crop quota: " + crop);
        }
        var config = (TreeConfiguration) tree.config();
        var roots = config.rootPlacer.orElseThrow();
        var accepts = MangroveRootPlacer.class.getDeclaredMethod("canPlaceRoot", LevelSimulatedReader.class, BlockPos.class);
        accepts.setAccessible(true);
        var place = RootPlacer.class.getDeclaredMethod("placeRoot", LevelSimulatedReader.class, BiConsumer.class,
                RandomSource.class, BlockPos.class, TreeConfiguration.class);
        place.setAccessible(true);
        for (var flower : List.of(Blocks.BLUE_ORCHID, Blocks.PINK_PETALS, Blocks.PITCHER_PLANT)) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            BlockPos pos = new BlockPos(8, 101, 8);
            blocks.put(pos, flower.defaultBlockState());
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 0, true);
            helper.assertTrue((boolean) accepts.invoke(roots, world, pos), "Native root placer rejected " + flower);
            var staged = new GrowthPlacementBuffer(world);
            BiConsumer<BlockPos, BlockState> setter = (at, state) -> staged.setBlock(at, state, 2);
            place.invoke(roots, staged, setter, RandomSource.create(11), pos, config);
            helper.assertTrue(staged.commit() && TropicalPlantIntegrity.isMangroveRoot(world.getBlockState(pos)),
                    "Actual native root placement must replace the flower");
            helper.assertFalse(DocumentedFloraFeature.placePlant(world, pos, "minecraft:blue_orchid", RandomSource.create(2)),
                    "A later flower must not consume a root");
            var later = new GrowthPlacementBuffer(world);
            helper.assertFalse(later.setBlock(pos, Blocks.PITCHER_PLANT.defaultBlockState(), 2),
                    "Buffered native vegetation must not consume a root");
        }
        // Exercise the outer atomic tree validator as well: root replacement may
        // quietly remove the opposite half of a tall flower in the same transaction.
        var placement = TropicalVegetation.class.getDeclaredMethod("placeTree",
                net.minecraft.world.level.levelgen.feature.FeaturePlaceContext.class, BlockPos.class, String.class);
        placement.setAccessible(true);
        var layout = DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var owner = TropicalIslandPlan.forIsland(layout, layout.island(0, 0)).mounds().get(0);
        BlockPos origin = new BlockPos(owner.x() * 16 + 8, 101, owner.z() * 16 + 8);
        int successfulTrees = 0;
        for (int seed = 0; seed < 12; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                blocks.put(origin.offset(x, 0, z), Blocks.PITCHER_PLANT.defaultBlockState());
                blocks.put(origin.offset(x, 1, z), Blocks.PITCHER_PLANT.defaultBlockState().setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            }
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 0, true);
            var context = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(), world,
                    tropical.getChunkSource().getGenerator(), RandomSource.create(seed), origin,
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
            if ((boolean) placement.invoke(null, context, origin, "aoemod:rainforest/tree/natural_mangrove")) {
                successfulTrees++;
                helper.assertTrue(blocks.entrySet().stream().anyMatch(e -> e.getKey().getY() == 101
                                && TropicalPlantIntegrity.isMangroveRoot(e.getValue())),
                        "A native accepted mangrove must actually replace lower flower cells with roots");
                for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.PITCHER_PLANT)
                        && entry.getValue().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                    helper.assertTrue(world.getBlockState(entry.getKey().below()).is(Blocks.PITCHER_PLANT),
                            "Root replacement left a floating upper flower half");
            }
        }
        helper.assertTrue(successfulTrees > 0, "Atomic tree validation must allow roots through a tall flower carpet");
        var registry = tropical.registryAccess().registryOrThrow(Registries.BIOME);
        var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, tropical, registry, null);
        var region = new WorldGenRegion(tropical, List.of(chunk), ChunkStatus.FEATURES, 0);
        BlockPos pos = new BlockPos(8, 100, 8);
        region.setBlock(pos, Blocks.MANGROVE_ROOTS.defaultBlockState(), 2);
        region.setBlock(pos, Blocks.BLUE_ORCHID.defaultBlockState(), 2);
        helper.assertTrue(region.getBlockState(pos).is(Blocks.MANGROVE_ROOTS), "Actual WorldGenRegion bypassed root precedence");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1600)
    public static void nativeCherryDecorationRetainsFinalBambooAndExportsViews(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var island = layout.island(581, 733);
        int cx = (int) Math.floor(island.x() + TropicalLayout.CHERRY_RADIUS - 6) >> 4, cz = (int) Math.floor(island.z()) >> 4;
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) chunks.add(new ChunkPos(x, z));
        for (var chunk : chunks) level.getChunk(chunk.x, chunk.z);
        int complete = 0, cherries = 0, nests = 0;
        BlockPos bambooView = null;
        for (var chunk : chunks) {
            for (var blockEntity : level.getChunk(chunk.x, chunk.z).getBlockEntities().values()) {
                if (blockEntity.getBlockState().is(Blocks.BEE_NEST)) {
                    nests++;
                    helper.assertTrue(blockEntity.saveWithFullMetadata().getList("Bees", 10).size() >= 2,
                            "The two established central nests must stay populated");
                }
            }
            List<BlockPos> roots = new ArrayList<>();
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = 63; y < 145; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.CHERRY_LOG)) cherries++;
                    if (state.is(Blocks.BAMBOO) && !level.getBlockState(pos.below()).is(Blocks.BAMBOO)) {
                        roots.add(pos);
                        helper.assertTrue(GameplayDimensions.biomeIs(level, pos, "cherry_grove"), "A natural stem left cherry grove");
                        helper.assertFalse(level.getBlockState(pos.below()).is(Blocks.PODZOL), "Natural bamboo made podzol");
                    }
                }
            }
            if (roots.isEmpty()) continue;
            helper.assertTrue(roots.size() <= 10 && roots.stream().allMatch(pos -> pos.getY() == 76),
                    "Natural bamboo must remain a Y75 patch of at most ten stems: " + roots.size());
            int dx = roots.stream().mapToInt(BlockPos::getX).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow();
            int dz = roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
            helper.assertTrue(dx < 6 && dz < 6, "Actual patch exceeded six-by-six");
            complete++;
            if (bambooView == null) bambooView = roots.get(0);
        }
        helper.assertTrue(complete >= 1 && cherries > 0, "The restored natural grove needs cherry trees and final bamboo: " + complete + "/" + cherries);
        List<Map<String, Object>> views = List.of(
                Map.of("label", "restored-tropical-island", "dimension", level.dimension().location().toString(),
                        "x", island.x(), "y", 320, "z", island.z() + 300, "yaw", 180, "pitch", 65),
                Map.of("label", "cherry-final-bamboo", "dimension", level.dimension().location().toString(),
                        "x", bambooView.getX() + 7.5, "y", bambooView.getY() + 2, "z", bambooView.getZ() + 7.5, "yaw", 135, "pitch", -12));
        Files.writeString(Path.of("tropical-round2-views.json"), new GsonBuilder().setPrettyPrinting().create().toJson(views));
        helper.succeed();
    }
}
