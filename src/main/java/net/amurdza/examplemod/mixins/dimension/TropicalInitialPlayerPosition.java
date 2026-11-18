package net.amurdza.examplemod.mixins.dimension;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlayerSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public final class TropicalInitialPlayerPosition {
    @Inject(method = "fudgeSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void aoemod$safeTropicalStart(ServerLevel level, CallbackInfo callback) {
        if (level.dimension().equals(GameplayDimensions.TROPICAL)) {
            BlockPos pos = TropicalPlayerSpawn.safePosition(level);
            ((ServerPlayer) (Object) this).moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            callback.cancel();
        }
    }
}
