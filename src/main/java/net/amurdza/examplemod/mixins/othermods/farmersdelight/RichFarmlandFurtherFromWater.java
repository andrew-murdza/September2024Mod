package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;

@Mixin(RichSoilFarmlandBlock.class)
public class RichFarmlandFurtherFromWater {
    @ModifyConstant(method = "hasWater",constant = @Constant(intValue = 4),remap = false)
    private static int hi(int constant, LevelReader pLevel, BlockPos pPos){
        Integer value = Helper.getBiomeValue(pLevel, pPos, BlockBehaviorConfig.BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES);
        return value != null ? value : constant;
    }
    @ModifyConstant(method = "hasWater",constant = @Constant(intValue = -4),remap = false)
    private static int hi1(int constant, LevelReader pLevel, BlockPos pPos){
        Integer value = Helper.getBiomeValue(pLevel, pPos, BlockBehaviorConfig.BIOME_TO_MYCELIUM_SPREAD_NUM_TRIES);
        return value != null ? -value : constant;
    }
}
