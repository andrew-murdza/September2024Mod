package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.worldgen.RainforestSaplingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
public abstract class CustomTrees {
    @Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
    private void aoemod$growRainforestLargeTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
        if (!RainforestSaplingHelper.shouldHandle(level, pos, state)) {
            return;
        }

        ci.cancel();
        RainforestSaplingHelper.advanceTree(level, pos, state, random, level.getChunkSource().getGenerator());
    }
}
