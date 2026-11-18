package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.TropicalPlantIntegrity;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class TropicalPlantReplacementDuringGeneration {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$preserveMangroveRoots(BlockPos pos, BlockState state, int flags, int recursion,
                                            CallbackInfoReturnable<Boolean> cir) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        if (net.amurdza.examplemod.util.GameplayDimensions.is(region, net.amurdza.examplemod.util.GameplayDimensions.TROPICAL)
                && region.ensureCanWrite(pos)
                && TropicalPlantIntegrity.protectsRoot(region, region.getBlockState(pos), state)) cir.setReturnValue(false);
    }

    @Redirect(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState aoemod$clearDependentPlants(ChunkAccess chunk, BlockPos pos, BlockState state, boolean moving) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        BlockState oldState = chunk.setBlockState(pos, state, moving);
        if (oldState != null) {
            TropicalPlantIntegrity.afterReplacement(region, pos, oldState, state);
            CaveVinePlacement.trimForGeneratedObstacle(region, pos, oldState, state);
        }
        return oldState;
    }
}
