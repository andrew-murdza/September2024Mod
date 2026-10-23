package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;

@Mixin(RichSoilFarmlandBlock.class)
public class RichFarmlandFurtherFromWater {
    @ModifyConstant(method = "isNearWater",constant = @Constant(intValue = 4),remap = false, require = 1)
    private static int hi(int constant, LevelReader pLevel, BlockPos pPos){
        return BlockBehaviorConfig.RICH_FARMLAND_HYDRATION_DISTANCE;
    }
    @ModifyConstant(method = "isNearWater",constant = @Constant(intValue = -4),remap = false, require = 1)
    private static int hi1(int constant, LevelReader pLevel, BlockPos pPos){
        return -BlockBehaviorConfig.RICH_FARMLAND_HYDRATION_DISTANCE;
    }
}
