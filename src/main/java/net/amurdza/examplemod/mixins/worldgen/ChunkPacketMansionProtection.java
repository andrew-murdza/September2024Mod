package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundLevelChunkPacketData.class)
public abstract class ChunkPacketMansionProtection {
    @Shadow @Final private CompoundTag heightmaps;

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("RETURN"))
    private void aoemod$send(LevelChunk chunk, CallbackInfo callback) {
        MansionProtectionSnapshot.write(chunk, heightmaps);
        net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.write(chunk, heightmaps);
    }
}
