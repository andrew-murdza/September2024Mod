package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SugarCaneFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLOOR_SEARCH_RANGE = 8;

    public SugarCaneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        LevelAccessor level = context.level();
        BlockPos origin = context.origin();

        ChunkAccess chunk = level.getChunk(origin);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        int y = origin.getY();
        int regionChunkX = Math.floorMod(chunk.getPos().x, 30);

        boolean placedAny = false;

        int[] xs = riverFacingXPositions(minX, regionChunkX);

        int[] zs = {
                minZ + 6,
                minZ + 7,
                minZ + 8,
                minZ + 9
        };

        BlockState sugarCane = Blocks.SUGAR_CANE.defaultBlockState()
                .setValue(SugarCaneBlock.AGE, 0);

        for (int x : xs) {
            for (int z : zs) {
                BlockPos pos = findSugarCaneOrigin(level, x, y, z, sugarCane);

                if (pos != null) {
                    level.setBlock(pos, sugarCane, 2);
                    level.setBlock(pos.above(), sugarCane, 2);
                    level.setBlock(pos.above(2), sugarCane, 2);
                    placedAny = true;
                }
            }
        }

        return placedAny;
    }

    private static boolean canPlaceThreeSugarCane(LevelAccessor level, BlockPos pos, BlockState sugarCane) {
        BlockPos pos1 = pos;
        BlockPos pos2 = pos.above();
        BlockPos pos3 = pos.above(2);

        return level.getBlockState(pos1).isAir()
                && level.getBlockState(pos2).isAir()
                && level.getBlockState(pos3).isAir()
                && !level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)
                && hasHorizontalWater(level, pos.below())
                && sugarCane.canSurvive(level, pos);
    }

    private static int[] riverFacingXPositions(int minX, int regionChunkX) {
        return switch (regionChunkX) {
            // Chunks 0-5 are the river. Chunk 6 faces that river on its west edge.
            case 6 -> range(minX, minX + 7);
            // Chunk 29 faces the next repeated river on its east edge at chunk 30 / 0.
            case 29 -> range(minX + 8, minX + 15);
            default -> new int[]{};
        };
    }

    private static int[] range(int minInclusive, int maxInclusive) {
        int[] values = new int[maxInclusive - minInclusive + 1];

        for (int i = 0; i < values.length; i++) {
            values[i] = minInclusive + i;
        }

        return values;
    }

    private static boolean hasHorizontalWater(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private static BlockPos findSugarCaneOrigin(
            LevelAccessor level,
            int x,
            int y,
            int z,
            BlockState sugarCane
    ) {
        for (int yOffset = 0; yOffset <= FLOOR_SEARCH_RANGE; yOffset++) {
            BlockPos up = new BlockPos(x, y + yOffset, z);
            if (canPlaceThreeSugarCane(level, up, sugarCane)) {
                return up;
            }

            if (yOffset == 0) {
                continue;
            }

            BlockPos down = new BlockPos(x, y - yOffset, z);
            if (canPlaceThreeSugarCane(level, down, sugarCane)) {
                return down;
            }
        }

        return null;
    }
}
