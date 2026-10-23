package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class NetherRampDecoration {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$keepRampPassage(BlockPos pos, BlockState state, int flags, int recursion,
                                       CallbackInfoReturnable<Boolean> result) {
        if (!NetherRampDecorationGuard.allows((WorldGenRegion) (Object) this, pos, state)) result.setReturnValue(false);
    }
}
