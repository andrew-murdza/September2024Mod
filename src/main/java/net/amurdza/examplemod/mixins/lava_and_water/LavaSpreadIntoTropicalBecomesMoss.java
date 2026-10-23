package net.amurdza.examplemod.mixins.lava_and_water;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFluid.class)
public abstract class LavaSpreadIntoTropicalBecomesMoss {
    @Shadow
    protected abstract void fizz(LevelAccessor level, BlockPos pos);

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void aoe$spreadToTropicalFizzToMoss(LevelAccessor level, BlockPos pos, BlockState state,
                                                Direction dir, FluidState fluidState,
                                                CallbackInfo ci) {
        if (GameplayDimensions.isCustom(level) && !GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && !GameplayDimensions.is(level, GameplayDimensions.DESERT) && !GameplayDimensions.is(level, GameplayDimensions.NETHER) && !GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            ci.cancel();
            return;
        }
        // Tropical lava converts to moss; the other non-lava dimensions reject spread.
        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {

            // Place moss instead of the lava that would have been placed here
            level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), 3);

            // Force the fizz effect (sound/particles)
            this.fizz(level, pos);

            // Prevent normal lava placement/spread logic
            ci.cancel();
        }
    }
}

