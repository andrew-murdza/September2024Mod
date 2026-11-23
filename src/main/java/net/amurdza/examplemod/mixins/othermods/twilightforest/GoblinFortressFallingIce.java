package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.projectile.FallingIce;

/** Falling ice still lands and hurts entities, then disappears instead of obstructing the estate. */
@Mixin(FallingIce.class)
public abstract class GoblinFortressFallingIce {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean aoemod$keepFortressFloor(Level world, BlockPos pos, BlockState state, int flags) {
        // Native tick discards the projectile after a successful placement.
        // Returning true retains that lifecycle and its preceding fall damage,
        // while the update packet reads the real unchanged block from the world.
        if (world instanceof ServerLevel level && GoblinFortressProtection.boundaryAt(level, pos) != null) return true;
        return world.setBlock(pos, state, flags);
    }
}
