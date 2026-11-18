package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.amurdza.examplemod.worldgen.AtomicTreeGrowth;
import net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalVegetationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalGrassAndFoliageMatchRainforest(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var rainforest = registry.get(new ResourceLocation("aoemod:rainforest"));
        for (String name : new String[]{"mangrove_swamp", "cherry_grove", "coral_reef"}) {
            var biome = registry.get(new ResourceLocation("aoemod", name));
            helper.assertTrue(biome.getBaseTemperature() == rainforest.getBaseTemperature(),
                    name + " must share rainforest temperature");
            helper.assertTrue(biome.getGrassColor(0, 0) == rainforest.getGrassColor(0, 0)
                            && biome.getFoliageColor() == rainforest.getFoliageColor(),
                    name + " must use rainforest grass/foliage without swamp or cherry colour overrides");
        }
        var cherry = registry.get(new ResourceLocation("aoemod:cherry_grove"));
        Set<String> cherryFeatures = vegetation(cherry);
        helper.assertTrue(cherryFeatures.contains("aoemod:trees_cherry_spaced")
                        && !cherryFeatures.contains("minecraft:trees_cherry"),
                "Cherry groves must use the spaced vanilla-shape pass exactly once");
        var placedFeatures = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var spacedCherry = placedFeatures.get(new ResourceLocation("aoemod:trees_cherry_spaced"));
        helper.assertTrue(spacedCherry.feature().unwrapKey().orElseThrow().location()
                        .equals(new ResourceLocation("aoemod:tropical_cherry_trees")) && spacedCherry.placement().isEmpty(),
                "A single spatial pass must own the seeded cherry positions and survival checks");
        helper.assertFalse(cherryFeatures.contains("aoemod:rainforest/forest_floor"),
                "Cherry groves must not receive an additional legacy rainforest tree pass");
        helper.assertFalse(vegetation(rainforest).contains("minecraft:trees_cherry")
                        || vegetation(rainforest).contains("minecraft:trees_mangrove")
                        || vegetation(rainforest).contains("aoemod:rainforest/forest_floor"),
                "Rainforest must receive only its native tree pass, without old mixed trees or foreign tropical trees");
        helper.succeed();
    }

    private static Set<String> vegetation(net.minecraft.world.level.biome.Biome biome) {
        Set<String> features = new HashSet<>();
        var steps = biome.getGenerationSettings().features();
        int step = net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        if (steps.size() > step) steps.get(step).forEach(feature -> feature.unwrapKey()
                .ifPresent(key -> features.add(key.location().toString())));
        return features;
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalGroundSearchIgnoresExistingTreeCrowns(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int y = 101; y < 120; y++) blocks.put(new BlockPos(0, y, 0), Blocks.JUNGLE_LOG.defaultBlockState());
        blocks.put(new BlockPos(0, 120, 0), Blocks.JUNGLE_LEAVES.defaultBlockState());
        WorldGenLevel world = fixture(helper, "rainforest", blocks, 122);
        helper.assertTrue(new BlockPos(0, 101, 0).equals(TropicalVegetation.findGround(world, 0, 0)),
                "Natural trees must start at the actual soil, never on another tree's crown");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rainforestRestoresNativeJunglesAndMixedCanopy(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = net.amurdza.examplemod.worldgen.DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var island = layout.island(0, 0);
        var forestChunk = net.amurdza.examplemod.worldgen.TropicalIslandPlan.forIsland(layout, island).rainforest().stream()
                .filter(chunk -> {
                    var sample = layout.sample(chunk.x() * 16 + 8, chunk.z() * 16 + 8);
                    return sample.distance() > 48 && sample.distance() < sample.forestEdge() - 12;
                }).findFirst().orElseThrow();
        int minX = forestChunk.x() * 16, minZ = forestChunk.z() * 16;
        int tallJungleSamples = 0;
        // This is a mixed lottery, not a jungle quota in every chunk. Mushroom
        // clearance and added branch logs legitimately change later retries.
        for (int sample = 0; sample < 16; sample++) {
            long seed = 50881L + sample * 104729L;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 64, tropical, 63);
            boolean placed = TropicalVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world,
                    tropical.getChunkSource().getGenerator(), RandomSource.create(seed), new BlockPos(minX, 0, minZ),
                    NoneFeatureConfiguration.INSTANCE));
            helper.assertTrue(placed, "The rainforest pass must build its actual trees and understory; seed=" + seed);
            if (blocks.entrySet().stream().anyMatch(entry -> entry.getValue().is(Blocks.JUNGLE_LOG)
                    && entry.getKey().getY() >= 75)) tallJungleSamples++;
            helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(Blocks.MANGROVE_LOG)
                            || state.is(Blocks.MANGROVE_ROOTS)),
                    "Rainforest must not borrow mangrove trees from the swamp; seed=" + seed);
            Set<Long> canopyColumns = new HashSet<>();
            blocks.forEach((pos, state) -> {
                if (state.is(BlockTags.LEAVES) && pos.getX() >= minX && pos.getX() < minX + 16
                        && pos.getZ() >= minZ && pos.getZ() < minZ + 16) {
                    canopyColumns.add(((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL));
                }
            });
            helper.assertTrue(canopyColumns.size() >= 128,
                    "Mixed rainforest crowns must cover at least half the sampled chunk; seed=" + seed
                            + ", actual=" + canopyColumns.size());
        }
        helper.assertTrue(tallJungleSamples > 0,
                "The native rainforest mixture must include full jungle trunks beyond the obsolete fixed Y74 canopy");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void flowersShareSmallSoftEdgedPatchesInsteadOfSingletonsOrCarpets(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:torchflower", "minecraft:rose_bush", "minecraft:allium", "minecraft:lilac",
                "minecraft:melon", "minecraft:red_mushroom", "minecraft:brown_mushroom", "farmersdelight:wild_tomatoes"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
            int count = TropicalVegetation.placePatch(world, new BlockPos(8, 101, 8), "rainforest", id,
                    RandomSource.create(77281));
            var state = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState();
            boolean flower = state.is(BlockTags.FLOWERS);
            boolean tall = flower && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF);
            helper.assertTrue(id.equals("minecraft:melon") ? count == 2
                            : !flower ? count == 5 : count >= (tall ? 3 : 5) && count <= (tall ? 5 : 8),
                    id + " must form a consistent small group on open valid soil; actual=" + count);
            helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= 6 && pos.getX() <= 11
                            && pos.getZ() >= 6 && pos.getZ() <= 11),
                    "All flower, melon, mushroom and crop roots must remain in a six-by-six footprint");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void reservedRainforestMelonsStopAtTwoWhileOtherCropsKeepFive(GameTestHelper helper)
            throws ReflectiveOperationException {
        var place = TropicalVegetation.class.getDeclaredMethod("placeReservedLandPatch", WorldGenLevel.class,
                int.class, int.class, String.class, String.class, RandomSource.class);
        place.setAccessible(true);
        for (String species : new String[]{"minecraft:melon", "farmersdelight:wild_tomatoes", "pineapple_delight:pineapple_wild_crop"}) {
            int expected = species.equals("minecraft:melon") ? 2 : 5;
            var plant = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
            for (boolean blocked : new boolean[]{false, true}) for (int seed = 0; seed < 16; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                if (blocked) {
                    for (int x = 0; x < 8; x++) for (int z = 0; z < 16; z++) {
                        blocks.put(new BlockPos(x, 101, z), Blocks.JUNGLE_LOG.defaultBlockState());
                    }
                }
                var original = Map.copyOf(blocks);
                WorldGenLevel world = fixture(helper, "rainforest", blocks, blocked ? 104 : 101);
                int placed = (int) place.invoke(null, world, 0, 0, "rainforest", species, RandomSource.create(seed));
                var roots = blocks.entrySet().stream().filter(entry -> entry.getValue().is(plant)
                        && (!entry.getValue().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        || entry.getValue().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)).map(Map.Entry::getKey).toList();
                helper.assertTrue(placed == expected && roots.size() == expected,
                        "Reserved patch retries must commit exactly " + expected + " roots for " + species + "; actual=" + roots.size());
                int lowX = roots.stream().mapToInt(BlockPos::getX).min().orElseThrow();
                int highX = roots.stream().mapToInt(BlockPos::getX).max().orElseThrow();
                int lowZ = roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
                int highZ = roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
                helper.assertTrue(lowX >= 0 && highX < 16 && lowZ >= 0 && highZ < 16
                                && highX - lowX < 6 && highZ - lowZ < 6,
                        "A reserved crop must retain a single six-by-six footprint inside its owning chunk");
                original.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                        "Reserved crops must preserve obstructing trees"));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lavenderForestTreesAcceptTheirLegacyNullRootPlacer(GameTestHelper helper) {
        for (String name : List.of("big_jacaranda_tree", "big_flowering_oak_tree")) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 101, helper.getLevel());
            var tree = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("biomesoplenty", name));
            helper.assertTrue(tree != null, "The regression requires the native BOP tree " + name);
            boolean placed = tree.place(world, helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(17), new BlockPos(8, 101, 8));
            helper.assertTrue(placed && blocks.values().stream().anyMatch(state -> state.is(BlockTags.LOGS)),
                    "Restored native tree must place without a null root-placer crash: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalBranchesRejectForeignWoodWithoutLeavingPartialTrees(GameTestHelper helper) {
        for (int foreignX : new int[]{11, 12}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            blocks.put(new BlockPos(foreignX, 106, 8), Blocks.JUNGLE_LOG.defaultBlockState());
            Map<BlockPos, BlockState> original = Map.copyOf(blocks);
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
            boolean placed = stageTree(world, staged -> {
                for (int y = 101; y <= 106; y++) staged.setBlock(new BlockPos(8, y, 8), Blocks.OAK_LOG.defaultBlockState(), 2);
                for (int x = 9; x <= 11; x++) staged.setBlock(new BlockPos(x, 106, 8), Blocks.OAK_LOG.defaultBlockState(), 2);
                staged.setBlock(new BlockPos(8, 107, 8), Blocks.OAK_LEAVES.defaultBlockState(), 2);
            });
            helper.assertFalse(placed, "An intersecting or face-joined branch must reject the entire tree attempt");
            helper.assertTrue(blocks.equals(original), "Rejected branches must retain the foreign tree and leave no new trunk/crown");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void connectedTwoByTwoTrunksAllowOverlappingLeafCrowns(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(10, 108, 8), Blocks.JUNGLE_LEAVES.defaultBlockState());
        WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
        boolean placed = stageTree(world, staged -> {
            for (int x = 8; x <= 9; x++) for (int z = 8; z <= 9; z++) for (int y = 101; y <= 107; y++) {
                staged.setBlock(new BlockPos(x, y, z), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
            }
            staged.setBlock(new BlockPos(10, 107, 8), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
            for (int x = 7; x <= 11; x++) {
                staged.setBlock(new BlockPos(x, 108, 8), Blocks.DARK_OAK_LEAVES.defaultBlockState(), 2);
            }
        });
        helper.assertTrue(placed, "A legitimate wide trunk and its own branches must remain connected; leaf crowns may overlap");
        helper.assertTrue(blocks.get(new BlockPos(9, 107, 9)).is(Blocks.DARK_OAK_LOG),
                "Collision protection must preserve the complete two-by-two trunk");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyMushroomCapsReplaceLeavesWhileStemsAndTreeWoodPreserveCrowns(GameTestHelper helper) {
        for (BlockState proposed : new BlockState[]{Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(),
                Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(), Blocks.MUSHROOM_STEM.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState()}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            blocks.put(new BlockPos(11, 107, 8), Blocks.JUNGLE_LEAVES.defaultBlockState());
            Map<BlockPos, BlockState> original = Map.copyOf(blocks);
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
            boolean placed = stageTree(world, staged -> {
                staged.setBlock(new BlockPos(8, 101, 8), Blocks.MUSHROOM_STEM.defaultBlockState(), 2);
                staged.setBlock(new BlockPos(11, 107, 8), proposed, 2);
            });
            if (proposed.is(Blocks.OAK_LOG) || proposed.is(Blocks.MUSHROOM_STEM)) {
                helper.assertFalse(placed, "Tree wood and mushroom stems must preserve existing leaf crowns");
                helper.assertTrue(blocks.equals(original), "Rejected crown intersections must be atomic");
            } else {
                helper.assertTrue(placed && blocks.get(new BlockPos(11, 107, 8)).is(proposed.getBlock()),
                        "Only red and brown mushroom caps may replace existing leaves");
            }
        }
        for (BlockState mushroom : new BlockState[]{Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(),
                Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(), Blocks.MUSHROOM_STEM.defaultBlockState()}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            blocks.put(new BlockPos(11, 107, 8), mushroom);
            Map<BlockPos, BlockState> original = Map.copyOf(blocks);
            WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
            boolean placed = stageTree(world, staged ->
                    staged.setBlock(new BlockPos(11, 107, 8), Blocks.JUNGLE_LEAVES.defaultBlockState(), 2));
            helper.assertFalse(placed, "Leaves must never replace giant mushroom caps or stems");
            helper.assertTrue(blocks.equals(original), "Rejected foliage must preserve the complete mushroom");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeFancyOakDoesNotGraftOntoAnExistingForeignBranch(GameTestHelper helper) {
        Map<BlockPos, BlockState> baseline = new HashMap<>();
        WorldGenLevel empty = fixture(helper, "rainforest", baseline, 101);
        var feature = empty.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:rainforest/tree/natural_fancy_oak"));
        BlockPos origin = new BlockPos(8, 101, 8);
        helper.assertTrue(feature.place(empty, empty.getLevel().getChunkSource().getGenerator(), RandomSource.create(5), origin),
                "Control fancy oak must generate its actual native branching shape");
        BlockPos collision = baseline.entrySet().stream().filter(entry -> TreeGrowthCollisionRules.isWood(entry.getValue())
                        && entry.getKey().getY() >= 105
                        && Math.abs(entry.getKey().getX() - 8) + Math.abs(entry.getKey().getZ() - 8) >= 2)
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(collision, Blocks.JUNGLE_LOG.defaultBlockState());
        WorldGenLevel occupied = fixture(helper, "rainforest", blocks, 101);
        feature.place(occupied, occupied.getLevel().getChunkSource().getGenerator(), RandomSource.create(5), origin);
        helper.assertTrue(blocks.get(collision).is(Blocks.JUNGLE_LOG), "Actual fancy-oak generation must preserve the foreign branch");
        blocks.forEach((pos, state) -> {
            if (!pos.equals(collision) && TreeGrowthCollisionRules.isWood(state)) {
                helper.assertTrue(pos.distManhattan(collision) > 1,
                        "An accepted alternate native shape must not graft any new wood onto the old branch");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cherryFlowersUseRequestedCountsAndDoNotShareAdjacentRootCells(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, "cherry_grove", blocks, 101);
        BlockPos centre = cherryInterior(helper);
        int patch = 0;
        for (String flower : new String[]{"minecraft:pink_petals", "minecraft:allium", "minecraft:peony", "minecraft:lilac", "biomesoplenty:pink_daffodil", "biomeswevegone:japanese_orchid"}) {
            WorldGenLevel open = fixture(helper, "cherry_grove", new HashMap<>(), 101);
            int count = TropicalVegetation.placePatch(open, centre, "cherry_grove", flower, RandomSource.create(77281));
            if (flower.equals("minecraft:pink_petals")) {
                helper.assertTrue(count >= 6 && count <= 10, "Pink petals must fill six to ten cells in their sparse patch");
            } else if (Set.of("minecraft:peony", "minecraft:lilac", "biomeswevegone:japanese_orchid").contains(flower)) {
                helper.assertTrue(count >= 3 && count <= 5, "Tall flowers must stop after at most five successful placements");
            } else {
                helper.assertTrue(count >= 5 && count <= 8, "Other single-block flowers must fill five to eight cells: " + flower);
            }
            TropicalVegetation.placePatch(world, centre.offset(patch++ * 4, 0, 0), "cherry_grove", flower,
                    RandomSource.create(77281));
        }
        WorldGenLevel crops = fixture(helper, "cherry_grove", new HashMap<>(), 101);
        int cropCount = TropicalVegetation.placePatch(crops, centre, "cherry_grove",
                "farmersdelight:wild_carrots", RandomSource.create(77281));
        helper.assertTrue(cropCount >= 3 && cropCount <= 6, "Wild crop patches must retain the same smaller footprint density");
        helper.assertTrue(blocks.values().stream().filter(state -> state.is(Blocks.PINK_PETALS))
                        .allMatch(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FLOWER_AMOUNT) == 4),
                "Every world-generated pink-petal block must contain four petals");
        var flowers = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                        && entry.getValue().is(BlockTags.FLOWERS)).map(Map.Entry::getKey).toList();
        for (int i = 0; i < flowers.size(); i++) for (int j = i + 1; j < flowers.size(); j++) {
            BlockPos first = flowers.get(i), second = flowers.get(j);
            helper.assertTrue(Math.abs(first.getX() - second.getX()) + Math.abs(first.getZ() - second.getZ()) >= 2,
                    "Different flower species and neighbouring patches must share the same empty-block spacing");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pinkPetalPatchesCanActuallyReachTenSpacedRoots(GameTestHelper helper) {
        Set<Integer> counts = new HashSet<>();
        BlockPos centre = cherryInterior(helper);
        for (int seed = 0; seed < 128; seed++) {
            WorldGenLevel world = fixture(helper, "cherry_grove", new HashMap<>(), 101);
            counts.add(TropicalVegetation.placePatch(world, centre, "cherry_grove",
                    "minecraft:pink_petals", RandomSource.create(net.amurdza.examplemod.worldgen.TropicalLayout.mix(seed))));
        }
        helper.assertTrue(counts.containsAll(Set.of(6, 7, 8, 9, 10)),
                "Four-neighbour spacing must permit every requested six-to-ten petal patch size on open soil: " + counts);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cherryHasContainedFlowerPatchesWithFewerTallSelections(GameTestHelper helper) {
        int shortPatches = 0, tallPatches = 0;
        var layout = net.amurdza.examplemod.worldgen.DimensionTerrain.tropicalLayout(helper.getLevel().getServer()
                .getLevel(GameplayDimensions.TROPICAL).getChunkSource().randomState().router().temperature());
        var plan = net.amurdza.examplemod.worldgen.TropicalIslandPlan.forIsland(layout, layout.island(1, 0));
        for (var chunk : plan.cherry()) {
            String flower = plan.cherryFlower(chunk.x(), chunk.z());
            if (flower == null) continue;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "cherry_grove", blocks, 101);
            BlockPos center = new BlockPos(chunk.x() * 16 + 8, 101, chunk.z() * 16 + 8);
            int count = TropicalVegetation.placePatch(world, center, "cherry_grove", flower, RandomSource.create(chunk.x() * 37L + chunk.z()));
            boolean tall = Set.of("minecraft:peony", "minecraft:lilac", "biomeswevegone:japanese_orchid").contains(flower);
            if (tall) tallPatches++; else shortPatches++;
            helper.assertTrue(tall ? count >= 3 && count <= 5 : flower.equals("minecraft:pink_petals")
                    ? count >= 6 && count <= 10 : count >= 5 && count <= 8, "Restored flower patch lost its requested size: " + flower);
            helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= center.getX() - 2 && pos.getX() <= center.getX() + 3
                    && pos.getZ() >= center.getZ() - 2 && pos.getZ() <= center.getZ() + 3), "Flower patch escaped its six-by-six footprint");
        }
        helper.assertTrue(shortPatches > 2 * tallPatches, "Tall flowers must remain less frequent than short flowers");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cherryFlowerCycleRestoresOriginalSpeciesAndWeights(GameTestHelper helper) {
        Map<String, Integer> expected = Map.of("minecraft:allium", 5, "minecraft:pink_petals", 5,
                "biomesoplenty:pink_daffodil", 5, "biomeswevegone:japanese_orchid", 2, "minecraft:peony", 2, "minecraft:lilac", 2);
        for (int seed = 0; seed < 16; seed++) {
            var layout = new net.amurdza.examplemod.worldgen.TropicalLayout(seed);
            var plan = net.amurdza.examplemod.worldgen.TropicalIslandPlan.forIsland(layout, layout.island(0, 0));
            var flowers = plan.cherry().stream().filter(c -> plan.cherryFlower(c.x(), c.z()) != null).toList();
            helper.assertTrue(flowers.size() >= 42, "Every restored grove must support two complete flower cycles");
            for (int start : new int[]{0, 21}) {
                Map<String, Integer> actual = new HashMap<>();
                for (int index = start; index < start + 21; index++) {
                    var chunk = flowers.get(index);
                    actual.merge(plan.cherryFlower(chunk.x(), chunk.z()), 1, Integer::sum);
                }
                helper.assertTrue(actual.equals(expected), "Restored flower cycle changed: " + actual);
            }
        }
        helper.succeed();
    }

    private static BlockPos cherryInterior(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = net.amurdza.examplemod.worldgen.DimensionTerrain.tropicalLayout(
                tropical.getChunkSource().randomState().router().temperature());
        var island = layout.island(1, 0);
        return BlockPos.containing(island.x(), 101, island.z());
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void eachSeededCherryRegionHasAnActualPopulatedCentralNest(GameTestHelper helper) {
        var feature = new net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature(NoneFeatureConfiguration.CODEC);
        int regionsWithNests = 0, eligibleGroves = 0;
        for (int sample = 0; sample < 1024; sample++) {
            long seed = net.amurdza.examplemod.worldgen.TropicalLayout.mix(88381L + sample * 1000003L);
            var layout = new net.amurdza.examplemod.worldgen.TropicalLayout(seed);
            var island = layout.island(sample % 2, sample / 2 % 2);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "cherry_grove", blocks, 101);
            int centerX = Math.floorDiv((int) Math.floor(island.x()), 20);
            int centerZ = Math.floorDiv((int) Math.floor(island.z()), 20);
            BlockPos central = null;
            int guaranteed = 0;
            for (int x = centerX - 1; x <= centerX + 1; x++) for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                if (net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature
                        .guaranteedNestCandidate(layout, world.getSeed(), x, z)) {
                    guaranteed++;
                    central = net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature.candidate(world.getSeed(), x, z);
                }
            }
            helper.assertTrue(guaranteed == 2, "Exactly two central trees are selected for populated nests");
            BlockPos chunkOrigin = new BlockPos(Math.floorDiv(central.getX(), 16) * 16, 0,
                    Math.floorDiv(central.getZ(), 16) * 16);
            feature.placeInLayout(new FeaturePlaceContext<>(Optional.empty(), world, world.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(seed), chunkOrigin, NoneFeatureConfiguration.INSTANCE), layout);
            var nests = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.BEE_NEST)).toList();
            eligibleGroves++;
            if (!nests.isEmpty() && nests.stream().anyMatch(entry -> world.getBlockEntity(entry.getKey()) != null
                    && world.getBlockEntity(entry.getKey()).saveWithFullMetadata().getList("Bees", 10).size() >= 2)) regionsWithNests++;
        }
        helper.assertTrue(eligibleGroves == 1024,
                "Every island must retain its cherry grove");
        helper.assertTrue(regionsWithNests == eligibleGroves,
                "Every eligible seeded grove must retain a populated native nest; expected=" + eligibleGroves + ", actual=" + regionsWithNests);
        // Derive the confidence bound from eligible groves, excluding islands
        // whose centers deliberately contain rainforest instead of cherry trees.
        helper.assertTrue(Math.pow(.05, 1.0 / eligibleGroves) > .995,
                "The eligible grove sample must retain a one-sided 95% lower coverage bound above 99.5%");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rareCropsAreIndependentFromFlowersAndCompleteTheirSelectedPatches(GameTestHelper helper) {
        for (String crop : new String[]{"farmersdelight:wild_carrots", "legumedelight:wild_beans"}) {
            for (int seed = 0; seed < 20; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                // Repeated trees may block large parts of an otherwise eligible
                // chunk. The crop pass must retry a different valid origin.
                for (int x = 0; x < 8; x++) for (int z = 0; z < 16; z++) {
                    blocks.put(new BlockPos(x, 101, z), Blocks.JUNGLE_LOG.defaultBlockState());
                }
                WorldGenLevel world = fixture(helper, "cherry_grove", blocks, 104);
                int count = TropicalVegetation.placeRareCropPatch(world, 0, 0, crop, RandomSource.create(seed));
                helper.assertTrue(count == 5,
                        "Selected crop patches must contain exactly five actual plants despite a half-blocked chunk: " + crop);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rainforestGrassDoublesExistingCoverWithoutAddingFerns(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
        for (int i = 0; i < 48; i++) {
            String species = i % 3 == 0 ? "minecraft:grass" : i % 3 == 1 ? "minecraft:fern" : "minecraft:large_fern";
            helper.assertTrue(net.amurdza.examplemod.worldgen.DocumentedFloraFeature.placePlant(world,
                    new BlockPos(i % 8 * 2, 101, i / 8 * 2), species, RandomSource.create(i)), "Control understory must place");
        }
        long ferns = blocks.values().stream().filter(state -> state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)).count();
        helper.assertTrue(TropicalVegetation.expandRainforestGrass(world, 0, 0, RandomSource.create(117)) == 48,
                "Grass expansion must add one new root for every previously covered ground cell");
        helper.assertTrue(blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                        && (entry.getValue().is(Blocks.GRASS) || entry.getValue().is(Blocks.FERN) || entry.getValue().is(Blocks.LARGE_FERN))).count() == 96,
                "Combined understory coverage must double on eligible open ground");
        helper.assertTrue(ferns == blocks.values().stream().filter(state -> state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)).count(),
                "Expansion cannot increase or replace either fern form");
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            helper.assertTrue(TropicalVegetation.selectMangroveTreeChunk(17, x, z)
                            != net.amurdza.examplemod.worldgen.TropicalLayout.moundCheckerboardChunk(x, z),
                    "Mangrove tree opportunities retain half the chunks across both land and water");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void fullCherryPassMakesTwoIndependentFlowerAttemptsAndRestoresPinkDaffodils(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = net.amurdza.examplemod.worldgen.DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var plan = net.amurdza.examplemod.worldgen.TropicalIslandPlan.forIsland(layout, layout.island(1, 0));
        var chunks = plan.cherry().stream().filter(c -> plan.cherryCrop(c.x(), c.z()) == null
                && layout.sample(c.x() * 16 + 8, c.z() * 16 + 8).distance() < 126).toList();
        Set<String> allowed = Set.of("minecraft:allium", "minecraft:peony", "minecraft:lilac", "minecraft:pink_petals",
                "biomesoplenty:pink_daffodil", "biomeswevegone:japanese_orchid");
        int empty = 0, daffodils = 0;
        for (int seed = 0; seed < 256; seed++) {
            var chunk = chunks.get(seed % chunks.size());
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "cherry_grove", blocks, 101);
            TropicalVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world, world.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(net.amurdza.examplemod.worldgen.TropicalLayout.mix(seed)),
                    new BlockPos(chunk.x() * 16, 0, chunk.z() * 16), NoneFeatureConfiguration.INSTANCE));
            Set<String> observed = new HashSet<>();
            blocks.forEach((pos, state) -> {
                String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                if (pos.getY() == 101 && allowed.contains(id)) observed.add(id);
            });
            if (observed.isEmpty()) empty++;
            if (observed.contains("biomesoplenty:pink_daffodil")) daffodils++;
            helper.assertTrue(observed.size() <= 1, "Both draws must preserve their owning chunk's selected flower");
        }
        helper.assertTrue(empty >= 15 && empty <= 65 && daffodils >= 15,
                "Two original flower attempts must restore visible pink daffodils: empty=" + empty + ", daffodils=" + daffodils);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveShoreGetsOneWeightedFlowerOrCropPatchOnItsActualGround(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = net.amurdza.examplemod.worldgen.DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var plan = net.amurdza.examplemod.worldgen.TropicalIslandPlan.forIsland(layout, layout.island(0, 0));
        int orchids = 0, pitchers = 0, mushrooms = 0, crops = 0;
        for (var chunk : plan.mounds()) {
            int minX = chunk.x() * 16, minZ = chunk.z() * 16;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 64, tropical, 63);
            int count = TropicalVegetation.placeMoundPatch(world, minX, minZ, RandomSource.create(chunk.x() * 341873128712L + chunk.z()));
            helper.assertTrue(count >= 3 && count <= 8, "Each valid dry shore chunk must receive a complete modest patch, actual=" + count);
            var roots = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 64).toList();
            helper.assertTrue(roots.size() == count && roots.stream().map(entry -> entry.getValue().getBlock()).distinct().count() == 1,
                    "Only one chosen species may occupy each shore chunk's land patch");
            for (var entry : roots) helper.assertTrue(entry.getKey().getX() >= minX && entry.getKey().getX() < minX + 16
                            && entry.getKey().getZ() >= minZ && entry.getKey().getZ() < minZ + 16,
                    "Shore flowers must stay in their allocated dry shore chunk");
            String name = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(roots.get(0).getValue().getBlock()).toString();
            if (name.equals("minecraft:blue_orchid")) orchids++;
            else if (name.equals("minecraft:pitcher_plant")) pitchers++;
            else if (name.endsWith(":red_mushroom") || name.endsWith(":brown_mushroom")) mushrooms++;
            else if (name.equals("hearthandharvest:wild_cotton") || name.equals("farmersdelight:wild_cabbages")) crops++;
            else helper.fail("Unexpected shore plant: " + name);
        }
        helper.assertTrue(mushrooms == 8 && crops == 8 && orchids == (plan.mounds().size() - 16) * 11 / 20
                        && pitchers == plan.mounds().size() - 16 - orchids,
                "Dry shores must honor exact crop quotas and the rounded-down 55% orchid share: " + orchids + "/" + pitchers + "/" + mushrooms + "/" + crops);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rainforestExtraFernsLeaveGrassAndLargeFernsUnchanged(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, "rainforest", blocks, 101);
        for (int i = 0; i < 18; i++) {
            String species = i < 6 ? "minecraft:grass" : i < 12 ? "minecraft:fern" : "minecraft:large_fern";
            net.amurdza.examplemod.worldgen.DocumentedFloraFeature.placePlant(world,
                    new BlockPos(i % 6 * 2, 101, i / 6 * 2), species, RandomSource.create(i));
        }
        helper.assertTrue(TropicalVegetation.addFerns(world, 0, 0, 6, RandomSource.create(27)) == 6,
                "The second fern pass must double the six original regular ferns");
        helper.assertTrue(blocks.entrySet().stream().filter(e -> e.getKey().getY() == 101 && e.getValue().is(Blocks.FERN)).count() == 12
                        && blocks.values().stream().filter(s -> s.is(Blocks.GRASS)).count() == 6
                        && blocks.entrySet().stream().filter(e -> e.getKey().getY() == 101 && e.getValue().is(Blocks.LARGE_FERN)).count() == 6,
                "Additional regular ferns must not replace or add grass or tall ferns");
        helper.succeed();
    }

    private static boolean stageTree(WorldGenLevel world, java.util.function.Consumer<WorldGenLevel> generate) {
        return AtomicTreeGrowth.place(new FeaturePlaceContext<>(Optional.empty(), world,
                world.getLevel().getChunkSource().getGenerator(), RandomSource.create(37), new BlockPos(8, 101, 8),
                NoneFeatureConfiguration.INSTANCE), staged -> {
            generate.accept(staged.level());
            return true;
        });
    }

    private static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                         Map<BlockPos, BlockState> blocks, int height) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        return fixture(helper, biomeName, blocks, height, tropical);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                         Map<BlockPos, BlockState> blocks, int height,
                                         net.minecraft.server.level.ServerLevel tropical) {
        return fixture(helper, biomeName, blocks, height, tropical, 100);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                         Map<BlockPos, BlockState> blocks, int height,
                                         net.minecraft.server.level.ServerLevel tropical, int groundY) {
        var biome = tropical.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", biomeName)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= groundY ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        Map<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> blockEntities = new HashMap<>();
        return (WorldGenLevel) Proxy.newProxyInstance(TropicalVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return tropical;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? tropical.getHeight() : height;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            var entity = blockEntities.computeIfAbsent(pos, at -> {
                                var state = stateAt.apply(at);
                                return state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock
                                        ? entityBlock.newBlockEntity(at, state) : null;
                            });
                            return args.length == 1 ? entity : Optional.ofNullable(entity).filter(be -> be.getType() == args[1]);
                        }
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition":
                            return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition":
                            return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try {
                                return method.invoke(tropical, args);
                            } catch (InvocationTargetException failure) {
                                throw failure.getCause();
                            }
                    }
                });
    }
}
