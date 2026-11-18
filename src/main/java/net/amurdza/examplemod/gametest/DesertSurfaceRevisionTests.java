package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.surface_rule.DimensionSurfaceRule;
import net.amurdza.examplemod.worldgen.DeepDarkEcology;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.lang.reflect.Proxy;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertSurfaceRevisionTests {
    private DesertSurfaceRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void actualSurfaceBuilderUsesTopHeightAndContinuousDeepDarkFloor(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int firstRedSand = (int) Math.ceil(DesertHeight.RED_SAND_START_HEIGHT);
        int firstTerracotta = DesertHeight.FIRST_TERRACOTTA_Y;
        // Exercise both sides of the current material boundaries. The old
        // fixed Y80/Y87 fixture predates the lowered desert contour.
        int[] tops = {DesertHeight.SEA_LEVEL + 2, firstRedSand - 2, firstRedSand - 1,
                firstRedSand, firstRedSand + 1, firstTerracotta - 1, firstTerracotta, firstTerracotta + 1};
        for (long seed : new long[]{0, 12345, 987654321}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            for (ChunkPos chunkPos : new ChunkPos[]{new ChunkPos(0, 0), new ChunkPos(-171, 239)}) {
                var actual = new ProtoChunk(chunkPos, UpgradeData.EMPTY, level, biomes, null);
                var vanillaBands = new ProtoChunk(chunkPos, UpgradeData.EMPTY, level, biomes, null);
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int top = tops[dx % tops.length];
                    for (int y = -32; y <= top; y++) {
                        if (y > -22 && y < -5) continue;
                        BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + dx, y, chunkPos.getMinBlockZ() + dz);
                        actual.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                        vanillaBands.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                    }
                    // An artificial high waterline must not turn red sand back into sand.
                    if (dz % 2 == 0) for (int y = top + 1; y <= top + 3; y++) {
                        actual.setBlockState(new BlockPos(chunkPos.getMinBlockX() + dx, y, chunkPos.getMinBlockZ() + dz),
                                Blocks.WATER.defaultBlockState(), false);
                    }
                }
                var maps = Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
                Heightmap.primeHeightmaps(actual, maps);
                Heightmap.primeHeightmaps(vanillaBands, maps);
                var worldContext = new WorldGenerationContext(generator, level);
                random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                        worldContext, actual, null, new DimensionSurfaceRule("desert"));
                random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                        worldContext, vanillaBands, null, new SurfaceRules.RuleSource() {
                            @Override public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
                                var nativeBands = SurfaceRules.bandlands().apply(context);
                                return (x, y, z) -> nativeBands.tryApply(0, y - 2, 0);
                            }
                            @Override public net.minecraft.util.KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
                                return SurfaceRules.bandlands().codec();
                            }
                        });
                var deepDark = biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID("deep_dark")));
                var desert = biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID("desert")));
                // Reuse the real surface-builder result. Mirror its edge columns
                // so this isolated chunk represents a continuous cave, not air
                // outside the fixture that would expose its buried backing.
                java.util.function.Function<BlockPos, net.minecraft.world.level.block.state.BlockState> stateAt = pos ->
                        actual.getBlockState(new BlockPos(chunkPos.getMinBlockX() + Math.floorMod(pos.getX(), 16),
                                pos.getY(), chunkPos.getMinBlockZ() + Math.floorMod(pos.getZ(), 16)));
                WorldGenLevel coatingWorld = (WorldGenLevel) Proxy.newProxyInstance(DesertSurfaceRevisionTests.class.getClassLoader(),
                        new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "getLevel" -> level;
                                case "getMinBuildHeight" -> level.getMinBuildHeight();
                                case "getMaxBuildHeight" -> level.getMaxBuildHeight();
                                case "getBiome" -> ((BlockPos) args[0]).getY() < 0 ? deepDark : desert;
                                case "getBlockState" -> stateAt.apply((BlockPos) args[0]);
                                case "isEmptyBlock" -> stateAt.apply((BlockPos) args[0]).isAir();
                                case "setBlock" -> {
                                    actual.setBlockState((BlockPos) args[0], (net.minecraft.world.level.block.state.BlockState) args[1], false);
                                    yield true;
                                }
                                default -> throw new UnsupportedOperationException(method.getName());
                            };
                        });
                DeepDarkEcology.prepareSurfaces(coatingWorld, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int top = tops[dx % tops.length];
                    BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + dx, top, chunkPos.getMinBlockZ() + dz);
                    int materialTop = top;
                    if (materialTop < firstTerracotta) {
                        var sand = materialTop < firstRedSand ? Blocks.SAND : Blocks.RED_SAND;
                        var sandstone = materialTop < firstRedSand ? Blocks.SANDSTONE : Blocks.RED_SANDSTONE;
                        helper.assertTrue(actual.getBlockState(pos).is(sand) && actual.getBlockState(pos.below()).is(sand),
                                "Top Y" + top + " must select the same sand throughout the column at " + pos + " seed " + seed);
                        helper.assertTrue(actual.getBlockState(pos.below(2)).is(sandstone),
                                "The column must retain its matching sandstone below the sand");
                    } else {
                        for (int depth = 0; depth < 16; depth++) {
                            var block = actual.getBlockState(pos.below(depth));
                            helper.assertTrue(block.is(BlockTags.TERRACOTTA)
                                            && block.equals(vanillaBands.getBlockState(pos.below(depth))),
                                    "High columns must carry XZ-independent native terracotta bands, shifted from Y64 to Y66, down all 16 surface layers");
                        }
                    }
                    BlockPos floor = new BlockPos(pos.getX(), -22, pos.getZ());
                    helper.assertTrue(actual.getBlockState(floor).is(Blocks.SCULK)
                                    && actual.getBlockState(floor.below()).is(Blocks.END_STONE)
                                    && actual.getBlockState(floor.below(2)).is(Blocks.END_STONE)
                                    && actual.getBlockState(floor.below(3)).is(Blocks.DEEPSLATE),
                            "Every deep dark floor must have exactly one sculk and two end stone layers");
                    BlockPos ceiling = new BlockPos(pos.getX(), -5, pos.getZ());
                    helper.assertTrue(actual.getBlockState(ceiling).is(Blocks.DEEPSLATE)
                                    && actual.getBlockState(ceiling.above()).is(Blocks.DEEPSLATE)
                                    && actual.getBlockState(ceiling.above(2)).is(Blocks.DEEPSLATE)
                                    && actual.getBlockState(ceiling.above(3)).is(Blocks.DEEPSLATE),
                            "Cave ceilings must be plain deepslate without sculk or end-stone skins");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void desertMouthsKeepColumnMaterialsAndRemoveLooseSandRoofs(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), 12345);
        boolean[] checked = new boolean[3];
        for (int z = -1536; z <= 1536; z += 16) {
            var chunkPos = new ChunkPos(0, Math.floorDiv(z, 16));
            int x = chunkPos.getMinBlockX() + 8, blockZ = z + 8;
            int naturalTop = (int) Math.floor(random.router().initialDensityWithoutJaggedness()
                    .compute(new DensityFunction.SinglePointContext(x, 0, blockZ)) + .5);
            int material = naturalTop < DesertHeight.RED_SAND_START_HEIGHT ? 0
                    : naturalTop < DesertHeight.FIRST_TERRACOTTA_Y ? 1 : 2;
            if (checked[material] || naturalTop < DesertHeight.SEA_LEVEL + 2) continue;
            for (int roofThickness : new int[]{0, 1, 2}) {
                var chunk = new ProtoChunk(chunkPos, UpgradeData.EMPTY, level, biomes, null);
                int top = DesertHeight.SEA_LEVEL + 1;
                // The fixture cuts a deep mouth into the original contour. The
                // two roof fixtures leave only one/two surface blocks over air.
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    for (int y = -32; y <= top; y++) chunk.setBlockState(
                            new BlockPos(dx, y, z + dz), Blocks.STONE.defaultBlockState(), false);
                    for (int dy = 0; dy < roofThickness; dy++) chunk.setBlockState(
                            new BlockPos(dx, naturalTop - dy, z + dz), Blocks.STONE.defaultBlockState(), false);
                    if (roofThickness > 0) {
                        // Make a clear gap even for shallow sand bands.
                        for (int y = top; y >= top - 4; y--) chunk.setBlockState(
                                new BlockPos(dx, y, z + dz), Blocks.AIR.defaultBlockState(), false);
                    }
                }
                Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
                random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                        new WorldGenerationContext(generator, level), chunk, null, new DimensionSurfaceRule("desert"));
                var pos = new BlockPos(x, roofThickness == 0 ? top : naturalTop, blockZ);
                if (roofThickness > 0 && material < 2) {
                    for (int dy = 0; dy < roofThickness; dy++) helper.assertTrue(chunk.getBlockState(pos.below(dy)).isAir(),
                            "The entire loose sand roof must become air at " + pos + " thickness=" + roofThickness);
                } else {
                    var state = chunk.getBlockState(pos);
                    int visibleMaterial = pos.getY() < DesertHeight.RED_SAND_START_HEIGHT ? 0 : pos.getY() < DesertHeight.FIRST_TERRACOTTA_Y ? 1 : 2;
                    helper.assertTrue(visibleMaterial == 2 ? state.is(BlockTags.TERRACOTTA)
                                    : state.is(visibleMaterial == 0 ? Blocks.SAND : Blocks.RED_SAND),
                            "A cut mouth must follow its exposed surface height at " + pos + ": " + state);
                }
            }
            checked[material] = true;
        }
        helper.assertTrue(checked[0] && checked[1] && checked[2], "Exercise sand, red sand and terracotta mouths");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void flattenedColumnsUseActualHeightAtTheTerracottaBoundary(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int formerlyMisclassified = 0, checkedColumns = 0;
        for (long seed : new long[]{0, 12345}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            ChunkPos selected = null;
            search: for (int x : new int[]{-8192, -4096, 0, 8192}) for (int z = -512; z < 512; z++) {
                int actualTop = (int) Math.floor(random.router().initialDensityWithoutJaggedness()
                        .compute(new DensityFunction.SinglePointContext(x, 0, z)) + .5);
                if (actualTop < DesertHeight.FIRST_TERRACOTTA_Y
                        && materialTop(random, x, z) >= DesertHeight.TERRACOTTA_START_HEIGHT) {
                    selected = new ChunkPos(x >> 4, z >> 4); break search;
                }
            }
            helper.assertTrue(selected != null, "The fixture must find a real height/material disagreement after flattening");
            var chunk = new ProtoChunk(selected, UpgradeData.EMPTY, level, biomes, null);
            int[][] tops = new int[16][16];
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int x = selected.getMinBlockX() + dx, z = selected.getMinBlockZ() + dz;
                int top = tops[dx][dz] = (int) Math.floor(random.router().initialDensityWithoutJaggedness()
                        .compute(new DensityFunction.SinglePointContext(x, 0, z)) + .5);
                for (int y = -32; y <= top; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), false);
            }
            Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
            random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                    new WorldGenerationContext(generator, level), chunk, null, new DimensionSurfaceRule("desert"));
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int x = selected.getMinBlockX() + dx, z = selected.getMinBlockZ() + dz;
                int top = tops[dx][dz], reference = Math.max(top, materialTop(random, x, z));
                var state = chunk.getBlockState(new BlockPos(x, top, z));
                helper.assertTrue(top < DesertHeight.RED_SAND_START_HEIGHT ? state.is(Blocks.SAND)
                                : top < DesertHeight.FIRST_TERRACOTTA_Y ? state.is(Blocks.RED_SAND) : state.is(BlockTags.TERRACOTTA),
                        "The actual surface builder must follow the final height at " + x + "," + z + ": " + state);
                if (top < DesertHeight.FIRST_TERRACOTTA_Y && reference >= DesertHeight.TERRACOTTA_START_HEIGHT)
                    formerlyMisclassified++;
                checkedColumns++;
            }
        }
        helper.assertTrue(formerlyMisclassified >= 2 && checkedColumns == 512,
                "Each seed must exercise at least one real disagreement between the former color map and the visible surface");
        AOEMod.LOGGER.info("Desert color-map audit: {} surface columns, {} columns below the old material band",
                checkedColumns, formerlyMisclassified);
        helper.succeed();
    }

    private static int materialTop(RandomState random, int x, int z) {
        var point = new DensityFunction.SinglePointContext(x, 0, z);
        return (int) Math.floor(DesertHeight.materialHeight(random.router().temperature().compute(point),
                random.router().vegetation().compute(point)) + .5);
    }
}
