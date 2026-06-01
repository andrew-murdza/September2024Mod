package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.HashSet;
import java.util.Set;

public class FixedCountRandomPatchFeature extends Feature<FixedCountRandomPatchConfiguration> {

    public FixedCountRandomPatchFeature(Codec<FixedCountRandomPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FixedCountRandomPatchConfiguration> context) {
        FixedCountRandomPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();

        int targetDistinctPositions = config.count();
        int maxTries = triesForDistinctPositions(targetDistinctPositions, config.ySpread());

        int successfulPlacements = 0;
        int distinctPositionsChosen = 0;

        int xzRange = config.xzSpread() + 1;
        int yRange = config.ySpread() + 1;

        Set<Long> chosenPositions = new HashSet<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int triesUsed = 0; triesUsed < maxTries && distinctPositionsChosen < targetDistinctPositions; triesUsed++) {
            mutablePos.setWithOffset(
                    origin,
                    random.nextInt(xzRange) - random.nextInt(xzRange),
                    random.nextInt(yRange) - random.nextInt(yRange),
                    random.nextInt(xzRange) - random.nextInt(xzRange)
            );

            long packedPos = mutablePos.asLong();

            if (!chosenPositions.add(packedPos)) {
                continue;
            }

            distinctPositionsChosen++;

            if (config.feature().value().place(level, context.chunkGenerator(), random, mutablePos)) {
                successfulPlacements++;
            }
        }

        return successfulPlacements > 0;
    }

    private static int triesForDistinctPositions(int count, int ySpread) {
        if (count <= 0) {
            return 0;
        }

        if (count > 85) {
            throw new IllegalArgumentException("This table assumes count <= 85.");
        }

        if (ySpread == 0) {
            // 100% success chance
            if (count <= 16) return 19;
            if (count <= 32) return 39;
            if (count <= 48) return 60;
            if (count <= 64) return 83;
            if (count <= 80) return 109;
            return 117;
        }

        if (ySpread == 1) {
            // 1/3 success chance, or 2/3 failure chance
            if (count <= 16) return 80;
            if (count <= 32) return 146;
            if (count <= 48) return 214;
            if (count <= 64) return 286;
            if (count <= 80) return 364;
            return 389;
        }

        throw new IllegalArgumentException("FixedCountRandomPatchFeature only supports ySpread 0 or 1.");
    }
}