package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

/** Raise short ceilings without moving cave floors, then add uncut floor-to-ceiling supports. */
public final class SnowyCaveColumns {
    private SnowyCaveColumns() { }

    public static int raisedHeight(int height) {
        // Round the linear 1..12 -> 8..12 ramp to the nearest whole block.
        return height <= 0 || height >= 12 ? height : 8 + (4 * (height - 1) + 5) / 11;
    }

    public static SnowyCaveSupports supports(RandomState random) {
        var noise = random.router().vegetation();
        long seed = TropicalLayout.mix(Double.doubleToLongBits(noise.compute(new DensityFunction.SinglePointContext(0, 0, 0)))
                ^ Long.rotateLeft(Double.doubleToLongBits(noise.compute(new DensityFunction.SinglePointContext(8192, 0, -8192))), 29));
        return new SnowyCaveSupports(seed);
    }

    public static void finish(ChunkAccess chunk, RandomState random, BlockState rock) {
        var supports = supports(random);
        int min = chunk.getMinBuildHeight(), max = chunk.getMaxBuildHeight();
        BlockState[] original = new BlockState[max - min];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int surface = SnowyOceanFloor.floor(random, x, z);
                for (int y = min; y < max; y++) original[y - min] = chunk.getBlockState(pos.set(x, y, z));
                var finished = finishColumn(original, min, surface, supports.clearance(x, z), rock);
                for (int i = 0; i < finished.length; i++) {
                    if (finished[i] != original[i]) chunk.setBlockState(pos.set(x, min + i, z), finished[i], false);
                }
            }
        }
    }

    /** Shared with off-chunk connectivity sampling so a ceiling lift or pillar cannot split a search seam. */
    static BlockState[] finishColumn(BlockState[] original, int min, int surface, double clearance, BlockState rock) {
        BlockState[] finished = original.clone();
        int protectedFrom = Math.min(original.length, (surface <= MountainLakeOffset.SEA_LEVEL + 2
                ? SnowyOceanFloor.protectedFrom(surface) : surface + 1) - min);
        for (int i = 1; i < protectedFrom; i++) {
            if (!original[i].isAir()) continue;
            int first = i;
            while (i + 1 < original.length && original[i + 1].isAir()) i++;
            if (i >= protectedFrom) break;
            int target = first + raisedHeight(i - first + 1) - 1;
            for (int j = i + 1; j <= target && j < protectedFrom; j++) {
                var state = original[j];
                if (state.isAir()) continue;
                if (!carvableSurface(state)
                        || clearance <= SnowyCaveSupports.flare(j, first, target)) break;
                finished[j] = Blocks.AIR.defaultBlockState();
            }
        }
        if (clearance <= SnowyCaveSupports.FLARE) {
            for (int i = 1; i < protectedFrom; i++) {
                if (!finished[i].isAir()) continue;
                int first = i;
                while (i + 1 < finished.length && finished[i + 1].isAir()) i++;
                if (i >= protectedFrom) break;
                for (int j = first; j <= i; j++)
                    if (clearance <= SnowyCaveSupports.flare(j, first, i)) finished[j] = rock;
            }
        }
        return finished;
    }

    public static boolean carvableSurface(BlockState state) {
        return !state.is(Blocks.MUD) && (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(Blocks.SNOW_BLOCK));
    }
}
