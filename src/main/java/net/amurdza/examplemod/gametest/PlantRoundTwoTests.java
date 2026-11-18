package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TreeLogConnections;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantRoundTwoTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void actualAcaciaFeaturesHaveOneSupportedTrunk(GameTestHelper helper) {
        var savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var registry = savanna.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (String name : new String[]{"minecraft:acacia", "aoemod:trees/acacia"}) {
            var feature = registry.get(new ResourceLocation(name));
            helper.assertTrue(feature != null, "Actual acacia feature must exist: " + name);
            for (int seed = 0; seed < 128; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                var world = fixture(helper, blocks);
                helper.assertTrue(feature.place(world, savanna.getChunkSource().getGenerator(),
                        RandomSource.create(seed * 7919L), new BlockPos(8, 101, 8)), "Actual acacia must generate");
                Set<BlockPos> logs = new HashSet<>();
                blocks.forEach((pos, state) -> { if (state.is(Blocks.ACACIA_LOG)) logs.add(pos); });
                helper.assertTrue(logs.size() >= 5 && logs.size() <= 28, "Native acacia must remain slender: " + logs.size());
                BlockPos root = logs.stream().min(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
                helper.assertTrue(logs.stream().allMatch(pos -> TreeLogConnections.connected(logs, root, pos)),
                        "Actual registered acacia has disconnected logs: " + name + " seed=" + seed);
                helper.assertTrue(logs.contains(root.above()),
                        "An acacia cannot bend away after a one-block base: " + name + " seed=" + seed);
                helper.assertTrue(logs.stream().noneMatch(pos -> pos.getY() <= root.getY() + 1
                                && (pos.getX() != root.getX() || pos.getZ() != root.getZ())),
                        "A secondary fork cannot recreate a one-block stump: " + name + " seed=" + seed);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cornerChoiceKeepsUpperLogsAndUsesFaceConnections(GameTestHelper helper) {
        BlockPos lower = new BlockPos(0, 100, 0), upper = lower.offset(1, 1, 0);
        helper.assertTrue(TreeLogConnections.connectors(Set.of(lower, upper), pos -> true)
                .equals(java.util.List.of(lower.above())), "Sloping limbs keep their upper joining corner");
        Set<BlockPos> columns = Set.of(lower.below(), lower, upper, upper.above(), upper.above(2));
        helper.assertTrue(TreeLogConnections.connectors(columns, pos -> true).equals(java.util.List.of(lower.above())),
                "Branches keep the upper corner instead of adding a hanging lower log");
        helper.assertTrue(TreeLogConnections.connectors(columns, pos -> true, pos -> pos.equals(lower.above()))
                        .equals(java.util.List.of(lower.above())),
                "The upper joining corner may replace leaves");
        var joined = new HashSet<>(columns); joined.add(lower.above());
        helper.assertTrue(TreeLogConnections.connectors(joined, pos -> true).isEmpty(),
                "Existing connected limbs must receive no filler logs");
        Set<BlockPos> diagonal = Set.of(lower, lower.east(), lower.offset(1, 0, 1), lower.offset(2, 0, 1),
                lower.offset(2, 0, 2), lower.offset(2, 1, 2), lower.offset(1, 1, 2), lower.offset(1, 1, 1));
        helper.assertTrue(TreeLogConnections.connectors(diagonal, pos -> true).isEmpty(),
                "An indirect face-connected route must not gain redundant logs");
        var threeAxes = TreeLogConnections.connectors(Set.of(lower, lower.offset(1, 1, 1)), pos -> true);
        helper.assertTrue(threeAxes.size() == 2 && threeAxes.stream().allMatch(pos -> pos.getY() == upper.getY()),
                "A three-axis diagonal needs two upper corners to connect by faces");
        helper.assertTrue(TreeLogConnections.connectors(Set.of(lower, upper), pos -> !pos.equals(lower.above()))
                        .equals(java.util.List.of(upper.below())),
                "An obstructed upper route retains its necessary lower support");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void redundantLowerAcaciaLogsAreRemovedWithoutBreakingBranches(GameTestHelper helper) {
        BlockPos root = new BlockPos(0, 100, 0);
        Set<BlockPos> logs = new HashSet<>();
        for (int y = 0; y <= 3; y++) logs.add(root.above(y));
        for (int y = 2; y <= 4; y++) {
            logs.add(root.offset(-1, y, 0));
            logs.add(root.offset(1, y, 0));
        }
        for (int y = 3; y <= 5; y++) logs.add(root.offset(-2, y, 0));
        Set<BlockPos> cleaned = TreeLogConnections.withoutRedundantLowerLogs(logs, Set.of(root));
        Set<BlockPos> removed = new HashSet<>(logs);
        removed.removeAll(cleaned);
        helper.assertTrue(removed.equals(Set.of(root.offset(-1, 2, 0), root.offset(1, 2, 0), root.offset(-2, 3, 0))),
                "The three hanging lower logs must be removed while the upper branch logs remain: " + removed);
        helper.assertTrue(cleaned.stream().allMatch(pos -> TreeLogConnections.connected(cleaned, root, pos)),
                "Removing the lower duplicates must preserve face-connected branches");
        helper.assertTrue(TreeLogConnections.withoutRedundantLowerLogs(cleaned, Set.of(root)).equals(cleaned),
                "No further redundant lower logs may remain");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void oneBlockAcaciaBendsBecomeStraightTrunks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("minecraft:acacia"));
        var config = (net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration) feature.config();
        BlockPos origin = new BlockPos(8, 101, 8);
        int straightened = 0;
        for (int height = 4; height <= 6; height++) for (int seed = 0; seed < 128; seed++) {
            RandomSource nativeRoll = RandomSource.create(seed);
            net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(nativeRoll);
            if (height - nativeRoll.nextInt(4) - 1 > 1) continue;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = fixture(helper, blocks);
            var foliage = config.trunkPlacer.placeTrunk(world,
                    (pos, state) -> blocks.put(pos.immutable(), state), RandomSource.create(seed), height, origin, config);
            for (int y = 0; y < height; y++) helper.assertTrue(
                    world.getBlockState(origin.above(y)).is(Blocks.ACACIA_LOG),
                    "An early native bend must become a complete straight trunk: height=" + height + ", seed=" + seed);
            helper.assertTrue(foliage.get(0).pos().equals(origin.above(height)),
                    "The main canopy must stay above the straightened trunk");
            straightened++;
        }
        helper.assertTrue(straightened > 60, "The regression must exercise many native one-block bend rolls");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cornRequiresFarmlandAndHydrationSpeedsActualTicks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var corn = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:corn_stalk"));
        BlockPos root = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 250, 40);
        BlockState young = corn.defaultBlockState();
        int[] growth = new int[2];
        for (int y = -1; y <= 3; y++) level.setBlock(root.above(y), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(root.below(), Blocks.FARMLAND.defaultBlockState(), 18);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                alabaster.hearthandharvest.common.block.CornStalkBlock.hasSufficientLight(level, root),
                "Native corn requires propagated fixture lighting")).thenExecute(() -> {
        try {
            for (var soil : new net.minecraft.world.level.block.Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SAND}) {
                level.setBlock(root.below(), soil.defaultBlockState(), 18);
                helper.assertFalse(young.canSurvive(level, root), "Corn must reject uncultivated soil: " + soil);
            }
            for (int moist = 0; moist < 2; moist++) {
                level.setBlock(root.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, moist * 7), 18);
                helper.assertTrue(young.canSurvive(level, root), "Dry and hydrated farmland both support corn");
                for (int sample = 0; sample < 256; sample++) {
                    level.setBlock(root, young, 18);
                    young.randomTick(level, root, RandomSource.create(sample * 1723L));
                    if (!level.getBlockState(root).equals(young)) growth[moist]++;
                }
            }
            helper.assertTrue(growth[0] >= 90 && growth[0] <= 170 && growth[1] >= 240,
                    "Hydration must speed actual corn ticks: dry=" + growth[0] + ", hydrated=" + growth[1]);
            var rich = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight:rich_soil_farmland"));
            level.setBlock(root.below(), rich.defaultBlockState(), 18);
            helper.assertTrue(young.canSurvive(level, root), "Other farmland types must support corn");
        } finally {
            for (int y = -1; y <= 3; y++) level.setBlock(root.above(y), Blocks.AIR.defaultBlockState(), 18);
        }
        }).thenSucceed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void desertGrassDropsPeanutSeedsAndRabbitsEatPeanuts(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var peanutSeeds = ForgeRegistries.ITEMS.getValue(new ResourceLocation("legumedelight:peanuts"));
        var dragonFruitSeeds = net.amurdza.examplemod.registry.RetainedDragonFruit.SEEDS.get();
        helper.assertTrue(peanutSeeds != null && peanutSeeds != net.minecraft.world.item.Items.AIR,
                "Retained peanut seeds must exist");
        for (var grass : new net.minecraft.world.level.block.Block[]{
                net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get(),
                net.amurdza.examplemod.registry.ModBlocks.DESERT_TALL_GRASS.get()}) {
            Map<net.minecraft.world.item.Item, Integer> counts = new HashMap<>();
            for (int i = 0; i < 24000; i++) {
                var drops = net.minecraft.world.level.block.Block.getDrops(grass.defaultBlockState(), level,
                        new BlockPos(0, 300, 0), null, null, net.minecraft.world.item.ItemStack.EMPTY);
                helper.assertTrue(drops.size() <= 1, "Dry grass has one seed lottery");
                drops.forEach(stack -> counts.merge(stack.getItem(), stack.getCount(), Integer::sum));
            }
            helper.assertTrue(counts.keySet().equals(Set.of(peanutSeeds, dragonFruitSeeds)),
                    "Dry grass must use the retained peanut and dragon fruit seed pool: " + counts);
            double rate = counts.get(peanutSeeds) / 24000.0;
            double expected = grass == net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get() ? .04 : .08;
            helper.assertTrue(Math.abs(rate - expected) < .015, "Peanut seed chance must remain unchanged: " + rate);
        }
        var rabbit = net.minecraft.world.entity.EntityType.RABBIT.create(level);
        var peanut = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vintagedelight:peanut"));
        for (var food : new net.minecraft.world.item.Item[]{peanut}) {
            var stack = new net.minecraft.world.item.ItemStack(food);
            helper.assertTrue(rabbit.isFood(stack)
                    && net.amurdza.examplemod.util.AnimalFoodOverrides.isExtraFood(rabbit, stack),
                    "Native rabbit feeding and the interaction handler must accept peanuts");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wildAndCultivatedRiceDropRiceInsteadOfPanicles(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 248, 80);
        var rice = vectorwing.farmersdelight.common.registry.ModItems.RICE.get();
        try {
            for (String name : new String[]{"farmersdelight:wild_rice", "farmersdelight:rice", "farmersdelight:rice_panicles"}) {
                var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
                BlockState state = block.defaultBlockState();
                for (var property : state.getProperties()) if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age
                        && property.getName().equals("age")) state = state.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElseThrow());
                level.getChunkAt(origin).setBlockState(origin, state, false);
                if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    level.getChunkAt(origin).setBlockState(origin.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), false);
                }
                var drops = net.minecraft.world.level.block.Block.getDrops(state, level, origin, null, null,
                        net.minecraft.world.item.ItemStack.EMPTY);
                helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(stack -> stack.is(rice)),
                        "Mature rice must drop only rice: " + name + " -> " + drops);
            }
        } finally {
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 18);
        }
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        java.util.function.Function<BlockPos, BlockState> at = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 100 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(PlantRoundTwoTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 101;
                        case "getBlockState": return at.apply((BlockPos) args[0]);
                        case "getFluidState": return at.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return at.apply((BlockPos) args[0]).isAir();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(at.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(at.apply((BlockPos) args[0]).getFluidState());
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException error) { throw error.getCause(); }
                    }
                });
    }
}
