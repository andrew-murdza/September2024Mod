package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;

public class WorldGenUtils {
    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level) {
        return getTotalWaterAbove(pos, level, 20);
    }

    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level, int max) {
        int depth = 0;
        int limit = Math.min(max, level.getMaxBuildHeight() - pos.getY());
        while (depth < limit && level.getFluidState(pos.above(depth)).is(FluidTags.WATER)) {
            depth++;
        }
        return depth;
    }
}
