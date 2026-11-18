package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.mixins.accessor.TrunkPlacerHeightAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

import java.util.function.IntSupplier;

/** Adds a clear lower trunk while leaving the sampled spruce crown unchanged. */
public final class SnowySpruceHeight {
    public static final int EXTRA_TRUNK_HEIGHT = 4;

    private SnowySpruceHeight() { }

    public static int sampleHeight(WorldGenLevel level, BlockPos origin, TreeConfiguration configuration,
                                   RandomSource random, IntSupplier original) {
        int extension = extension(level, origin, configuration);
        TrunkPlacer placer = configuration.trunkPlacer;
        if (extension > 0 && placer instanceof GiantTrunkPlacer) {
            TrunkPlacerHeightAccess heights = (TrunkPlacerHeightAccess) placer;
            int firstRange = heights.aoemod$heightRandA(), secondRange = heights.aoemod$heightRandB();
            // Raise the minimum four, retain the previous maximum, and raise
            // the mean two by removing four from the random ranges' sum.
            int reduction = Math.min(4, firstRange + secondRange);
            int secondReduction = Math.min(reduction, secondRange);
            return heights.aoemod$baseHeight() + reduction + extension
                    + random.nextInt(firstRange - (reduction - secondReduction) + 1)
                    + random.nextInt(secondRange - secondReduction + 1);
        }
        return original.getAsInt() + extension;
    }

    public static int extension(WorldGenLevel level, BlockPos origin, TreeConfiguration configuration) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                || !GameplayDimensions.biomeIs(level, origin, "snowy_forest")
                && !GameplayDimensions.biomeIs(level, origin, "dappled_forest")) return 0;
        if (!(configuration.trunkPlacer instanceof StraightTrunkPlacer
                || configuration.trunkPlacer instanceof GiantTrunkPlacer)
                || !(configuration.foliagePlacer instanceof SpruceFoliagePlacer
                || configuration.foliagePlacer instanceof MegaPineFoliagePlacer)
                || !(configuration.trunkProvider instanceof SimpleStateProvider)
                || !(configuration.foliageProvider instanceof SimpleStateProvider)) return 0;
        // The natural and sapling spruce configurations use constant providers.
        // Inspect them without advancing the tree's generation random source.
        RandomSource inspection = RandomSource.create(0);
        return configuration.trunkProvider.getState(inspection, origin).is(Blocks.SPRUCE_LOG)
                && configuration.foliageProvider.getState(inspection, origin).is(Blocks.SPRUCE_LEAVES)
                ? EXTRA_TRUNK_HEIGHT : 0;
    }
}
