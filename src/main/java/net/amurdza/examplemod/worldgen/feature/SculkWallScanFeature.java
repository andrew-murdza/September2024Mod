package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class SculkWallScanFeature extends Feature<SculkWallScanFeatureConfig> {

    public SculkWallScanFeature(Codec<SculkWallScanFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SculkWallScanFeatureConfig> context) {
        WorldGenLevel level = context.level();
        SculkWallScanFeatureConfig cfg = context.config();

        ChunkPos chunkPos = new ChunkPos(context.origin());

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        int minY = cfg.minY();
        int maxY = cfg.maxY();

        boolean placedAny = false;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);

                    if (aoe$shouldConvertToSculk(level, mutable)) {
                        level.setBlock(mutable, Blocks.SCULK.defaultBlockState(), 2);
                        placedAny = true;
                    }
                }
            }
        }

        return placedAny;
    }

    private static boolean aoe$shouldConvertToSculk(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.is(BlockTags.SCULK_REPLACEABLE)||state.is(Blocks.SOUL_SAND)) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));

            if (aoe$isCaveExposure(neighbor)) {
                return true;
            }
        }

        return false;
    }

    private static boolean aoe$isCaveExposure(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }
}