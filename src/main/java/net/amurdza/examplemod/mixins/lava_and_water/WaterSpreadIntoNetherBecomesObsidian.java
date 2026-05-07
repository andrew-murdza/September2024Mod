package net.amurdza.examplemod.mixins.lava_and_water;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class WaterSpreadIntoNetherBecomesObsidian {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void aoe$spreadToNetherFizzToObsidian(LevelAccessor level, BlockPos pos, BlockState state,
                                                  Direction dir, FluidState fluidState,
                                                  CallbackInfo ci) {
        if (level.getBiome(pos).is(ModTags.Biomes.netherBiomes) && fluidState.is(Fluids.FLOWING_WATER)) {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.levelEvent(1501, pos, 0);
            ci.cancel();
        }
    }
}