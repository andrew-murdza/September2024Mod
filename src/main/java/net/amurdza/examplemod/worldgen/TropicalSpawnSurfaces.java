package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Giant mushroom roofs are canopy, rather than natural tropical spawn floors. */
public final class TropicalSpawnSurfaces {
    private TropicalSpawnSurfaces() { }

    public static boolean giantMushroom(BlockState state) {
        return state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM);
    }

    public static boolean blocked(LevelReader level, BlockPos feet) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return false;
        BlockPos support = feet.below();
        if (level instanceof WorldGenRegion region
                && !region.hasChunk(support.getX() >> 4, support.getZ() >> 4)) return false;
        return giantMushroom(level.getBlockState(support));
    }
}
