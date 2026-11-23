package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FenceGateBlock.class)
public abstract class GoblinFortressGateProtection {
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockFortressPens(BlockState state, Level world, BlockPos pos, Block neighbor,
            BlockPos neighborPos, boolean moving, CallbackInfo callback) {
        if (world instanceof ServerLevel level && GoblinFortressProtection.rewardsLocked(level, pos)) {
            if (state.getValue(FenceGateBlock.OPEN) || state.getValue(FenceGateBlock.POWERED))
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false).setValue(FenceGateBlock.POWERED, false), 2);
            callback.cancel();
        }
    }
}
