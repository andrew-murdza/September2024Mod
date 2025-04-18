package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MangrovePropaguleBlock.class)
public class MangrovePropaguleGrowthRate {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos){
        return Helper.nextIntCropsGrow(world,pos,state,random,n);
    }
}
