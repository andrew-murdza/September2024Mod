package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Function;

public class SeaPicklePond extends Feature<NoneFeatureConfiguration> {
    public SeaPicklePond(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
        WorldGenLevel level=pContext.level();
        BlockPos pos = pContext.origin();
        Function<Integer,Integer> getCenter= x->((int)((double)x)/16)*16+8;
        pos=new BlockPos(getCenter.apply(pos.getX()),pos.getY(),getCenter.apply(pos.getZ()));
        for(int j=0;j<2;j++){
            for(int k=0;k<2;k++){
                for(int l=0;l<2;l++){
                    for(int m=0;m<2;m++){
                        for(int h=0;h<2;h++){
                            BlockState state=Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES,4)
                                    .setValue(BlockStateProperties.WATERLOGGED,true);
                            if(h==1&&m==1&&l==1||h==0){
                                state=Blocks.WATER.defaultBlockState();
                            }
//                            level.setBlock(pos.offset(j+(j==0?-1:1)*(6+l),-h-1,k+(k==0?-1:1)*(6+m)),state,19);
                        }
                    }
                }
            }
        }
        return true;
    }
}
