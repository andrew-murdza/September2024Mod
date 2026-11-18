package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

import java.util.function.Predicate;

/** The red cap's three hollow layers stay open without clearing the foliage beneath it. */
public final class RedMushroomCapSpace {
    private RedMushroomCapSpace() { }

    public static void clearInteriorLeaves(LevelAccessor level, BlockPos origin, int height, int foliageRadius) {
        int radius = foliageRadius - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = height - 3; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                cursor.setWithOffset(origin, dx, dy, dz);
                // Mushroom caps and wart blocks share the harvesting leaves tag, but are not tree leaves.
                if (level.getBlockState(cursor).getBlock() instanceof LeavesBlock) {
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    public static boolean isInsideRedCap(LevelSimulatedReader level, BlockPos pos) {
        return isInsideRedCap(pos, sample -> level.isStateAtPosition(sample, state -> state.is(Blocks.RED_MUSHROOM_BLOCK)));
    }

    public static boolean isInsideRedCap(BlockPos pos, Predicate<BlockPos> redCapAt) {
        boolean roof = false;
        for (int dy = 1; dy <= 3; dy++) {
            if (redCapAt.test(pos.above(dy))) {
                roof = true;
                break;
            }
        }
        if (!roof) return false;
        // Native and pack red caps use radius two: an interior point is at most
        // three blocks from each wall. Requiring every wall excludes under-cap foliage.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            boolean wall = false;
            for (int distance = 1; distance <= 3; distance++) {
                if (redCapAt.test(pos.relative(direction, distance))) {
                    wall = true;
                    break;
                }
            }
            if (!wall) return false;
        }
        return true;
    }
}
