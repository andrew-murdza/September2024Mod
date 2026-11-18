package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlantIntegrity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrowingPlantBlock.class)
public class TropicalDetachedVines {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aoemod$removeUnsupportedVinesWithoutLoot(BlockState state, ServerLevel level,
                                                        BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL) && TropicalPlantIntegrity.isCaveVine(state)
                && !state.canSurvive(level, pos)) {
            TropicalPlantIntegrity.clearDetachedVines(level, pos);
            ci.cancel();
        }
    }
}
