package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMansionProtection {
    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void aoemod$receive(FriendlyByteBuf buffer, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> entities, CallbackInfo callback) {
        MansionProtectionSnapshot.read((LevelChunk) (Object) this, tag);
        net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.read((LevelChunk) (Object) this, tag);
    }
}
