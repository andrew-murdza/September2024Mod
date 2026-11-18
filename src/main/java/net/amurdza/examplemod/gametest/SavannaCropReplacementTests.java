package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaFlowerBands;
import net.amurdza.examplemod.worldgen.SavannaVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCropReplacementTests {
    private static final long SEED = 50881;
    private static final int ROOT_Y = 71;

    private SavannaCropReplacementTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void savannaContainsOnlyTheNineCropAndTwoMushroomPatches(GameTestHelper helper) {
        for (long seed : new long[]{0, SEED, -49835}) for (int tx = -1; tx <= 1; tx++) {
            var patches = net.amurdza.examplemod.worldgen.CropTiles.plan(seed, tx, -tx,
                    net.amurdza.examplemod.worldgen.CropTiles.SAVANNA_SPECIES, (x, z) -> true);
            helper.assertTrue(patches.size() == 11 && patches.stream().noneMatch(p -> p.species().contains("corn")
                    || p.species().contains("onion")), "The crop plan cannot generate wild corn or onions");
        }
        Fixture fixture = new Fixture(helper, chunk(false));
        fixture.grass(true);
        var before = fixture.snapshot();
        helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world,
                fixture.patch(10, 10, "hearthandharvest:corn_stalk"), RandomSource.create(SEED)) == 0,
                "Wild corn placement must fail even on open grass");
        helper.assertTrue(fixture.snapshot().equals(before), "Rejected corn may not disturb the soil or grass");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void everyPatchSpeciesReplacesGrassWithoutOrphanHalves(GameTestHelper helper) {
        int shortReplaced = 0, tallReplaced = 0;
        for (String species : SavannaVegetation.PATCH_SPECIES) {
            Fixture fixture = new Fixture(helper, chunk(false));
            fixture.grass(true);
            var before = fixture.snapshot();
            helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, species), RandomSource.create(17))
                            == net.amurdza.examplemod.worldgen.CropTiles.plantsPerPatch(species),
                    "A grass-covered chunk must support its full " + species + " root quota");
            var roots = assertPatch(helper, fixture, species);
            for (BlockPos root : roots) {
                if (before.get(root).is(Blocks.GRASS)) shortReplaced++;
                if (before.get(root).is(Blocks.TALL_GRASS)) tallReplaced++;
            }
            assertUnchangedExceptRoots(helper, fixture, before, roots);
        }
        helper.assertTrue(shortReplaced > 0 && tallReplaced > 0, "The species audit must exercise both short and tall grass replacement");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fiveCaneAndFiveTrunksDoNotConsumePatchSuccesses(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, chunk(false));
        fixture.grass(true);
        for (int i = 0; i < 10; i++) {
            BlockPos root = fixture.pos(9 + i % 5, 9 + i / 5);
            BlockState protectedPlant = (i < 5 ? Blocks.SUGAR_CANE : Blocks.ACACIA_LOG).defaultBlockState();
            for (int height = 0; height < 3; height++) fixture.blocks.put(root.above(height), protectedPlant);
        }
        var before = fixture.snapshot();
        helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, "minecraft:pumpkin"), RandomSource.create(9)) == 2,
                "Cane and trunks must be skipped until two pumpkin replacements actually succeed");
        assertUnchangedExceptRoots(helper, fixture, before, assertPatch(helper, fixture, "minecraft:pumpkin"));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void lateCornerWithOnlyTheSpeciesQuotaStillCompletes(GameTestHelper helper) {
        for (String species : new String[]{"minecraft:pumpkin", "farmersdelight:wild_carrots"}) {
            Fixture fixture = new Fixture(helper, chunk(false));
            fixture.blocked();
            Set<BlockPos> available = species.equals("minecraft:pumpkin")
                    ? Set.of(fixture.pos(7, 6), fixture.pos(7, 7))
                    : Set.of(fixture.pos(5, 7), fixture.pos(6, 6), fixture.pos(7, 6),
                            fixture.pos(6, 7), fixture.pos(7, 7));
            int index = 0;
            for (BlockPos root : available) fixture.grassAt(root, index++ % 2 == 0);
            var before = fixture.snapshot();
            // The requested corner is blocked. Only the far end of this same
            // eight-block cell can fit the two-pumpkin or five-carrot quota.
            helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(2, 2, species), RandomSource.create(23)) == available.size(),
                    "Retries must exhaust the reserved cell instead of stopping after unsuccessful candidates");
            helper.assertTrue(assertPatch(helper, fixture, species).equals(available),
                    "All viable cells must be used, without leaving crops in failed footprints");
            assertUnchangedExceptRoots(helper, fixture, before, available);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rejectedAndNoOpWritesRestoreGrassAndDoNotCount(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, chunk(false));
        fixture.grass(true);
        var before = fixture.snapshot();
        Set<BlockPos> rejected = new HashSet<>();
        int[] attempts = {0};
        fixture.writes = (pos, state) -> {
            if (state.is(Blocks.PUMPKIN) && attempts[0]++ < 3) {
                rejected.add(pos.immutable());
                if (attempts[0] == 3) return true; // Accepted return value, but no block actually changed.
                fixture.accept(pos, state); // An interceptor mutates the destination before rejecting it.
                fixture.accept(pos.above(), Blocks.AIR.defaultBlockState());
                return false;
            }
            return fixture.accept(pos, state);
        };
        helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, "minecraft:pumpkin"), RandomSource.create(31)) == 2,
                "Only verified crop writes count toward two pumpkin successes");
        var roots = assertPatch(helper, fixture, "minecraft:pumpkin");
        helper.assertTrue(rejected.size() == 3 && roots.stream().noneMatch(rejected::contains),
                "The first three failed writes must be skipped while the remaining footprint completes");
        assertUnchangedExceptRoots(helper, fixture, before, roots);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void doublePlantUpperWriteFailuresRestoreTheOriginalRoot(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, chunk(false));
        // Tall grass is a real allowed double plant; none exists before this control.
        // The five patch species themselves are single-height crops and mushrooms.
        fixture.grass(false);
        var before = fixture.snapshot();
        Set<BlockPos> rejected = new HashSet<>();
        int[] attempts = {0};
        fixture.writes = (pos, state) -> {
            if (state.is(Blocks.TALL_GRASS) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                    && attempts[0]++ < 3) {
                rejected.add(pos.below().immutable());
                fixture.accept(pos, state);
                return false;
            }
            return fixture.accept(pos, state);
        };
        helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, "minecraft:tall_grass"), RandomSource.create(43)) == 5,
                "Rejected upper halves must roll back and leave room for five complete double plants");
        var roots = assertPatch(helper, fixture, "minecraft:tall_grass");
        helper.assertTrue(rejected.size() == 3 && roots.stream().noneMatch(rejected::contains),
                "Rejected double plants must not count or leave lower halves behind");
        assertUnchangedExceptRoots(helper, fixture, before, roots);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void impossiblePatchRestoresAllGrassSoilAndProtectedBlocks(GameTestHelper helper) {
        for (String species : new String[]{"minecraft:pumpkin", "farmersdelight:wild_carrots"}) {
            Fixture fixture = new Fixture(helper, chunk(false));
            fixture.blocked();
            int available = species.equals("minecraft:pumpkin") ? 1 : 4;
            for (int i = 0; i < available; i++) fixture.grassAt(fixture.pos(10 + i % 2, 10 + i / 2), i % 2 == 0);
            fixture.blocks.put(fixture.pos(9, 8), Blocks.SUGAR_CANE.defaultBlockState());
            fixture.blocks.put(fixture.pos(8, 9), Blocks.ACACIA_LOG.defaultBlockState());
            fixture.blocks.put(fixture.pos(9, 9), Blocks.RED_TULIP.defaultBlockState());
            var before = fixture.snapshot();
            Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
            int[] attemptedPlants = {0};
            fixture.writes = (pos, state) -> {
                if (state.is(target)) attemptedPlants[0]++;
                return fixture.accept(pos, state);
            };
            helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, species), RandomSource.create(17)) == 0,
                    "A patch with one fewer viable cell than its species quota must fail: " + species);
            helper.assertTrue(attemptedPlants[0] >= available, "This fixture must exercise rollback of actual successful replacements");
            assertUnchangedExceptRoots(helper, fixture, before, Set.of());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void flowerBandsKeepTheirFlowersAndGrass(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, chunk(true));
        fixture.grass(true);
        fixture.blocks.put(fixture.pos(7, 7), Blocks.RED_TULIP.defaultBlockState());
        fixture.blocks.put(fixture.pos(7, 7).above(), Blocks.AIR.defaultBlockState());
        var before = fixture.snapshot();
        helper.assertTrue(SavannaVegetation.placeCropPatch(fixture.world, fixture.patch(10, 10, "minecraft:pumpkin"), RandomSource.create(17)) == 0,
                "Grass replacement must not bypass the flower-band exclusion");
        assertUnchangedExceptRoots(helper, fixture, before, Set.of());
        helper.succeed();
    }

    private static Set<BlockPos> assertPatch(GameTestHelper helper, Fixture fixture, String species) {
        Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
        helper.assertTrue(target != null && target != Blocks.AIR, "Missing registered patch species " + species);
        Set<BlockPos> roots = new HashSet<>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            BlockPos root = fixture.pos(x, z);
            BlockState state = fixture.state(root);
            if (!state.is(target)) continue;
            roots.add(root);
            helper.assertTrue(state.canSurvive(fixture.world, root), "Replacement must pass real plant survival for " + species);
            BlockState above = fixture.state(root.above());
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                helper.assertTrue(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                                && above.is(target) && above.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                                && above.canSurvive(fixture.world, root.above()),
                        "A double replacement needs both matching, surviving halves");
            } else helper.assertTrue(above.isAir(), "Single-height crops must remove the old tall-grass upper half");
        }
        helper.assertTrue(roots.size() == net.amurdza.examplemod.worldgen.CropTiles.plantsPerPatch(species),
                "Expected the full " + species + " root quota, found " + roots.size());
        return roots;
    }

    private static void assertUnchangedExceptRoots(GameTestHelper helper, Fixture fixture,
                                                   Map<BlockPos, BlockState> before, Set<BlockPos> roots) {
        Set<BlockPos> changed = new HashSet<>(roots);
        roots.forEach(root -> changed.add(root.above()));
        before.forEach((pos, state) -> {
            if (!changed.contains(pos)) helper.assertTrue(fixture.state(pos).equals(state), "Unrelated plant, grass half, or soil changed at " + pos);
        });
        fixture.blocks.keySet().forEach(pos -> helper.assertTrue(pos.getX() >= fixture.minX && pos.getX() < fixture.minX + 16
                        && pos.getZ() >= fixture.minZ && pos.getZ() < fixture.minZ + 16,
                "A retry escaped its reserved chunk at " + pos));
    }

    private static BlockPos chunk(boolean insideFlowers) {
        var bands = new SavannaFlowerBands(SEED);
        for (int cx = -16; cx <= 16; cx++) for (int cz = -16; cz <= 16; cz++) {
            boolean matches = true;
            for (int x = cx * 16; x < cx * 16 + 16 && matches; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
                if ((bands.colour(x, z) >= 0) != insideFlowers) { matches = false; break; }
            }
            if (matches) return new BlockPos(cx * 16, ROOT_Y, cz * 16);
        }
        throw new IllegalStateException("Missing test chunk insideFlowers=" + insideFlowers);
    }

    /** Real survival/transaction code operates on detached chunks; no test writes reach the server world. */
    private static final class Fixture {
        final int minX, minZ;
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final WorldGenLevel world;
        BiPredicate<BlockPos, BlockState> writes = this::accept;

        Fixture(GameTestHelper helper, BlockPos origin) {
            minX = origin.getX(); minZ = origin.getZ();
            var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            var biome = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
            Map<Long, ProtoChunk> chunks = new HashMap<>();
            java.util.function.BiFunction<Integer, Integer, ProtoChunk> chunkAt = (x, z) -> chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored ->
                    new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, biomes, null) {
                        @Override public BlockState getBlockState(BlockPos pos) { return state(pos); }
                        @Override public BlockState setBlockState(BlockPos pos, BlockState next, boolean moving) {
                            BlockState previous = state(pos); accept(pos, next); return previous;
                        }
                    });
            var light = new LevelLightEngine(new LightChunkGetter() {
                @Override public LightChunk getChunkForLighting(int x, int z) { return chunkAt.apply(x, z); }
                @Override public BlockGetter getLevel() { return level; }
            }, false, false);
            world = (WorldGenLevel) Proxy.newProxyInstance(SavannaCropReplacementTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getLevel": return level;
                            case "getSeed": return SEED;
                            case "getBiome": return biome;
                            case "getHeight": return args == null || args.length == 0 ? level.getHeight() : ROOT_Y;
                            case "getBlockState": return state((BlockPos) args[0]);
                            case "getBlockEntity": return null;
                            case "getFluidState": return state((BlockPos) args[0]).getFluidState();
                            case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(state((BlockPos) args[0]));
                            case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(state((BlockPos) args[0]).getFluidState());
                            case "isEmptyBlock": return state((BlockPos) args[0]).isAir();
                            case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                            case "canSeeSky", "ensureCanWrite": return true;
                            case "setBlock": return writes.test(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            case "getLightEngine": return light;
                            case "getChunk": {
                                if (args[0] instanceof BlockPos pos) return chunkAt.apply(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                                return chunkAt.apply((Integer) args[0], (Integer) args[1]);
                            }
                            default:
                                try { return method.invoke(level, args); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                        }
                    });
        }

        BlockPos pos(int x, int z) { return new BlockPos(minX + x, ROOT_Y, minZ + z); }
        SavannaVegetation.CropPatch patch(int x, int z, String species) { return new SavannaVegetation.CropPatch(minX + x, minZ + z, species); }
        BlockState state(BlockPos pos) { return blocks.getOrDefault(pos, pos.getY() < ROOT_Y ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState()); }
        boolean accept(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); return true; }
        void grass(boolean mixed) { for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) grassAt(pos(x, z), mixed && (x + z) % 2 == 0); }
        void grassAt(BlockPos root, boolean tall) {
            blocks.put(root, (tall ? Blocks.TALL_GRASS : Blocks.GRASS).defaultBlockState());
            blocks.put(root.above(), tall ? Blocks.TALL_GRASS.defaultBlockState()
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER) : Blocks.AIR.defaultBlockState());
        }
        void blocked() { for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) blocks.put(pos(x, z), Blocks.STONE.defaultBlockState()); }
        Map<BlockPos, BlockState> snapshot() {
            Map<BlockPos, BlockState> result = new HashMap<>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = ROOT_Y - 1; y <= ROOT_Y + 2; y++) {
                BlockPos pos = new BlockPos(minX + x, y, minZ + z); result.put(pos, state(pos));
            }
            return result;
        }
    }
}
