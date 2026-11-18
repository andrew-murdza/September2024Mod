package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SavannaCampTrees;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class SavannaFinalCampTrees {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void aoemod$fillCampLawns(WorldGenLevel level, ChunkAccess chunk,
                                    StructureManager structures, CallbackInfo callback) {
        SavannaCampTrees.placeNatural(level, chunk.getPos(), structures);
    }
}
