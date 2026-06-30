package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.FixedCountRandomPatchConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixedCountRandomPatchFeature extends Feature<FixedCountRandomPatchConfiguration> {
    private static final int FLOOR_SEARCH_RANGE = 8;

    public FixedCountRandomPatchFeature(Codec<FixedCountRandomPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FixedCountRandomPatchConfiguration> context) {
        FixedCountRandomPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();

        int targetPlacements = adjustedCount(config, origin);

        int successfulPlacements = 0;

        List<CellOffset> offsets = buildShuffledOffsets(config.xzSpread(), random);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (CellOffset offset : offsets) {
            if (successfulPlacements >= targetPlacements) {
                break;
            }

            mutablePos.setWithOffset(origin, offset.x(), 0, offset.z());

            if (tryPlaceNearFloor(context, config, random, level, mutablePos)) {
                successfulPlacements++;
            }
        }

        return successfulPlacements > 0;
    }

    private List<CellOffset> buildShuffledOffsets(int xzSpread, RandomSource random) {
        List<CellOffset> offsets = new ArrayList<>((xzSpread * 2 + 1) * (xzSpread * 2 + 1));

        for (int x = -xzSpread; x <= xzSpread; x++) {
            for (int z = -xzSpread; z <= xzSpread; z++) {
                offsets.add(new CellOffset(x, z));
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

    private boolean tryPlaceNearFloor(
            FeaturePlaceContext<FixedCountRandomPatchConfiguration> context,
            FixedCountRandomPatchConfiguration config,
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

    private static int adjustedCount(FixedCountRandomPatchConfiguration config, BlockPos origin) {
        int count = config.count();

        if (isShorelineLandPatch(config, origin)) {
            return Math.max(1, count / 2);
        }

        return count;
    }

    private static boolean isShorelineLandPatch(FixedCountRandomPatchConfiguration config, BlockPos origin) {
        if (!config.landOnly()) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(origin);
        int regionChunkX = Math.floorMod(chunkPos.x, 30);

        return regionChunkX == 0 || regionChunkX == 6;
    }

    private static boolean canTryLandPatch(
            FixedCountRandomPatchConfiguration config,
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

    private record CellOffset(int x, int z) {
    }
}
