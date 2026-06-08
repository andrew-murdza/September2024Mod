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
        int maxTries = 400;

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
}