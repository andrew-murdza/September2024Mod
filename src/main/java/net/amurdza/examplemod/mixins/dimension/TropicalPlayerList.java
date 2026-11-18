package net.amurdza.examplemod.mixins.dimension;

import net.amurdza.examplemod.worldgen.TropicalPlayerSpawn;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public final class TropicalPlayerList {
    @Shadow @Final private MinecraftServer server;

    @Redirect(method = {"getPlayerForLogin", "placeNewPlayer", "respawn"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel aoemod$tropicalFallback(MinecraftServer server) {
        // respawn reaches this fallback only after checking the player's saved
        // bed/anchor. Saved login dimensions are still decoded by vanilla.
        return TropicalPlayerSpawn.level(server);
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/Level;OVERWORLD:Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> aoemod$newPlayerDimension() {
        return TropicalPlayerSpawn.level(server).dimension();
    }
}
