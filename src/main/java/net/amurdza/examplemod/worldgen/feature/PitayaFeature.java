package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PitayaFeature extends Feature<NoneFeatureConfiguration> {
    public PitayaFeature(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
        WorldGenLevel level=pContext.level();
        BlockPos pos=pContext.origin();
        int i=0;
        for(int k=0;k<3;k++){
            if(!level.getBlockState(pos.above(k)).is(Blocks.AIR)){
                break;
            }
            i++;
        }
        level.setBlock(pos, NethersExoticismModBlocks.PITAYA_STEM.get().defaultBlockState(),2);
        if(i>1){
            level.setBlock(pos.above(), NethersExoticismModBlocks.PITAYA_STEM.get().defaultBlockState(),2);
        }
        if(i>2){
            level.setBlock(pos.above(2), NethersExoticismModBlocks.PITAYA_BLOCK.get().defaultBlockState(),2);
        }
        return i>0;
    }
}
