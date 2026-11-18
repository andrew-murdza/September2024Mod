package net.amurdza.examplemod.mixins.dimension;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlayerSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class TropicalSharedSpawn {
    @Inject(method = "getSharedSpawnPos", at = @At("HEAD"), cancellable = true)
    private void aoemod$tropicalSpawn(CallbackInfoReturnable<BlockPos> result) {
        if ((Object) this instanceof ServerLevel level && level.dimension().equals(GameplayDimensions.TROPICAL)) {
            // DerivedLevelData delegates to the Overworld and ignores setters;
            // the tropical position therefore needs its own persistent data.
            result.setReturnValue(TropicalPlayerSpawn.position(level));
        }
    }

    @Inject(method = "getSharedSpawnAngle", at = @At("HEAD"), cancellable = true)
    private void aoemod$tropicalSpawnAngle(CallbackInfoReturnable<Float> result) {
        if ((Object) this instanceof ServerLevel level && level.dimension().equals(GameplayDimensions.TROPICAL)) {
            result.setReturnValue(0.0F);
        }
    }
}
