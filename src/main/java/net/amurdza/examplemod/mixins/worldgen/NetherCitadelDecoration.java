package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.NetherCitadelPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Later ores, lakes, trees and vegetation cannot replace the authored fortress or its air. */
@Mixin(WorldGenRegion.class)
public abstract class NetherCitadelDecoration {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$preserveCitadel(BlockPos pos, BlockState state, int flags, int recursion,
                                       CallbackInfoReturnable<Boolean> result) {
        if (!NetherCitadelPiece.building() && NetherCitadelPiece.reserves((WorldGenRegion) (Object) this, pos))
            result.setReturnValue(false);
    }
}
