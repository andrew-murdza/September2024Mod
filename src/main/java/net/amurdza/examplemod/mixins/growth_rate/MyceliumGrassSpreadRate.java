package net.amurdza.examplemod.mixins.growth_rate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class MyceliumGrassSpreadRate extends Block  {
    public MyceliumGrassSpreadRate(Properties pProperties) {
        super(pProperties);
    }

    @ModifyConstant(method = "randomTick", constant = @Constant(intValue = 4))
    public int randomTick1(int constant, BlockState state, ServerLevel world, BlockPos pos, RandomSource random){
        return 4;
    }

}
