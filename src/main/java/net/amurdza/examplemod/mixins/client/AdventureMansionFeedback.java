package net.amurdza.examplemod.mixins.client;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class AdventureMansionFeedback {
    // Adventure mode normally stops locally before the server can explain the mansion lock.
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aoemod$reportRestrictedHit(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> result) {
        var client = Minecraft.getInstance();
        if (client.player != null && client.level != null && !client.player.isCreative()
                && (MansionProtectionSnapshot.protects(client.level, pos)
                || net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.protects(client.level, pos))) {
            client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face));
            result.setReturnValue(false);
            return;
        }
        if (client.player != null && client.level != null && client.level.dimension().equals(GameplayDimensions.PLAINS)
                && ((MultiPlayerGameMode) (Object) this).getPlayerMode() == GameType.ADVENTURE
                && client.player.blockActionRestricted(client.level, pos, GameType.ADVENTURE)) {
            client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face));
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aoemod$preventProtectedCracks(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> result) {
        var client = Minecraft.getInstance();
        if (client.player != null && client.level != null && !client.player.isCreative()
                && (MansionProtectionSnapshot.protects(client.level, pos)
                || net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.protects(client.level, pos))) {
            ((MultiPlayerGameMode) (Object) this).stopDestroyBlock();
            result.setReturnValue(false);
        }
    }
}
