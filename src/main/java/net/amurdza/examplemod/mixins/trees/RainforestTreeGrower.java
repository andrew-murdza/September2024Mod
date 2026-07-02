package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.worldgen.RainforestSaplingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTreeGrower.class)
public abstract class RainforestTreeGrower {
    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void aoemod$growRainforestLargeTree(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!RainforestSaplingHelper.shouldHandle(level, pos, state)) {
            if (RainforestSaplingHelper.shouldHandleNormalSapling(level, pos, state)) {
                cir.setReturnValue(RainforestSaplingHelper.growNormalSaplingTree(level, pos, state, random, generator));
            }
            return;
        }

        cir.setReturnValue(RainforestSaplingHelper.advanceTree(level, pos, state, random, generator));
    }
}
