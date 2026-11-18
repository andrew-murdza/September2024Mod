package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class ChunkHabitatStorage implements SurfaceHabitatColumns {
    @Unique private volatile byte[] aoemod$habitats;

    @Override public byte[] aoemod$getHabitats() { return aoemod$habitats; }
    @Override public void aoemod$setHabitats(byte[] habitats) { aoemod$habitats = habitats; }
}
