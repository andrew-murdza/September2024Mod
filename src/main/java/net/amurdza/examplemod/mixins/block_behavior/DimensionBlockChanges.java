package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers dispensers, pistons, growth, commands and direct event placement as well as BlockItem. */
@Mixin({Level.class, WorldGenRegion.class})
public class DimensionBlockChanges {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$enforceDimension(BlockPos pos, BlockState state, int flags, int recursion,
                                        CallbackInfoReturnable<Boolean> cir) {
        LevelReader level = (LevelReader) (Object) this;
        if (GameplayDimensions.isCustom(level) && !state.isAir()
                && !BiomePlacementRestrictions.isAllowed(level, pos, state)) cir.setReturnValue(false);
    }
}
