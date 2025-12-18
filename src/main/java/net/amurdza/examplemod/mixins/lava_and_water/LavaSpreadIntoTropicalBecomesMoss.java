package net.amurdza.examplemod.mixins.lava_and_water;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFluid.class)
public abstract class LavaSpreadIntoTropicalBecomesMoss {
    @Shadow
    protected abstract void fizz(LevelAccessor level, BlockPos pos);

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void aoe$spreadToTropicalFizzToMoss(LevelAccessor level, BlockPos pos, BlockState state,
                                                Direction dir, FluidState fluidState,
                                                CallbackInfo ci) {
        // Only when the *spread target position* is in tropical biomes
        if (level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes)) {

            // Place moss instead of the lava that would have been placed here
            level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), 3);

            // Force the fizz effect (sound/particles)
            this.fizz(level, pos);

            // Prevent normal lava placement/spread logic
            ci.cancel();
        }
    }
}

