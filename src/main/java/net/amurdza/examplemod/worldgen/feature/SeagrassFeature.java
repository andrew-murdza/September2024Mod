package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

public class SeagrassFeature extends Feature<ProbabilityFeatureConfiguration> {
    public SeagrassFeature(Codec<ProbabilityFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> p_160318_) {
        boolean flag = false;
        WorldGenLevel worldgenlevel = p_160318_.level();
        BlockPos blockpos1 = p_160318_.origin();
        float prob=p_160318_.config().probability;
        if (worldgenlevel.getBlockState(blockpos1).is(Blocks.WATER)) {
            BlockState blockstate = Blocks.TALL_SEAGRASS.defaultBlockState();
            if (blockstate.canSurvive(worldgenlevel, blockpos1)) {
                BlockPos blockpos2 = blockpos1.above();
                BlockPos blockpos3 = blockpos2.above();
                if (worldgenlevel.getBlockState(blockpos2).is(Blocks.WATER)) {
                    if(p_160318_.random().nextFloat()<prob&&worldgenlevel.getBlockState(blockpos3).is(Blocks.WATER)){
                        BlockState blockstate1 = blockstate.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
                        worldgenlevel.setBlock(blockpos1, blockstate, 2);
                        worldgenlevel.setBlock(blockpos2, blockstate1, 2);
                    }
                    else{
                        worldgenlevel.setBlock(blockpos1, Blocks.SEAGRASS.defaultBlockState(), 2);
                    }
                }
                flag = true;
            }
        }
        return flag;
    }
}
