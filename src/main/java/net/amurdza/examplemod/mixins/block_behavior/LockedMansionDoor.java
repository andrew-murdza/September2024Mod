package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoorBlock.class)
public abstract class LockedMansionDoor {
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockDoor(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos source,
                                 boolean moving, CallbackInfo callback) {
        if (state.is(Blocks.IRON_DOOR) && level instanceof ServerLevel server) {
            var basement = MansionProtection.door(server, pos);
            if (basement != null && basement.layoutVersion() < 11) callback.cancel();
        }
    }
}
