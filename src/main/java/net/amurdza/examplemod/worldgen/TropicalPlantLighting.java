package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Dimension-aware emission without reading blocks or consulting a global client world. */
public final class TropicalPlantLighting {
    private TropicalPlantLighting() { }

    public interface DimensionContext {
        ResourceKey<Level> aoemod$lightingDimension();
    }

    public static int emission(BlockState state, BlockGetter view) {
        if (state.is(Blocks.SCULK_CATALYST)) return Math.min(15, state.getLightEmission() + 2);
        if (!state.is(Blocks.TORCHFLOWER)) return state.getLightEmission();
        ResourceKey<Level> dimension = view instanceof LevelReader reader ? GameplayDimensions.dimension(reader)
                : view instanceof DimensionContext context ? context.aoemod$lightingDimension() : null;
        // Forge can scan unbound proto-chunk palettes using BlockPos.ZERO. A
        // conservative nonzero candidate keeps every possible source in the
        // scan; the light engine resolves its emission against the real world.
        if (dimension == null) return 15;
        return GameplayDimensions.TROPICAL.equals(dimension) ? 15 : 0;
    }
}
