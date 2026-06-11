package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SugarCaneFeature extends Feature<NoneFeatureConfiguration> {

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

        boolean placedAny = false;

        int[] xs = {
                minX,
                minX + 15
        };

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
                BlockPos pos = new BlockPos(x, y, z);

                if (canPlaceThreeSugarCane(level, pos, sugarCane)) {
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
                && sugarCane.canSurvive(level, pos);
    }
}