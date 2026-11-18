package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.DappledPumpkins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DappledPumpkinTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void signedTilesScatterOneTwoPumpkinPatchPer120Square(GameTestHelper helper) {
        for (int tile : new int[]{-2, -1, 0, 1, 2}) {
            int start = tile * 120;
            for (long seed : new long[]{0, 50881, -1234}) {
                var plan = DappledPumpkins.plan(seed, tile, tile, (x, z) -> true);
                helper.assertTrue(plan.size() == 1 && DappledPumpkins.PUMPKINS_PER_PATCH == 2
                                && plan.stream().allMatch(p -> p.species().equals("minecraft:pumpkin")),
                        "Every suitable 120 tile needs exactly one two-pumpkin patch");
                helper.assertTrue(plan.equals(DappledPumpkins.plan(seed, tile, tile, (x, z) -> true)),
                        "Pumpkin locations must be deterministic");
                for (var patch : plan) {
                    helper.assertTrue(patch.x() - 2 >= start && patch.x() + 3 < start + 120
                                    && patch.z() - 2 >= start && patch.z() + 3 < start + 120,
                            "Complete pumpkin patches must stay in their tile");
                    int cx = Math.floorDiv(patch.x(), 16), cz = Math.floorDiv(patch.z(), 16);
                    helper.assertTrue(DappledPumpkins.forChunk(seed, cx, cz, (x, z) -> true).contains(patch),
                            "Tile seams must preserve deterministic owner chunks");
                }
                helper.assertTrue(DappledPumpkins.plan(seed, tile, tile, (x, z) -> false).isEmpty(),
                        "Forbidden-biome tiles must not receive pumpkins");
            }
        }
        helper.assertTrue(!DappledPumpkins.plan(50881, 0, 0, (x, z) -> true).equals(
                        DappledPumpkins.plan(50882, 0, 0, (x, z) -> true)), "World seeds must change patch locations");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pumpkinFeatureRunsOnceBeforeTreesAcrossAllSnowyBiomes(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : new String[]{"dappled_forest", "snowy_forest", "cold_ocean", "mountain_lake", "mushroom_caves"}) {
            var features = registry.get(AOEMod.makeID(biome)).getGenerationSettings().features();
            java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature>> pumpkin = feature ->
                    feature.unwrapKey().map(key -> key.location().equals(AOEMod.makeID("dappled_pumpkins"))).orElse(false);
            helper.assertTrue(features.size() > 2 && features.get(2).stream().filter(pumpkin).count() == 1
                            && features.stream().flatMap(set -> set.stream()).filter(pumpkin).count() == 1,
                    "The single pumpkin feature must run before trees even when the owning chunk crosses into " + biome);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void completePumpkinPatchesRetryInsideTheirCellAndKeepOtherPlants(GameTestHelper helper) {
        for (int seam : new int[]{-120, 0, 120}) {
            int min = seam - 8;
            var fixture = new Fixture(helper, "dappled_forest");
            fixture.fillGrass(min, min);
            for (int x = min; x < min + 6; x++) for (int z = min; z < min + 6; z++) {
                fixture.blocks.put(new BlockPos(x, 101, z), Blocks.OAK_LOG.defaultBlockState());
                fixture.blocks.put(new BlockPos(x, 102, z), Blocks.OAK_LOG.defaultBlockState());
            }
            var before = Map.copyOf(fixture.blocks);
            helper.assertTrue(DappledPumpkins.placePatch(fixture.world, new CropTiles.Patch(min + 2, min + 2, "minecraft:pumpkin")) == 2,
                    "A blocked first footprint must retry all legal centres inside the same cell");
            Set<BlockPos> roots = fixture.pumpkins();
            helper.assertTrue(roots.size() == 2 && roots.stream().allMatch(pos -> pos.getX() >= min && pos.getX() < min + 8
                            && pos.getZ() >= min && pos.getZ() < min + 8), "Retry escaped its eight-block reservation");
            assertCompact(helper, roots);
            before.forEach((pos, state) -> {
                if (!roots.contains(pos) && !roots.contains(pos.below())) helper.assertTrue(fixture.state(pos).equals(state),
                        "Pumpkins replaced an unrelated plant or obstacle at " + pos);
            });
            for (BlockPos pos : roots) helper.assertTrue(fixture.state(pos.above()).isAir(),
                    "Replacing tall grass must remove its upper half");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void forbiddenBiomesAndRejectedWritesCannotLeavePartialPumpkins(GameTestHelper helper) {
        var patch = new CropTiles.Patch(2, 2, "minecraft:pumpkin");
        for (String biome : new String[]{"snowy_forest", "cold_ocean", "mushroom_caves"}) {
            var fixture = new Fixture(helper, biome);
            fixture.fillGrass(0, 0);
            var before = Map.copyOf(fixture.blocks);
            helper.assertTrue(DappledPumpkins.placePatch(fixture.world, patch) == 0 && fixture.blocks.equals(before),
                    "Pumpkins must never modify even dry grass in " + biome);
        }
        var fixture = new Fixture(helper, "dappled_forest");
        fixture.fillGrass(0, 0);
        var before = Map.copyOf(fixture.blocks);
        int[] writes = {0};
        fixture.writes = (pos, state) -> {
            if (state.is(Blocks.PUMPKIN) && ++writes[0] >= 2) {
                fixture.accept(pos, state); // A misbehaving interceptor writes before rejecting.
                return false;
            }
            return fixture.accept(pos, state);
        };
        helper.assertTrue(DappledPumpkins.placePatch(fixture.world, patch) == 0 && writes[0] >= 2,
                "A failed second write must reject a partially committed pumpkin patch");
        helper.assertTrue(fixture.blocks.equals(before), "Rejected patches must restore every original grass half");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1600)
    public static void freshDappledPumpkinsSurviveCompleteTreeAndShrubDecoration(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        List<CropTiles.Patch> plan = List.of();
        int tileX = 12803, tileZ = -12809;
        for (int candidate = 0; candidate < 32; candidate++) {
            tileX = 12803 + candidate % 8;
            tileZ = -12809 + candidate / 8;
            plan = DappledPumpkins.plan(level, tileX, tileZ);
            if (!plan.isEmpty()) break;
        }
        helper.assertTrue(plan.size() == 1, "Must locate a fresh tile with one suitable dappled owner chunk before decoration");
        for (var patch : plan) {
            int cx = Math.floorDiv(patch.x(), 16), cz = Math.floorDiv(patch.z(), 16);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk(cx + dx, cz + dz);
            int minX = Math.floorDiv(patch.x(), 8) * 8, minZ = Math.floorDiv(patch.z(), 8) * 8;
            Set<BlockPos> roots = new HashSet<>();
            for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = 1; y <= height; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Blocks.PUMPKIN)) continue;
                    helper.assertTrue(GameplayDimensions.biomeIs(level, pos, "dappled_forest")
                                    && level.getFluidState(pos).isEmpty(),
                            "Natural pumpkins must remain on dry dappled land at " + pos);
                    roots.add(pos);
                }
            }
            helper.assertTrue(roots.size() == 2, "Fresh tree/shrub decoration lost a complete pumpkin patch: " + patch + " -> " + roots);
            assertCompact(helper, roots);
            AOEMod.LOGGER.info("Fresh dappled pumpkin tile ({},{}): {} surviving roots after full decoration at {}", tileX, tileZ, roots.size(), roots);
        }
        helper.succeed();
    }

    private static void assertCompact(GameTestHelper helper, Set<BlockPos> roots) {
        helper.assertTrue(roots.stream().mapToInt(BlockPos::getX).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 2
                        && roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 2,
                "Pumpkins must remain in one compact two-by-two footprint");
    }

    private static final class Fixture {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final WorldGenLevel world;
        BiPredicate<BlockPos, BlockState> writes = this::accept;

        Fixture(GameTestHelper helper, String biomeName) {
            var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            var biome = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(biomeName)));
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
            world = (WorldGenLevel) Proxy.newProxyInstance(DappledPumpkinTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getLevel": return level;
                            case "getSeed": return 50881L;
                            case "getBiome": return biome;
                            case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 101;
                            case "getHeightmapPos": return new BlockPos(((BlockPos) args[1]).getX(), 101, ((BlockPos) args[1]).getZ());
                            case "getBlockState": return state((BlockPos) args[0]);
                            case "getBlockEntity": return null;
                            case "getFluidState": return state((BlockPos) args[0]).getFluidState();
                            case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(state((BlockPos) args[0]));
                            case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(state((BlockPos) args[0]).getFluidState());
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

        BlockState state(BlockPos pos) { return blocks.getOrDefault(pos, (pos.getY() < 101 ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState()); }
        boolean accept(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); return true; }
        Set<BlockPos> pumpkins() { return blocks.entrySet().stream().filter(e -> e.getValue().is(Blocks.PUMPKIN)).map(Map.Entry::getKey).collect(Collectors.toSet()); }

        void fillGrass(int minX, int minZ) {
            for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
                boolean tall = (x + z) % 2 == 0;
                blocks.put(new BlockPos(x, 101, z), (tall ? Blocks.TALL_GRASS : Blocks.GRASS).defaultBlockState());
                blocks.put(new BlockPos(x, 102, z), tall ? Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER) : Blocks.AIR.defaultBlockState());
            }
        }
    }
}
