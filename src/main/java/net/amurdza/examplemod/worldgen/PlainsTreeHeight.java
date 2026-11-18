package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.mixins.accessor.TrunkPlacerHeightAccess;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

public final class PlainsTreeHeight {
    private PlainsTreeHeight() { }

    public static int extension(WorldGenLevel level, BlockPos origin, TreeConfiguration config) {
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return 0;
        if (config.trunkPlacer instanceof StraightTrunkPlacer
                && config.trunkProvider.getState(RandomSource.create(0), origin).is(Blocks.OAK_LOG)) {
            int base = ((TrunkPlacerHeightAccess) config.trunkPlacer).aoemod$baseHeight();
            return 1 + Math.max(0, 5 - base);
        }
        return 1;
    }
}
