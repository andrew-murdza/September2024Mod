package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
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
    private static final int RIVER_REGION_WIDTH_CHUNKS = 64;
    private static final int BIOME_BAND_WIDTH_CHUNKS = 64;
    private static final int BIOME_BAND_COUNT = 10;
    private static final int BIOME_PERIOD_CHUNKS = BIOME_BAND_WIDTH_CHUNKS * BIOME_BAND_COUNT;
    private static final int MOUNTAIN_TREE_SPLIT_Y = 64;

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

    private PlacementResult tryPlaceGuaranteedFeatureNearY(
            WorldGenLevel level,
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
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

        if (isMountainTreeGrid(configuredFeature)) {
            boolean hasUpperSurface = hasMountainGrassSurfaceAtSplitY(level, x, z);

            if (cfg.allLayers && maxY < MOUNTAIN_TREE_SPLIT_Y && hasUpperSurface) {
                return false;
            }

            if (!cfg.allLayers
                    && minY == MOUNTAIN_TREE_SPLIT_Y
                    && maxY == MOUNTAIN_TREE_SPLIT_Y
                    && hasUpperSurface) {
                pos.set(x, MOUNTAIN_TREE_SPLIT_Y, z);
                return tryPlaceGuaranteedFeatureAt(
                        level,
                        pos,
                        context,
                        cfg,
                        configuredFeature
                ).placedAnything();
            }
        }

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
            FeaturePlaceContext<AllSurfacesFeatureConfig> context,
            AllSurfacesFeatureConfig cfg,
            Holder<ConfiguredFeature<?, ?>> configuredFeature
    ) {
        if (isRainforestTreeGrid(configuredFeature)
                && !level.getBiome(pos).is(ModTags.Biomes.rainforestBiomes)) {
            return PlacementResult.NONE;
        }

        if (!isValidGuaranteedFeaturePlacementOrigin(level, pos, cfg)
                && !hasValidGuaranteedFeaturePlacementOriginInChunk(level, pos, cfg)
                && !shouldAllowRainforestGridSpill(pos, configuredFeature)) {
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

    private boolean hasValidGuaranteedFeaturePlacementOriginInChunk(
            WorldGenLevel level,
            BlockPos origin,
            AllSurfacesFeatureConfig cfg
    ) {
        ChunkAccess chunk = level.getChunk(origin);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkPos.getMinBlockX() + dx;

            for (int dz = 0; dz < 16; dz++) {
                int z = chunkPos.getMinBlockZ() + dz;

                if (cfg.allLayers) {
                    candidate.set(x, origin.getY(), z);
                } else {
                    candidate.set(x, getSurfaceStartY(chunk, cfg, dx, dz), z);
                }

                if (isValidGuaranteedFeaturePlacementOrigin(level, candidate, cfg)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldAllowRainforestGridSpill(
            BlockPos pos,
            Holder<ConfiguredFeature<?, ?>> configuredFeature
    ) {
        if (!isRainforestPatchGrid(configuredFeature)) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(pos);

        return isRiverShorelineSpillChunk(chunkPos)
                || isJungleSavannaBoundarySpillChunk(chunkPos);
    }

    private boolean isRiverShorelineSpillChunk(ChunkPos chunkPos) {
        return Math.floorMod(chunkPos.x, RIVER_REGION_WIDTH_CHUNKS) == 0;
    }

    private boolean isJungleSavannaBoundarySpillChunk(ChunkPos chunkPos) {
        int localZ = Math.floorMod(chunkPos.z, BIOME_PERIOD_CHUNKS);

        return localZ == BIOME_BAND_WIDTH_CHUNKS
                || localZ == BIOME_BAND_WIDTH_CHUNKS * 9 - 1;
    }

    private boolean isRainforestPatchGrid(Holder<ConfiguredFeature<?, ?>> configuredFeature) {
        return configuredFeature.unwrapKey()
                .map(key -> key.location().getNamespace().equals("aoemod")
                        && (key.location().getPath().equals("rainforest/floor/grid_choice")
                        || key.location().getPath().equals("rainforest/floor/sugar_cane_grid_choice")))
                .orElse(false);
    }

    private boolean isRainforestTreeGrid(Holder<ConfiguredFeature<?, ?>> configuredFeature) {
        return configuredFeature.unwrapKey()
                .map(key -> key.location().getNamespace().equals("aoemod")
                        && key.location().getPath().equals("rainforest/floor/tree_grid_choice"))
                .orElse(false);
    }

    private boolean isMountainTreeGrid(Holder<ConfiguredFeature<?, ?>> configuredFeature) {
        return configuredFeature.unwrapKey()
                .map(key -> key.location().getNamespace().equals("aoemod")
                        && key.location().getPath().equals("mountains/floor/grid_choice"))
                .orElse(false);
    }

    private boolean hasMountainGrassSurfaceAtSplitY(WorldGenLevel level, int originX, int originZ) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(originX, MOUNTAIN_TREE_SPLIT_Y, originZ));
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkPos.getMinBlockX() + dx;

            for (int dz = 0; dz < 16; dz++) {
                int z = chunkPos.getMinBlockZ() + dz;
                candidate.set(x, MOUNTAIN_TREE_SPLIT_Y, z);

                if (level.getBiome(candidate).is(ModTags.Biomes.mountainBiomes)
                        && level.getBlockState(candidate.below()).is(Blocks.GRASS_BLOCK)
                        && level.getBlockState(candidate).isAir()
                        && level.getBlockState(candidate.above()).isAir()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValidGuaranteedFeaturePlacementOrigin(
            WorldGenLevel level,
            BlockPos pos,
            AllSurfacesFeatureConfig cfg
    ) {
        return (cfg.biomes == null || level.getBiome(pos).is(cfg.biomes))
                && !isBlockedExposedUndergroundCaveSurface(level, pos, cfg)
                && isValidGuaranteedFeatureOrigin(level, pos, cfg)
                && cfg.predicate.test(level, pos.below())
                && (cfg.target != AIR || isSafeAirOrigin(level, pos));
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
        if (cfg.biomes != null && !level.getBiome(pos).is(cfg.biomes)) {
            return PlacementResult.NONE;
        }

        if (isBlockedExposedUndergroundCaveSurface(level, pos, cfg)) {
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
                    && level.getFluidState(pos.above()).isEmpty()
                    && isDryGround(level, pos.below());

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
                                && level.getFluidState(pos.above()).isEmpty()
                                && isDryGround(level, pos.below());

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
                && level.getFluidState(pos).isEmpty()
                && isDryGround(level, pos.below());
    }

    private boolean isDryGround(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty();
    }

    private boolean isBlockedExposedUndergroundCaveSurface(
            WorldGenLevel level,
            BlockPos pos,
            AllSurfacesFeatureConfig cfg
    ) {
        return cfg.biomes != null
                && isUndergroundCaveBiomeTag(cfg.biomes)
                && isAtWorldSurface(level, pos);
    }

    private boolean isAtWorldSurface(WorldGenLevel level, BlockPos pos) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        return pos.getY() >= surfaceY;
    }

    private static boolean isUndergroundCaveBiomeTag(TagKey<Biome> tag) {
        return tag.equals(ModTags.Biomes.deepDarkBiomes)
                || tag.equals(ModTags.Biomes.soulSandValleyBiomes)
                || tag.equals(ModTags.Biomes.warpedForestBiomes)
                || tag.equals(ModTags.Biomes.crimsonForestBiomes)
                || tag.equals(ModTags.Biomes.basaltDeltasBiomes)
                || tag.equals(ModTags.Biomes.netherBiomes);
    }

    private record PlacementResult(boolean foundLayer, boolean placedAnything) {
        private static final PlacementResult NONE = new PlacementResult(false, false);
    }
}
