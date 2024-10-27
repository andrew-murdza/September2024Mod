package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(VineBlock.class)
public class VineGrowthRate {

    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    public int hi(RandomSource pRandom, int i, BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom1){
        return Helper.nextIntCropsGrow(pLevel,pPos,pState,pRandom,i);
    }
}
