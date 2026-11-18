package net.amurdza.examplemod.mixins.dimension;

import net.amurdza.examplemod.worldgen.TropicalPlayerSpawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public final class TropicalStartRegion {
    @Redirect(method = "prepareLevels", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel aoemod$prepareTropicalSpawn(MinecraftServer server) {
        return TropicalPlayerSpawn.level(server);
    }
}
