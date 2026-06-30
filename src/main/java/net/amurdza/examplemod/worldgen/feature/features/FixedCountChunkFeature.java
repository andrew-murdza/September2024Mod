package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.FixedCountChunkPatchConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FixedCountChunkFeature extends Feature<FixedCountChunkPatchConfiguration> {
    private static final int FLOOR_SEARCH_RANGE = 8;

    private static final int GRID_WIDTH = 16;
    private static final int GRID_HALF_WIDTH = GRID_WIDTH / 2;

    private static final int QUADRANTS_PER_AXIS = 2;
    private static final int TOTAL_QUADRANTS = QUADRANTS_PER_AXIS * QUADRANTS_PER_AXIS;

    private static final int CELLS_PER_AXIS = 4;
    private static final int TOTAL_CELLS = CELLS_PER_AXIS * CELLS_PER_AXIS;

    public FixedCountChunkFeature(Codec<FixedCountChunkPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FixedCountChunkPatchConfiguration> context) {
        FixedCountChunkPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();

        int targetPlacements = adjustedCount(config, origin);

        Set<Long> chosenPositions = new HashSet<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int successfulPlacements;

        if (isShorelineLandPatch(config, origin)) {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    1, targetPlacements
            );
        } else if (targetPlacements % TOTAL_CELLS == 0) {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    CELLS_PER_AXIS, targetPlacements / TOTAL_CELLS
            );
        } else if (targetPlacements % TOTAL_QUADRANTS == 0) {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    QUADRANTS_PER_AXIS, targetPlacements / TOTAL_QUADRANTS
            );
        } else {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    1, targetPlacements
            );
        }

        return successfulPlacements > 0;
    }

    private int placeGrid(
            FeaturePlaceContext<FixedCountChunkPatchConfiguration> context,
            FixedCountChunkPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos origin,
            BlockPos.MutableBlockPos mutablePos,
            Set<Long> chosenPositions,
            int cellsPerAxis,
            int targetSuccessfulPlacementsPerCell
    ) {
        int successfulPlacements = 0;

        int cellSize = GRID_WIDTH / cellsPerAxis;
        for (int cellX = 0; cellX < cellsPerAxis; cellX++) {
            for (int cellZ = 0; cellZ < cellsPerAxis; cellZ++) {
                successfulPlacements += placeCell(
                        context, config, random, level, origin, mutablePos, chosenPositions,
                        cellSize, cellX, cellZ,
                        targetSuccessfulPlacementsPerCell
                );
            }
        }

        return successfulPlacements;
    }

    private int placeCell(
            FeaturePlaceContext<FixedCountChunkPatchConfiguration> context,
            FixedCountChunkPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos origin,
            BlockPos.MutableBlockPos mutablePos,
            Set<Long> chosenPositions,
            int cellSize,
            int cellX,
            int cellZ,
            int targetSuccessfulPlacements
    ) {
        int successfulPlacements = 0;

        int minXOffset = getCellMinOffset(config, origin.getX(), cellX, cellSize);
        int minZOffset = getCellMinOffset(config, origin.getZ(), cellZ, cellSize);

        List<CellOffset> offsets = buildShuffledCellOffsets(minXOffset, minZOffset, cellSize, random);

        for (CellOffset offset : offsets) {
            if (successfulPlacements >= targetSuccessfulPlacements) {
                break;
            }

            mutablePos.setWithOffset(origin, offset.x(), 0, offset.z());

            long packedPos = packXZ(mutablePos);

            if (chosenPositions.contains(packedPos)) {
                continue;
            }

            if (tryPlaceNearFloor(context, config, random, level, mutablePos)) {
                successfulPlacements++;
                chosenPositions.add(packedPos);
            }
        }

        return successfulPlacements;
    }

    private List<CellOffset> buildShuffledCellOffsets(
            int minXOffset,
            int minZOffset,
            int cellSize,
            RandomSource random
    ) {
        List<CellOffset> offsets = new ArrayList<>(cellSize * cellSize);

        for (int dx = 0; dx < cellSize; dx++) {
            for (int dz = 0; dz < cellSize; dz++) {
                offsets.add(new CellOffset(minXOffset + dx, minZOffset + dz));
            }
        }

        shuffle(offsets, random);

        return offsets;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            Collections.swap(values, i, random.nextInt(i + 1));
        }
    }

    private static int getCenteredCellMinOffset(int cellIndex, int cellSize) {
        return -GRID_HALF_WIDTH + cellIndex * cellSize;
    }

    private static int getCellMinOffset(
            FixedCountChunkPatchConfiguration config,
            int originCoordinate,
            int cellIndex,
            int cellSize
    ) {
        if (config.landOnly()) {
            return getCenteredCellMinOffset(cellIndex, cellSize);
        }

        if (Math.floorMod(originCoordinate, GRID_WIDTH) == 0) {
            return cellIndex * cellSize;
        }

        return getCenteredCellMinOffset(cellIndex, cellSize);
    }

    private boolean tryPlaceNearFloor(
            FeaturePlaceContext<FixedCountChunkPatchConfiguration> context,
            FixedCountChunkPatchConfiguration config,
            RandomSource random,
            WorldGenLevel level,
            BlockPos origin
    ) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int yOffset = 0; yOffset <= FLOOR_SEARCH_RANGE; yOffset++) {
            mutablePos.set(origin.getX(), origin.getY() + yOffset, origin.getZ());
            if (canTryLandPatch(config, level, mutablePos)
                    && config.feature().value().place(level, context.chunkGenerator(), random, mutablePos)) {
                return true;
            }

            if (yOffset == 0) {
                continue;
            }

            mutablePos.set(origin.getX(), origin.getY() - yOffset, origin.getZ());
            if (canTryLandPatch(config, level, mutablePos)
                    && config.feature().value().place(level, context.chunkGenerator(), random, mutablePos)) {
                return true;
            }
        }

        return false;
    }

    private static int adjustedCount(FixedCountChunkPatchConfiguration config, BlockPos origin) {
        int count = config.count();

        if (isShorelineLandPatch(config, origin)) {
            return Math.max(1, count / 2);
        }

        return count;
    }

    private static boolean isShorelineLandPatch(FixedCountChunkPatchConfiguration config, BlockPos origin) {
        if (!config.landOnly()) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(origin);
        int regionChunkX = Math.floorMod(chunkPos.x, 30);

        return regionChunkX == 0 || regionChunkX == 6;
    }

    private static boolean canTryLandPatch(
            FixedCountChunkPatchConfiguration config,
            WorldGenLevel level,
            BlockPos pos
    ) {
        if (!config.landOnly()) {
            return true;
        }

        BlockPos groundPos = pos.below();

        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(groundPos).isEmpty()
                && !level.getBlockState(groundPos).isAir();
    }

    private long packXZ(BlockPos pos) {
        return ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL);
    }

    private record CellOffset(int x, int z) {
    }
}
