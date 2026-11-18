package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.TropicalBamboo;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class TropicalFinalBamboo {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void aoemod$bambooAfterVegetation(WorldGenLevel level, ChunkAccess chunk,
                                             StructureManager structures, CallbackInfo ci) {
        TropicalBamboo.placeChunk(level, chunk.getPos());
    }
}
