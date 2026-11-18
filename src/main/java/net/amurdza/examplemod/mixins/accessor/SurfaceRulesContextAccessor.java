package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {

    @Accessor("chunk")
    ChunkAccess aoemod$getSurfaceChunk();

    @Accessor("stoneDepthAbove")
    int aoemod$getStoneDepthAbove();

    @Accessor("stoneDepthBelow")
    int aoemod$getStoneDepthBelow();

    @Accessor("waterHeight")
    int aoemod$getWaterHeight();
}
