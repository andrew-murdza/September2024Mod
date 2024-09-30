package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripStoneTickRate {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextFloat()F",ordinal = 0))
    private float nextFloat(RandomSource random, BlockState state, ServerLevel world, BlockPos pos){
        return Helper.nextFloatCropsGrow(world,pos,state,random,0.011377778F);
    }
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextFloat()F",ordinal = 1))
    private float nextFloat1(RandomSource random, BlockState state, ServerLevel world, BlockPos pos){
        return Helper.nextFloatCropsGrow(world,pos,state,random,0.17578125F);
    }
}
