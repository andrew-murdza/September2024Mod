package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BuddingAmethystBlock.class)
public class AmethystGrowthRate {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    public int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos, RandomSource random1) {
        return Helper.nextIntCropsGrow(world,pos,state,random,n);
    }
}
