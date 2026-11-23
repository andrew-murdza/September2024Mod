package net.amurdza.examplemod.mixins.client;

import net.amurdza.examplemod.event_handlers.ClientNetherCitadelMining;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class NetherCitadelMiningFeedback {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aoemod$citadelMiningStart(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> result) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.player.isCreative()
                || !GameplayDimensions.is(client.level, GameplayDimensions.NETHER)) return;
        if (NetherCitadelProtectionSnapshot.protects(client.level, pos)) {
            if (ClientNetherCitadelMining.shouldExplainBlocked(client.level)) client.player.connection.send(
                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face));
            result.setReturnValue(false);
            return;
        }
        ClientNetherCitadelMining.recordAllowedCropHit(client.level, pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aoemod$citadelMiningContinue(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> result) {
        var client = Minecraft.getInstance();
        if (client.player != null && client.level != null && !client.player.isCreative()
                && NetherCitadelProtectionSnapshot.protects(client.level, pos)) {
            ((MultiPlayerGameMode) (Object) this).stopDestroyBlock();
            result.setReturnValue(false);
        }
    }
}
