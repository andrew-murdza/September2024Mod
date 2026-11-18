package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalBiomeZoomState;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalChannelSurfaceTests {
    private TropicalChannelSurfaceTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void normalizationMovesRealBlocksAndHeightmaps(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var mangrove = registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE);
        var coral = registry.getHolderOrThrow(TropicalChannelSurface.CORAL);
        var forest = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "rainforest")));
        BiomeManager biomes = new BiomeManager((x, y, z) -> x < 2 ? mangrove : x < 3 ? coral : forest,
                BiomeManager.obfuscateSeed(12345));
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level, registry, null);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            var biome = biomes.getBiome(pos.set(x, 63, z));
            int bed = biome.is(TropicalChannelSurface.MANGROVE) ? 52
                    : biome.is(TropicalChannelSurface.CORAL) ? (z % 2 == 0 ? 58 : 51) : 70;
            for (int y = 0; y <= Math.max(bed, 63); y++) {
                chunk.setBlockState(pos.set(x, y, z), (y > bed ? Blocks.WATER : Blocks.MOSS_BLOCK).defaultBlockState(), false);
            }
        }
        chunk.setStatus(ChunkStatus.NOISE);
        TropicalChannelSurface.normalize(chunk, biomes);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            var biome = biomes.getBiome(pos.set(x, 63, z));
            int bed = biome.is(TropicalChannelSurface.MANGROVE) ? 57
                    : biome.is(TropicalChannelSurface.CORAL) ? (z % 2 == 0 ? 58 : 55) : 70;
            helper.assertTrue(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) == bed, "Seabed heightmap was not reconciled");
            helper.assertTrue(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) == Math.max(bed, 63), "Water surface heightmap was not reconciled");
            for (int y = bed - 2; y <= bed; y++) helper.assertTrue(chunk.getBlockState(pos.set(x, y, z)).is(Blocks.MOSS_BLOCK), "Moved bed lost its moss profile");
            helper.assertTrue(chunk.getBlockState(pos.set(x, bed - 3, z)).is(Blocks.MOSS_BLOCK), "Moved bed must also have a moss foundation");
            for (int y = bed + 1; y <= 63; y++) helper.assertTrue(chunk.getBlockState(pos.set(x, y, z)).is(Blocks.WATER), "Moved bed has a hole in its water column");
            if (bed < 63) helper.assertTrue(chunk.getBlockState(pos.set(x, 64, z)).isAir(), "Normalization raised the sea level");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void fullChunksHaveTheDepthRequiredByTheActualSurfaceBiome(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        BiomeManager preview = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()),
                BiomeManager.obfuscateSeed(level.getSeed()));
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        ((TropicalBiomeZoomState) preview).aoemod$setTropicalLayout(
                DimensionTerrain.tropicalLayout(random.router().temperature()),
                registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST),
                registry.getHolderOrThrow(TropicalChannelSurface.CHERRY));
        Map<Long, BlockPos> samples = new LinkedHashMap<>();
        int mangroveEdges = 0, coralEdges = 0;
        // runGameTestServer deliberately retains run/world. Fixed near-origin
        // FULL chunks would test a previous terrain revision without executing
        // the current surface pipeline. Match the other generated-world probes
        // by moving each run to a logged fresh region inside the world border.
        int cycle = 1000 + (int) Math.floorMod(System.currentTimeMillis() / 1000, 10_000L);
        int originX = cycle * 2048, originZ = -(cycle + 23) * 2048;
        AOEMod.LOGGER.info("Tropical fresh FULL-depth probe starts at {},{} with seed {}", originX, originZ, level.getSeed());
        search: for (int z = originZ - 1024; z <= originZ + 1024; z += 32) {
            for (int x = originX - 1024; x <= originX + 1024; x += 2) {
                double height = random.router().initialDensityWithoutJaggedness().compute(new DensityFunction.SinglePointContext(x, 0, z));
                int depth = DimensionTerrain.tropicalWaterDepth(height);
                if (depth < 5 || depth > 8) continue;
                BlockPos pos = new BlockPos(x, 63, z);
                var biome = preview.getBiome(pos);
                boolean mangrove = biome.is(TropicalChannelSurface.MANGROVE) && depth >= 5;
                boolean coral = biome.is(TropicalChannelSurface.CORAL) && depth <= 6;
                long chunk = ChunkPos.asLong(x >> 4, z >> 4);
                if ((mangrove && mangroveEdges < 2 || coral && coralEdges < 2) && !samples.containsKey(chunk)) {
                    samples.put(chunk, pos);
                    if (mangrove) mangroveEdges++; else coralEdges++;
                    if (mangroveEdges >= 2 && coralEdges >= 2) break search;
                }
            }
        }
        helper.assertTrue(mangroveEdges >= 2 && coralEdges >= 2, "Did not find both actual Voronoi biome boundary cases");
        int shallow = 0, deep = 0, changedFromBase = 0, maxAdjustment = 0;
        for (BlockPos sample : samples.values()) {
            ChunkAccess chunk = level.getChunk(sample.getX() >> 4, sample.getZ() >> 4, ChunkStatus.FULL, true);
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getPos().getMinBlockX() + dx, z = chunk.getPos().getMinBlockZ() + dz;
                BlockPos pos = new BlockPos(x, 63, z);
                var biome = level.getBiome(pos);
                int depth = Math.max(0, 63 - naturalBed(chunk, x, z));
                if (biome.is(TropicalChannelSurface.MANGROVE) && depth > 0) {
                    helper.assertTrue(depth <= 6, "FULL mangrove column is deeper than six at " + pos + ": " + depth);
                    shallow++;
                } else if (biome.is(TropicalChannelSurface.CORAL)) {
                    helper.assertTrue(depth > 0 && depth <= 8, "FULL reef must remain wet and at most eight deep at " + pos + ": " + depth);
                    deep++;
                }
            }
            // Base queries intentionally describe noise. Check the surface-stage
            // correction against physical FULL blocks, not the pre-surface estimate.
            int base = generator.getBaseHeight(sample.getX(), sample.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, random) - 1;
            int actual = naturalBed(chunk, sample.getX(), sample.getZ());
            if (base != actual) changedFromBase++;
            maxAdjustment = Math.max(maxAdjustment, Math.abs(base - actual));
        }
        helper.assertTrue(shallow > 20 && deep > 20, "FULL chunk audit lacked shallow or deep water");
        helper.assertTrue(maxAdjustment <= 4, "Biome correction moved sampled seabeds excessively: " + maxAdjustment);
        AOEMod.LOGGER.info("Tropical FULL audit: {} chunks, {} shallow mangrove / {} deep coral columns, {} boundary corrections, max bed adjustment {}",
                samples.size(), shallow, deep, changedFromBase, maxAdjustment);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void physicalShorelineReconcilesInterpolationWithoutTwoHighDiagonalSteps(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int checked = 0, seaLevel = 0, upland = 0;
        for (long seed : new long[]{12345L, -7091263362684584719L}) {
            RandomState random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            TropicalLayout layout = DimensionTerrain.tropicalLayout(random.router().temperature());
            var island = layout.island(0, 0);
            BiomeManager actual = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()),
                    BiomeManager.obfuscateSeed(seed));
            ((TropicalBiomeZoomState) actual).aoemod$setTropicalLayout(layout,
                    registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE),
                    registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST),
                    registry.getHolderOrThrow(TropicalChannelSurface.CHERRY));
            for (double angle : new double[]{0, Math.PI / 8, Math.PI / 4, 3 * Math.PI / 8}) {
                for (double radius : new double[]{TropicalLayout.FOREST_RADIUS, TropicalLayout.MANGROVE_LAND_RADIUS}) {
                    int centerX = (int) Math.round(island.x() + radius * Math.cos(angle));
                    int centerZ = (int) Math.round(island.z() + radius * Math.sin(angle));
                    Map<Long, ProtoChunk> chunks = new LinkedHashMap<>();
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                    for (int cx = (centerX >> 4) - 1; cx <= (centerX >> 4) + 1; cx++) {
                        for (int cz = (centerZ >> 4) - 1; cz <= (centerZ >> 4) + 1; cz++) {
                            ProtoChunk chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, level, registry, null);
                            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                                int x = cx * 16 + dx, z = cz * 16 + dz;
                                int bed = interpolatedBed(layout, x, z);
                                for (int y = 48; y <= Math.max(63, bed); y++) {
                                    chunk.setBlockState(cursor.set(x, y, z), (y > bed ? Blocks.WATER : Blocks.MOSS_BLOCK).defaultBlockState(), false);
                                }
                            }
                            chunk.setStatus(ChunkStatus.NOISE);
                            TropicalChannelSurface.normalize(chunk, actual, layout);
                            chunks.put(ChunkPos.asLong(cx, cz), chunk);
                        }
                    }
                    for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                        int x = centerX + dx, z = centerZ + dz;
                        int bed = physicalBed(chunks, x, z);
                        helper.assertTrue(bed == (int) Math.floor(layout.surfaceHeight(x, z) + .5),
                                "The physical bank must follow the exact layout height, without a ledge at a quart-biome edge");
                        var biome = actual.getBiome(new BlockPos(x, bed + 1, z));
                        helper.assertTrue(biome.is(TropicalChannelSurface.MANGROVE) == (layout.sample(x, z).biome() == 1),
                                "The physical Y63/Y64 bank must choose mangrove/rainforest in the same X/Z column");
                        if (bed == 63) seaLevel++;
                        if (bed >= 64) upland++;
                        for (int nx = -1; nx <= 1; nx++) for (int nz = -1; nz <= 1; nz++) {
                            helper.assertTrue(Math.abs(bed - physicalBed(chunks, x + nx, z + nz)) <= 1,
                                    "Physical land transition has a two-high cardinal/diagonal step at " + x + "," + z);
                            checked++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(seaLevel > 100 && upland > 100,
                "Shoreline audit must exercise both sides of the exact Y63/Y64 contour: " + seaLevel + "/" + upland);
        AOEMod.LOGGER.info("Tropical physical shoreline audit: {} cardinal/diagonal neighbor pairs", checked);
        helper.succeed();
    }

    private static int interpolatedBed(TropicalLayout layout, int x, int z) {
        int gx = Math.floorDiv(x, 4) * 4, gz = Math.floorDiv(z, 4) * 4;
        double tx = (x - gx) / 4.0, tz = (z - gz) / 4.0;
        double north = layout.sample(gx, gz).height() * (1 - tx) + layout.sample(gx + 4, gz).height() * tx;
        double south = layout.sample(gx, gz + 4).height() * (1 - tx) + layout.sample(gx + 4, gz + 4).height() * tx;
        return (int) Math.floor(north * (1 - tz) + south * tz + .5);
    }

    private static int physicalBed(Map<Long, ProtoChunk> chunks, int x, int z) {
        var chunk = chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (chunk == null) throw new IllegalStateException("Shoreline check escaped the three-chunk fixture");
        return chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
    }

    private static int naturalBed(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 120; y >= 1; y--) {
            var state = chunk.getBlockState(pos.set(x, y, z));
            if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                    || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.DIRT)) return y;
        }
        throw new IllegalStateException("No natural tropical floor at " + x + "," + z);
    }
}
