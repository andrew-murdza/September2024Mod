package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FarmBlock.class)
public class FarmlandFurtherFromWater {
    @ModifyConstant(method = "isNearWater",constant = @Constant(intValue = 4))
    private static int hi(int constant, LevelReader pLevel, BlockPos pPos){
        if(Helper.isSpecialBiome(pLevel,pPos)){
            return Config.FARMLAND_DISTANCE;
        }
        return constant;
    }
    @ModifyConstant(method = "isNearWater",constant = @Constant(intValue = -4))
    private static int hi1(int constant, LevelReader pLevel, BlockPos pPos){
        if(Helper.isSpecialBiome(pLevel,pPos)){
            return -Config.FARMLAND_DISTANCE;
        }
        return constant;
    }
}
