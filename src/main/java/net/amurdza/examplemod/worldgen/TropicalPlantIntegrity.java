package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Remove only dependent plant parts, quietly, after another block replaces their support. */
public final class TropicalPlantIntegrity {
    private TropicalPlantIntegrity() { }

    public static boolean isMangroveRoot(BlockState state) {
        return state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    public static boolean flowerRootMayReplace(BlockState state) {
        if (!state.is(net.minecraft.tags.BlockTags.FLOWERS)) return false;
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        // Wild crops share vanilla flower tags but retain their independent food quota.
        return id != null && !id.getPath().startsWith("wild_") && !id.getPath().endsWith("_wild_crop");
    }

    /** Decoration may replace flowers with roots, never the reverse. */
    public static boolean protectsRoot(LevelAccessor level, BlockState previous, BlockState proposed) {
        return GameplayDimensions.is(level, GameplayDimensions.TROPICAL) && isMangroveRoot(previous)
                && (proposed.getBlock() instanceof BushBlock || proposed.is(net.minecraft.tags.BlockTags.FLOWERS)
                || proposed.is(Blocks.MOSS_CARPET) || proposed.is(Blocks.VINE));
    }

    public static void afterReplacement(LevelAccessor level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (oldState == newState || oldState.isAir() || !GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return;
        TropicalBamboo.trimForObstacle(level, pos, oldState, newState);
        if (oldState.getBlock() instanceof BushBlock && oldState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && (!newState.is(oldState.getBlock())
                || !newState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || newState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                != oldState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF))) {
            var half = oldState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState partner = level.getBlockState(other);
            if (partner.is(oldState.getBlock()) && partner.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && partner.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half) {
                clearQuietly(level, other, partner);
            }
        }
        BlockPos below = pos.below();
        BlockState hanging = level.getBlockState(below);
        if (isCaveVine(hanging) && !hanging.canSurvive(level, below)) clearDetachedVines(level, below);
    }

    public static boolean isCaveVine(BlockState state) {
        return state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT);
    }

    public static void clearDetachedVines(LevelAccessor level, BlockPos start) {
        // Bottom first avoids recursive support-loss cleanup in the replacement hook.
        int bottom = start.getY();
        while (bottom > level.getMinBuildHeight()
                && isCaveVine(level.getBlockState(new BlockPos(start.getX(), bottom - 1, start.getZ())))) bottom--;
        for (int y = bottom; y <= start.getY(); y++) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            BlockState state = level.getBlockState(pos);
            if (isCaveVine(state)) clearQuietly(level, pos, state);
        }
    }

    private static void clearQuietly(LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
