package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class TropicalChannelSurfaceNormalization {
    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
            at = @At("TAIL"))
    private void aoemod$matchChannelDepthToSurfaceBiome(WorldGenRegion region, StructureManager structures,
                                                       RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        if (region.getLevel().dimension().equals(GameplayDimensions.TROPICAL)) {
            TropicalChannelSurface.normalize(chunk, region.getBiomeManager(),
                    DimensionTerrain.tropicalLayout(random.router().temperature()));
        }
    }
}
