package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.feature.configs.CaveVineConfig;
import net.amurdza.examplemod.worldgen.feature.features.CaveVineColumn;
import net.amurdza.examplemod.worldgen.tree_decorators.RainforestCanopyDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalCanopyRefinementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void giantJungleBranchesKeepNativeCrownsAndBecomeFaceConnected(GameTestHelper helper) {
        for (String feature : new String[]{"aoemod:rainforest/tree/natural_jungle", "minecraft:mega_jungle_tree"}) {
            for (long seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
                helper.assertTrue(TropicalTreeShapeTests.place(world, feature, seed),
                        "Adding lower jungle corners must retain actual native tree generation");
                Set<BlockPos> logs = new HashSet<>();
                blocks.forEach((pos, state) -> { if (state.is(Blocks.JUNGLE_LOG)) logs.add(pos); });
                Set<BlockPos> connected = new HashSet<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(new BlockPos(8, 101, 8));
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.removeFirst();
                    if (!logs.contains(pos) || !connected.add(pos)) continue;
                    for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
                }
                helper.assertTrue(connected.equals(logs), "Every giant jungle branch must connect through faces, seed=" + seed);
                helper.assertTrue(blocks.values().stream().filter(state -> state.is(Blocks.JUNGLE_LEAVES)).count() > 100,
                        "Corner repair must retain the giant jungle's full native leafy crown");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void mangroveCrownsAreBroadFilledAndHaveFruitAndVinesInsteadOfNests(GameTestHelper helper) {
        int successes = 0, nativeSuccesses = 0, mulberry = 0, caveTips = 0;
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        for (long seed = 1; seed <= 20; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 6, true);
            Map<BlockPos, BlockState> controlBlocks = new HashMap<>();
            WorldGenLevel nativeWorld = TropicalTreeShapeTests.fixture(helper, controlBlocks, "mangrove_swamp", 6, false);
            var feature = nativeWorld.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("aoemod:rainforest/tree/natural_mangrove"));
            RandomSource nativeRandom = RandomSource.create(seed);
            boolean nativePlaced = false;
            // Atomic custom trees receive four attempts. Give the same native
            // roots/trunk/seed that budget outside the canopy-override scope.
            for (int attempt = 0; attempt < 4 && !nativePlaced; attempt++) {
                nativePlaced = feature.place(nativeWorld, nativeWorld.getLevel().getChunkSource().getGenerator(),
                        nativeRandom, new BlockPos(8, 101, 8));
            }
            if (nativePlaced) nativeSuccesses++;
            boolean actual = TropicalTreeShapeTests.place(world, "aoemod:rainforest/tree/natural_mangrove", seed);
            helper.assertTrue(actual == nativePlaced,
                    "Broader tropical foliage must preserve native root-placement success under the same retry budget; seed="
                            + seed + ", native=" + nativePlaced + ", tropical=" + actual);
            if (!actual) continue;
            successes++;
            Map<Long, int[]> columns = new HashMap<>();
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int leaves = 0;
            for (var entry : blocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                helper.assertFalse(state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE) || state.is(Blocks.SPORE_BLOSSOM),
                        "Mangrove tree decoration must not create bee homes or spore blossoms");
                if (ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString().equals("upgrade_aquatic:mulberry_vine")) mulberry++;
                if (state.is(Blocks.CAVE_VINES)) {
                    caveTips++;
                    helper.assertTrue(CaveVinePlacement.keepTropicalColumn(world.getSeed(), pos.getX(), pos.getZ()),
                            "Mangrove canopy vines must respect the shared sparsity rule");
                    helper.assertTrue(state.getValue(BlockStateProperties.AGE_25) == 25,
                            "Mangrove cave-vine tips must remain mature");
                    int floor = pos.getY() - 1;
                    while (floor > 100) {
                        BlockState below = world.getBlockState(new BlockPos(pos.getX(), floor, pos.getZ()));
                        if (below.isSolid() || below.getFluidState().is(FluidTags.WATER)) break;
                        floor--;
                    }
                    helper.assertTrue(pos.getY() >= floor + 4,
                            "Mangrove vines must retain at least four blocks above the nearest solid or water surface");
                }
                if (!state.is(Blocks.MANGROVE_LEAVES)) continue;
                leaves++;
                minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
                helper.assertTrue(pos.getY() >= 111, "The lowest mangrove leaves must rise two more blocks");
                long key = ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL);
                int[] range = columns.computeIfAbsent(key, ignored -> new int[]{pos.getY(), pos.getY()});
                range[0] = Math.min(range[0], pos.getY()); range[1] = Math.max(range[1], pos.getY());
            }
            long treeFruit = blocks.values().stream().filter(state -> ForgeRegistries.BLOCKS.getKey(state.getBlock())
                    .toString().equals("upgrade_aquatic:mulberry_vine")).count();
            int bottomLeaves = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.MANGROVE_LEAVES))
                    .mapToInt(entry -> entry.getKey().getY()).min().orElseThrow();
            int bottomLog = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.MANGROVE_LOG))
                    .mapToInt(entry -> entry.getKey().getY()).min().orElseThrow();
            helper.assertTrue(bottomLeaves >= bottomLog + 2,
                    "At least two mangrove log layers must remain below the lowest leaves, seed=" + seed);
            helper.assertTrue(world.getBlockState(new BlockPos(8, bottomLog, 8)).is(Blocks.MANGROVE_LOG)
                            && world.getBlockState(new BlockPos(8, bottomLog + 1, 8)).is(Blocks.MANGROVE_LOG),
                    "The exposed mangrove trunk must contain two actual logs above the roots");
            helper.assertTrue(treeFruit == 0, "An unselected mangrove must not acquire random mulberries");
            blocks.forEach((pos, state) -> {
                if (ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString().equals("upgrade_aquatic:mulberry_vine")) {
                    helper.assertTrue(pos.getY() == bottomLeaves - 1, "Mulberries must hang below the lowest leaf layer");
                }
            });
            helper.assertTrue(leaves > 120 && maxX - minX >= 8,
                    "A mangrove must retain a broad, substantial canopy; leaves=" + leaves);
            for (var column : columns.entrySet()) {
                for (int y = column.getValue()[0]; y <= column.getValue()[1]; y++) {
                    BlockPos pos = new BlockPos((int) (column.getKey() >> 32), y, (int) (long) column.getKey());
                    helper.assertFalse(world.isEmptyBlock(pos), "Mangrove crowns cannot leave air between leaves above and below");
                }
            }
        }
        helper.assertTrue(successes == nativeSuccesses && successes > 0 && mulberry == 0,
                "Every retained native success must preserve the broad crown, no unselected mulberries and permitted hanging vines: "
                        + successes + "/" + nativeSuccesses + "/" + mulberry + "/" + caveTips);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveCanopyAndCeilingPassesNeverIntroduceSporeBlossoms(GameTestHelper helper) {
        for (boolean tropical : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            Set<BlockPos> roof = new HashSet<>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, 120, z);
                roof.add(pos); blocks.put(pos, Blocks.STONE.defaultBlockState());
            }
            WorldGenLevel world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", 4, tropical);
            new RainforestCanopyDecorator(0, 1, 0, ConstantInt.of(25)).place(new TreeDecorator.Context(world,
                    (pos, state) -> blocks.put(pos.immutable(), state), RandomSource.create(7), Set.of(), roof, Set.of()));
            long spores = blocks.values().stream().filter(state -> state.is(Blocks.SPORE_BLOSSOM)).count();
            helper.assertTrue(tropical ? spores == 0 : spores == 256,
                    "Only tropical mangrove canopy must exclude spore blossoms");
            blocks.entrySet().removeIf(entry -> entry.getValue().is(Blocks.SPORE_BLOSSOM));
            var config = new CaveVineConfig(ConstantInt.of(25), 1, 0, 100000000F);
            new CaveVineColumn(CaveVineConfig.CODEC).place(new FeaturePlaceContext<>(Optional.empty(), world,
                    world.getLevel().getChunkSource().getGenerator(), RandomSource.create(7), BlockPos.ZERO, config));
            spores = blocks.values().stream().filter(state -> state.is(Blocks.SPORE_BLOSSOM)).count();
            helper.assertTrue(tropical ? spores == 0 : spores == 256,
                    "Only tropical mangrove ceiling columns must exclude spore blossoms");
        }
        helper.succeed();
    }
}
