package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(TurtleEggBlock.class)
public class TurtleEggGrowthRate {


    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/TurtleEggBlock;shouldUpdateHatchLevel(Lnet/minecraft/world/level/Level;)Z"))
    private boolean shouldHatchProgress(TurtleEggBlock instance, Level pLevel, BlockState pState, ServerLevel level, BlockPos pPos, RandomSource pRandom){
        float f = pLevel.getTimeOfDay(1.0F);
        return Helper.nextFloatCropsGrow(pLevel,pPos,pState,pRandom,(double)f < 0.69D && (double)f > 0.65D?1:0.002)==0;
    }


}
