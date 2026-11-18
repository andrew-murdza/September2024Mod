package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.NetherCropClusters;
import net.amurdza.examplemod.worldgen.NetherVegetation;
import net.amurdza.examplemod.worldgen.PowderCaneShape;
import net.amurdza.examplemod.worldgen.PowderCanePatches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherVegetationRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void compactEyeblossomsAndWartKeepTheirPatchSizes(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel valley = fixture(helper, blocks, "soul_sand_valley", pos -> pos.getY() <= 104
                ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.AIR.defaultBlockState());
        BlockPos anchor = new BlockPos(8, 105, 8);
        int count = NetherVegetation.placeEyeblossomPatch(valley, anchor, RandomSource.create(652));
        helper.assertTrue(count == 5 && blocks.size() == 5, "A flower patch must place exactly five flowers");
        helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= 6 && pos.getX() <= 11
                        && pos.getZ() >= 6 && pos.getZ() <= 11), "Eyeblossoms must remain in their six-square footprint");
        blocks.clear();
        helper.assertTrue(NetherVegetation.placeEyeblossomPatch(valley, anchor.above(), RandomSource.create(652)) == 0
                        && NetherVegetation.placeEyeblossomPatch(valley, anchor.below(), RandomSource.create(652)) == 0
                        && blocks.isEmpty(), "Nearby floors must not rescue a patch whose sampled origin missed the floor");
        int wart = NetherCropClusters.placePatch(valley, new CropTiles.Patch(8, 8, "minecraft:nether_wart"));
        helper.assertTrue(wart == 5 && blocks.size() == 5 && blocks.values().stream().allMatch(state ->
                        state.is(Blocks.NETHER_WART) && state.getValue(BlockStateProperties.AGE_3) == 3),
                "Wart patches must place exactly five mature plants");
        helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= 6 && pos.getX() <= 11
                        && pos.getZ() >= 6 && pos.getZ() <= 11), "Nether wart must remain in its six-square footprint");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void eyeblossomPatchCountMatchesVanillaVinesAfterThreefoldReduction(GameTestHelper helper) {
        Map<BlockPos, BlockState> flowers = new HashMap<>(), vines = new HashMap<>();
        WorldGenLevel valley = fixture(helper, flowers, "soul_sand_valley", pos -> pos.getY() <= 104
                ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.AIR.defaultBlockState());
        WorldGenLevel forest = fixture(helper, vines, "warped_forest", pos -> pos.getY() <= 104
                ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.AIR.defaultBlockState());
        var generator = forest.getLevel().getChunkSource().getGenerator();
        var vanillaVines = forest.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("minecraft:twisting_vines"));
        PlacementContext placement = new PlacementContext(forest, generator, Optional.of(vanillaVines));
        int flowerPatches = 0;
        int[] vinePatches = {0};
        RandomSource flowerRandom = RandomSource.create(16393), vineRandom = RandomSource.create(85273);
        for (int sample = 0; sample < 8192; sample++) {
            flowers.clear();
            vines.clear();
            int patches = NetherVegetation.placeEyeblossomPatches(valley, BlockPos.ZERO, flowerRandom);
            flowerPatches += patches;
            helper.assertTrue(flowers.size() == patches * 5, "Density counts complete five-flower patches");
            // Execute the registered vanilla placement modifiers and configured
            // shape; count successful patch calls, not individual vine blocks.
            java.util.stream.Stream<BlockPos> origins = java.util.stream.Stream.of(BlockPos.ZERO);
            for (var modifier : vanillaVines.placement()) {
                origins = origins.flatMap(pos -> modifier.getPositions(placement, vineRandom, pos));
            }
            origins.forEach(pos -> {
                int before = vines.size();
                vanillaVines.feature().value().place(forest, generator, vineRandom, pos);
                if (vines.size() > before) vinePatches[0]++;
            });
        }
        double ratio = flowerPatches / (double) vinePatches[0];
        helper.assertTrue(vinePatches[0] > 450 && ratio > .8 && ratio < 1.2,
                "One third of the previous frequency must produce about as many complete patches as vanilla vines: "
                        + flowerPatches + "/" + vinePatches[0]);
        AOEMod.LOGGER.info("Nether patch audit: {} complete eyeblossom patches / {} vanilla twisting-vine patches",
                flowerPatches, vinePatches[0]);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void powderCanePatchesRetryLavaAndRockWithoutLeavingPartialGroves(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel basalt = fixture(helper, blocks, "basalt_deltas", pos -> {
            if (pos.getX() < 8) return Blocks.BASALT.defaultBlockState();
            if (pos.getX() < 11 || pos.getZ() < 11) return pos.getY() <= 10
                    ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();
            return pos.getY() <= 8 || pos.getY() >= 16 ? Blocks.BASALT.defaultBlockState() : Blocks.AIR.defaultBlockState();
        });
        var patch = new CropTiles.Patch(4, 4, "mynethersdelight:powdery_cane");
        helper.assertTrue(PowderCanePatches.placePatch(basalt, patch) == 5,
                "A blocked reservation must retry actual dry land and fit seven-block mature canes under its ceiling");
        long roots = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 9
                && PowderCaneShape.isStem(entry.getValue())).count();
        helper.assertTrue(roots == 5 && blocks.size() == 35,
                "A successful patch must contain exactly five complete seven-block trees");
        helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= 11 && pos.getZ() >= 11
                        && pos.getX() < 16 && pos.getZ() < 16), "Cane placement must avoid lava and solid rock");
        int before = blocks.size();
        helper.assertTrue(PowderCanePatches.placePatch(basalt, patch) == 0 && blocks.size() == before,
                "Repeated vegetation passes must not duplicate a tile's grove");
        blocks.clear();
        WorldGenLevel insufficient = fixture(helper, blocks, "basalt_deltas", pos -> pos.getX() >= 14 && pos.getZ() >= 14
                ? pos.getY() <= 8 ? Blocks.BASALT.defaultBlockState() : Blocks.AIR.defaultBlockState()
                : Blocks.LAVA.defaultBlockState());
        helper.assertTrue(PowderCanePatches.placePatch(insufficient, patch) == 0 && blocks.isEmpty(),
                "Only four dry roots must leave no partial patch or plants in lava");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void powderCaneTilesReserveOneNineSquarePatch(GameTestHelper helper) {
        for (int tx = -2; tx <= 2; tx++) for (int tz = -2; tz <= 2; tz++) {
            var patches = PowderCanePatches.plan(97513, tx, tz, (x, z) -> Math.floorMod(x, 32) >= 16);
            helper.assertTrue(patches.size() == 1, "Each suitable tile needs one five-cane patch");
            var patch = patches.get(0);
            helper.assertTrue(patch.x() - 4 >= tx * 120 && patch.x() + 4 < tx * 120 + 120
                            && patch.z() - 4 >= tz * 120 && patch.z() + 4 < tz * 120 + 120,
                    "The whole nine-square footprint must remain in its tile, including negative coordinates");
            helper.assertTrue(Math.floorDiv(patch.x() - 4, 16) == Math.floorDiv(patch.x() + 4, 16)
                            && Math.floorDiv(patch.z() - 4, 16) == Math.floorDiv(patch.z() + 4, 16),
                    "The grove must be written entirely by one decorating chunk");
            helper.assertTrue(patches.equals(PowderCanePatches.plan(97513, tx, tz, (x, z) -> Math.floorMod(x, 32) >= 16)),
                    "Chunk order must not change powder cane reservations");
        }
        helper.assertTrue(PowderCanePatches.plan(97513, 0, 0, (x, z) -> false).isEmpty(),
                "A tile without dry, clear columns must not reserve a patch in lava or solid rock");
        helper.assertTrue(PowderCanePatches.plan(97513, 0, 0, (x, z) -> z == 0 && x >= 0 && x < 5).size() == 1,
                "Five valid roots at a patch edge must remain eligible when the geometric centre is obstructed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void powderCaneReservationsAcceptMatureTreesUnderLowCeilings(GameTestHelper helper) {
        for (int clearance = 0; clearance <= 12; clearance++) {
            BlockState[] states = new BlockState[40];
            java.util.Arrays.fill(states, Blocks.BASALT.defaultBlockState());
            for (int y = 9; y < 9 + clearance; y++) states[y] = Blocks.AIR.defaultBlockState();
            helper.assertTrue(PowderCanePatches.hasDryClearance(new net.minecraft.world.level.NoiseColumn(0, states))
                            == (clearance >= 7),
                    "Reservation must use the same seven-block minimum as an actual mature cane: " + clearance);
            states[8] = Blocks.LAVA.defaultBlockState();
            helper.assertFalse(PowderCanePatches.hasDryClearance(new net.minecraft.world.level.NoiseColumn(0, states)),
                    "Clear air above lava cannot become a dry cane reservation");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void generatedBasaltTilesContainFiveCompletePowderCanes(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        for (int tx : new int[]{-3, 3}) for (int tz : new int[]{-6, 6}) {
            var patches = PowderCanePatches.plan(nether, tx, tz);
            helper.assertTrue(patches.size() == 1, "Every sampled terrain tile needs a dry powder cane reservation: " + tx + "," + tz);
            var patch = patches.get(0);
            int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
            nether.getChunk(Math.floorDiv(minX, 16), Math.floorDiv(minZ, 16));
            Set<BlockPos> roots = new HashSet<>();
            for (BlockPos mutable : BlockPos.betweenClosed(new BlockPos(minX, 2, minZ), new BlockPos(minX + 15, 24, minZ + 15))) {
                BlockPos pos = mutable.immutable();
                BlockState state = nether.getBlockState(pos);
                if (!PowderCaneShape.isStem(state) || PowderCaneShape.isPlant(nether.getBlockState(pos.below()))) continue;
                roots.add(pos);
                int height = 0;
                while (PowderCaneShape.isPlant(nether.getBlockState(pos.above(height)))) height++;
                helper.assertTrue(height >= 7 && height <= 12 && state.canSurvive(nether, pos),
                        "Generated powder cane must be a complete surviving tree at " + pos + ", height " + height);
            }
            helper.assertTrue(roots.size() == 5, "Tile " + tx + "," + tz + " must generate five actual canes; found " + roots.size());
            int width = roots.stream().mapToInt(BlockPos::getX).max().orElse(0) - roots.stream().mapToInt(BlockPos::getX).min().orElse(0);
            int depth = roots.stream().mapToInt(BlockPos::getZ).max().orElse(0) - roots.stream().mapToInt(BlockPos::getZ).min().orElse(0);
            helper.assertTrue(width <= 8 && depth <= 8, "All five roots must fit in a nine-by-nine area");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void lavaPlantsSplitColorsAndDoubleShortSeagrass(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel lava = fixture(helper, blocks, "basalt_deltas", pos -> pos.getY() <= 0
                ? Blocks.BASALT.defaultBlockState() : pos.getY() <= 12 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState());
        var registry = lava.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (String featureId : new String[]{"water_plants_full", "water_plants_shallow_full"}) {
            var feature = registry.get(new ResourceLocation("aoemod:basalt_deltas/water/" + featureId));
            RandomSource random = RandomSource.create(3517);
            int crimson = 0, warped = 0, shortGrass = 0, allGrass = 0;
            for (int i = 0; i < 20000; i++) {
                blocks.clear();
                feature.place(lava, lava.getLevel().getChunkSource().getGenerator(), random, new BlockPos(0, 1, 0));
                String id = ForgeRegistries.BLOCKS.getKey(lava.getBlockState(new BlockPos(0, 1, 0)).getBlock()).getPath();
                if (id.contains("crimson")) crimson++;
                if (id.contains("warped")) warped++;
                if (id.contains("seagrass")) {
                    allGrass++;
                    if (!id.startsWith("tall_")) shortGrass++;
                }
            }
            helper.assertTrue(crimson > 9600 && crimson < 10400 && warped > 9600 && warped < 10400,
                    "Both lava palettes must split crimson and warped plants equally: " + crimson + "/" + warped);
            double shortShare = shortGrass / (double) allGrass;
            helper.assertTrue(shortShare > .78 && shortShare < .82,
                    "Short lava seagrass must double from forty to eighty percent: " + shortShare);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void commonMixturesMatchOriginalAoECaves(GameTestHelper helper) {
        checkMixture(helper, "crimson_forest", Map.of("minecraft:crimson_roots", .12, "minecraft:crimson_fungus", .005));
        checkMixture(helper, "warped_forest", Map.of("minecraft:warped_roots", .105, "minecraft:nether_sprouts", .192,
                "minecraft:warped_fungus", .003));
        checkMixture(helper, "soul_sand_valley", Map.of("nourished_nether:soul_weeds", .08));
        helper.succeed();
    }

    private static void checkMixture(GameTestHelper helper, String biome, Map<String, Double> expected) {
        RandomSource random = RandomSource.create(0x5041337L);
        Map<String, Integer> counts = new HashMap<>();
        int columns = 200_000;
        for (int i = 0; i < columns; i++) counts.merge(NetherVegetation.commonPlant(biome, random), 1, Integer::sum);
        for (var entry : expected.entrySet()) {
            double actual = counts.getOrDefault(entry.getKey(), 0) / (double) columns;
            helper.assertTrue(Math.abs(actual - entry.getValue()) < .003,
                    "Original AoE cover changed for " + entry.getKey() + ": " + actual);
        }
        helper.assertTrue(counts.keySet().stream().allMatch(id -> id.isEmpty() || expected.containsKey(id)),
                "Unexpected common species in " + biome + ": " + counts.keySet());
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void giantFungiUseVariableHalfChunkCoverage(GameTestHelper helper) {
        for (String biome : new String[]{"crimson_forest", "warped_forest"}) {
            int total = 0;
            Set<Integer> regionalCounts = new HashSet<>();
            for (int region = -8; region < 8; region++) {
                int selected = 0;
                for (int x = region * 16; x < region * 16 + 16; x++) for (int z = -16; z < 16; z++) {
                    boolean first = NetherVegetation.giantFungusChunk(50051, x, z, biome);
                    helper.assertTrue(first == NetherVegetation.giantFungusChunk(50051, x, z, biome),
                            "Giant-fungus decisions must not depend on neighboring chunk order");
                    if (first) selected++;
                }
                total += selected;
                regionalCounts.add(selected);
            }
            helper.assertTrue(total > 3900 && total < 4300, "Expected about half of 8192 chunks, got " + total);
            helper.assertTrue(regionalCounts.size() > 4, "Giant fungi must not use a fixed regional quota");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void netherWartTilesContainOneScatteredPatchAndNoWildBeets(GameTestHelper helper) {
        for (int tx = -2; tx <= 2; tx++) for (int tz = -2; tz <= 2; tz++) {
            var patches = NetherCropClusters.plan(750221, tx, tz, (x, z) -> true);
            helper.assertTrue(patches.size() == 1 && patches.get(0).species().equals("minecraft:nether_wart"),
                    "Each suitable 120-square tile needs one nether-wart patch and no wild beets");
            var patch = patches.get(0);
            helper.assertTrue(patch.x() - 2 >= tx * 120 && patch.x() + 3 < tx * 120 + 120
                            && patch.z() - 2 >= tz * 120 && patch.z() + 3 < tz * 120 + 120,
                    "The entire wart footprint must stay within its tile");
            helper.assertTrue(patches.equals(NetherCropClusters.plan(750221, tx, tz, (x, z) -> true)),
                    "Chunk generation order must not change wart reservations");
        }
        helper.assertTrue(NetherCropClusters.plan(99, 0, 0, (x, z) -> false).isEmpty(),
                "A completely solid tile must not force crops into rock");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void generatedNetherContainsCompleteMatureNetherWartPatches(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        // Plans inspect seeded base columns only: broad rock masses must still leave a
        // complete cluster in every tile, including tiles left/below the origin.
        for (int tileX = -2; tileX <= 2; tileX++) for (int tileZ = -2; tileZ <= 2; tileZ++) {
            helper.assertTrue(NetherCropClusters.plan(nether, tileX, tileZ).size() == 1,
                    "Generated terrain lost its nether-wart patch in tile " + tileX + "," + tileZ);
        }
        var patches = NetherCropClusters.plan(nether, 3, 4);
        helper.assertTrue(patches.size() == 1, "Generated terrain must offer one nether-wart patch");
        for (var patch : patches) {
            int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
            nether.getChunk(Math.floorDiv(minX, 16), Math.floorDiv(minZ, 16));
            Block crop = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(patch.species()));
            int plants = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(minX, 96, minZ), new BlockPos(minX + 15, 124, minZ + 15))) {
                BlockState state = nether.getBlockState(pos);
                if (!state.is(crop)) continue;
                plants++;
                helper.assertTrue(state.hasProperty(BlockStateProperties.AGE_3) && state.getValue(BlockStateProperties.AGE_3) == 3,
                        "Naturally generated soul crops must be mature at " + pos);
                helper.assertTrue(state.canSurvive(nether, pos), "Natural soul crop has invalid soil/biome at " + pos);
            }
            helper.assertTrue(plants == 5,
                    "Expected five actual generated " + patch.species() + " plants in chunk " + minX / 16 + "," + minZ / 16 + "; found " + plants);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void selectedGiantFungusChunksPlaceCompleteOriginalCanopies(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        for (String biome : new String[]{"crimson_forest", "warped_forest"}) {
            int chunkX = 25, chunkZ = 25;
            while (!NetherVegetation.giantFungusChunk(nether.getSeed(), chunkX, chunkZ, biome)) chunkX++;
            int floorY = biome.equals("crimson_forest") ? 40 : 72;
            Block nylium = biome.equals("crimson_forest") ? Blocks.CRIMSON_NYLIUM : Blocks.WARPED_NYLIUM;
            Block stem = biome.equals("crimson_forest") ? Blocks.CRIMSON_STEM : Blocks.WARPED_STEM;
            Block cap = biome.equals("crimson_forest") ? Blocks.NETHER_WART_BLOCK : Blocks.WARPED_WART_BLOCK;
            BlockPos corner = new BlockPos(chunkX * 16, floorY, chunkZ * 16);
            BlockPos min = corner.offset(-8, -1, -8), max = corner.offset(23, 19, 23);
            for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) nether.getChunk(x, z);
            int filled = nether.getServer().getCommands().performPrefixedCommand(nether.getServer().createCommandSourceStack()
                    .withLevel(nether).withSuppressedOutput(), "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ()
                            + " " + max.getX() + " " + max.getY() + " " + max.getZ() + " aoemod:" + biome);
            helper.assertTrue(filled > 0, "Giant fungus fixture biome must be installed");
            Map<BlockPos, BlockState> original = new HashMap<>();
            try {
                for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
                    BlockPos pos = mutable.immutable();
                    original.put(pos, nether.getBlockState(pos));
                    nether.setBlock(pos, pos.getY() == floorY - 1 ? nylium.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
                // Exercise the real vegetation pass and tree templates while isolating this gallery's height range.
                WorldGenLevel gallery = (WorldGenLevel) Proxy.newProxyInstance(NetherVegetationRevisionTests.class.getClassLoader(),
                        new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                            if (method.getName().equals("getMinBuildHeight")) return floorY - 2;
                            if (method.getName().equals("getMaxBuildHeight")) return floorY + 20;
                            try {
                                return method.invoke(nether, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
                NetherVegetation.place(new FeaturePlaceContext<>(Optional.empty(), gallery, nether.getChunkSource().getGenerator(),
                        RandomSource.create(7934), corner, NoneFeatureConfiguration.INSTANCE));
                int stems = 0, caps = 0;
                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    if (nether.getBlockState(pos).is(stem)) stems++;
                    if (nether.getBlockState(pos).is(cap)) caps++;
                }
                helper.assertTrue(stems >= 2 && caps >= 12,
                        "Selected " + biome + " chunk failed to place its original giant fungus: stems=" + stems + ", cap=" + caps);
                for (BlockPos pos : BlockPos.betweenClosed(min.above(), max)) nether.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                BlockPos blockedOrigin = corner.offset(8, 0, 8);
                nether.setBlock(blockedOrigin.above(), Blocks.NETHERRACK.defaultBlockState(), 2);
                var feature = nether.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                        .get(new ResourceLocation("aoemod:giant_mushrooms/"
                                + (biome.equals("crimson_forest") ? "crimson" : "warped") + "_fungus_tree_worldgen"));
                helper.assertFalse(feature.place(nether, nether.getChunkSource().getGenerator(), RandomSource.create(98), blockedOrigin),
                        "Original giant template must not replace a solid cave support with its trunk");
                helper.assertTrue(nether.getBlockState(blockedOrigin.above()).is(Blocks.NETHERRACK)
                                && nether.isEmptyBlock(blockedOrigin),
                        "Rejected giant fungus must retain the rock and leave no partial trunk");
            } finally {
                original.forEach((pos, state) -> nether.setBlock(pos, state, 2));
            }
        }
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         String biomeName, Function<BlockPos, BlockState> terrain) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var biome = nether.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:" + biomeName)));
        Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos, terrain.apply(pos));
        return (WorldGenLevel) Proxy.newProxyInstance(NetherVegetationRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return nether;
                        case "getSeed": return 97513L;
                        case "getBiome": return biome;
                        case "getMinBuildHeight": return 0;
                        case "getMaxBuildHeight": return 128;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getBlockTicks", "getFluidTicks": return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList();
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try { return method.invoke(nether, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
