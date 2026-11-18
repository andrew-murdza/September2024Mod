package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.worldgen.SnowyOceanFloor;
import net.amurdza.examplemod.worldgen.SnowyCaveColumns;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SnowyLakeCaveExclusion {
    @Shadow @Final private Holder<NoiseGeneratorSettings> settings;

    @Unique
    private boolean aoemod$isSnowy() {
        return settings.unwrapKey().map(key -> key.location().equals(
                new ResourceLocation("aoemod", "snowy_dimension"))).orElse(false);
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("RETURN"))
    private void aoemod$rememberSurface(WorldGenRegion region, StructureManager structures, RandomState random,
                                        ChunkAccess chunk, CallbackInfo ci) {
        if (region.getLevel().dimension().equals(net.amurdza.examplemod.util.GameplayDimensions.DESERT))
            net.amurdza.examplemod.worldgen.DesertLakeSurface.fillWater(chunk, random, (NoiseBasedChunkGenerator) (Object) this);
        net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.capture(region.getLevel(), chunk, false);
    }

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void aoemod$finishOceanAndCaves(Executor executor, Blender blender, RandomState random,
                                          StructureManager structures, ChunkAccess chunk,
                                          CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (!aoemod$isSnowy()) return;
        // Vanilla's returned future includes its section-release callback. Normal
        // block writes are safe only after that callback, not inside doFill where
        // section palettes remain acquired. Downstream stages await this repair.
        cir.setReturnValue(cir.getReturnValue().thenApply(filled -> {
            SnowyOceanFloor.restoreCoverAndWater(filled, random, settings.value().defaultBlock());
            SnowyCaveColumns.finish(filled, random, settings.value().defaultBlock());
            net.amurdza.examplemod.worldgen.SnowyCavePockets.remove((NoiseBasedChunkGenerator) (Object) this,
                    filled, random, settings.value().defaultBlock());
            return filled;
        }));
    }
}
