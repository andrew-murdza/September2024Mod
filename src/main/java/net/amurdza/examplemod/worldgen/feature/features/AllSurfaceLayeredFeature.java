package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfaceLayeredFeaturesConfig;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig.Target.AIR;

public class AllSurfaceLayeredFeature extends Feature<AllSurfaceLayeredFeaturesConfig> {

    public AllSurfaceLayeredFeature(Codec<AllSurfaceLayeredFeaturesConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context) {
        AllSurfaceLayeredFeaturesConfig layeredCfg = context.config();

        if (layeredCfg.features.isEmpty()) {
            return false;
        }

        WorldGenLevel level = context.level();
        ChunkAccess chunk = level.getChunk(context.origin());
        ChunkPos chunkPos = chunk.getPos();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        int defaultMaxY = chunk.getMaxBuildHeight() - 1;
        int defaultMinY = chunk.getMinBuildHeight() - 1;

        int globalMaxY = getGlobalMaxY(layeredCfg, defaultMaxY);
        int globalMinY = getGlobalMinY(layeredCfg, defaultMinY);

        if (globalMaxY < globalMinY) {
            return false;
        }

        boolean placedAnything = false;

        placedAnything |= placeGuaranteedFeatures(
                context,
                layeredCfg,
                chunkPos,
                defaultMinY,
                defaultMaxY
        );

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = minX + dx;

            for (int dz = 0; dz < 16; dz++) {
                int z = minZ + dz;

                placedAnything |= placeRegularFeaturesInColumn(
                        level,
                        chunk,
                        context,
                        layeredCfg,
                        pos,
                        x,
                        z,
                        dx,
                        dz,
                        globalMinY,
                        globalMaxY,
                        defaultMinY,
                        defaultMaxY
                );
            }
        }

        return placedAnything;
    }

    private PlacementResult tryPlaceGuaranteedFeatureNearY(
            WorldGenLevel level,
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
            AllSurfacesFeatureConfig cfg,
            Holder<ConfiguredFeature<?, ?>> configuredFeature,
            int x,
            int y,
            int z,
            int minY,
            int maxY
    ) {
        int maxOffset = Math.max(Mth.ceil(cfg.skipHeight / 2.0F), 1);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= maxOffset; i++) {
            if (i == 0) {
                if (y >= minY && y <= maxY) {
                    pos.set(x, y, z);

                    PlacementResult result = tryPlaceGuaranteedFeatureAt(
                            level,
                            pos,
                            context,
                            cfg,
                            configuredFeature
                    );

                    if (result.foundLayer()) {
                        return result;
                    }
                }

                continue;
            }

            int upY = y + i;
            if (upY <= maxY) {
                pos.set(x, upY, z);

                PlacementResult result = tryPlaceGuaranteedFeatureAt(
                        level,
                        pos,
                        context,
                        cfg,
                        configuredFeature
                );

                if (result.foundLayer()) {
                    return result;
                }
            }

            int downY = y - i;
            if (downY >= minY) {
                pos.set(x, downY, z);

                PlacementResult result = tryPlaceGuaranteedFeatureAt(
                        level,
                        pos,
                        context,
                        cfg,
                        configuredFeature
                );

                if (result.foundLayer()) {
                    return result;
                }
            }
        }

        return PlacementResult.NONE;
    }

    private boolean placeRegularFeaturesInColumn(
            WorldGenLevel level,
            ChunkAccess chunk,
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
            AllSurfaceLayeredFeaturesConfig layeredCfg,
            BlockPos.MutableBlockPos pos,
            int x,
            int z,
            int localX,
            int localZ,
            int globalMinY,
            int globalMaxY,
            int defaultMinY,
            int defaultMaxY
    ) {
        boolean placedAnything = false;

        /*
         * Single-surface configs are still handled independently, because they do not
         * vertically scan through layers.
         */
        for (AllSurfacesFeatureConfig cfg : layeredCfg.features) {
            if (cfg.allLayers) {
                continue;
            }

            int maxY = getMaxY(cfg, defaultMaxY);
            int minY = getMinY(cfg, defaultMinY);

            if (maxY < minY) {
                continue;
            }

            int y = getSurfaceStartY(chunk, cfg, localX, localZ);

            if (y < minY || y > maxY) {
                continue;
            }

            pos.set(x, y, z);

            PlacementResult result = tryPlace(level, pos, context, cfg);
            placedAnything |= result.placedAnything();
        }

        /*
         * all_layers configs share one vertical scan.
         *
         * This is the important difference from running several separate
         * AllSurfacesFeature placements. If one config finds a layer and skips
         * downward, that skip carries into whatever config owns the lower Y range.
         */
        int y = globalMaxY;

        while (y >= globalMinY) {
            pos.set(x, y, z);

            PlacementResult result = tryPlaceAnyLayeredConfigAt(
                    level,
                    pos,
                    context,
                    layeredCfg,
                    y,
                    defaultMinY,
                    defaultMaxY
            );

            placedAnything |= result.placedAnything();

            if (result.foundLayer() && result.skipHeight() > 0) {
                y -= result.skipHeight();
            } else {
                y--;
            }
        }

        return placedAnything;
    }

    private PlacementResult tryPlaceAnyLayeredConfigAt(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
            AllSurfaceLayeredFeaturesConfig layeredCfg,
            int y,
            int defaultMinY,
            int defaultMaxY
    ) {
        for (AllSurfacesFeatureConfig cfg : layeredCfg.features) {
            if (!cfg.allLayers) {
                continue;
            }

            if (!isInYRange(cfg, y, defaultMinY, defaultMaxY)) {
                continue;
            }

            PlacementResult result = tryPlace(level, pos, context, cfg);

            if (result.foundLayer()) {
                return result;
            }
        }

        return PlacementResult.NONE;
    }

    private boolean placeGuaranteedFeatures(
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
            AllSurfaceLayeredFeaturesConfig layeredCfg,
            ChunkPos chunkPos,
            int defaultMinY,
            int defaultMaxY
    ) {
        boolean placedAnything = false;

        for (AllSurfacesFeatureConfig cfg : layeredCfg.features) {
            placedAnything |= placeGuaranteedFeaturesForConfig(
                    context,
                    cfg,
                    chunkPos,
                    defaultMinY,
                    defaultMaxY
            );
        }

        return placedAnything;
    }

    private boolean placeGuaranteedFeaturesForConfig(
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
            AllSurfacesFeatureConfig cfg,
            ChunkPos chunkPos,
            int defaultMinY,
            int defaultMaxY
    ) {
        if (cfg.guaranteedFeatures.isEmpty()) {
            return false;
        }

        int minY = getMinY(cfg, defaultMinY);
        int maxY = getMaxY(cfg, defaultMaxY);

        if (maxY < minY) {
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
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
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
                PlacementResult result = tryPlaceGuaranteedFeatureNearY(
                        level,
                        context,
                        cfg,
                        configuredFeature,
                        x,
                        y,
                        z,
                        minY,
                        maxY
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
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
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

        return new PlacementResult(true, placedAnything, Math.max(1, cfg.skipHeight));
    }

    private PlacementResult tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            FeaturePlaceContext<AllSurfaceLayeredFeaturesConfig> context,
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

        return new PlacementResult(true, placedAnything, cfg.skipHeight);
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

    private int getGlobalMaxY(AllSurfaceLayeredFeaturesConfig layeredCfg, int defaultMaxY) {
        int globalMaxY = Integer.MIN_VALUE;

        for (AllSurfacesFeatureConfig cfg : layeredCfg.features) {
            if (!cfg.allLayers) {
                continue;
            }

            globalMaxY = Math.max(globalMaxY, getMaxY(cfg, defaultMaxY));
        }

        return globalMaxY == Integer.MIN_VALUE ? defaultMaxY : globalMaxY;
    }

    private int getGlobalMinY(AllSurfaceLayeredFeaturesConfig layeredCfg, int defaultMinY) {
        int globalMinY = Integer.MAX_VALUE;

        for (AllSurfacesFeatureConfig cfg : layeredCfg.features) {
            if (!cfg.allLayers) {
                continue;
            }

            globalMinY = Math.min(globalMinY, getMinY(cfg, defaultMinY));
        }

        return globalMinY == Integer.MAX_VALUE ? defaultMinY : globalMinY;
    }

    private boolean isInYRange(
            AllSurfacesFeatureConfig cfg,
            int y,
            int defaultMinY,
            int defaultMaxY
    ) {
        return y >= getMinY(cfg, defaultMinY)
                && y <= getMaxY(cfg, defaultMaxY);
    }

    private int getMinY(AllSurfacesFeatureConfig cfg, int defaultMinY) {
        return cfg.minY != null ? cfg.minY : defaultMinY;
    }

    private int getMaxY(AllSurfacesFeatureConfig cfg, int defaultMaxY) {
        return cfg.maxY != null ? cfg.maxY : defaultMaxY;
    }

    private record PlacementResult(boolean foundLayer, boolean placedAnything, int skipHeight) {
        private static final PlacementResult NONE = new PlacementResult(false, false, 0);
    }
}