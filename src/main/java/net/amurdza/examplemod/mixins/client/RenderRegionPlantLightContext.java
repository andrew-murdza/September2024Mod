package net.amurdza.examplemod.mixins.client;

import net.amurdza.examplemod.worldgen.TropicalPlantLighting;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderChunkRegion.class)
public abstract class RenderRegionPlantLightContext implements TropicalPlantLighting.DimensionContext {
    @Shadow @Final protected Level level;

    @Override
    public ResourceKey<Level> aoemod$lightingDimension() { return level.dimension(); }
}
