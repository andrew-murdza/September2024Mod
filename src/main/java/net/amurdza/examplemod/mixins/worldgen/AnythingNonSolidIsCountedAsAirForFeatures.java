package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(Feature.class)
public abstract class AnythingNonSolidIsCountedAsAirForFeatures {

    @Inject(
            method = "isAdjacentToAir",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aoemod$isAdjacentToAirOrFluidOrNonSolid(
            Function<BlockPos, BlockState> pAdjacentStateAccessor,
            BlockPos pPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(Feature.checkNeighbors(
                pAdjacentStateAccessor,
                pPos,
                AnythingNonSolidIsCountedAsAirForFeatures::aoemod$countsAsAirForAdjacency
        ));
    }

    @Unique
    private static boolean aoemod$countsAsAirForAdjacency(BlockState state) {
        return state.isAir()
                || !state.getFluidState().isEmpty()
                || !state.blocksMotion();
    }
}