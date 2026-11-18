package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.ShoreSugarCane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoreSugarCaneTests {
    private ShoreSugarCaneTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void vanillaPlacementRarityAndUniformChunkOriginsReplaceRegionalQuotas(GameTestHelper helper) {
        for (String biome : new String[]{"plains", "savanna", "dappled_forest", "cold_ocean", "mountain_lake", "desert", "mesa"}) {
            int expectedRarity = biome.equals("desert") ? 2 : biome.equals("mesa") ? 10 : 6;
            helper.assertTrue(ShoreSugarCane.vanillaRarity(biome) == expectedRarity, "Wrong vanilla cane frequency for " + biome);
            int attempts = 0;
            int[] xs = new int[16], zs = new int[16];
            for (int cx = -64; cx < 64; cx++) for (int cz = -64; cz < 64; cz++) {
                RandomSource random = ShoreSugarCane.chunkRandom(50881, cx, cz);
                int x = random.nextInt(16), z = random.nextInt(16);
                if (random.nextInt(expectedRarity) == 0) { attempts++; xs[x]++; zs[z]++; }
            }
            double expected = 16384.0 / expectedRarity;
            helper.assertTrue(Math.abs(attempts - expected) < expected * .07, "Cane must use vanilla per-chunk attempts: " + biome + "=" + attempts);
            for (int offset = 0; offset < 16; offset++) helper.assertTrue(xs[offset] > attempts / 32 && zs[offset] > attempts / 32,
                    "Uniform origins must cover every column offset, including chunk edges");
        }
        helper.assertTrue(ShoreSugarCane.vanillaRarity("snowy_forest") == 0 && ShoreSugarCane.vanillaRarity("mushroom_caves") == 0,
                "The new distribution must stay out of taiga and underground mushroom caves");
        helper.succeed();
    }


    @GameTest(template = "plant_gameplay_empty")
    public static void transformedCaneAcceptsWaterOnlyInItsBottomSection(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, "savanna", 41L, 0, 0);
        SimpleWaterloggedBlock cane = (SimpleWaterloggedBlock) Blocks.SUGAR_CANE;
        BlockPos root = fixture.root(8, 7);
        BlockState dry = Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        fixture.blocks.put(root, dry);
        fixture.blocks.put(root.above(), dry);
        helper.assertTrue(cane.canPlaceLiquid(fixture.world, root, dry, Fluids.WATER), "Flowing water must be accepted at a cane root");
        helper.assertFalse(cane.canPlaceLiquid(fixture.world, root, dry, Fluids.LAVA), "Cane must not accept lava");
        helper.assertFalse(cane.canPlaceLiquid(fixture.world, root.above(), dry, Fluids.WATER), "Upper cane sections must reject water");
        helper.assertFalse(cane.placeLiquid(fixture.world, root.above(), dry, Fluids.WATER.defaultFluidState()), "Water must not fill upper cane");
        helper.assertTrue(cane.placeLiquid(fixture.world, root, dry, Fluids.WATER.defaultFluidState()),
                "Actual transformed cane must waterlog without an interface-super linkage error");
        BlockState wet = fixture.state(root);
        helper.assertTrue(wet.getValue(BlockStateProperties.WATERLOGGED) && wet.getFluidState().is(FluidTags.WATER),
                "Successful liquid placement must retain water in the bottom section");
        helper.assertFalse(cane.placeLiquid(fixture.world, root, wet, Fluids.WATER.defaultFluidState()), "Already wet cane must reject another fill");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void dryShorePatchesHaveFiveCompactThreeBlockStalks(GameTestHelper helper) {
        for (String biome : new String[]{"plains", "savanna", "dappled_forest"}) {
            for (int sign : new int[]{0, -1}) {
                Fixture fixture = new Fixture(helper, biome, 1451L, sign, sign);
                fixture.grass();
                if (biome.equals("plains")) {
                    // Leave a complete bank footprint beside the protected
                    // tall-grass columns rather than requiring their removal.
                    for (int z = 9; z < 16; z++) {
                        fixture.blocks.put(fixture.root(8, z), Blocks.GRASS.defaultBlockState());
                        fixture.blocks.put(fixture.root(8, z).above(), Blocks.AIR.defaultBlockState());
                    }
                }
                BlockPos flower = fixture.root(8, 1), blockedHead = fixture.root(8, 3).above(2);
                fixture.blocks.put(flower, Blocks.DANDELION.defaultBlockState());
                fixture.blocks.put(flower.above(), Blocks.AIR.defaultBlockState());
                fixture.blocks.put(blockedHead, Blocks.OAK_LEAVES.defaultBlockState());
                helper.assertTrue(ShoreSugarCane.placePatchInChunk(fixture.world, sign, sign) == 5, "A valid " + biome + " bank needs one complete patch");
                Set<BlockPos> roots = roots(fixture);
                assertPatch(helper, fixture.world, roots, sign, sign);
                helper.assertTrue(fixture.state(flower).is(Blocks.DANDELION) && fixture.state(blockedHead).is(Blocks.OAK_LEAVES),
                        "Shore patches must preserve flowers and canopy obstacles");
                helper.assertTrue(ShoreSugarCane.placePatchInChunk(fixture.world, sign, sign) == 0 && roots(fixture).equals(roots),
                        "Repeating decoration must not create another sugarcane cluster");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsShoreCanePreservesCompleteTallGrassPatches(GameTestHelper helper) {
        for (long seed = 0; seed < 16; seed++) {
            Fixture fixture = new Fixture(helper, "plains", seed, 0, 0);
            Map<BlockPos, BlockState> tallGrass = new HashMap<>();
            for (int z = 1; z <= 4; z++) {
                BlockPos root = fixture.root(8, z);
                tallGrass.put(root, Blocks.TALL_GRASS.defaultBlockState());
                tallGrass.put(root.above(), Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            }
            fixture.blocks.putAll(tallGrass);
            helper.assertTrue(ShoreSugarCane.placePatchInChunk(fixture.world, 0, 0) == 5,
                    "A shore patch must still find five roots beside a complete tall-grass cluster");
            assertPatch(helper, fixture.world, roots(fixture), 0, 0);
            tallGrass.forEach((pos, state) -> helper.assertTrue(fixture.state(pos).equals(state),
                    "Late sugarcane decoration fragmented an existing Plains tall-grass patch at " + pos));
            helper.assertFalse(DocumentedFloraFeature.placePlant(fixture.world, fixture.root(8, 1),
                            "minecraft:sugar_cane", RandomSource.create(seed)),
                    "The single-column generation path must preserve complete tall-grass patches too");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void shorePatchesVaryBySeedAndRespectChunkOrderAndBiomeSeams(GameTestHelper helper) {
        Set<Set<BlockPos>> layouts = new HashSet<>();
        for (long seed = 0; seed < 32; seed++) {
            Fixture first = new Fixture(helper, "plains", seed, 0, 0);
            Fixture repeated = new Fixture(helper, "plains", seed, 0, 0);
            ShoreSugarCane.placePatchInChunk(first.world, 0, 0);
            ShoreSugarCane.placePatchInChunk(first.world, 0, -1);
            ShoreSugarCane.placePatchInChunk(repeated.world, 0, -1);
            ShoreSugarCane.placePatchInChunk(repeated.world, 0, 0);
            helper.assertTrue(first.blocks.equals(repeated.blocks), "Neighbor chunk order changed seeded dry-shore patches");
            layouts.add(roots(first));
        }
        helper.assertTrue(layouts.size() > 16, "Sugarcane positions must vary with the world seed");
        Fixture boundary = new Fixture(helper, "dappled_forest", 251L, 0, 0);
        boundary.biomeAt = pos -> pos.getZ() < 4 ? "dappled_forest" : "snowy_forest";
        helper.assertTrue(ShoreSugarCane.placePatchInChunk(boundary.world, 0, 0) == 0 && boundary.blocks.isEmpty(),
                "Four dappled roots must not borrow a fifth from snowy forest");
        boundary.biomeAt = pos -> pos.getZ() < 9 ? "dappled_forest" : "cold_ocean";
        helper.assertTrue(ShoreSugarCane.placePatchInChunk(boundary.world, 0, 0) == 5, "A complete dappled shore footprint should generate");
        helper.assertTrue(roots(boundary).stream().allMatch(pos -> pos.getZ() < 9), "Cane crossed a cold-ocean biome boundary");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rejectedShorePatchesRestoreAllGrassAndLeaveNoPartialStalks(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, "savanna", 972L, 0, 0);
        fixture.grass();
        Map<BlockPos, BlockState> before = snapshot(fixture);
        int[] written = {0};
        fixture.writes = (pos, state) -> {
            if (state.is(Blocks.SUGAR_CANE) && pos.getY() == fixture.rootY + 2) return false;
            if (state.is(Blocks.SUGAR_CANE)) written[0]++;
            return fixture.accept(pos, state);
        };
        helper.assertTrue(ShoreSugarCane.placePatchInChunk(fixture.world, 0, 0) == 0 && written[0] > 0,
                "The write-veto fixture must exercise rollback after real lower sections were accepted");
        before.forEach((pos, state) -> helper.assertTrue(fixture.state(pos).equals(state), "Rejected cane changed original grass or water at " + pos));
        Fixture narrow = new Fixture(helper, "plains", 972L, 0, 0);
        for (int z = 4; z < 16; z++) narrow.blocks.put(narrow.root(8, z), Blocks.STONE.defaultBlockState());
        Map<BlockPos, BlockState> narrowBefore = snapshot(narrow);
        helper.assertTrue(ShoreSugarCane.placePatchInChunk(narrow.world, 0, 0) == 0, "A four-root bank must not leave an incomplete patch");
        narrowBefore.forEach((pos, state) -> helper.assertTrue(narrow.state(pos).equals(state), "Incomplete patch changed a bank cell"));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalDryShoreRulePreservesManualWaterloggingAndTropicalWetCane(GameTestHelper helper) {
        for (String biome : new String[]{"plains", "savanna", "dappled_forest", "cold_ocean", "mountain_lake"}) {
            Fixture fixture = new Fixture(helper, biome, 41L, 0, 0);
            BlockPos wet = fixture.root(7, 7).below();
            helper.assertFalse(DocumentedFloraFeature.placePlant(fixture.world, wet, "minecraft:sugar_cane", RandomSource.create(1)),
                    biome + " must not generate cane in water");
            helper.assertTrue(fixture.blocks.isEmpty() && fixture.state(wet).is(Blocks.WATER), "Rejected natural cane consumed water");
            BlockState manual = Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
            helper.assertTrue(manual.canSurvive(fixture.world, wet)
                            && BiomePlacementRestrictions.isAllowed(fixture.world, wet, manual),
                    "Players must retain native waterlogged cane placement in " + biome);
            helper.assertTrue(DocumentedFloraFeature.placePlant(fixture.world, fixture.root(8, 7), "minecraft:sugar_cane", RandomSource.create(1)),
                    "A natural dry-bank cane stalk should place in " + biome);
            for (int y = 0; y < 3; y++) helper.assertTrue(fixture.state(fixture.root(8, 7).above(y)).is(Blocks.SUGAR_CANE)
                            && fixture.state(fixture.root(8, 7).above(y)).getFluidState().isEmpty(),
                    "Dry natural stalks must have exactly three dry sections");
            helper.assertTrue(fixture.state(fixture.root(8, 7).above(3)).isAir(), "Natural stalk exceeded three blocks");
        }
        Fixture rainforest = new Fixture(helper, "rainforest", 41L, 0, 0);
        BlockPos wet = rainforest.root(7, 7).below();
        helper.assertTrue(DocumentedFloraFeature.placePlant(rainforest.world, wet, "minecraft:sugar_cane", RandomSource.create(1)),
                "Rainforest cane must retain water-rooted natural placement");
        helper.assertTrue(rainforest.state(wet).getFluidState().is(FluidTags.WATER)
                        && rainforest.state(wet.above()).getFluidState().isEmpty()
                        && rainforest.state(wet.above(2)).is(Blocks.SUGAR_CANE),
                "Rainforest cane needs one wet root and two dry upper sections");
        helper.assertFalse(DocumentedFloraFeature.placePlant(rainforest.world, rainforest.root(8, 7), "minecraft:sugar_cane", RandomSource.create(1)),
                "The dry-shore change must not move rainforest cane onto land");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void vanillaDistributedConfiguredPatchesRetainFiveDryThreeHighStalks(GameTestHelper helper) {
        for (String biome : new String[]{"plains", "savanna", "dappled_forest", "cold_ocean", "mountain_lake"}) {
            int completed = 0;
            for (long seed = 0; seed < 256; seed++) {
                Fixture fixture = new Fixture(helper, biome, seed, -2, 1);
                int count = ShoreSugarCane.placeChunk(fixture.world, -2, 1);
                Set<BlockPos> roots = roots(fixture);
                helper.assertTrue(count == 0 || count == 5, "Only complete configured patches may commit");
                helper.assertTrue(roots.size() == count, "Reported patch size differs from committed roots");
                if (count == 0) continue;
                completed++;
                helper.assertTrue(roots.stream().mapToInt(BlockPos::getX).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 6
                                && roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 6,
                        "The existing compact configured feature must be preserved");
                for (BlockPos root : roots) for (int y = 0; y < 3; y++) helper.assertTrue(fixture.state(root.above(y)).is(Blocks.SUGAR_CANE)
                                && fixture.state(root.above(y)).getFluidState().isEmpty(),
                        "Every configured stalk must retain three dry sections in " + biome);
                helper.assertTrue(ShoreSugarCane.placeChunk(fixture.world, -2, 1) == 0,
                        "Repeating chunk decoration must not add a second configured patch");
            }
            helper.assertTrue(completed > 0, "Vanilla distributed attempts must reach eligible banks in " + biome);
        }
        helper.succeed();
    }


    private static void assertPatch(GameTestHelper helper, WorldGenLevel level, Set<BlockPos> roots, int cx, int cz) {
        helper.assertTrue(roots.size() == 5, "A successful sugarcane patch must contain exactly five roots: " + roots);
        helper.assertTrue(roots.stream().mapToInt(BlockPos::getX).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 6
                        && roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 6,
                "Sugarcane roots escaped a compact six-square patch");
        for (BlockPos root : roots) {
            helper.assertTrue(Math.floorDiv(root.getX(), 16) == cx && Math.floorDiv(root.getZ(), 16) == cz, "Cane escaped its owning chunk");
            helper.assertTrue(ShoreSugarCane.isHabitat(level, root), "Cane escaped its allowed surface biome");
            boolean water = false;
            for (Direction direction : Direction.Plane.HORIZONTAL) water |= level.getFluidState(root.below().relative(direction)).is(FluidTags.WATER);
            helper.assertTrue(water && level.getFluidState(root.below()).isEmpty(), "Cane must stand on dry soil beside water");
            for (int y = 0; y < 3; y++) {
                var state = level.getBlockState(root.above(y));
                helper.assertTrue(state.is(Blocks.SUGAR_CANE) && state.getFluidState().isEmpty() && state.canSurvive(level, root.above(y)),
                        "Each dry cane stalk needs three surviving dry sections");
            }
            helper.assertFalse(level.getBlockState(root.above(3)).is(Blocks.SUGAR_CANE), "Cane exceeds three blocks");
        }
    }

    private static Set<BlockPos> roots(Fixture fixture) {
        Set<BlockPos> result = new HashSet<>();
        fixture.blocks.forEach((pos, state) -> { if (state.is(Blocks.SUGAR_CANE) && !fixture.state(pos.below()).is(Blocks.SUGAR_CANE)) result.add(pos); });
        return Set.copyOf(result);
    }

    private static Map<BlockPos, BlockState> snapshot(Fixture fixture) {
        Map<BlockPos, BlockState> result = new HashMap<>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -1; y < 3; y++) {
            BlockPos pos = fixture.root(x, z).above(y); result.put(pos, fixture.state(pos));
        }
        return result;
    }

    private static ResourceKey<Level> dimension(String biome) {
        return switch (biome) {
            case "plains" -> GameplayDimensions.PLAINS;
            case "savanna" -> GameplayDimensions.SAVANNA;
            case "dappled_forest", "snowy_forest", "cold_ocean", "mountain_lake" -> GameplayDimensions.SNOWY;
            case "rainforest" -> GameplayDimensions.TROPICAL;
            default -> GameplayDimensions.DESERT;
        };
    }

    private static final class Fixture {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final WorldGenLevel world;
        final int minX, minZ, rootY;
        final BlockState soil;
        Function<BlockPos, String> biomeAt;
        BiPredicate<BlockPos, BlockState> writes = this::accept;

        Fixture(GameTestHelper helper, String biome, long seed, int cx, int cz) {
            var level = helper.getLevel().getServer().getLevel(dimension(biome));
            minX = cx * 16; minZ = cz * 16; rootY = level.getChunkSource().getGenerator().getSeaLevel();
            soil = (biome.equals("desert") || biome.equals("mesa") ? Blocks.SAND : biome.equals("rainforest") ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK).defaultBlockState();
            biomeAt = pos -> biome;
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            Map<Long, ProtoChunk> chunks = new HashMap<>();
            java.util.function.BiFunction<Integer, Integer, ProtoChunk> chunkAt = (x, z) -> chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored ->
                    new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, biomes, null) {
                        @Override public BlockState getBlockState(BlockPos pos) { return state(pos); }
                        @Override public BlockState setBlockState(BlockPos pos, BlockState next, boolean moving) {
                            BlockState before = state(pos); accept(pos, next); return before;
                        }
                    });
            var light = new LevelLightEngine(new LightChunkGetter() {
                @Override public LightChunk getChunkForLighting(int x, int z) { return chunkAt.apply(x, z); }
                @Override public BlockGetter getLevel() { return level; }
            }, false, false);
            world = (WorldGenLevel) Proxy.newProxyInstance(ShoreSugarCaneTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getLevel": return level;
                            case "getSeed": return seed;
                            case "getBiome": return biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", biomeAt.apply((BlockPos) args[0]))));
                            case "getHeight": return args == null || args.length == 0 ? level.getHeight() : rootY;
                            case "getBlockState": return state((BlockPos) args[0]);
                            case "getBlockEntity": return null;
                            case "getFluidState": return state((BlockPos) args[0]).getFluidState();
                            case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(state((BlockPos) args[0]));
                            case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(state((BlockPos) args[0]).getFluidState());
                            case "isEmptyBlock": return state((BlockPos) args[0]).isAir();
                            case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                            case "canSeeSky", "ensureCanWrite": return true;
                            case "scheduleTick": return null;
                            case "setBlock": return writes.test(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            case "getLightEngine": return light;
                            case "getChunk": {
                                if (args[0] instanceof BlockPos pos) return chunkAt.apply(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                                return chunkAt.apply((Integer) args[0], (Integer) args[1]);
                            }
                            default:
                                try { return method.invoke(level, args); }
                                catch (InvocationTargetException error) { throw error.getCause(); }
                        }
                    });
        }

        BlockPos root(int x, int z) { return new BlockPos(minX + x, rootY, minZ + z); }
        BlockState state(BlockPos pos) {
            return blocks.getOrDefault(pos, pos.getY() < rootY - 1 ? soil : pos.getY() == rootY - 1
                    ? pos.getX() < minX + 8 ? Blocks.WATER.defaultBlockState() : soil : Blocks.AIR.defaultBlockState());
        }
        boolean accept(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); return true; }
        void grass() {
            for (int x = 8; x < 16; x++) for (int z = 0; z < 16; z++) {
                boolean tall = z % 2 == 0;
                blocks.put(root(x, z), (tall ? Blocks.TALL_GRASS : Blocks.GRASS).defaultBlockState());
                if (tall) blocks.put(root(x, z).above(), Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            }
        }
    }
}
