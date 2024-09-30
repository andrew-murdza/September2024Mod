package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(VineBlock.class)
public class VineGrowth {
    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/core/Direction;getRandom(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/core/Direction;"))
    public Direction hi(RandomSource pRandom){
        return Direction.DOWN;
    }
    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    public int hi(RandomSource pRandom, int i, BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom1){
        return Helper.nextIntCropsGrow(pLevel,pPos,pState,pRandom,i);
    }
}
