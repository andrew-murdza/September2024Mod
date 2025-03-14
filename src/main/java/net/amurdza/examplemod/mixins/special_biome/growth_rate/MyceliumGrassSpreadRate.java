package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
@Mixin(SpreadingSnowyDirtBlock.class)
public class MyceliumGrassSpreadRate {
    @ModifyConstant(method = "randomTick", constant = @Constant(intValue = 4))
    public int randomTick1(int constant, BlockState state, ServerLevel world, BlockPos pos, RandomSource random){
        return Helper.withChanceToInt(world,0.25);//Helper.isSpecialBiome(world,pos)?Config.MYCELIUM_GRASS_SPREAD_NUM_TRIES:constant;
    }
}
