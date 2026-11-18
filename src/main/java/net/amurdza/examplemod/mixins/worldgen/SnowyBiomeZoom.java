package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyBiomeZoomState;
import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeManager.class)
public abstract class SnowyBiomeZoom implements SnowyBiomeZoomState {
    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;
    @Unique private Level aoemod$habitatLevel;
    @Unique private WorldGenRegion aoemod$region;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$recognizeLevel(BiomeManager.NoiseBiomeSource source, long seed, CallbackInfo ci) {
        if (source instanceof Level level) aoemod$setHabitatLevel(level, null);
        else if (source instanceof WorldGenRegion region) aoemod$setHabitatLevel(region.getLevel(), region);
    }

    @Override
    public void aoemod$setHabitatLevel(Level level, WorldGenRegion region) {
        if (level != null && GameplayDimensions.isCustom(level) && !level.dimension().equals(GameplayDimensions.TROPICAL)) {
            aoemod$habitatLevel = level;
            aoemod$region = region;
        }
    }

    @Inject(method = "withDifferentSource", at = @At("RETURN"))
    private void aoemod$inheritScope(BiomeManager.NoiseBiomeSource source, CallbackInfoReturnable<BiomeManager> result) {
        if (!(source instanceof Level) && !(source instanceof WorldGenRegion))
            ((SnowyBiomeZoomState) result.getReturnValue()).aoemod$setHabitatLevel(aoemod$habitatLevel, aoemod$region);
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void aoemod$exactHabitat(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> result) {
        var level = aoemod$habitatLevel;
        if (level == null) return;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (SurfaceHabitatColumns.mapped(level.dimension()) && SurfaceHabitatColumns.surfaceHabitat(level.dimension(), y)
                && (aoemod$region != null || noiseBiomeSource == level)) {
            var chunk = aoemod$region == null ? level.getChunkSource().getChunkNow(x >> 4, z >> 4)
                    : aoemod$region.getChunk(x >> 4, z >> 4, ChunkStatus.NOISE, false);
            if (chunk != null) {
                byte[] columns = SurfaceHabitatColumns.data(chunk).aoemod$getHabitats();
                if (columns != null) {
                    result.setReturnValue(SurfaceHabitatColumns.biome(level, columns[(z & 15) * 16 + (x & 15)]));
                    return;
                }
            }
        }
        result.setReturnValue(noiseBiomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
    }
}
