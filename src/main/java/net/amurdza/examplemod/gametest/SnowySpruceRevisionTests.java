package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowySpruceRevisionTests {
    private static final BlockPos ORIGIN = new BlockPos(8, 101, 8);

    private SnowySpruceRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void taigaSelectsOneTreePerChunkWithTheSameGiantShare(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var taiga = registry.get(new ResourceLocation("aoemod:snowy/trees"));
        var count = count(taiga);
        RandomSource random = RandomSource.create(714311);
        for (int i = 0; i < 4096; i++) {
            helper.assertTrue(count.getPositions(null, random, BlockPos.ZERO).count() == 1,
                    "Every eligible chunk must select exactly one tree candidate");
        }
        var selector = (RandomFeatureConfiguration) taiga.feature().value().config();
        helper.assertTrue(selector.features.size() == 1 && Math.abs(selector.features.get(0).chance - 1.0 / 15) < 1e-7
                        && selector.features.get(0).feature.unwrapKey().orElseThrow().location()
                        .equals(new ResourceLocation("aoemod:snowy/centered_mega_spruce"))
                        && selector.defaultFeature.unwrapKey().orElseThrow().location()
                        .equals(new ResourceLocation("aoemod:snowy/centered_spruce")),
                "Every selection must be spruce, retaining one giant per fifteen trees");
        helper.assertTrue(taiga.placement().size() == 1,
                "Only the selected trunk-size branch may choose its coordinate and test its biome");
        for (boolean giant : new boolean[]{false, true}) {
            var branch = (giant ? selector.features.get(0).feature : selector.defaultFeature).value();
            helper.assertTrue(branch.feature().unwrapKey().orElseThrow().location()
                            .equals(new ResourceLocation(giant ? "minecraft:mega_spruce" : "minecraft:spruce")),
                    "Centered branches must retain the native tree configurations");
            helper.assertTrue(branch.placement().stream().anyMatch(net.minecraft.world.level.levelgen.placement.BiomeFilter.class::isInstance)
                            && branch.placement().stream().anyMatch(net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.class::isInstance),
                    "Both branches must retain biome filtering and sapling survival checks");
            var offset = branch.placement().stream().filter(net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement.class::isInstance)
                    .findFirst().orElseThrow();
            for (BlockPos chunk : new BlockPos[]{BlockPos.ZERO, new BlockPos(-32, 0, -48)}) {
                java.util.Set<BlockPos> selected = new java.util.HashSet<>();
                for (int i = 0; i < 4096; i++) {
                    BlockPos pos = offset.getPositions(null, random, chunk).findFirst().orElseThrow();
                    double centerX = pos.getX() - chunk.getX() + (giant ? 1 : 0.5);
                    double centerZ = pos.getZ() - chunk.getZ() + (giant ? 1 : 0.5);
                    helper.assertTrue(centerX > 3 && centerX < 13 && centerZ > 3 && centerZ < 13,
                            "Both geometric trunk centers must remain strictly inside the central ten-by-ten square");
                    selected.add(pos);
                }
                helper.assertTrue(selected.size() == (giant ? 81 : 100),
                        "Every permitted trunk origin must remain available, including negative chunks");
            }
        }
        var dappled = registry.get(new ResourceLocation("aoemod:dappled/trees"));
        helper.assertTrue(dappled.feature().value().feature() instanceof net.amurdza.examplemod.worldgen.feature.features.DappledTreesFeature
                        && net.amurdza.examplemod.worldgen.feature.features.DappledTreesFeature.TREES_PER_CHUNK == 3.0 * 611 / 1114,
                "Dappled trees must keep their independently spaced tree budget");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void selectedTrunkCentersAndBiomeEdgesUseTheActualWorldgenBranches(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var placed = snowy.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(AOEMod.makeID("snowy/trees"));
        var selector = (RandomFeatureConfiguration) placed.feature().value().config();
        int regular = 0, giant = 0, blocked = 0, admitted = 0;
        for (BlockPos chunk : new BlockPos[]{BlockPos.ZERO, new BlockPos(-32, 0, -48)}) {
            for (long sample = 0; sample < 96; sample++) {
                // Adjacent small LCG seeds have correlated first floats; spread
                // them out so the selector audit includes its rare giant branch.
                long seed = sample * 714311L;
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                var clear = centeredTreeFixture(helper, blocks, chunk, localX -> true);
                helper.assertTrue(placed.placeWithBiomeCheck(clear, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), chunk),
                        "Each clear eligible chunk must place its single selected tree");
                var roots = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                                && entry.getValue().is(Blocks.SPRUCE_LOG)).map(Map.Entry::getKey).toList();
                boolean large = roots.size() == 4;
                helper.assertTrue(large || roots.size() == 1, "One selection must create exactly one ordinary or two-by-two trunk");
                int rootX = roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() - chunk.getX();
                int rootZ = roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() - chunk.getZ();
                double centerX = rootX + (large ? 1 : 0.5), centerZ = rootZ + (large ? 1 : 0.5);
                helper.assertTrue(centerX > 3 && centerX < 13 && centerZ > 3 && centerZ < 13,
                        "Actual generated trunks must have geometric centers inside the central ten-by-ten square");
                if (large) giant++; else regular++;

                // Existing atomic growth rules require the entire crown to stay
                // inside its horizontal habitat. Test the root filter only where
                // that independent whole-tree requirement is already satisfied.
                boolean containedCrown = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.SPRUCE_LOG)
                                || entry.getValue().is(Blocks.SPRUCE_LEAVES))
                        .allMatch(entry -> entry.getKey().getX() - chunk.getX() > 0);
                if (!containedCrown) continue;
                Map<BlockPos, BlockState> edgeBlocks = new HashMap<>();
                var edge = centeredTreeFixture(helper, edgeBlocks, chunk, localX -> localX > 0);
                helper.assertTrue(placed.placeWithBiomeCheck(edge, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), chunk)
                                && edgeBlocks.values().stream().anyMatch(state -> state.is(Blocks.SPRUCE_LOG)),
                        "A whole crown inside snowy forest must be admitted even when the chunk corner is dappled; seed=" + seed);
                admitted++;

                Map<BlockPos, BlockState> rejectedBlocks = new HashMap<>();
                var rejected = centeredTreeFixture(helper, rejectedBlocks, chunk, localX -> localX <= 0);
                helper.assertFalse(placed.placeWithBiomeCheck(rejected, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), chunk)
                                || rejectedBlocks.values().stream().anyMatch(state -> state.is(Blocks.SPRUCE_LOG)),
                        "A dappled root must be rejected even when the chunk corner is snowy; seed=" + seed);
                blocked++;

                // Positive control: that rejected root can grow the same native
                // tree in dappled forest. Thus the rejection above exercises the
                // worldgen biome filter, not a canopy collision or growth veto.
                var branch = (large ? selector.features.get(0).feature : selector.defaultFeature).value();
                var replay = RandomSource.create(seed);
                replay.nextFloat();
                var offset = branch.placement().stream().filter(net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement.class::isInstance)
                        .findFirst().orElseThrow();
                BlockPos replayRoot = offset.getPositions(null, replay, chunk).findFirst().orElseThrow().atY(101);
                helper.assertTrue(replayRoot.equals(new BlockPos(chunk.getX() + rootX, 101, chunk.getZ() + rootZ))
                                && branch.feature().value().place(rejected, snowy.getChunkSource().getGenerator(), replay, replayRoot),
                        "The rejected dappled candidate must independently support its complete native tree; seed=" + seed);
            }
        }
        helper.assertTrue(giant > 0 && regular > giant * 4 && blocked > 40 && admitted > 40,
                "Exercise both trunk sizes and both sides of the biome edge: " + regular + "/" + giant + "/" + admitted + "/" + blocked);
        helper.succeed();
    }

    private static WorldGenLevel centeredTreeFixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                                     BlockPos chunk, java.util.function.IntPredicate forestAtX) {
        for (BlockPos soil : BlockPos.betweenClosed(chunk.offset(-8, 100, -8), chunk.offset(24, 100, 24)))
            blocks.put(soil.immutable(), Blocks.GRASS_BLOCK.defaultBlockState());
        WorldGenLevel memory = fixture(helper, blocks, "snowy_forest", true);
        var biomes = memory.registryAccess().registryOrThrow(Registries.BIOME);
        return (WorldGenLevel) Proxy.newProxyInstance(SnowySpruceRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBiome")) {
                        String biome = forestAtX.test(((BlockPos) args[0]).getX() - chunk.getX()) ? "snowy_forest" : "dappled_forest";
                        return biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID(biome)));
                    }
                    try { return method.invoke(memory, args); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
                });
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void forestSprucesKeepTheirCrownShapeAboveTheAdjustedHeightDistribution(GameTestHelper helper) {
        for (String biome : new String[]{"snowy_forest", "dappled_forest"}) {
            for (String feature : new String[]{"minecraft:spruce", "minecraft:mega_spruce", "minecraft:mega_pine", "aoemod:trees/spruce"}) {
                int generated = 0;
                for (long seed = 0; seed < 64; seed++) {
                    Map<BlockPos, BlockState> ordinary = generate(helper, biome, feature, seed, false);
                    Map<BlockPos, BlockState> raised = generate(helper, biome, feature, seed, true);
                    helper.assertTrue(ordinary != null && raised != null,
                            "An unobstructed paired spruce must generate in both scopes: " + feature + ", " + biome + ", seed=" + seed);
                    assertRaised(helper, ordinary, raised, feature + ", " + biome + ", seed=" + seed);
                    generated++;
                }
                helper.assertTrue(generated == 64, "Geometry audit must retain all deterministic tree samples");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void poplarCrownsAndOtherSnowyBiomesKeepTheirExistingGeometry(GameTestHelper helper) {
        for (String feature : new String[]{"minecraft:red_poplar", "minecraft:orange_poplar", "minecraft:yellow_poplar"}) {
            for (long seed = 0; seed < 12; seed++) {
                var ordinary = generate(helper, "dappled_forest", feature, seed, false);
                var snowy = generate(helper, "dappled_forest", feature, seed, true);
                helper.assertTrue(ordinary != null && snowy != null && geometry(ordinary).equals(geometry(snowy)),
                        "Snowy's spruce height change must preserve the poplar shape: " + feature + ", seed=" + seed);
            }
        }
        var configured = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("minecraft:spruce"));
        var configuration = (net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration) configured.config();
        for (String biome : new String[]{"mountain_lake", "cold_ocean", "mushroom_caves"}) {
            var world = fixture(helper, new HashMap<>(), biome, true);
            helper.assertTrue(net.amurdza.examplemod.worldgen.SnowySpruceHeight.extension(world, ORIGIN, configuration) == 0,
                    "The height change must not extend into other Snowy habitats: " + biome);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void plantedSprucesUseTheSameSmallLiftAndRaisedGiantMinimum(GameTestHelper helper) {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int index = 0;
        for (String biome : new String[]{"snowy_forest", "dappled_forest"}) {
            BlockPos snowyOrigin = new BlockPos(120 + index++ * 32, 240, 120);
            BlockPos controlOrigin = helper.absolutePos(new BlockPos(8, 0, 8)).atY(240);
            try {
                prepare(helper.getLevel(), controlOrigin);
                prepare(snowy, snowyOrigin);
                BlockPos min = snowyOrigin.offset(-8, -8, -8), max = snowyOrigin.offset(8, 36, 8);
                int filled = snowy.getServer().getCommands().performPrefixedCommand(
                        snowy.getServer().createCommandSourceStack().withLevel(snowy).withSuppressedOutput(),
                        "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ() + " "
                                + max.getX() + " " + max.getY() + " " + max.getZ() + " aoemod:" + biome);
                helper.assertTrue(filled > 0 && GameplayDimensions.biomeIs(snowy, snowyOrigin, biome),
                        "Planted-tree fixture must use the actual target Snowy biome");
                for (boolean giant : new boolean[]{false, true}) {
                    prepare(helper.getLevel(), controlOrigin);
                    prepare(snowy, snowyOrigin);
                    var ordinary = grow(helper.getLevel(), controlOrigin, 31851, giant);
                    var raised = grow(snowy, snowyOrigin, 31851, giant);
                    if (!giant) {
                        assertRaised(helper, ordinary, raised, "single planted spruce in " + biome);
                    } else {
                        int trunkHeight = raised.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.SPRUCE_LOG)
                                && entry.getKey().getX() == ORIGIN.getX() && entry.getKey().getZ() == ORIGIN.getZ())
                                .mapToInt(entry -> entry.getKey().getY() - ORIGIN.getY() + 1).max().orElse(0);
                        helper.assertTrue(trunkHeight >= 21 && trunkHeight <= 33,
                                "Planted giant spruce uses the raised minimum and unchanged maximum: " + trunkHeight);
                    }
                }
            } finally {
                clear(helper.getLevel(), controlOrigin);
                clear(snowy, snowyOrigin);
            }
        }
        helper.succeed();
    }

    static double meanAttempts(PlacedFeature feature) {
        var count = count(feature);
        RandomSource random = RandomSource.create(714311);
        long total = 0;
        for (int i = 0; i < 16384; i++) total += count.getPositions(null, random, BlockPos.ZERO).count();
        return total / 16384.0;
    }

    private static CountPlacement count(PlacedFeature feature) {
        return (CountPlacement) feature.placement().stream().filter(CountPlacement.class::isInstance).findFirst().orElseThrow();
    }

    private static Map<BlockPos, BlockState> generate(GameTestHelper helper, String biome, String feature, long seed, boolean snowy) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        // The memory fixture supplies solid moss below this grass surface, so
        // short giant crowns are clipped by actual ground rather than air.
        for (BlockPos pos : BlockPos.betweenClosed(-8, 100, -8, 24, 100, 24))
            blocks.put(pos.immutable(), Blocks.GRASS_BLOCK.defaultBlockState());
        var world = fixture(helper, blocks, biome, snowy);
        var configured = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(feature));
        if (!snowy && configured.config() instanceof net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration config
                && config.trunkPlacer instanceof net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer) {
            // Compare the giant crown against its newly narrowed distribution,
            // before the existing four-block basal extension is applied.
            var heights = (net.amurdza.examplemod.mixins.accessor.TrunkPlacerHeightAccess) config.trunkPlacer;
            var builder = new net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.TreeConfigurationBuilder(
                    config.trunkProvider, new net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer(
                    heights.aoemod$baseHeight() + 4, heights.aoemod$heightRandA(), heights.aoemod$heightRandB() - 4),
                    config.foliageProvider, config.foliagePlacer, config.rootPlacer, config.minimumSize)
                    .dirt(config.dirtProvider).decorators(config.decorators);
            if (config.ignoreVines) builder.ignoreVines();
            if (config.forceDirt) builder.forceDirt();
            boolean placed = net.minecraft.world.level.levelgen.feature.Feature.TREE.place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                    java.util.Optional.empty(), world, world.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), ORIGIN, builder.build()));
            return placed ? blocks : null;
        }
        return TropicalTreeShapeTests.place(world, feature, seed) ? blocks : null;
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, String biome, boolean snowy) {
        WorldGenLevel memory = TropicalTreeShapeTests.fixture(helper, blocks, biome, 0, false);
        ServerLevel dimension = snowy ? helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY) : helper.getLevel();
        return (WorldGenLevel) Proxy.newProxyInstance(SnowySpruceRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return dimension;
                    if (method.getName().equals("getBlockEntity")) return null;
                    try {
                        return method.invoke(memory, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    private static Map<BlockPos, Block> geometry(Map<BlockPos, BlockState> blocks) {
        return blocks.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LOGS) || entry.getValue().is(BlockTags.LEAVES))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getBlock()));
    }

    private static void assertRaised(GameTestHelper helper, Map<BlockPos, BlockState> ordinary,
                                     Map<BlockPos, BlockState> raised, String label) {
        Set<BlockPos> oldLeaves = ordinary.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        Set<BlockPos> newLeaves = raised.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        Set<BlockPos> expected = oldLeaves.stream().map(pos -> pos.above(4)).collect(Collectors.toSet());
        helper.assertTrue(oldLeaves.size() >= 10 && newLeaves.equals(expected),
                "Existing crown must translate exactly +4 without widening, deepening, or exposing clipped leaves: "
                        + label + ", old/new leaves=" + oldLeaves.size() + "/" + newLeaves.size()
                        + ", missing=" + expected.stream().filter(pos -> !newLeaves.contains(pos)).count()
                        + ", extra=" + newLeaves.stream().filter(pos -> !expected.contains(pos)).count());
        Map<BlockPos, Block> oldGeometry = geometry(ordinary), newGeometry = geometry(raised);
        int oldTop = oldGeometry.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
        int newTop = newGeometry.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
        helper.assertTrue(newTop == oldTop + 4, "Overall tree height must rise exactly four blocks: " + label);
        for (var entry : oldGeometry.entrySet())
            helper.assertTrue(newGeometry.get(entry.getKey().above(4)) == entry.getValue(),
                    "The full existing trunk and crown must remain above the extension: " + label + " at " + entry.getKey());
        int originY = ordinary.keySet().stream().filter(pos -> ordinary.get(pos).is(BlockTags.LOGS))
                .mapToInt(BlockPos::getY).min().orElseThrow();
        for (var entry : ordinary.entrySet()) {
            if (!entry.getValue().is(BlockTags.LOGS) || entry.getKey().getY() != originY) continue;
            for (int dy = 0; dy < 4; dy++) helper.assertTrue(raised.getOrDefault(entry.getKey().above(dy),
                            Blocks.AIR.defaultBlockState()).is(entry.getValue().getBlock()),
                    "The raised crown needs four connected additional basal trunk blocks: " + label);
        }
    }

    private static Iterable<BlockPos> volume(BlockPos origin) {
        return BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, 36, 8));
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : volume(origin)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
    }

    private static void prepare(ServerLevel level, BlockPos origin) {
        clear(level, origin);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, -2, 8)))
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 18);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -1, -8), origin.offset(8, -1, 8)))
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 18);
    }

    private static Map<BlockPos, BlockState> grow(ServerLevel level, BlockPos origin, long seed, boolean giant) {
        var sapling = Blocks.SPRUCE_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        level.setBlock(origin, sapling, 18);
        if (giant) {
            level.setBlock(origin.east(), sapling, 18);
            level.setBlock(origin.south(), sapling, 18);
            level.setBlock(origin.east().south(), sapling, 18);
        }
        ((SaplingBlock) Blocks.SPRUCE_SAPLING).advanceTree(level, origin, sapling, RandomSource.create(seed));
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (BlockPos pos : volume(origin)) blocks.put(pos.subtract(origin).offset(ORIGIN), level.getBlockState(pos));
        return blocks;
    }
}
