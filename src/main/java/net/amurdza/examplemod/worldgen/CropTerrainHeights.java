package net.amurdza.examplemod.worldgen;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Exact base heights, sharing Minecraft's interpolator across each crop reservation cell. */
public final class CropTerrainHeights {
    private static final DensityFunctions.BeardifierOrMarker NO_STRUCTURES = new DensityFunctions.BeardifierOrMarker() {
        @Override public double compute(DensityFunction.FunctionContext context) { return 0; }
        @Override public double minValue() { return 0; }
        @Override public double maxValue() { return 0; }
    };
    private final LevelHeightAccessor level;
    private final NoiseBasedChunkGenerator generator;
    private final RandomState random;
    private final int dryBelowY;
    private final Map<Long, int[]> cells = new HashMap<>();

    public CropTerrainHeights(LevelHeightAccessor level, NoiseBasedChunkGenerator generator, RandomState random) {
        this(level, generator, random, switch (generator.generatorSettings().unwrapKey()
                .map(key -> key.location().getPath()).orElse("")) {
            case "plains_dimension" -> 0;
            case "desert_dimension" -> 54;
            default -> Integer.MIN_VALUE;
        });
    }

    public CropTerrainHeights(LevelHeightAccessor level, NoiseBasedChunkGenerator generator, RandomState random, int dryBelowY) {
        this.level = level;
        this.generator = generator;
        this.random = random;
        this.dryBelowY = dryBelowY;
    }

    public int height(int x, int z) {
        int cellX = Math.floorDiv(x, CropTiles.CELL_SIZE), cellZ = Math.floorDiv(z, CropTiles.CELL_SIZE);
        int[] heights = cells.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), ignored ->
                sample(cellX * CropTiles.CELL_SIZE, cellZ * CropTiles.CELL_SIZE));
        return heights[Math.floorMod(x, CropTiles.CELL_SIZE) * CropTiles.CELL_SIZE + Math.floorMod(z, CropTiles.CELL_SIZE)];
    }

    private int[] sample(int minX, int minZ) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        NoiseSettings noise = settings.noiseSettings().clampToHeightAccessor(level);
        int width = noise.getCellWidth(), height = noise.getCellHeight();
        int size = CropTiles.CELL_SIZE, count = size / width;
        int[] result = new int[size * size];
        Arrays.fill(result, level.getMinBuildHeight());
        if (count == 0 || size % width != 0) {
            for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
                result[x * size + z] = generator.getBaseHeight(minX + x, minZ + z,
                        Heightmap.Types.OCEAN_FLOOR_WG, level, random);
            }
            return result;
        }
        // Match the generator's fluid cutoff, including Snowy's deep aquifers.
        // A stale cutoff changes aquifer pressure and therefore even solid base heights.
        Aquifer.FluidStatus water = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        Sampler sampler = new Sampler(count, random, minX, minZ, noise, settings,
                (x, y, z) -> y < dryBelowY ? dry : water);
        int minCellY = Math.floorDiv(noise.minY(), height), cellsY = noise.height() / height;
        sampler.initializeForFirstCellX();
        try {
            for (int cellX = 0; cellX < count; cellX++) {
                sampler.advanceCellX(cellX);
                for (int cellZ = 0; cellZ < count; cellZ++) {
                    boolean[] found = new boolean[width * width];
                    int remaining = found.length;
                    for (int cellY = cellsY - 1; cellY >= 0 && remaining > 0; cellY--) {
                        sampler.selectCellYZ(cellY, cellZ);
                        for (int dy = height - 1; dy >= 0 && remaining > 0; dy--) {
                            int y = (minCellY + cellY) * height + dy;
                            sampler.updateForY(y, dy / (double) height);
                            for (int dx = 0; dx < width; dx++) {
                                int x = cellX * width + dx;
                                sampler.updateForX(minX + x, dx / (double) width);
                                for (int dz = 0; dz < width; dz++) {
                                    int index = dx * width + dz;
                                    if (found[index]) continue;
                                    int z = cellZ * width + dz;
                                    sampler.updateForZ(minZ + z, dz / (double) width);
                                    BlockState block = sampler.block();
                                    if (Heightmap.Types.OCEAN_FLOOR_WG.isOpaque().test(block == null ? settings.defaultBlock() : block)) {
                                        result[x * size + z] = y + 1;
                                        found[index] = true;
                                        remaining--;
                                    }
                                }
                            }
                        }
                    }
                }
                sampler.swapSlices();
            }
        } finally {
            sampler.stopInterpolation();
        }
        return result;
    }

    private static final class Sampler extends NoiseChunk {
        private Sampler(int count, RandomState random, int x, int z, NoiseSettings noise,
                        NoiseGeneratorSettings settings, Aquifer.FluidPicker fluids) {
            super(count, random, x, z, noise, NO_STRUCTURES, settings, fluids, Blender.empty());
        }
        private BlockState block() { return getInterpolatedState(); }
    }
}
