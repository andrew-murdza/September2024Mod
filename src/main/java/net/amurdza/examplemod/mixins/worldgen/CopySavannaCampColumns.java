package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class CopySavannaCampColumns {
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("RETURN"))
    private void aoemod$copyCampColumns(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor postLoad,
            CallbackInfo ci) {
        SavannaCampColumns.copy(proto, (LevelChunk) (Object) this);
    }
}
