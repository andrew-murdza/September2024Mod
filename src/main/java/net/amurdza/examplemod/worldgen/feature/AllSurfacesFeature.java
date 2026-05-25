package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static net.amurdza.examplemod.worldgen.feature.AllSurfacesFeatureConfig.Target.AIR;

public class AllSurfacesFeature extends Feature<AllSurfacesFeatureConfig> {
    public AllSurfacesFeature(Codec<AllSurfacesFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AllSurfacesFeatureConfig> context) {
        AllSurfacesFeatureConfig cfg = context.config();

        WorldGenLevel level = context.level();
        ChunkAccess chunk = level.getChunk(context.origin());
        ChunkPos chunkPos = chunk.getPos();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        int defaultMaxY = chunk.getMaxBuildHeight() - 1;
        int defaultMinY = chunk.getMinBuildHeight() - 1;

        int maxY = cfg.maxY != null ? cfg.maxY : defaultMaxY;
        int minY = cfg.minY != null ? cfg.minY : defaultMinY;

        if (maxY < minY) {
            return false;
        }

        boolean placedAnything = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = minX + dx;

            for (int dz = 0; dz < 16; dz++) {
                int z = minZ + dz;

                if (cfg.allLayers) {
                    for (int y = maxY; y >= minY; y--) {
                        pos.set(x, y, z);

                        boolean placed = tryPlace(level, pos, context, cfg);
                        placedAnything |= placed;
                    }
                } else {
                    int y = getSurfaceStartY(chunk, cfg, dx, dz);

                    if (y > maxY || y < minY) {
                        continue;
                    }

                    pos.set(x, y, z);

                    boolean placed = tryPlace(level, pos, context, cfg);
                    placedAnything |= placed;
                }
            }
        }

        return placedAnything;
    }

    private int getSurfaceStartY(
            ChunkAccess chunk,
            AllSurfacesFeatureConfig cfg,
            int localX,
            int localZ
    ) {
        Heightmap.Types heightmap = switch (cfg.target) {
            case AIR -> Heightmap.Types.WORLD_SURFACE_WG;
            case WATER, LAVA -> Heightmap.Types.OCEAN_FLOOR_WG;
        };

        int height = chunk.getHeight(heightmap, localX, localZ);

        return height + 1;
    }

    private boolean tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg
    ) {
        if (cfg.biomes != null && !level.getBiome(pos.below()).is(cfg.biomes)) {
            return false;
        }

        Holder<PlacedFeature> selectedFeature = getFeatureForTargetPosition(level, pos, cfg);

        if (selectedFeature == null) {
            return false;
        }

        if (!cfg.predicate.test(level, pos.below())) {
            return false;
        }

        if (cfg.target == AIR) {
            if (!isSafeAirOrigin(level, pos)) {
                return false;
            }
        }

        return selectedFeature.value().place(level, context.chunkGenerator(), context.random(), pos);
    }

    private Holder<PlacedFeature> getFeatureForTargetPosition(
            WorldGenLevel level,
            BlockPos pos,
            AllSurfacesFeatureConfig cfg
    ) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        return switch (cfg.target) {
            case AIR -> {
                boolean validAir =
                        state.isAir()
                                && level.getFluidState(pos).isEmpty()
                                && level.getFluidState(pos.above()).isEmpty();

                if (!validAir) {
                    yield null;
                }

                yield cfg.feature;
            }

            case WATER -> {
                boolean hereWater = state.is(Blocks.WATER);

                if (!hereWater) {
                    yield null;
                }

                boolean aboveWater = above.is(Blocks.WATER);

                if (aboveWater) {
                    yield cfg.deepFeature;
                }

                yield cfg.feature;
            }

            case LAVA -> {
                boolean hereLava = state.is(Blocks.LAVA);

                if (!hereLava) {
                    yield null;
                }

                boolean aboveLava = above.is(Blocks.LAVA);

                if (aboveLava) {
                    yield cfg.deepFeature;
                }

                yield cfg.feature;
            }
        };
    }

    private boolean isSafeAirOrigin(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getFluidState(pos).isEmpty();
    }
}