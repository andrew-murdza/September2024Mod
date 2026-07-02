package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.worldgen.RainforestSaplingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMegaTreeGrower.class)
public abstract class RegularSpruceSaplingsUseConfiguredTree {
    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void aoemod$useConfiguredRegularTree(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (RainforestSaplingHelper.shouldHandleNormalSapling(level, pos, state)) {
            cir.setReturnValue(RainforestSaplingHelper.growNormalSaplingTree(level, pos, state, random, generator));
        }
    }
}
