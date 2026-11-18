package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.PlainsWaterProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ProtoChunk promotion is not a disk load and therefore needs an explicit metadata handoff. */
@Mixin(LevelChunk.class)
public abstract class CopyPlainsWaterProof {
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("RETURN"))
    private void aoemod$copySurfaceWaterProof(ServerLevel level, ProtoChunk proto,
                                              LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci) {
        PlainsWaterProof.copy(proto, (LevelChunk) (Object) this);
    }
}
