package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SaplingBlock.class)
public class SaplingGrowthRate {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos){
        return Helper.nextIntCropsGrow(world,pos,state,random,7);
    }
}
