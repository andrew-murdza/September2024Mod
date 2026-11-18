package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.event_handlers.SavannaCampProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class SavannaCampBlockDestruction {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void aoemod$protectDestruction(BlockPos pos, boolean drops, Entity actor, int recursion, CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof ServerLevel level && !SavannaCampProtection.creativeBreak()
                && !(actor instanceof Player player && player.isCreative())
                && net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks.protectedBlock(level, pos)) result.setReturnValue(false);
    }
    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true)
    private void aoemod$protectRemoval(BlockPos pos, boolean moving, CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof ServerLevel level && !SavannaCampProtection.creativeBreak()
                && net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks.protectedBlock(level, pos)) result.setReturnValue(false);
    }
}
