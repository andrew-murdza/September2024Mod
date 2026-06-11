package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig.Target.AIR;

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

        placedAnything |= placeGuaranteedFeatures(context, cfg, chunkPos, minY, maxY);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = minX + dx;

            for (int dz = 0; dz < 16; dz++) {
                int z = minZ + dz;

                if (cfg.allLayers) {
                    int y = maxY;

                    while (y >= minY) {
                        pos.set(x, y, z);

                        PlacementResult result = tryPlace(level, pos, context, cfg);
                        placedAnything |= result.placedAnything();

                        if (result.foundLayer() && cfg.skipHeight > 0) {
                            y -= cfg.skipHeight + 1;
                        } else {
                            y--;
                        }
                    }
                } else {
                    int y = getSurfaceStartY(chunk, cfg, dx, dz);

                    if (y > maxY || y < minY) {
                        continue;
                    }

                    pos.set(x, y, z);

                    PlacementResult result = tryPlace(level, pos, context, cfg);
                    placedAnything |= result.placedAnything();
                }
            }
        }

        return placedAnything;
    }

    private boolean placeGuaranteedFeatures(
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            ChunkPos chunkPos,
            int minY,
            int maxY
    ) {
        if (cfg.guaranteedFeatures.isEmpty()) {
            return false;
        }

        boolean placedAnything = false;

        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        PlacementContext placementContext = new PlacementContext(
                level,
                context.chunkGenerator(),
                Optional.empty()
        );

        BlockPos placementOrigin = new BlockPos(minX, maxY, minZ);

        for (Holder<PlacedFeature> guaranteedFeatureHolder : cfg.guaranteedFeatures) {
            PlacedFeature placedFeature = guaranteedFeatureHolder.value();

            List<BlockPos> candidatePositions = applyPlacementModifiers(
                    placementContext,
                    random,
                    placementOrigin,
                    placedFeature
            );

            for (BlockPos candidatePos : candidatePositions) {
                int x = candidatePos.getX();
                int z = candidatePos.getZ();

                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                    continue;
                }

                placedAnything |= placeGuaranteedFeatureInColumn(
                        level,
                        context,
                        cfg,
                        placedFeature.feature(),
                        x,
                        z,
                        minY,
                        maxY
                );
            }
        }

        return placedAnything;
    }

    private List<BlockPos> applyPlacementModifiers(
            PlacementContext placementContext,
            RandomSource random,
            BlockPos origin,
            PlacedFeature placedFeature
    ) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(origin);

        for (PlacementModifier modifier : placedFeature.placement()) {
            List<BlockPos> nextPositions = new ArrayList<>();

            for (BlockPos pos : positions) {
                modifier.getPositions(placementContext, random, pos).forEach(nextPositions::add);
            }

            positions = nextPositions;

            if (positions.isEmpty()) {
                break;
            }
        }

        return positions;
    }

    private boolean placeGuaranteedFeatureInColumn(
            WorldGenLevel level,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            Holder<ConfiguredFeature<?, ?>> configuredFeature,
            int x,
            int z,
            int minY,
            int maxY
    ) {
        boolean placedAnything = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (cfg.allLayers) {
            int y = maxY;

            while (y >= minY) {
                pos.set(x, y, z);

                PlacementResult result = tryPlaceGuaranteedFeatureAt(
                        level,
                        pos,
                        context,
                        cfg,
                        configuredFeature
                );

                placedAnything |= result.placedAnything();

                if (result.foundLayer()) {
                    y -= Math.max(1, cfg.skipHeight);
                } else {
                    y--;
                }
            }
        } else {
            ChunkAccess chunk = level.getChunk(pos.set(x, maxY, z));
            int localX = x & 15;
            int localZ = z & 15;

            int y = getSurfaceStartY(chunk, cfg, localX, localZ);

            if (y >= minY && y <= maxY) {
                pos.set(x, y, z);

                PlacementResult result = tryPlaceGuaranteedFeatureAt(
                        level,
                        pos,
                        context,
                        cfg,
                        configuredFeature
                );

                placedAnything |= result.placedAnything();
            }
        }

        return placedAnything;
    }

    private PlacementResult tryPlaceGuaranteedFeatureAt(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            Holder<ConfiguredFeature<?, ?>> configuredFeature
    ) {
        if (cfg.biomes != null && !level.getBiome(pos.below()).is(cfg.biomes)) {
            return PlacementResult.NONE;
        }

        if (!isValidGuaranteedFeatureOrigin(level, pos, cfg)) {
            return PlacementResult.NONE;
        }

        if (!cfg.predicate.test(level, pos.below())) {
            return PlacementResult.NONE;
        }

        if (cfg.target == AIR && !isSafeAirOrigin(level, pos)) {
            return PlacementResult.NONE;
        }

        boolean placedAnything = configuredFeature.value().place(
                level,
                context.chunkGenerator(),
                context.random(),
                pos
        );

        return new PlacementResult(true, placedAnything);
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

    private PlacementResult tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg
    ) {
        if (cfg.biomes != null && !level.getBiome(pos.below()).is(cfg.biomes)) {
            return PlacementResult.NONE;
        }

        Holder<PlacedFeature> selectedFeature = getFeatureForTargetPosition(level, pos, cfg);

        if (selectedFeature == null) {
            return PlacementResult.NONE;
        }

        if (!cfg.predicate.test(level, pos.below())) {
            return PlacementResult.NONE;
        }

        if (cfg.target == AIR && !isSafeAirOrigin(level, pos)) {
            return PlacementResult.NONE;
        }

        boolean placedAnything = selectedFeature.value().place(
                level,
                context.chunkGenerator(),
                context.random(),
                pos
        );

        return new PlacementResult(true, placedAnything);
    }

    private boolean isValidGuaranteedFeatureOrigin(
            WorldGenLevel level,
            BlockPos pos,
            AllSurfacesFeatureConfig cfg
    ) {
        BlockState state = level.getBlockState(pos);

        return switch (cfg.target) {
            case AIR -> state.isAir()
                    && level.getFluidState(pos).isEmpty()
                    && level.getFluidState(pos.above()).isEmpty();

            case WATER -> state.is(Blocks.WATER);

            case LAVA -> state.is(Blocks.LAVA);
        };
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

    private record PlacementResult(boolean foundLayer, boolean placedAnything) {
        private static final PlacementResult NONE = new PlacementResult(false, false);
    }
}