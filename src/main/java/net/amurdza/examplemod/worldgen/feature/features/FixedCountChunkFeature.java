package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.FixedCountChunkPatchConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.HashSet;
import java.util.Set;

public class FixedCountChunkFeature extends Feature<FixedCountChunkPatchConfiguration> {

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

        int targetPlacements = config.count();
        int yRange = config.ySpread() + 1;

        Set<Long> chosenPositions = new HashSet<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int successfulPlacements;

        if (targetPlacements % TOTAL_CELLS == 0) {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    yRange, CELLS_PER_AXIS, targetPlacements / TOTAL_CELLS
            );
        } else if (targetPlacements % TOTAL_QUADRANTS == 0) {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    yRange, QUADRANTS_PER_AXIS, targetPlacements / TOTAL_QUADRANTS
            );
        } else {
            successfulPlacements = placeGrid(
                    context, config, random, level, origin, mutablePos, chosenPositions,
                    yRange, 1, targetPlacements
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
            int yRange,
            int cellsPerAxis,
            int targetSuccessfulPlacementsPerCell
    ) {
        int successfulPlacements = 0;

        int cellSize = GRID_WIDTH / cellsPerAxis;
        int maxTriesPerCell = 400;

        for (int cellX = 0; cellX < cellsPerAxis; cellX++) {
            for (int cellZ = 0; cellZ < cellsPerAxis; cellZ++) {
                successfulPlacements += placeCell(
                        context, config, random, level, origin, mutablePos, chosenPositions,
                        yRange, cellSize, cellX, cellZ,
                        targetSuccessfulPlacementsPerCell, maxTriesPerCell
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
            int yRange,
            int cellSize,
            int cellX,
            int cellZ,
            int targetSuccessfulPlacements,
            int maxTries
    ) {
        int successfulPlacements = 0;

        int minXOffset = getCenteredCellMinOffset(cellX, cellSize);
        int minZOffset = getCenteredCellMinOffset(cellZ, cellSize);

        for (int triesUsed = 0; triesUsed < maxTries && successfulPlacements < targetSuccessfulPlacements; triesUsed++) {
            int xOffset = minXOffset + random.nextInt(cellSize);
            int yOffset = random.nextInt(yRange) - random.nextInt(yRange);
            int zOffset = minZOffset + random.nextInt(cellSize);

            mutablePos.setWithOffset(origin, xOffset, yOffset, zOffset);

            long packedPos = mutablePos.asLong();

            if (!chosenPositions.add(packedPos)) {
                continue;
            }

            if (config.feature().value().place(level, context.chunkGenerator(), random, mutablePos)) {
                successfulPlacements++;
            }
        }

        return successfulPlacements;
    }

    private static int getCenteredCellMinOffset(int cellIndex, int cellSize) {
        return -(GRID_HALF_WIDTH - 1) + cellIndex * cellSize;
    }
}