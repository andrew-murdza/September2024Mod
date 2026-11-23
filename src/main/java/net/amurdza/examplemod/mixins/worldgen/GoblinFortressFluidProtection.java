package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class GoblinFortressFluidProtection {
    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void aoemod$preserveFortressWater(BlockGetter reader, BlockPos source, BlockState sourceState,
            Direction direction, BlockPos target, BlockState targetState, FluidState fluidState, Fluid fluid,
            CallbackInfoReturnable<Boolean> result) {
        if (reader instanceof ServerLevel level && GoblinFortressProtection.blocksFluid(level, source, target))
            result.setReturnValue(false);
    }
}
