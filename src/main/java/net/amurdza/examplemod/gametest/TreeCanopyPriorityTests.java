package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.TropicalBranchConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreeCanopyPriorityTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void diagonalGraphRepairAddsUpperLogEvenWhenBranchesAlreadyConnect(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos first = new BlockPos(8, 110, 8), second = first.offset(1, 0, 1), elbow = first.east();
        for (BlockPos pos : new BlockPos[]{first, second, elbow}) blocks.put(pos, Blocks.JUNGLE_LOG.defaultBlockState());
        Map<BlockPos, BlockState> logs = new java.util.LinkedHashMap<>(blocks);
        var level = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
        helper.assertTrue(net.amurdza.examplemod.worldgen.TropicalTreeShapes.completeDiagonalBridges(level, logs,
                        (pos, state) -> blocks.put(pos.immutable(), state))
                        && blocks.getOrDefault(elbow.above(), Blocks.AIR.defaultBlockState()).is(Blocks.JUNGLE_LOG),
                "An already face-connected lower elbow still needs its second log above");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void completeTwoByTwoTrunksDoNotGrowExtraTopLogs(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = 8; x <= 9; x++) for (int z = 8; z <= 9; z++) for (int y = 101; y <= 110; y++) {
            blocks.put(new BlockPos(x, y, z), Blocks.SPRUCE_LOG.defaultBlockState());
        }
        Map<BlockPos, BlockState> logs = new java.util.LinkedHashMap<>(blocks);
        var level = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
        helper.assertTrue(net.amurdza.examplemod.worldgen.TropicalTreeShapes.completeDiagonalBridges(level, logs,
                        (pos, state) -> blocks.put(pos.immutable(), state)) && logs.size() == 40 && blocks.size() == 40,
                "A complete two-by-two trunk already has both X/Z routes and must retain its exact height");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void savannaAcaciaGenerationPreservesMinimalUpperConnections(GameTestHelper helper) {
        var savanna = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.SAVANNA);
        for (long seed = 0; seed < 64; seed++) {
            Map<BlockPos, BlockState> nativeBlocks = new HashMap<>(), repairedBlocks = new HashMap<>();
            var ordinary = TreeBalanceRevisionTests.dimensionFixture(helper, nativeBlocks, "savanna", helper.getLevel());
            var repaired = TreeBalanceRevisionTests.dimensionFixture(helper, repairedBlocks, "savanna", savanna);
            helper.assertTrue(TropicalTreeShapeTests.place(ordinary, "minecraft:acacia", seed)
                            && TropicalTreeShapeTests.place(repaired, "minecraft:acacia", seed),
                    "Native and Savanna acacia shapes must both remain placeable, seed=" + seed);
            Set<BlockPos> original = new HashSet<>();
            nativeBlocks.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) original.add(pos); });
            Set<BlockPos> savannaLogs = new HashSet<>();
            repairedBlocks.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) savannaLogs.add(pos); });
            helper.assertTrue(savannaLogs.equals(original),
                    "Savanna postprocessing must not add redundant logs back to the pruned acacia, seed=" + seed);
            BlockPos root = new BlockPos(8, 101, 8);
            helper.assertTrue(savannaLogs.stream().allMatch(pos ->
                            net.amurdza.examplemod.worldgen.TreeLogConnections.connected(savannaLogs, root, pos)),
                    "Every Savanna acacia branch must stay face-connected, seed=" + seed);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void actualNativeTreeGraphsFillEveryOriginalDiagonalElbowAndItsUpperLog(GameTestHelper helper) {
        int diagonalPairs = 0;
        for (String feature : new String[]{"natural_jungle", "natural_fancy_oak", "natural_dark_oak", "natural_mangrove"}) {
            int generated = 0;
            for (long seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                String biome = feature.equals("natural_mangrove") ? "mangrove_swamp" : "rainforest";
                var nativeWorld = TropicalTreeShapeTests.fixture(helper, blocks, biome, 0, false);
                if (!placeNativeShapeBeforeDecorators(nativeWorld, "aoemod:rainforest/tree/" + feature, seed)) continue;
                generated++;
                Map<BlockPos, BlockState> logs = new java.util.LinkedHashMap<>();
                blocks.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) logs.put(pos, state); });
                Set<BlockPos> original = Set.copyOf(logs.keySet());
                var tropical = TropicalTreeShapeTests.fixture(helper, blocks, biome, 0, true);
                helper.assertTrue(net.amurdza.examplemod.worldgen.TropicalTreeShapes.completeDiagonalBridges(tropical, logs,
                                (pos, state) -> blocks.put(pos.immutable(), state)),
                        "The complete native graph must accept its diagonal bridges: " + feature + ", seed=" + seed);
                for (BlockPos first : original) {
                    for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) {
                        BlockPos second = first.offset(dx, 0, dz);
                        if (!original.contains(second) || first.asLong() >= second.asLong()) continue;
                        BlockPos one = new BlockPos(second.getX(), first.getY(), first.getZ());
                        BlockPos two = new BlockPos(first.getX(), first.getY(), second.getZ());
                        if (original.contains(one) && original.contains(two)) continue;
                        diagonalPairs++;
                        helper.assertTrue(logs.containsKey(one) && logs.containsKey(one.above())
                                        || logs.containsKey(two) && logs.containsKey(two.above()),
                                "Every original flat diagonal needs a two-log elbow, including already-connected limbs: "
                                        + feature + ", seed=" + seed + ", pair=" + first + "/" + second);
                    }
                }
            }
            helper.assertTrue(generated > 0, "The graph audit must exercise actual " + feature + " generation");
        }
        helper.assertTrue(diagonalPairs > 30, "Native graph coverage must include substantial diagonal branching");
        helper.succeed();
    }

    private static boolean placeNativeShapeBeforeDecorators(WorldGenLevel world, String feature, long seed) {
        var config = (TreeConfiguration) world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation(feature)).config();
        // Production bridges run at the end of doPlace, before decorators can
        // occupy the new log positions with cocoa, vines or shelf mushrooms.
        // Preserve the loaded trunk, foliage and root geometry at that stage.
        var builder = new TreeConfiguration.TreeConfigurationBuilder(config.trunkProvider, config.trunkPlacer,
                config.foliageProvider, config.foliagePlacer, config.rootPlacer, config.minimumSize)
                .dirt(config.dirtProvider).decorators(List.of());
        if (config.ignoreVines) builder.ignoreVines();
        if (config.forceDirt) builder.forceDirt();
        return Feature.TREE.place(new FeaturePlaceContext<>(Optional.empty(), world,
                world.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed),
                new BlockPos(8, 101, 8), builder.build()));
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void mangrovesKeepTwoVisibleLogsAcrossDryAndDeepWaterOrigins(GameTestHelper helper) {
        for (int waterDepth : new int[]{0, 6}) {
            int generated = 0;
            for (long seed = 0; seed < 64; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                var level = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", waterDepth, true);
                if (!TropicalTreeShapeTests.place(level, "aoemod:rainforest/tree/natural_mangrove", seed)) continue;
                generated++;
                int firstLog = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.MANGROVE_LOG))
                        .mapToInt(entry -> entry.getKey().getY()).min().orElseThrow();
                int firstLeaf = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.MANGROVE_LEAVES))
                        .mapToInt(entry -> entry.getKey().getY()).min().orElseThrow();
                helper.assertTrue(firstLeaf >= firstLog + 2 && firstLeaf >= 111,
                        "Every native root roll must expose two logs before the crown, seed=" + seed + ", depth=" + waterDepth);
                helper.assertTrue(level.getBlockState(new BlockPos(8, firstLog, 8)).is(Blocks.MANGROVE_LOG)
                                && level.getBlockState(new BlockPos(8, firstLog + 1, 8)).is(Blocks.MANGROVE_LOG),
                        "The two exposed mangrove layers must contain actual main-trunk logs");
            }
            helper.assertTrue(generated >= 16, "The taller mangrove must remain placeable across native root choices");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void actualGiantMushroomCapsReplaceLeavesWhileStemsStayClear(GameTestHelper helper) {
        for (String feature : new String[]{"minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom"}) {
            Map<BlockPos, BlockState> expected = new HashMap<>();
            var clear = TropicalTreeShapeTests.fixture(helper, expected, "rainforest", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(clear, feature, 7), "Control mushroom must generate");
            Map<BlockPos, BlockState> occupied = new HashMap<>();
            expected.forEach((pos, state) -> {
                if (state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)) {
                    occupied.put(pos, Blocks.JUNGLE_LEAVES.defaultBlockState());
                }
            });
            var leafy = TropicalTreeShapeTests.fixture(helper, occupied, "rainforest", 0, true);
            helper.assertTrue(TropicalTreeShapeTests.place(leafy, feature, 7), "Leaves confined to the cap remain replaceable");
            expected.forEach((pos, state) -> {
                if (state.getBlock() instanceof HugeMushroomBlock) {
                    helper.assertTrue(leafy.getBlockState(pos).is(state.getBlock()),
                            "The mushroom must retain its full cap and unobstructed stem at " + pos);
                }
            });
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void actualTreeFoliagePreservesMushroomCapsInsideAndOutsideTropical(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> expected = new HashMap<>();
            var clear = TropicalTreeShapeTests.fixture(helper, expected, "rainforest", 0, tropical);
            helper.assertTrue(TropicalTreeShapeTests.place(clear, "aoemod:rainforest/tree/natural_fancy_oak", 7),
                    "Control fancy oak must generate");
            BlockPos canopy = expected.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.OAK_LEAVES))
                    .filter(entry -> {
                        for (Direction direction : Direction.values()) {
                            if (clear.getBlockState(entry.getKey().relative(direction)).is(BlockTags.LOGS)) return false;
                        }
                        return true;
                    }).map(Map.Entry::getKey).max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
            for (var mushroom : new net.minecraft.world.level.block.Block[]{Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK}) {
                Map<BlockPos, BlockState> occupied = new HashMap<>();
                occupied.put(canopy, mushroom.defaultBlockState());
                var withMushroom = TropicalTreeShapeTests.fixture(helper, occupied, "rainforest", 0, tropical);
                TropicalTreeShapeTests.place(withMushroom, "aoemod:rainforest/tree/natural_fancy_oak", 7);
                helper.assertTrue(withMushroom.getBlockState(canopy).is(mushroom),
                        "Real foliage placement must preserve mushroom caps even though they carry the leaves tag");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void diagonalLimbsHaveUpperHorizontalBridgesAndKeepLowerSupports(GameTestHelper helper) {
        BlockPos start = new BlockPos(8, 104, 8);
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) for (int dy : new int[]{0, 1}) {
            BlockPos end = start.offset(dx, dy, dz);
            Set<BlockPos> logs = new HashSet<>(TropicalBranchConnections.limbCorners(start, end));
            logs.add(start);
            logs.add(end);
            helper.assertTrue(logs.contains(new BlockPos(end.getX(), start.getY(), start.getZ())),
                    "Diagonal limbs retain their lower horizontal support");
            helper.assertTrue(logs.contains(start.above(dy))
                            && logs.contains(new BlockPos(end.getX(), end.getY(), start.getZ())),
                    "Upper corners must join both X and Z at the higher branch elevation");
            if (dy == 0) helper.assertTrue(logs.contains(new BlockPos(end.getX(), start.getY() + 1, start.getZ())),
                    "A flat X/Z diagonal must include a log above its joining elbow");
            Set<BlockPos> reached = new HashSet<>();
            ArrayDeque<BlockPos> pending = new ArrayDeque<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                BlockPos pos = pending.removeFirst();
                if (!logs.contains(pos) || !reached.add(pos)) continue;
                for (Direction direction : Direction.values()) pending.add(pos.relative(direction));
            }
            helper.assertTrue(reached.equals(logs), "Every added corner log must have a face connection");
        }
        helper.assertTrue(TropicalBranchConnections.limbCorners(start, start.east(4)).isEmpty()
                        && TropicalBranchConnections.limbCorners(start, start.north(4)).isEmpty(),
                "Straight horizontal limbs must not grow extra top logs");
        helper.succeed();
    }
}
