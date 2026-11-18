package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.SavannaVegetation;
import net.amurdza.examplemod.worldgen.SavannaFlowerBands;
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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaVegetationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void savannaRetainsOriginalSeafloorAndPlacesGrassAfterFlowers(GameTestHelper helper) {
        var biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .get(new ResourceLocation("aoemod:savanna"));
        var features = biome.getGenerationSettings().features()
                .get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).stream()
                .map(feature -> feature.unwrapKey().orElseThrow().location().toString()).toList();
        helper.assertTrue(features.contains("aoemod:savanna/seafloor"),
                "Savanna must retain the original weighted seafloor feature");
        helper.assertTrue(features.indexOf("aoemod:savanna/grass")
                        > features.indexOf("aoemod:documented_flora") && features.contains("aoemod:documented_flora"),
                "Grass patches must run after flowers/crops so highland planting space remains available");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void flowerStripesKeepSixOrderedColoursWidthAndCoverage(GameTestHelper helper) {
        helper.assertTrue(SavannaFlowerBands.TULIPS.equals(java.util.List.of("minecraft:red_tulip", "minecraft:white_tulip",
                        "floral:blue_tulip", "minecraft:orange_tulip", "minecraft:pink_tulip", "floral:yellow_tulip")),
                "Every stripe must retain the chosen red, white, blue, orange, pink, yellow sequence");
        for (long seed : new long[]{0, 50881, -49835}) {
            SavannaFlowerBands bands = new SavannaFlowerBands(seed);
            int allowed = 0, flowers = 0;
            for (int x = -512; x < 512; x++) for (int z = -512; z < 512; z++) {
                if (bands.colour(x, z) >= 0) allowed++;
                if (bands.hasFlower(x, z)) flowers++;
            }
            helper.assertTrue(allowed > 1048576 * .235 && allowed < 1048576 * .255,
                    "39-block composite stripes with 120-block gaps must cover about 24.5%: " + allowed + "/1048576");
            helper.assertTrue(flowers > allowed * .043 && flowers < allowed * .051,
                    "Flower density inside stripes must be 12/256: " + flowers + "/" + allowed);
            for (int along = -1000; along <= 1000; along += 97) {
                double x = -bands.normalZ() * along, z = bands.normalX() * along;
                double start = -bands.phase(x, z);
                for (int colour = 0; colour < 6; colour++) {
                    double centre = start + (colour + .5) * SavannaFlowerBands.COLOUR_WIDTH;
                    double cx = x + centre * bands.normalX(), cz = z + centre * bands.normalZ();
                    helper.assertTrue(bands.colour(cx, cz) == colour,
                            "Each composite stripe must contain all six adjacent colours exactly once and in order");
                    double dx = (bands.phase(cx + .01, cz) - bands.phase(cx - .01, cz)) / .02;
                    double dz = (bands.phase(cx, cz + .01) - bands.phase(cx, cz - .01)) / .02;
                    double physicalWidth = SavannaFlowerBands.COLOUR_WIDTH / Math.hypot(dx, dz);
                    helper.assertTrue(physicalWidth >= 5 && physicalWidth <= 8,
                            "A colour subband is outside the 5–8 block physical width: " + physicalWidth);
                }
                double gap = start + SavannaFlowerBands.FLOWER_WIDTH + 1;
                helper.assertTrue(bands.colour(x + gap * bands.normalX(), z + gap * bands.normalZ()) == -1,
                        "The rest of the shared stripe phase must prohibit all tulip colours");
                // Measure in physical space by searching the opposite curved edge,
                // including tangent displacement rather than only the phase normal.
                double sourceNormal = SavannaFlowerBands.FLOWER_WIDTH - bands.phase(x, z);
                double nearest = Double.POSITIVE_INFINITY;
                for (double shift = -140; shift <= 140; shift += .5) {
                    double targetPhase = bands.phase(-bands.normalZ() * (along + shift), bands.normalX() * (along + shift));
                    double across = SavannaFlowerBands.PERIOD - targetPhase - sourceNormal;
                    nearest = Math.min(nearest, Math.hypot(across, shift));
                }
                helper.assertTrue(nearest >= 100 && nearest <= 140,
                        "The physical flower-free edge gap must stay within 100–140 blocks: " + nearest);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cropsHaveThreeAdjacentChunksAndMushroomsTenRootsOutsideFlowerStripes(GameTestHelper helper) {
        for (long seed : new long[]{50881, -49835}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, 70, 0, seed);
            var generator = world.getLevel().getChunkSource().getGenerator();
            int origin = seed > 0 ? 0 : -288;
            for (int x = origin; x < origin + 288; x += 16) for (int z = origin; z < origin + 288; z += 16) {
                SavannaVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world, generator,
                        RandomSource.create(seed ^ new BlockPos(x, 0, z).asLong()), new BlockPos(x, 0, z), NoneFeatureConfiguration.INSTANCE),
                        (px, pz) -> 70);
            }
            int eligible = 0, flowers = 0;
            Map<String, Integer> counts = new HashMap<>();
            for (int x = origin; x < origin + 288; x++) for (int z = origin; z < origin + 288; z++) {
                String expected = SavannaVegetation.tulip(seed, x, z);
                if (expected != null) eligible++;
                BlockState state = world.getBlockState(new BlockPos(x, 71, z));
                String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                if (id.endsWith("_tulip")) {
                    helper.assertTrue(id.equals(expected), "Tulip colour or stripe mismatch at " + x + "," + z);
                    flowers++;
                } else if (isPatchSpecies(id)) {
                    helper.assertTrue(expected == null, "Wild crops and mushrooms must stay outside flower stripes");
                    counts.merge(id, 1, Integer::sum);
                } else {
                    helper.assertTrue(state.isAir(), "Unexpected scattered plant in Savanna: " + id);
                }
            }
            for (String species : SavannaVegetation.PATCH_SPECIES) {
                int expected = species.endsWith("_mushroom") ? 10 : species.equals("minecraft:pumpkin") ? 6 : 15;
                helper.assertTrue(counts.getOrDefault(species, 0) == expected,
                        "Each dry 288 square needs six pumpkins, fifteen roots per wild crop and ten per mushroom: " + species + "=" + counts.get(species));
            }
            helper.assertTrue(counts.getOrDefault(net.amurdza.examplemod.worldgen.CropTiles.SAVANNA_CORN, 0) == 0,
                    "The natural vegetation feature must not add wild corn");
            helper.assertTrue(flowers > eligible * .035 && flowers < eligible * .06,
                    "Only 12/256 of allowed stripe columns should contain tulips: " + flowers + "/" + eligible);
            AOEMod.LOGGER.info("Savanna flower fixture seed {}: {} tulips in {} allowed columns; per-species roots {}",
                    seed, flowers, eligible, counts);
            int rx = Math.floorDiv(origin, 288), rz = Math.floorDiv(origin, 288);
            var patches = SavannaVegetation.cropPatches(seed, rx, rz, (x, z) -> 70);
            helper.assertTrue(patches.size() == 11 && patches.equals(SavannaVegetation.cropPatches(seed, rx, rz, (x, z) -> 70)),
                    "The per-species patch plan must be deterministic");
            for (var patch : patches) {
                int count = 0, minX = Math.floorDiv(patch.x(), 8) * 8, minZ = Math.floorDiv(patch.z(), 8) * 8;
                int firstX = Integer.MAX_VALUE, lastX = Integer.MIN_VALUE, firstZ = Integer.MAX_VALUE, lastZ = Integer.MIN_VALUE;
                for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
                    String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(world.getBlockState(new BlockPos(x, 71, z)).getBlock()).toString();
                    if (isPatchSpecies(id)) {
                        helper.assertTrue(id.equals(patch.species()), "A reserved eight-block cell must contain one patch species"); count++;
                        firstX = Math.min(firstX, x); lastX = Math.max(lastX, x); firstZ = Math.min(firstZ, z); lastZ = Math.max(lastZ, z);
                    }
                }
                helper.assertTrue(count == CropTiles.plantsPerPatch(patch.species())
                                && lastX - firstX < 6 && lastZ - firstZ < 6,
                        "Every complete patch must have its species quota inside a compact 6x6 footprint");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void submergedCandidatesDoNotConsumeSpeciesQuotas(GameTestHelper helper) {
        long seed = 50881;
        java.util.function.DoubleBinaryOperator surface = (x, z) -> Math.floorMod((int) x, 288) < 80 ? 59 : 70;
        var patches = SavannaVegetation.cropPatches(seed, 0, 0, surface);
        helper.assertTrue(patches.size() == 11, "Water exclusions must be applied before allocating the eleven-patch quota");
        for (String species : SavannaVegetation.PATCH_SPECIES) {
            helper.assertTrue(patches.stream().filter(patch -> patch.species().equals(species)).count() == (species.endsWith("_mushroom") ? 1 : 3),
                    "A lake must not consume " + species + " patch opportunities");
        }
        for (var patch : patches) {
            int suitable = 0;
            for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
                int x = patch.x() + dx, z = patch.z() + dz;
                if (surface.applyAsDouble(x, z) >= SavannaVegetation.MINIMUM_DRY_HEIGHT
                        && SavannaVegetation.tulip(seed, x, z) == null) suitable++;
            }
            helper.assertTrue(suitable >= CropTiles.plantsPerPatch(patch.species()),
                    "Selected footprint must fit its dry, flower-free plant quota");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void exposedGrassAtY66RetainsEverySpeciesQuota(GameTestHelper helper) {
        long seed = 50881;
        helper.assertTrue(SavannaVegetation.cropPatches(seed, 0, 0, (x, z) -> 61.5).isEmpty(),
                "Density zero at Y62 leaves a water block below sea level63");
        helper.assertTrue(SavannaVegetation.cropPatches(seed, 0, 0, (x, z) -> Math.nextUp(61.5)).size() == 11,
                "A positive density at Y62 makes the shoreline dry and eligible");
        var patches = SavannaVegetation.cropPatches(seed, 0, 0, (x, z) -> 66);
        helper.assertTrue(patches.size() == 11, "Exposed shore grass at Y66 must remain eligible for all patch quotas");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 66, 0, seed);
        for (var patch : patches) {
            helper.assertTrue(SavannaVegetation.placeCropPatch(world, patch, RandomSource.create(seed ^ patch.x()))
                            == CropTiles.plantsPerPatch(patch.species()),
                    "A dry Y66 shoreline must support a complete patch of " + patch.species());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void grassKeepsCoverageWithOneQuarterTallRoots(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 70, 0, 50881);
        var generator = world.getLevel().getChunkSource().getGenerator();
        for (int x = 0; x < 128; x += 16) for (int z = 0; z < 128; z += 16) {
            SavannaVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world, generator,
                    RandomSource.create(x * 31L + z), new BlockPos(x, 0, z), NoneFeatureConfiguration.INSTANCE), (px, pz) -> 70);
        }
        var plants = Map.copyOf(blocks);
        Map<BlockPos, BlockState> baselineBlocks = new HashMap<>(plants);
        WorldGenLevel baseline = fixture(helper, baselineBlocks, 70, 0, 50881);
        var registry = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var grass = registry.get(new ResourceLocation("aoemod:savanna/grass"));
        var vanilla = registry.get(new ResourceLocation("minecraft:patch_grass_savanna"));
        // The baseline feature was intentionally removed from the biome. Keep its
        // count/spread/height placement, omitting only that membership filter.
        var baselineGrass = new net.minecraft.world.level.levelgen.placement.PlacedFeature(vanilla.feature(),
                vanilla.placement().stream().filter(modifier -> !(modifier instanceof
                        net.minecraft.world.level.levelgen.placement.BiomeFilter)).toList());
        for (int x = 0; x < 128; x += 16) for (int z = 0; z < 128; z += 16) {
            long seed = 77281 + x * 31L + z;
            grass.placeWithBiomeCheck(world, generator, RandomSource.create(seed), new BlockPos(x, 0, z));
            baselineGrass.place(baseline, generator, RandomSource.create(seed), new BlockPos(x, 0, z));
        }
        plants.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                "Grass decoration must preserve flowers and crops"));
        int empty = 0, grassy = 0, tall = 0, original = 0;
        for (int x = 0; x < 128; x++) for (int z = 0; z < 128; z++) {
            BlockPos pos = new BlockPos(x, 71, z);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) empty++;
            if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)) grassy++;
            if (state.is(Blocks.TALL_GRASS)) {
                tall++;
                helper.assertTrue(world.getBlockState(pos.above()).is(Blocks.TALL_GRASS), "Tall grass needs both halves");
            }
            if (baseline.getBlockState(pos).is(Blocks.GRASS)) original++;
        }
        helper.assertTrue(tall >= grassy * 0.22 && tall <= grassy * 0.28,
                "About 25% of actual grass roots should be tall; tall=" + tall + ", total=" + grassy);
        helper.assertTrue(grassy > original * 0.85 && grassy < original * 1.15 && empty > 3000,
                "Keep the original patch coverage and bare space; new=" + grassy + ", old=" + original + ", empty=" + empty);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void neighbouringGrassCannotCancelReservedTulips(GameTestHelper helper) {
        long seed = 50881;
        var bands = new SavannaFlowerBands(seed);
        java.util.List<BlockPos> wanted = new java.util.ArrayList<>();
        int minX = 0, minZ = 0;
        search: for (int cx = 0; cx < 32; cx++) for (int cz = 0; cz < 32; cz++) {
            wanted.clear(); minX = cx * 16; minZ = cz * 16;
            for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
                if (bands.hasFlower(x, z)) wanted.add(new BlockPos(x, 71, z));
            }
            if (wanted.size() >= 8) break search;
        }
        helper.assertTrue(wanted.size() >= 8, "Fixture needs a populated flower stripe");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 70, 0, seed);
        var half = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
        for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
            BlockPos pos = new BlockPos(x, 71, z);
            if ((x + z) % 2 == 0) {
                blocks.put(pos, Blocks.TALL_GRASS.defaultBlockState());
                blocks.put(pos.above(), Blocks.TALL_GRASS.defaultBlockState().setValue(half,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            } else blocks.put(pos, Blocks.GRASS.defaultBlockState());
        }
        BlockPos crop = wanted.get(0), mushroom = wanted.get(1), canopy = wanted.get(2);
        blocks.put(crop, Blocks.PUMPKIN.defaultBlockState()); blocks.remove(crop.above());
        blocks.put(mushroom, Blocks.RED_MUSHROOM.defaultBlockState()); blocks.remove(mushroom.above());
        blocks.put(canopy, Blocks.GRASS.defaultBlockState()); blocks.put(canopy.above(), Blocks.ACACIA_LEAVES.defaultBlockState());
        var before = Map.copyOf(blocks);
        SavannaVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world,
                world.getLevel().getChunkSource().getGenerator(), RandomSource.create(17), new BlockPos(minX, 0, minZ),
                NoneFeatureConfiguration.INSTANCE), (x, z) -> 70);
        for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
            BlockPos pos = new BlockPos(x, 71, z);
            if (bands.hasFlower(x, z) && !pos.equals(crop) && !pos.equals(mushroom)) {
                String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(world.getBlockState(pos).getBlock()).toString();
                helper.assertTrue(id.equals(bands.tulip(x, z)), "Pre-existing grass cancelled a reserved flower at " + pos);
                if (!pos.equals(canopy)) helper.assertTrue(world.isEmptyBlock(pos.above()), "Old tall-grass upper half remains above a tulip");
            } else helper.assertTrue(world.getBlockState(pos).equals(before.get(pos)), "Flowers replaced an unreserved plant at " + pos);
        }
        helper.assertTrue(world.getBlockState(canopy.above()).is(Blocks.ACACIA_LEAVES), "Single-height tulips must preserve overhanging leaves");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rejectedTulipWritesRestoreBothGrassHalves(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 70, 0, 50881);
        BlockPos pos = new BlockPos(0, 71, 0);
        BlockState lower = Blocks.TALL_GRASS.defaultBlockState();
        BlockState upper = lower.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        blocks.put(pos, lower); blocks.put(pos.above(), upper);
        WorldGenLevel rejecting = (WorldGenLevel) Proxy.newProxyInstance(SavannaVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setBlock") && ((BlockState) args[1]).is(Blocks.RED_TULIP)) {
                        // Model a rejected write after an interceptor touched the destination.
                        world.setBlock((BlockPos) args[0], (BlockState) args[1], (int) args[2]);
                        world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
                        return false;
                    }
                    try { return method.invoke(world, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        helper.assertFalse(SavannaVegetation.placeTulip(rejecting, 0, 0, "minecraft:red_tulip", RandomSource.create(17)),
                "Fixture should reject the tulip placement");
        helper.assertTrue(world.getBlockState(pos).equals(lower) && world.getBlockState(pos.above()).equals(upper),
                "Rejected flower placement must restore both original grass halves");
        helper.succeed();
    }

    private static boolean isPatchSpecies(String id) {
        return SavannaVegetation.PATCH_SPECIES.contains(id)
                || id.equals(net.amurdza.examplemod.worldgen.CropTiles.SAVANNA_CORN);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         int floor, int waterDepth) {
        return fixture(helper, blocks, floor, waterDepth, 50881);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         int floor, int waterDepth, long seed) {
        var savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var biome = savanna.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= floor ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : pos.getY() <= floor + waterDepth ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(SavannaVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return savanna;
                        case "getSeed": return seed;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? savanna.getHeight() : floor + waterDepth + 1;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition":
                            return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition":
                            return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                        case "canSeeSky": return true;
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try {
                                return method.invoke(savanna, args);
                            } catch (InvocationTargetException failure) {
                                throw failure.getCause();
                            }
                    }
                });
    }
}
