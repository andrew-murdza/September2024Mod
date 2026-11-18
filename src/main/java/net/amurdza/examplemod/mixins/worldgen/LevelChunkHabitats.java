package net.amurdza.examplemod.mixins.worldgen;

import java.util.function.Consumer;
import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkHabitats {
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("RETURN"))
    private void aoemod$promote(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci) {
        byte[] saved = SurfaceHabitatColumns.data(proto).aoemod$getHabitats();
        if (saved != null) ((SurfaceHabitatColumns) this).aoemod$setHabitats(saved);
        else SurfaceHabitatColumns.capture(level, (LevelChunk) (Object) this, true);
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void aoemod$receive(FriendlyByteBuf buf, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> entities, CallbackInfo ci) {
        SurfaceHabitatColumns.read((LevelChunk) (Object) this, tag);
    }
}
