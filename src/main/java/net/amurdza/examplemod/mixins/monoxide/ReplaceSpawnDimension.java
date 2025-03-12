package net.amurdza.examplemod.mixins.monoxide;

import net.amurdza.examplemod.worldgen.dimension.ModDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public class ReplaceSpawnDimension {
  // Not needed since we adjust the actual cost below
  // It would be ideal to modify the code that removes the lapis instead of doing this, but that code is inside of a lambda and seems impossible to target with a mixin
  @ModifyVariable(
    method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
    name = "resourcekey",
    at = @At(
      value = "STORE"
    )
  )
  private ResourceKey<Level> remove20Lapis(final ResourceKey<Level> resourcekey) {
    return ModDimensions.AOEDIM_LEVEL_KEY;
  }
}
