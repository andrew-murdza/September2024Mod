package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops a player from losing XP when they die */
@Mixin(ServerPlayer.class)
public class RestoreXpOnRespawn {
  @Inject(method = "restoreFrom", at = @At("RETURN"))
  private void restoreXpOnRespawn(final ServerPlayer oldPlayer, final boolean keepEverything, final CallbackInfo info) {
    final ServerPlayer player = (ServerPlayer)(Object)this;

    player.experienceLevel = oldPlayer.experienceLevel;
    player.totalExperience = oldPlayer.totalExperience;
    player.experienceProgress = oldPlayer.experienceProgress;
    player.setScore(oldPlayer.getScore());
  }
}
