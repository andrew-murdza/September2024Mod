package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalTreeShapes;
import net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalTreeShapeTests {
    private TropicalTreeShapeTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void restoredNativeRainforestTreesKeepTheirOriginalHeightRanges(GameTestHelper helper) {
        var features = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Map<String, int[]> ranges = Map.of("jungle", new int[]{10, 31},
                "fancy_oak", new int[]{12, 17}, "dark_oak", new int[]{6, 9});
        for (var entry : ranges.entrySet()) {
            var feature = features.get(new ResourceLocation("aoemod:rainforest/tree/natural_" + entry.getKey()));
            helper.assertTrue(feature != null && feature.feature() == net.minecraft.world.level.levelgen.feature.Feature.TREE,
                    "Restored rainforest trees must use full native tree generation: " + entry.getKey());
            var config = (net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration) feature.config();
            RandomSource random = RandomSource.create(524819);
            int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
            for (int i = 0; i < 4096; i++) {
                int height = config.trunkPlacer.getTreeHeight(random);
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
            }
            helper.assertTrue(minimum == entry.getValue()[0] && maximum == entry.getValue()[1],
                    "Restored " + entry.getKey() + " trunk range must match the corner-log revision; actual="
                            + minimum + ".." + maximum);
            if (entry.getKey().equals("fancy_oak")) {
                var encoded = net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.CODEC
                        .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, config).result().orElseThrow().getAsJsonObject();
                helper.assertTrue(encoded.getAsJsonObject("foliage_placer").get("radius").getAsInt() == 2,
                        "Restored fancy-oak leaf clusters use radius two, before the later canopy expansion");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void singleTropicalOakSaplingUsesTheEnlargedFancyTree(GameTestHelper helper) {
        ServerLevel world = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos origin = new BlockPos(104, 220, 40);
        BlockPos min = origin.offset(-12, -2, -12);
        BlockPos max = origin.offset(12, 26, 12);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        world.setBlock(origin.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        try {
            BlockState sapling = Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            world.setBlock(origin, sapling, 2);
            ((SaplingBlock) Blocks.OAK_SAPLING).advanceTree(world, origin, sapling, RandomSource.create(7));
            helper.assertTrue(world.getBlockState(origin).is(Blocks.OAK_LOG),
                    "One tropical oak sapling must grow the enlarged fancy oak without requiring a two-by-two square");
            int top = origin.getY();
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (world.getBlockState(pos).is(Blocks.OAK_LEAVES)) top = Math.max(top, pos.getY());
            }
            helper.assertTrue(top >= origin.getY() + 11,
                    "Tropical sapling growth must not select a short oak variety; top=" + top);
        } finally {
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fancyOakAndAzaleaHaveLargeUnclippedShapes(GameTestHelper helper) {
        for (String species : new String[]{"fancy_oak", "azalea"}) {
            for (long seed : new long[]{1, 7, 42, 91}) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, blocks, "rainforest", 0, true);
                helper.assertTrue(place(world, "aoemod:rainforest/tree/natural_" + species, seed),
                        "The enlarged " + species + " must generate a complete natural tree at seed " + seed);
                int top = blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                        .mapToInt(entry -> entry.getKey().getY()).max().orElse(0);
                helper.assertTrue(top >= (species.equals("fancy_oak") ? 112 : 109),
                        "The small clipped " + species + " variety must be absent; top=" + top);
                if (species.equals("azalea")) {
                    int minX = blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                            .mapToInt(entry -> entry.getKey().getX()).min().orElse(0);
                    int maxX = blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                            .mapToInt(entry -> entry.getKey().getX()).max().orElse(0);
                    int minZ = blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                            .mapToInt(entry -> entry.getKey().getZ()).min().orElse(0);
                    int maxZ = blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                            .mapToInt(entry -> entry.getKey().getZ()).max().orElse(0);
                    helper.assertTrue(Math.max(maxX - minX, maxZ - minZ) >= 8,
                            "Azalea must have a broad branching crown, actual spans=" + (maxX - minX) + "," + (maxZ - minZ));
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void fancyOakBranchesAreConnectedByFacesAcrossNativeShapes(GameTestHelper helper) {
        for (long seed = 0; seed < 64; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, "rainforest", 0, true);
            helper.assertTrue(place(world, "aoemod:rainforest/tree/natural_fancy_oak", seed),
                    "The complete fancy oak must remain placeable after adding lower corners, seed=" + seed);
            java.util.Set<BlockPos> logs = new java.util.HashSet<>();
            blocks.forEach((pos, state) -> {
                if (state.is(Blocks.OAK_LOG)) {
                    logs.add(pos);
                    helper.assertTrue(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS) == Direction.Axis.Y,
                            "Every tropical fancy-oak log, including native limbs and added lower corners, must use the vertical axis");
                }
            });
            java.util.Set<BlockPos> connected = new java.util.HashSet<>();
            java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
            queue.add(new BlockPos(8, 101, 8));
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                if (!logs.contains(pos) || !connected.add(pos)) continue;
                for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
            }
            helper.assertTrue(logs.size() > 15 && connected.equals(logs),
                    "Every actual branch log must join the rooted trunk through faces; seed=" + seed
                            + ", connected=" + connected.size() + ", logs=" + logs.size());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void giantJungleVinesAttachOnlyToTheirMainTwoByTwoTrunk(GameTestHelper helper) {
        for (String feature : new String[]{"aoemod:rainforest/tree/natural_jungle", "minecraft:mega_jungle_tree"}) {
            for (long seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, blocks, "rainforest", 0, true);
                helper.assertTrue(place(world, feature, seed), "Actual giant jungle shape must generate: " + feature);
                int vines = 0;
                for (var entry : blocks.entrySet()) {
                    if (!entry.getValue().is(Blocks.VINE)) continue;
                    vines++;
                    boolean mainTrunkFace = false;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (!entry.getValue().getValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(direction))) continue;
                        BlockPos trunk = entry.getKey().relative(direction);
                        helper.assertTrue(trunk.getX() >= 8 && trunk.getX() <= 9 && trunk.getZ() >= 8 && trunk.getZ() <= 9
                                        && world.getBlockState(trunk).is(Blocks.JUNGLE_LOG),
                                "A native vine cannot attach to a branch or leaf canopy: " + entry.getKey());
                        mainTrunkFace = true;
                    }
                    helper.assertTrue(mainTrunkFace, "Giant jungle vines must have an actual main-trunk face");
                }
                helper.assertTrue(vines >= 8, "Removing branch vines must retain ordinary vines around the giant trunk");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void allCanopyAndColumnSporeRoutesAreQuarteredOnlyInTropical(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> canopyBlocks = new HashMap<>();
            java.util.Set<BlockPos> roof = new java.util.HashSet<>();
            for (int x = 0; x < 64; x++) for (int z = 0; z < 64; z++) {
                BlockPos pos = new BlockPos(x, 120, z);
                roof.add(pos);
                canopyBlocks.put(pos, Blocks.STONE.defaultBlockState());
            }
            WorldGenLevel world = fixture(helper, canopyBlocks, "rainforest", 0, tropical);
            var decorator = new net.amurdza.examplemod.worldgen.tree_decorators.RainforestCanopyDecorator(0, 1, 0,
                    net.minecraft.util.valueproviders.ConstantInt.of(25));
            decorator.place(new net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator.Context(world,
                    (pos, state) -> canopyBlocks.put(pos.immutable(), state), RandomSource.create(35171),
                    java.util.Set.of(), roof, java.util.Set.of()));
            long blossoms = canopyBlocks.values().stream().filter(state -> state.is(Blocks.SPORE_BLOSSOM)).count();
            helper.assertTrue(tropical ? blossoms >= 930 && blossoms <= 1120 : blossoms == 4096,
                    "The canopy route must retain one quarter only in tropical, actual=" + blossoms);

            Map<BlockPos, BlockState> columnBlocks = new HashMap<>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                columnBlocks.put(new BlockPos(x, 120, z), Blocks.STONE.defaultBlockState());
            }
            WorldGenLevel columns = fixture(helper, columnBlocks, "rainforest", 0, tropical);
            var config = new net.amurdza.examplemod.worldgen.feature.configs.CaveVineConfig(
                    net.minecraft.util.valueproviders.ConstantInt.of(4), 1, 0, 100000000F);
            new net.amurdza.examplemod.worldgen.feature.features.CaveVineColumn(
                    net.amurdza.examplemod.worldgen.feature.configs.CaveVineConfig.CODEC).place(
                    new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), columns,
                            columns.getLevel().getChunkSource().getGenerator(), RandomSource.create(35171), BlockPos.ZERO, config));
            blossoms = columnBlocks.values().stream().filter(state -> state.is(Blocks.SPORE_BLOSSOM)).count();
            helper.assertTrue(tropical ? blossoms >= 44 && blossoms <= 84 : blossoms == 256,
                    "The cave-ceiling route must retain one quarter only in tropical, actual=" + blossoms);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyTropicalSaplingTemplatesKeepWaterWithoutCoralRings(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, "mangrove_swamp", 4, tropical);
            helper.assertTrue(place(world, "aoemod:rainforest/tree/mangrove_large_tree_from_sapling", 7),
                    "The actually registered legacy mangrove sapling template must still grow");
            long coral = blocks.values().stream().filter(state -> state.is(BlockTags.CORAL_BLOCKS)).count();
            if (!tropical) {
                helper.assertTrue(coral > 0, "The older non-tropical template retains its existing water-ring behavior");
                continue;
            }
            helper.assertTrue(coral == 0, "Tropical non-coral trees must never manufacture coral-block rings");
            int plants = 0;
            for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++) {
                if (dx >= 0 && dx < 2 && dz >= 0 && dz < 2) continue;
                for (int y = 101; y <= 104; y++) {
                    BlockPos pos = new BlockPos(8 + dx, y, 8 + dz);
                    var state = world.getBlockState(pos);
                    helper.assertTrue(state.getFluidState().is(net.minecraft.tags.FluidTags.WATER),
                            "Legacy tropical tree rings preserve all existing water cells: " + pos);
                    if (state.is(Blocks.SEAGRASS) || state.is(Blocks.SEA_PICKLE)) plants++;
                }
            }
            helper.assertTrue(plants == 12, "Every available ring-floor cell can receive short sea life without solid columns");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tallMangrovesKeepLeavesClearOfSixDeepWater(GameTestHelper helper) {
        int successes = 0;
        for (long seed = 1; seed <= 12; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, "mangrove_swamp", 6, true);
            if (!place(world, "aoemod:rainforest/tree/natural_mangrove", seed)) continue;
            successes++;
            long leaves = blocks.values().stream().filter(state -> state.is(Blocks.MANGROVE_LEAVES)).count();
            helper.assertTrue(leaves > 20, "Tall mangrove must retain a real crown above the water");
            for (var entry : blocks.entrySet()) {
                if (!entry.getValue().is(Blocks.MANGROVE_LEAVES)) continue;
                helper.assertTrue(TropicalTreeShapes.mangroveLeafAllowed(world, new BlockPos(8, 101, 8), entry.getKey()),
                        "Mangrove leaf must be at least ten blocks above its origin and clear of water: " + entry.getKey());
                helper.assertTrue(entry.getKey().getY() >= 111, "Mangrove leaves must leave two log layers above the roots exposed");
            }
        }
        helper.assertTrue(successes >= 4, "Tall mangroves must reliably grow on deep shelves, successes=" + successes);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushroomExtraHeightIsTropicalOnly(GameTestHelper helper) {
        java.util.Set<Integer> tropicalHeights = new java.util.HashSet<>();
        for (String mushroom : new String[]{"huge_red_mushroom", "huge_brown_mushroom"}) {
            for (long seed = 1; seed <= 32; seed++) {
                int vanillaStem = 0;
                for (boolean tropical : new boolean[]{false, true}) {
                    Map<BlockPos, BlockState> blocks = new HashMap<>();
                    WorldGenLevel world = fixture(helper, blocks, "rainforest", 0, tropical);
                    helper.assertTrue(place(world, "minecraft:" + mushroom, seed), "The actual giant mushroom must generate");
                    int stem = (int) blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.MUSHROOM_STEM)
                            && entry.getKey().getX() == 8 && entry.getKey().getZ() == 8).count();
                    if (!tropical) vanillaStem = stem;
                    else {
                        tropicalHeights.add(stem);
                        helper.assertTrue(stem == vanillaStem + 1,
                                "Restored tropical giant mushrooms must be exactly one block taller than the native external shape; seed=" + seed
                                        + ", control=" + vanillaStem + ", tropical=" + stem);
                    }
                    helper.assertFalse(TropicalTreeShapes.tallMushroom(), "Mushroom height scope must be restored after generation");
                }
            }
        }
        helper.assertTrue(tropicalHeights.size() >= 3, "Tropical mushroom roofs must retain the native variable height choices");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cherryCandidatesHaveStableSpacingAndCrownsCannotIntersect(GameTestHelper helper) {
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            BlockPos candidate = TropicalCherryTreesFeature.candidate(12987, x, z);
            helper.assertTrue(candidate.equals(TropicalCherryTreesFeature.candidate(12987, x, z)),
                    "Cherry spatial candidates must be stable across generation order");
            for (int dx = 0; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz <= 0) continue;
                BlockPos neighbor = TropicalCherryTreesFeature.candidate(12987, x + dx, z + dz);
                helper.assertTrue(candidate.distSqr(neighbor) >= 16 * 16,
                        "Independent cherry trunks need at least sixteen blocks of Euclidean separation");
            }
        }
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, "cherry_grove", 0, true);
        helper.assertTrue(place(world, "minecraft:cherry_bees_005", 7), "A solitary vanilla-shape cherry must generate");
        BlockPos leaf = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.CHERRY_LEAVES))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        helper.assertFalse(net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules.isAllowed(world, leaf,
                        Blocks.CHERRY_LEAVES.defaultBlockState()), "Cherry crowns cannot overwrite another cherry's leaves");
        helper.assertFalse(net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules.isAllowed(world, leaf.above(),
                        Blocks.CHERRY_LEAVES.defaultBlockState()), "Cherry crowns retain an air gap next to other cherry leaves");
        helper.assertTrue(net.amurdza.examplemod.worldgen.TreeGrowthCollisionRules.isAllowed(world, leaf,
                        Blocks.OAK_LEAVES.defaultBlockState()), "Mixed rainforest leaves may still overlap a cherry at the transition");
        helper.succeed();
    }

    static boolean place(WorldGenLevel world, String id, long seed) {
        var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(id));
        return feature != null && feature.place(world, world.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(seed), new BlockPos(8, 101, 8));
    }

    static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         String biomeName, int waterDepth, boolean tropicalDimension) {
        ServerLevel world = tropicalDimension ? helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL)
                : helper.getLevel();
        var biome = world.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID(biomeName)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 100 ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : pos.getY() <= 100 + waterDepth ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(TropicalTreeShapeTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return world;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? world.getHeight() : 101 + waterDepth;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        // Default interface calls otherwise delegate to the live
                        // server, leaking unrelated GameTest water into this map.
                        case "isWaterAt": return stateAt.apply((BlockPos) args[0]).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try {
                                return method.invoke(world, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                    }
                });
    }
}
