package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.event_handlers.SavannaCampProtection;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reject locked gates before either client prediction or authoritative server use can open them. */
@Mixin(FenceGateBlock.class)
public abstract class LockedEncounterFenceGates {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockUse(BlockState state, Level level, BlockPos pos, Player player,
                              InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> result) {
        if (player.isCreative() || !aoemod$locked(level, pos)) return;
        // Do not change client state: even a single predicted OPEN update flashes the gate and plays its sound.
        // The normal use packet is still sent, letting the server supply the current remaining-enemy message.
        if (level instanceof ServerLevel server) {
            aoemod$close(server, pos, state);
            player.displayClientMessage(SavannaCampProtection.rewardsLocked(server, pos)
                    ? SavannaCampProtection.gatesMessage(server, pos) : MansionProtection.gatesMessage(server, pos), true);
        }
        result.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void aoemod$lockRedstone(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean moving, CallbackInfo ci) {
        if (!aoemod$locked(level, pos)) return;
        if (level instanceof ServerLevel server) aoemod$close(server, pos, state);
        ci.cancel();
    }

    @Unique
    private static boolean aoemod$locked(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server)
            return SavannaCampProtection.rewardsLocked(server, pos) || MansionProtection.rewardsLocked(server, pos);
        return SavannaCampProtectionSnapshot.rewardsLocked(level, pos) || MansionProtectionSnapshot.rewardsLocked(level, pos);
    }

    @Unique
    private static void aoemod$close(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(FenceGateBlock.OPEN) || state.getValue(FenceGateBlock.POWERED))
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false).setValue(FenceGateBlock.POWERED, false), 2);
    }
}
