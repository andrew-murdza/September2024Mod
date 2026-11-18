package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class, WorldGenRegion.class})
public abstract class SavannaCampVegetationWrites {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$preserveAuthoredColumns(BlockPos pos, BlockState state, int flags, int recursion,
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof WorldGenLevel level && !SavannaCampVegetation.allows(level, pos)) {
            callback.setReturnValue(false);
        }
    }
}
