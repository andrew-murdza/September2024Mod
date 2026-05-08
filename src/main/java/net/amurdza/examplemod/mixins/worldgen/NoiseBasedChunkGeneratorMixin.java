package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * Optional: narrows the vanilla-style global lava aquifer to only -64..-54.
 * This does not create a floor under every lava pocket; that requires density changes
 * or post-processing. Keep this mixin only if you still want lava aquifers in that band.
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(
            method = "getInterpolatedState",
            at = @At("RETURN"),
            cancellable = true
    )
    private void aoemod$removeGeneratedLava(CallbackInfoReturnable<@Nullable BlockState> cir) {
        BlockState state = cir.getReturnValue();

        if (state != null && state.is(Blocks.LAVA)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
