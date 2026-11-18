package net.amurdza.examplemod.mixins.block_behavior.light;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlantLighting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkAccess.class)
public abstract class ChunkPlantLightContext implements TropicalPlantLighting.DimensionContext {
    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

    @Override
    public ResourceKey<Level> aoemod$lightingDimension() {
        return levelHeightAccessor instanceof LevelReader reader ? GameplayDimensions.dimension(reader) : null;
    }
}
