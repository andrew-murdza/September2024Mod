package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WorldGenUtils {
    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level){
        return getTotalWaterAbove(pos,level,20);
    }

    private static boolean isWater(BlockPos pos, BlockGetter level){
        BlockState state=level.getBlockState(pos);
        return state.getBlock()==Blocks.WATER&&state.getValue(BlockStateProperties.LEVEL)==8;

    }
    public static int getTotalWaterAbove(BlockPos pos, BlockGetter level, int max){
        int sum=0;
        for(int i=0;i<Math.min(max,-pos.getY()+level.getMaxBuildHeight()-1);i++){
            if(isWater(pos.above(i),level)){
                sum++;
                if(sum>=max){
                    return sum;
                }
            }
            else{
                break;
            }
        }
        return sum;
    }
}
