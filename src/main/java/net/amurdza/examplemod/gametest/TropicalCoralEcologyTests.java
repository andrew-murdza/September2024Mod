package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.TropicalCoralReefs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalCoralEcologyTests {
    private static final BlockPos ROOT = new BlockPos(8, 101, 8);

    @GameTest(template = "plant_gameplay_empty")
    public static void bonemealBuildsSameFamilyCoralAndPreservesAnObstructedFan(GameTestHelper helper) {
        for (int depth : new int[]{3, 7}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, depth);
            blocks.put(ROOT, Blocks.FIRE_CORAL_FAN.defaultBlockState());
            blocks.put(ROOT.east(2).above(depth == 3 ? 0 : 3), Blocks.STONE.defaultBlockState());
            helper.assertFalse(TropicalCoralReefs.growFromCoral(world, ROOT, RandomSource.create(17)),
                    "An obstructed arm must cancel the whole tree");
            helper.assertTrue(blocks.size() == 2 && blocks.get(ROOT).is(Blocks.FIRE_CORAL_FAN),
                    "Failed coral growth must preserve the fan");
            blocks.clear();
            blocks.put(ROOT, Blocks.FIRE_CORAL_FAN.defaultBlockState());
            helper.assertTrue(TropicalCoralReefs.growFromCoral(world, ROOT, RandomSource.create(17))
                            && blocks.get(ROOT).is(Blocks.FIRE_CORAL_BLOCK),
                    "A living fan must grow into a branching colony of its family");
        }
        Map<BlockPos, BlockState> wall = new HashMap<>();
        var world = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", wall, 7);
        for (int y = 0; y < 7; y++) for (int z = -3; z <= 3; z++)
            wall.put(ROOT.west().offset(0, y, z), Blocks.STONE.defaultBlockState());
        wall.put(ROOT, Blocks.HORN_CORAL_WALL_FAN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        helper.assertTrue(TropicalCoralReefs.growFromCoral(world, ROOT, RandomSource.create(8))
                        && wall.get(ROOT).is(Blocks.HORN_CORAL_BLOCK) && wall.get(ROOT.east(2)).is(Blocks.HORN_CORAL_BLOCK),
                "A wall fan must extend its colony into the open water and stay attached to its support");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void reefFloorUsesItsRequestedWeightsAndHasNoLooseCorals(GameTestHelper helper) {
        int sponge = 0, pickles = 0, kelp = 0, tall = 0, shortGrass = 0;
        RandomSource random = RandomSource.create(0x163A8F9C73L);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, 8);
        int samples = 8192;
        for (int i = 0; i < samples; i++) {
            blocks.clear();
            helper.assertTrue(TropicalCoralReefs.placeFloorColumn(world, ROOT, random),
                    "Every clear supported reef-floor column receives its selected aquatic cover");
            BlockState root = blocks.get(ROOT);
            if (root.is(Blocks.WET_SPONGE)) sponge++;
            else if (root.is(Blocks.SEA_PICKLE)) pickles++;
            else if (root.is(Blocks.KELP) || root.is(Blocks.KELP_PLANT)) kelp++;
            else if (root.is(Blocks.TALL_SEAGRASS)) tall++;
            else if (root.is(Blocks.SEAGRASS)) shortGrass++;
            else helper.fail("Unexpected reef-floor material: " + root);
            for (var entry : blocks.entrySet()) {
                if (entry.getValue().is(Blocks.SEA_PICKLE)) helper.assertTrue(entry.getValue().getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.PICKLES) == 4,
                        "Every generated sea-pickle block contains four pickles");
                helper.assertFalse(ForgeRegistries.BLOCKS.getKey(entry.getValue().getBlock()).getPath().contains("coral"),
                        "Independent seafloor plants must never add coral bodies, plants or fans");
                helper.assertTrue(entry.getValue().canSurvive(world, entry.getKey()),
                        "Weighted floor plants retain native support and complete columns");
                helper.assertTrue(entry.getKey().getY() < ROOT.getY() + 8,
                        "No floor column may create a water-bearing cell above the real surface");
            }
        }
        helper.assertTrue(sponge == 0, "Independent floor columns must not restore per-cell sponge generation");
        helper.assertTrue(Math.abs((double) pickles / samples - 1.0 / 16) < 0.012,
                "Sea pickle frequency remains near one in 16: " + pickles);
        helper.assertTrue(Math.abs((double) kelp / samples - 1.0 / 16) < 0.012,
                "Kelp frequency remains near one in 16: " + kelp);
        helper.assertTrue(Math.abs((double) tall / (tall + shortGrass) - 0.499) < 0.025,
                "Remaining grass cover is 49.9 percent tall: " + tall + "/" + shortGrass);
        AOEMod.LOGGER.info("Tropical reef floor audit: {} columns, {} sponge, {} pickles, {} kelp, {} tall grass, {} short grass",
                samples, sponge, pickles, kelp, tall, shortGrass);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveFloorIsCoveredWithoutKelpAndKeepsRequestedGrassMix(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", blocks, 4);
        RandomSource random = RandomSource.create(982151L);
        int tall = 0, shortGrass = 0, pickles = 0;
        for (int i = 0; i < 4096; i++) {
            blocks.clear();
            helper.assertTrue(TropicalCoralReefs.placeFloorColumn(world, ROOT, random), "Every clear mangrove floor cell gets cover");
            BlockState state = blocks.get(ROOT);
            if (state.is(Blocks.TALL_SEAGRASS)) tall++;
            else if (state.is(Blocks.SEAGRASS)) shortGrass++;
            else if (state.is(Blocks.SEA_PICKLE)) pickles++;
            else helper.fail("Mangrove floor cannot generate kelp, loose coral or per-cell sponges: " + state);
        }
        helper.assertTrue(Math.abs(pickles / 4096.0 - .0625) < .015, "Mangrove pickle probability is one in sixteen");
        helper.assertTrue(Math.abs((double) tall / (tall + shortGrass) - .499) < .035, "Mangrove grass uses 49.9 percent tall cover");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void waterloggedUpperObstaclesUseShortFloorCoverWithoutBeingOverwritten(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", blocks, 4);
        BlockState obstacle = Blocks.MANGROVE_ROOTS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        for (int seed = 0; seed < 128; seed++) {
            blocks.clear();
            blocks.put(ROOT.above(), obstacle);
            helper.assertTrue(TropicalCoralReefs.placeFloorColumn(world, ROOT, RandomSource.create(seed * 1000003L)),
                    "Blocked tall-floor choices must still fill the supported root cell");
            helper.assertTrue(world.getBlockState(ROOT).is(Blocks.SEAGRASS) || world.getBlockState(ROOT).is(Blocks.SEA_PICKLE),
                    "A waterlogged upper obstacle requires short floor cover");
            helper.assertTrue(world.getBlockState(ROOT.above()) == obstacle,
                    "A rejected tall plant cannot overwrite a waterlogged root or create a partial upper half");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void spongesAreOneFeatureInEveryEligibleChunk(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, 8);
        int selected = 0;
        for (int cx = -16; cx < 16; cx++) for (int cz = -16; cz < 16; cz++) {
            blocks.clear();
            java.util.List<BlockPos> candidates = java.util.List.of(new BlockPos(cx * 16 + 4, 101, cz * 16 + 4),
                    new BlockPos(cx * 16 + 8, 101, cz * 16 + 8));
            boolean placed = TropicalCoralReefs.placeSponge(world, candidates, cx, cz, RandomSource.create(cx * 341873128712L + cz));
            helper.assertTrue(placed == TropicalCoralReefs.spongeChunk(world.getSeed(), cx, cz), "Every selected valid chunk gets its sponge");
            helper.assertTrue(blocks.size() == (placed ? 1 : 0), "Sponge feature must create at most one block per chunk");
            if (placed) selected++;
        }
        helper.assertTrue(selected == 1024, "Every eligible chunk receives a sponge: " + selected);
        blocks.clear();
        WorldGenLevel shallow = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", blocks, 1);
        for (int cx = 0; cx < 8; cx++) helper.assertFalse(TropicalCoralReefs.placeSponge(shallow,
                        java.util.List.of(new BlockPos(cx * 16 + 8, 101, 8)), cx, 0, RandomSource.create(cx)),
                "Sponge feature cannot consume one-deep water");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void reefFloorUsesShortFallbacksInShallowWater(GameTestHelper helper) {
        for (int depth = 1; depth <= 2; depth++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, depth);
            RandomSource random = RandomSource.create(163841L);
            for (int i = 0; i < 512; i++) {
                blocks.clear();
                helper.assertTrue(TropicalCoralReefs.placeFloorColumn(world, ROOT, random),
                        "Shallow reef margins retain short aquatic cover");
                helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(Blocks.TALL_SEAGRASS)),
                        "Tall seagrass must never enter one- or two-block-deep water");
                if (depth == 1) helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(Blocks.WET_SPONGE)),
                        "Wet sponges must never enter one-block-deep water");
                helper.assertTrue(world.getBlockState(ROOT.above(depth)).isAir(),
                        "Shallow fallback cannot create floating water above the original pool");
            }
        }
        Map<BlockPos, BlockState> blocked = new HashMap<>();
        for (String biome : new String[]{"rainforest", "cherry_grove"}) {
            blocked.clear();
            WorldGenLevel forest = AquaticPlantGenerationTests.fixture(helper, biome, blocked, 8);
            helper.assertTrue(TropicalCoralReefs.placeFloorColumn(forest, ROOT, RandomSource.create(1)),
                    "Forest-edge surface water shares the dedicated floor owner");
            helper.assertFalse(blocked.values().stream().anyMatch(state -> state.is(Blocks.KELP)
                            || state.is(Blocks.KELP_PLANT) || state.is(Blocks.WET_SPONGE)),
                    "Forest-edge cover cannot add reef kelp or independent sponges");
        }
        blocked.clear();
        WorldGenLevel overworld = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocked, 8,
                helper.getLevel().getServer().overworld());
        helper.assertFalse(TropicalCoralReefs.placeFloorColumn(overworld, ROOT, RandomSource.create(1)),
                "The dedicated coral-floor pass cannot change any other dimension");
        helper.assertTrue(blocked.isEmpty(), "Rejected habitats remain untouched");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeCanopyHeightmapsCannotHideMangroveFloorOrOverwriteWetPlants(GameTestHelper helper) {
        SurfaceFixture fixture = surfaceFixture(helper, 481531L, (x, z) -> "mangrove_swamp", 8);
        var world = fixture.world();
        var chunk = fixture.chunk();
        BlockState leaves = Blocks.MANGROVE_LEAVES.defaultBlockState();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            chunk.setBlockState(new BlockPos(x, 114, z), leaves, false);
        }
        for (int y = 109; y < 114; y++) chunk.setBlockState(new BlockPos(8, y, 8),
                Blocks.MANGROVE_LOG.defaultBlockState(), false);
        BlockState root = Blocks.MANGROVE_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        BlockPos upperRoot = new BlockPos(4, 102, 4), bottomRoot = new BlockPos(5, 101, 5);
        chunk.setBlockState(upperRoot, root, false);
        chunk.setBlockState(bottomRoot, root, false);
        BlockPos plant = new BlockPos(6, 101, 6);
        BlockState lower = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        chunk.setBlockState(plant, lower, false);
        chunk.setBlockState(plant.above(), upper, false);
        // A missing WG map can be reconstructed after trees already exist,
        // unlike the earlier terrain-only map retained during normal features.
        Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        helper.assertTrue(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, 4, 4) + 1 == 115,
                "The native ocean-floor heightmap must reproduce a canopy hiding the real floor");
        TropicalCoralReefs.place(context(world, 19));
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            helper.assertFalse(world.getBlockState(new BlockPos(x, 101, z)).is(Blocks.WATER),
                    "Every clear supported mangrove seabed cell under the native canopy needs cover");
            helper.assertTrue(world.getBlockState(new BlockPos(x, 114, z)) == leaves,
                    "The floor search must preserve the existing canopy");
        }
        helper.assertTrue(world.getBlockState(upperRoot) == root && world.getBlockState(bottomRoot) == root,
                "A floor search must skip existing roots without overwriting them");
        helper.assertTrue(world.getBlockState(plant) == lower && world.getBlockState(plant.above()) == upper,
                "Existing complete wet plants must survive the dedicated floor pass");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fullMixedBiomePassHasOneSpongeOwnerAndLeavesBuriedPoolsUntouched(GameTestHelper helper) {
        int spongeChunks = 0;
        for (int sample = 0; sample < 16; sample++) {
            long seed = 0x19831F451L + sample * 1000003L;
            SurfaceFixture fixture = surfaceFixture(helper, seed,
                    (x, z) -> z < 8 ? x < 8 ? "coral_reef" : "mangrove_swamp"
                            : x < 8 ? "rainforest" : "cherry_grove", 8);
            var world = fixture.world();
            var chunk = fixture.chunk();
            // A dry surface above an underground pool cannot transfer ownership
            // of that buried pool to the tropical surface-decoration feature.
            for (int y = 101; y <= 108; y++) chunk.setBlockState(new BlockPos(0, y, 0), Blocks.AIR.defaultBlockState(), false);
            chunk.setBlockState(new BlockPos(0, 89, 0), Blocks.MOSS_BLOCK.defaultBlockState(), false);
            for (int y = 90; y <= 92; y++) chunk.setBlockState(new BlockPos(0, y, 0), Blocks.WATER.defaultBlockState(), false);
            chunk.setBlockState(new BlockPos(0, 93, 0), Blocks.MOSS_BLOCK.defaultBlockState(), false);
            new DocumentedFloraFeature().place(context(world, sample * 91813L));
            int sponges = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (x == 0 && z == 0) continue;
                BlockState state = world.getBlockState(new BlockPos(x, 101, z));
                helper.assertFalse(state.is(Blocks.WATER), "All four surface-biome portions require aquatic floor cover");
                if (state.is(Blocks.WET_SPONGE)) sponges++;
                if (z >= 8) helper.assertFalse(state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT),
                        "Forest-edge cells use the non-reef floor palette");
            }
            int expected = TropicalCoralReefs.spongeChunk(seed, 0, 0) ? 1 : 0;
            helper.assertTrue(sponges == expected,
                    "A mixed biome chunk must retain exactly its single sponge opportunity: " + sponges + "/" + expected);
            spongeChunks += sponges;
            for (int y = 90; y <= 92; y++) helper.assertTrue(world.getBlockState(new BlockPos(0, y, 0)).is(Blocks.WATER),
                    "Neither the surface pass nor the generic flora scan may decorate buried tropical pools");
        }
        helper.assertTrue(spongeChunks == 16, "Every mixed-biome chunk must receive exactly one sponge");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fullReefPassKeepsOneTreeAndItsNineDecoratedTops(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, 8);
        helper.assertTrue(TropicalCoralReefs.place(new FeaturePlaceContext<>(Optional.empty(), world,
                        world.getLevel().getChunkSource().getGenerator(), RandomSource.create(42), ROOT,
                        NoneFeatureConfiguration.INSTANCE)), "The full dedicated reef pass must place its tree and floor");
        int roots = 0, tops = 0;
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (!ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath().endsWith("_coral_block")) continue;
            if (pos.getY() == ROOT.getY()) roots++;
            BlockState above = world.getBlockState(pos.above());
            if (!ForgeRegistries.BLOCKS.getKey(above.getBlock()).getPath().endsWith("_coral_block")) {
                tops++;
                helper.assertTrue(TropicalCoralReefs.isDocumentedCoral(above),
                        "The later floor pass must preserve every decorated tree top");
                helper.assertTrue(above.getFluidState().is(FluidTags.WATER),
                        "All coral-tree decorations remain in their original water");
            }
        }
        helper.assertTrue(roots == 1 && tops == 9, "A full reef chunk must keep one complete tree with nine occupied tops");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyCoralTreeDistributesThreeOrFourPickleSpotsAcrossBothLevels(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, 8);
        for (int seed = 0; seed < 128; seed++) {
            blocks.clear();
            helper.assertTrue(TropicalCoralReefs.placeColony(world, ROOT, RandomSource.create(seed * 1000003L), 0),
                    "Every unobstructed coral tree must place atomically");
            long total = blocks.values().stream().filter(state -> state.is(Blocks.SEA_PICKLE)).count();
            long upper = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.SEA_PICKLE)
                    && entry.getKey().getY() == ROOT.getY() + 6).count();
            long lower = blocks.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.SEA_PICKLE)
                    && entry.getKey().getY() == ROOT.getY() + 4
                    && (entry.getKey().getX() != ROOT.getX() || entry.getKey().getZ() != ROOT.getZ())).count();
            helper.assertTrue((total == 3 || total == 4) && upper >= 1 && lower >= 1,
                    "Pickles need three or four slots, including both arm levels: " + total + "/" + upper + "/" + lower);
        }
        helper.succeed();
    }

    private static FeaturePlaceContext<NoneFeatureConfiguration> context(WorldGenLevel world, long seed) {
        return new FeaturePlaceContext<>(Optional.empty(), world, world.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(seed), ROOT, NoneFeatureConfiguration.INSTANCE);
    }

    private record SurfaceFixture(WorldGenLevel world, ProtoChunk chunk) { }

    /** Use native heightmaps, including explicit reconstruction after a canopy. */
    private static SurfaceFixture surfaceFixture(GameTestHelper helper, long seed,
                                                 java.util.function.BiFunction<Integer, Integer, String> biomeAt,
                                                 int depth) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level, biomes, null);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            chunk.setBlockState(new BlockPos(x, 100, z), Blocks.MOSS_BLOCK.defaultBlockState(), false);
            for (int y = 101; y <= 100 + depth; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), false);
        }
        chunk.setStatus(ChunkStatus.CARVERS);
        Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        WorldGenLevel world = (WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(
                TropicalCoralEcologyTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getSeed": return seed;
                        case "getBiome": {
                            BlockPos pos = (BlockPos) args[0];
                            return biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                                    new ResourceLocation("aoemod", biomeAt.apply(pos.getX(), pos.getZ()))));
                        }
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight()
                                : chunk.getHeight((Heightmap.Types) args[0], (int) args[1] & 15, (int) args[2] & 15) + 1;
                        case "getBlockState": return chunk.getBlockState((BlockPos) args[0]);
                        case "getFluidState": return chunk.getFluidState((BlockPos) args[0]);
                        case "isEmptyBlock": return chunk.getBlockState((BlockPos) args[0]).isAir();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1])
                                .test(chunk.getBlockState((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(chunk.getFluidState((BlockPos) args[0]));
                        case "setBlock": return chunk.setBlockState((BlockPos) args[0], (BlockState) args[1], false) != null;
                        default:
                            try { return method.invoke(level, args); }
                            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
        return new SurfaceFixture(world, chunk);
    }
}
