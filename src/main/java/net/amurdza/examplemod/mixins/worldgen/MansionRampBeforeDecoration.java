package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.structure.MansionRampTerrain;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class MansionRampBeforeDecoration {
    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    private void aoemod$gradeMansionApproaches(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        if (level instanceof WorldGenRegion region && structures.shouldGenerateStructures())
            MansionRampTerrain.prepare(region, chunk, (ChunkGenerator) (Object) this);
    }
}
