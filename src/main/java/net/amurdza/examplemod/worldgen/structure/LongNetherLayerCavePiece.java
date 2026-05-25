package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.worldgen.feature.AllSurfacesFeatureConfig;
import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LongNetherLayerCavePiece extends StructurePiece {
    private static final int TARGET_Y = -126;
    private static final double FIXED_STARTING_YAW = 0.0D;

    private enum NetherLayer {
        DEEP_DARK,
        SOUL_SLUDGE,
        SOUL_SOIL,
        SOUL_SAND,
        WARPED_FOREST,
        CRIMSON_FOREST,
        BASALT_DELTAS,
        NONE
    }
    private final BlockPos origin;
    private final long seed;
    private final float horizontalRadius;
    private final float verticalRadius;
    private final int lavaLevel;
    private final float centralPillarDiameter;
    private final float minFloorThickness;
    private final float pitchLower;
    private final float pitchUpper;
    private final float liquidDepth;
    private final float liquidRadius;
    private final float verticalRadiusUpper;

    private final int maxDeepDarkY;
    private final int maxSoulSludgeY;
    private final int maxSoulSoilY;
    private final int maxSoulSandY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltas;

    private final List<AllSurfacesFeatureConfig> allSurfaceFeatures;
    private final List<ExactSurfaceFeatureConfig> exactSurfaceFeatures;

    public LongNetherLayerCavePiece(
            BlockPos origin,
            long seed,
            float horizontalRadius,
            float verticalRadius,
            int lavaLevel,
            float centralPillarDiameter,
            float minFloorThickness,
            float pitchLower,
            float pitchUpper,
            float liquidDepth,
            float liquidRadius,
            int maxDeepDarkY,
            int maxSoulSludgeY,
            int maxSoulSoilY,
            int maxSoulSandY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltas,
            float verticalRadiusUpper,
            List<AllSurfacesFeatureConfig> allSurfaceFeatures,
            List<ExactSurfaceFeatureConfig> exactSurfaceFeatures
    ) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), 0, makeBoundingBox(
                origin,
                horizontalRadius,
                verticalRadius,
                lavaLevel,
                centralPillarDiameter,
                liquidRadius
        ));

        this.origin = origin;
        this.seed = seed;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.verticalRadiusUpper = verticalRadiusUpper;
        this.lavaLevel = lavaLevel;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.pitchLower = pitchLower;
        this.pitchUpper = pitchUpper;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSludgeY = maxSoulSludgeY;
        this.maxSoulSoilY = maxSoulSoilY;
        this.maxSoulSandY = maxSoulSandY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltas = maxBasaltDeltas;

        this.allSurfaceFeatures = allSurfaceFeatures;
        this.exactSurfaceFeatures = exactSurfaceFeatures;
    }

    @SuppressWarnings("unused")
    public LongNetherLayerCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.seed = tag.getLong("Seed");
        this.horizontalRadius = tag.getFloat("HorizontalRadius");
        this.verticalRadius = tag.getFloat("VerticalRadius");
        this.lavaLevel = tag.getInt("LavaLevel");
        this.centralPillarDiameter = tag.getFloat("CentralPillarDiameter");
        this.minFloorThickness = tag.getFloat("MinFloorThickness");
        this.pitchLower = tag.getFloat("PitchLower");
        this.pitchUpper = tag.getFloat("PitchUpper");
        this.verticalRadiusUpper = tag.getFloat("VerticalRadiusUpper");
        this.liquidDepth = tag.getFloat("LiquidDepth");

        this.liquidRadius = tag.contains("LiquidRadius")
                ? tag.getFloat("LiquidRadius")
                : tag.getFloat("LiquidWidth");

        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxSoulSludgeY = tag.getInt("MaxSoulSludgeY");
        this.maxSoulSoilY = tag.getInt("MaxSoulSoilY");
        this.maxSoulSandY = tag.getInt("MaxSoulSandY");

        this.maxWarpedForestY = tag.getInt("MaxWarpedForestY");
        this.maxCrimsonForestY = tag.getInt("MaxCrimsonForestY");
        this.maxBasaltDeltas = tag.getInt("MaxBasaltDeltasY");

        /*
         * This piece normally receives these feature lists when it is created
         * from the structure config.
         *
         * I am not serializing the feature lists into the piece NBT because they
         * contain holders/predicates from codecs. If a saved piece is reloaded
         * through this constructor, it safely has no extra feature configs.
         */
        this.allSurfaceFeatures = List.of();
        this.exactSurfaceFeatures = List.of();
    }

    @Override
    protected void addAdditionalSaveData(
            @NotNull StructurePieceSerializationContext context,
            @NotNull CompoundTag tag
    ) {
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());
        tag.putLong("Seed", this.seed);
        tag.putFloat("HorizontalRadius", this.horizontalRadius);
        tag.putFloat("VerticalRadius", this.verticalRadius);
        tag.putInt("LavaLevel", this.lavaLevel);
        tag.putFloat("CentralPillarDiameter", this.centralPillarDiameter);
        tag.putFloat("MinFloorThickness", this.minFloorThickness);
        tag.putFloat("PitchLower", this.pitchLower);
        tag.putFloat("PitchUpper", this.pitchUpper);
        tag.putFloat("VerticalRadiusUpper", this.verticalRadiusUpper);
        tag.putFloat("LiquidDepth", this.liquidDepth);
        tag.putFloat("LiquidRadius", this.liquidRadius);

        tag.putInt("MaxDeepDarkY", this.maxDeepDarkY);
        tag.putInt("MaxSoulSludgeY", this.maxSoulSludgeY);
        tag.putInt("MaxSoulSoilY", this.maxSoulSoilY);
        tag.putInt("MaxSoulSandY", this.maxSoulSandY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltas);
    }

    private float getTunnelHorizontalRadius() {
        return this.horizontalRadius + this.liquidRadius;
    }

    private boolean shouldSkip(double relativeX, double relativeY, double relativeZ) {
        return relativeX * relativeX
                + relativeY * relativeY * relativeY * relativeY
                + relativeZ * relativeZ >= 1.0D;
    }

    private static double turnPerStepForPathRadius(double pathRadius) {
        return 1.0D / Math.max(pathRadius, 1.0D);
    }

    private void carveBlock(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos pos
    ) {
        BlockState oldState = level.getBlockState(pos);

        if (!canReplace(oldState)) {
            return;
        }

        BlockState carvedState = pos.getY() <= this.lavaLevel
                ? Blocks.LAVA.defaultBlockState()
                : Blocks.AIR.defaultBlockState();

        level.setBlock(pos, carvedState, Block.UPDATE_CLIENTS);

        if (carvedState.is(Blocks.LAVA)) {
            clearBlockAboveFluid(level, box, pos);
        }

        decorateCaveSurface(level, pos);
    }

    private boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }

    private boolean canReplaceWithFluid(BlockState state) {
        return canReplace(state) || state.is(Blocks.LAVA) || state.is(Blocks.WATER);
    }

    private void clearBlockAboveFluid(WorldGenLevel level, BoundingBox box, BlockPos fluidPos) {
        BlockPos above = fluidPos.above();

        if (!box.isInside(above)) {
            return;
        }

        BlockState aboveState = level.getBlockState(above);

        if (aboveState.is(Blocks.BEDROCK)) {
            return;
        }

        if (aboveState.isAir()) {
            return;
        }

        if (aboveState.is(Blocks.LAVA) || aboveState.is(Blocks.WATER)) {
            return;
        }

        level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos) {
        NetherLayer layer = getLayerAtY(carvedPos.getY());

        if (layer == NetherLayer.DEEP_DARK) {
            replaceAdjacentSolidsWithSculkAndEndStoneBacking(level, carvedPos);
            return;
        }

        NetherLayer belowLayer = getLayerAtY(carvedPos.getY() - 1);
        BlockState floorState = getFloorState(belowLayer);

        if (floorState != null) {
            BlockPos below = carvedPos.below();
            BlockState belowState = level.getBlockState(below);

            if (isNaturalReplaceableSurface(level, below, belowState)) {
                level.setBlock(below, floorState, Block.UPDATE_CLIENTS);
            }
        }
    }

    private NetherLayer getLayerAtY(int y) {
        if (y > this.maxDeepDarkY) {
            return NetherLayer.NONE;
        }
        if (y > this.maxSoulSludgeY) {
            return NetherLayer.DEEP_DARK;
        }
        if (y > this.maxSoulSoilY) {
            return NetherLayer.SOUL_SLUDGE;
        }
        if (y > this.maxSoulSandY) {
            return NetherLayer.SOUL_SOIL;
        }
        if (y > this.maxWarpedForestY) {
            return NetherLayer.SOUL_SAND;
        }
        if (y > this.maxCrimsonForestY) {
            return NetherLayer.WARPED_FOREST;
        }
        if (y > this.maxBasaltDeltas) {
            return NetherLayer.CRIMSON_FOREST;
        }
        return NetherLayer.BASALT_DELTAS;
    }

    private static BlockState getFloorState(NetherLayer layer) {
        return switch (layer) {
            case SOUL_SLUDGE -> NourishedNetherModBlocks.SOUL_SLUDGE.get().defaultBlockState();
            case SOUL_SOIL -> Blocks.SOUL_SOIL.defaultBlockState();
            case SOUL_SAND -> Blocks.SOUL_SAND.defaultBlockState();
            case CRIMSON_FOREST -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
            case WARPED_FOREST -> Blocks.WARPED_NYLIUM.defaultBlockState();
            case BASALT_DELTAS, DEEP_DARK, NONE -> null;
        };
    }

    private void replaceAdjacentSolidsWithSculkAndEndStoneBacking(WorldGenLevel level, BlockPos carvedPos) {
        for (Direction direction : Direction.values()) {
            BlockPos sculkPos = carvedPos.relative(direction);
            BlockState sculkState = level.getBlockState(sculkPos);

            if (!isNaturalReplaceableSurface(level, sculkPos, sculkState)) {
                continue;
            }

            level.setBlock(sculkPos, Blocks.SCULK.defaultBlockState(), Block.UPDATE_CLIENTS);

            BlockPos firstBackingPos = sculkPos.relative(direction);

            if (getLayerAtY(firstBackingPos.getY()) == NetherLayer.DEEP_DARK) {
                placeSculkOrEndStoneBacking(level, firstBackingPos);
            }

            BlockPos secondBackingPos = firstBackingPos.relative(direction);

            if (getLayerAtY(secondBackingPos.getY()) == NetherLayer.DEEP_DARK) {
                placeSculkOrEndStoneBacking(level, secondBackingPos);
            }
        }
    }

    private void placeSculkOrEndStoneBacking(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!isNaturalReplaceableSolid(state)) {
            return;
        }

        BlockState replacement = isExposedToAir(level, pos)
                ? Blocks.SCULK.defaultBlockState()
                : Blocks.END_STONE.defaultBlockState();

        level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);
    }

    private static boolean isNaturalReplaceableSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
        return isNaturalReplaceableSolid(state)
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean isNaturalReplaceableSolid(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.SCULK_CATALYST)
                && !state.is(Blocks.SCULK_SENSOR)
                && !state.is(Blocks.SCULK_SHRIEKER)
                && !state.is(Blocks.BEDROCK);
    }

    private static boolean isExposedToAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void replaceFloorBelowLavaIfNeeded(WorldGenLevel level, BoundingBox box, BlockPos lavaPos) {
        if (!box.isInside(lavaPos)) {
            return;
        }

        BlockState lavaState = level.getBlockState(lavaPos);

        if (!lavaState.is(Blocks.LAVA)) {
            return;
        }

        BlockPos below = lavaPos.below();

        if (!box.isInside(below)) {
            return;
        }

        BlockState belowState = level.getBlockState(below);

        if (!belowState.getFluidState().isEmpty()) {
            return;
        }

        if (!shouldReplaceFluidFloor(belowState)) {
            return;
        }

        BlockState floorState = getFluidFloorState(below.getY());

        level.setBlock(below, floorState, Block.UPDATE_CLIENTS);
    }

    private static boolean shouldReplaceFluidFloor(BlockState state) {
        return state.is(Blocks.GRAVEL) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.GLOWSTONE);
    }

    private BlockState getFluidFloorState(int y) {
        NetherLayer layer = getLayerAtY(y);

        return switch (layer) {
            case SOUL_SLUDGE -> NourishedNetherModBlocks.SOUL_SLUDGE.get().defaultBlockState();
            case SOUL_SOIL -> Blocks.SOUL_SOIL.defaultBlockState();
            case SOUL_SAND -> Blocks.SOUL_SAND.defaultBlockState();
            case WARPED_FOREST -> Blocks.WARPED_NYLIUM.defaultBlockState();
            case CRIMSON_FOREST -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
            default -> Blocks.BLACKSTONE.defaultBlockState();
        };
    }


    private void tryPlaceAllSurfaceFeatureConfigAt(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos pos,
            AllSurfacesFeatureConfig cfg
    ) {
        if (cfg.minY != null && pos.getY() < cfg.minY) {
            return;
        }

        if (cfg.maxY != null && pos.getY() > cfg.maxY) {
            return;
        }

        if (cfg.biomes != null && !level.getBiome(pos.below()).is(cfg.biomes)) {
            return;
        }

        Holder<PlacedFeature> selectedFeature = getAllSurfaceFeatureForTargetPosition(level, pos, cfg);

        if (selectedFeature == null) {
            return;
        }

        if (!cfg.predicate.test(level, pos.below())) {
            return;
        }

        if (cfg.target == AllSurfacesFeatureConfig.Target.AIR) {
            if (!isSafeAirOrigin(level, pos)) {
                return;
            }
        }

        selectedFeature.value().place(level, generator, random, pos);
    }

    private Holder<PlacedFeature> getAllSurfaceFeatureForTargetPosition(
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

    private void placeExactSurfaceFeatureConfigs(
            WorldGenLevel level,
            ChunkGenerator generator,
            BoundingBox currentBox
    ) {
        if (this.exactSurfaceFeatures.isEmpty()) {
            return;
        }

        BoundingBox fullPieceBox = this.getBoundingBox();

        for (int i = 0; i < this.exactSurfaceFeatures.size(); i++) {
            ExactSurfaceFeatureConfig cfg = this.exactSurfaceFeatures.get(i);
            placeExactSurfaceFeatureConfigForWholePiece(level, generator, currentBox, fullPieceBox, cfg, i);
        }
    }

    private void placeExactSurfaceFeatureConfigForWholePiece(
            WorldGenLevel level,
            ChunkGenerator generator,
            BoundingBox currentBox,
            BoundingBox fullPieceBox,
            ExactSurfaceFeatureConfig cfg,
            int configIndex
    ) {
        if (cfg.count <= 0) {
            return;
        }

        int minY = Math.max(fullPieceBox.minY(), cfg.minY.orElse(fullPieceBox.minY()));
        int maxY = Math.min(fullPieceBox.maxY(), cfg.maxY.orElse(fullPieceBox.maxY()));

        if (minY > maxY) {
            return;
        }

        /*
         * This is the important change:
         *
         * We select the global positions for the whole structure first.
         * Then this particular postProcess call only places the selected
         * positions that are inside the current chunk/currentBox.
         *
         * This prevents cfg.count from becoming "count per chunk".
         */
        List<BlockPos> selectedPositions = selectGlobalExactSurfaceFeaturePositions(
                fullPieceBox,
                cfg,
                configIndex,
                minY,
                maxY
        );

        for (BlockPos selectedPos : selectedPositions) {
            if (!currentBox.isInside(selectedPos)) {
                continue;
            }

            /*
             * Recheck before placing because another feature may already have occupied
             * this exact block during this chunk's generation.
             */
            if (!isValidExactSurfaceFeatureCandidate(level, selectedPos, cfg)) {
                continue;
            }

            cfg.feature.value().place(level, generator, RandomSource.create(getExactSurfacePlacementSeed(fullPieceBox, configIndex)), selectedPos);
        }
    }

    private List<BlockPos> selectGlobalExactSurfaceFeaturePositions(
            BoundingBox fullPieceBox,
            ExactSurfaceFeatureConfig cfg,
            int configIndex,
            int minY,
            int maxY
    ) {
        RandomSource placementRandom = RandomSource.create(getExactSurfacePlacementSeed(fullPieceBox, configIndex));

        List<BlockPos> selectedPositions = new java.util.ArrayList<>();

        int attempts = 0;
        int maxAttempts = Math.max(cfg.count * 5000, 20000);

        while (selectedPositions.size() < cfg.count && attempts < maxAttempts) {
            attempts++;

            BlockPos selectedPos = randomLandRadialPositionInWholePiece(
                    placementRandom,
                    fullPieceBox,
                    minY,
                    maxY
            );

            if (selectedPositions.contains(selectedPos)) {
                continue;
            }

            selectedPositions.add(selectedPos.immutable());
        }

        return selectedPositions;
    }

    private BlockPos randomLandRadialPositionInWholePiece(
            RandomSource random,
            BoundingBox fullPieceBox,
            int minY,
            int maxY
    ) {
        double tunnelHorizontalRadius = getTunnelHorizontalRadius();

        double centralPillarDiameter = this.centralPillarDiameter;

        double centralPillarRadius = centralPillarDiameter * 0.5D;
        double pathRadius = centralPillarRadius + tunnelHorizontalRadius;

        /*
         * This is the local offset from the cave path center.
         *
         * innerOffset avoids the river.
         * outerOffset stays inside the carved cave.
         */
        double innerOffset = this.liquidRadius + 1.0D;
        double outerOffset = tunnelHorizontalRadius - 1.0D;

        if (outerOffset <= innerOffset) {
            innerOffset = 0.0D;
            outerOffset = tunnelHorizontalRadius;
        }

        /*
         * Angle around the central pillar.
         */
        double pathAngle = random.nextDouble() * Math.PI * 2.0D;

        double pathCenterX = this.origin.getX() + Math.cos(pathAngle) * pathRadius;
        double pathCenterZ = this.origin.getZ() + Math.sin(pathAngle) * pathRadius;

        /*
         * Offset within the local cave cross-section, but outside the river.
         * The sqrt makes this uniform by area.
         */
        double localAngle = random.nextDouble() * Math.PI * 2.0D;

        double innerSquared = innerOffset * innerOffset;
        double outerSquared = outerOffset * outerOffset;
        double localRadius = Math.sqrt(innerSquared + random.nextDouble() * (outerSquared - innerSquared));

        int x = Mth.floor(pathCenterX + Math.cos(localAngle) * localRadius);
        int z = Mth.floor(pathCenterZ + Math.sin(localAngle) * localRadius);
        int y = minY + random.nextInt(maxY - minY + 1);

        x = Mth.clamp(x, fullPieceBox.minX(), fullPieceBox.maxX());
        y = Mth.clamp(y, minY, maxY);
        z = Mth.clamp(z, fullPieceBox.minZ(), fullPieceBox.maxZ());

        return new BlockPos(x, y, z);
    }

    private boolean isValidExactSurfaceFeatureCandidate(
            WorldGenLevel level,
            BlockPos pos,
            ExactSurfaceFeatureConfig cfg
    ) {
        if (cfg.minY.isPresent() && pos.getY() < cfg.minY.get()) {
            return false;
        }

        if (cfg.maxY.isPresent() && pos.getY() > cfg.maxY.get()) {
            return false;
        }

        if (!isLandFeatureOrigin(level, pos)) {
            return false;
        }

        for (int i = 0; i < cfg.predicates.size(); i++) {
            if (!cfg.predicates.get(i).test(level, pos)) {
                return false;
            }
        }

        return true;
    }

    private boolean isLandFeatureOrigin(WorldGenLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }

        if (!level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.isAir();
    }

    private long getExactSurfacePlacementSeed(BoundingBox box, int configIndex) {
        long result = this.seed;

        result ^= (long) this.origin.getX() * 341873128712L;
        result ^= (long) this.origin.getY() * 132897987541L;
        result ^= (long) this.origin.getZ() * 42317861L;

        result ^= (long) box.minX() * 73428767L;
        result ^= (long) box.minY() * 912931L;
        result ^= (long) box.minZ() * 1274126177L;

        result ^= (long) configIndex * -7046029254386353131L;

        return result;
    }

    @Override
    public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator generator,
            @NotNull RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos pivot
    ) {
        float tunnelHorizontalRadius = getTunnelHorizontalRadius();

        float centralPillarDiameter = this.centralPillarDiameter;

        double pathRadius = centralPillarDiameter * 0.5D + tunnelHorizontalRadius;
        double turnPerStep = turnPerStepForPathRadius(pathRadius);

        double stepsPerTurn = (Math.PI * 2.0D) / Math.abs(turnPerStep);
        int flatEntranceSteps = Math.max(1, Mth.ceil(stepsPerTurn * 0.75D));

        double carvedHeight = 2 * this.verticalRadius;
        double requiredVerticalSeparation = carvedHeight + this.minFloorThickness + 1 + this.liquidDepth;

        double minDropPerStep = requiredVerticalSeparation / stepsPerTurn;
        minDropPerStep = Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        float pitch = (float) -Math.asin(minDropPerStep);

        double yaw = FIXED_STARTING_YAW;
        double y = this.origin.getY();

        /*
         * Entrance section:
         *
         * This makes the first 3/4 of a turn stay at origin.getY().
         * Instead of a closed spiral cave, this carves an open-air bowl down to
         * the tunnel floor, with the same river channel underneath it.
         */
        for (int step = 0; step < flatEntranceSteps; step++) {
            double x = this.origin.getX() + Math.cos(yaw) * pathRadius;
            double z = this.origin.getZ() + Math.sin(yaw) * pathRadius;

            carveOpenAirEntranceBowl(level, box, x, y, z);

            yaw += turnPerStep;
        }

        /*
         * Descending section:
         *
         * After the flat open bowl, continue with the current tunnel logic.
         */
        int maxSteps = 4096;

        for (int step = 0; step < maxSteps; step++) {
            double x = this.origin.getX() + Math.cos(yaw) * pathRadius;
            double z = this.origin.getZ() + Math.sin(yaw) * pathRadius;

            carveEllipsoid(level, box, x, y, z);

            double verticalStep;
            if (hasBottomOfEllipsoidReachBadlands(y)) {
                verticalStep = -this.pitchUpper;
            } else if (hasBottomOfEllipsoidReachedLava(y)) {
                verticalStep = -this.pitchLower;
            } else {
                verticalStep = Mth.sin(pitch);
            }

            if (y < TARGET_Y + verticalRadius - 4) {
                break;
            }

            y += verticalStep;
            yaw += turnPerStep;
        }

        placeExactSurfaceFeatureConfigs(level, generator, box);
        placeAllSurfaceFeatureConfigs(level, generator, random, box);
    }

    private static BoundingBox makeBoundingBox(
            BlockPos origin,
            float horizontalRadius,
            float verticalRadius,
            int lavaLevel,
            float centralPillarDiameter,
            float liquidRadius
    ) {
        float tunnelHorizontalRadius = horizontalRadius + liquidRadius;

        int totalRadius = Mth.ceil(centralPillarDiameter * 0.5F + 2.0F * tunnelHorizontalRadius);

        int maxY = Math.max(origin.getY() + Mth.ceil(verticalRadius) + 48, lavaLevel + 32);

        return new BoundingBox(
                origin.getX() - totalRadius - 3,
                TARGET_Y,
                origin.getZ() - totalRadius - 3,
                origin.getX() + totalRadius + 3,
                maxY,
                origin.getZ() + totalRadius + 3
        );
    }

    private void placeAllSurfaceFeatureConfigs(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box
    ) {
        if (this.allSurfaceFeatures.isEmpty()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (AllSurfacesFeatureConfig cfg : this.allSurfaceFeatures) {
            int minY = box.minY();
            int maxY = box.maxY();

            if (cfg.minY != null) {
                minY = Math.max(minY, cfg.minY);
            }

            if (cfg.maxY != null) {
                maxY = Math.min(maxY, cfg.maxY);
            }

            if (minY > maxY) {
                continue;
            }

            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        pos.set(x, y, z);
                        tryPlaceAllSurfaceFeatureConfigAt(level, generator, random, pos, cfg);
                    }
                }
            }
        }
    }

    private boolean hasBottomOfEllipsoidReachedLava(double centerY) {
        double bottomOfEllipsoid = centerY - this.verticalRadius;
        return bottomOfEllipsoid <= this.lavaLevel;
    }

    private boolean hasBottomOfEllipsoidReachBadlands(double centerY) {
        double bottomOfEllipsoid = centerY - this.verticalRadius;
        return bottomOfEllipsoid > maxDeepDarkY;
    }

    private void carveEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ
    ) {
        double localHorizontalRadius = getTunnelHorizontalRadius();
        double localVerticalRadius = getLocalVerticalRadius(centerY);

        int minX = Mth.floor(centerX - localHorizontalRadius) - 1;
        int maxX = Mth.floor(centerX + localHorizontalRadius) + 1;
        int minY = Mth.floor(centerY - localVerticalRadius) - 1;
        int maxY = Mth.floor(centerY + localVerticalRadius) + 1;
        int minZ = Mth.floor(centerZ - localHorizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + localHorizontalRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        minY = Math.max(minY, TARGET_Y);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    double relativeX = (x + 0.5D - centerX) / localHorizontalRadius;
                    double relativeY = (y + 0.5D - centerY) / localVerticalRadius;
                    double relativeZ = (z + 0.5D - centerZ) / localHorizontalRadius;

                    if (shouldSkip(relativeX, relativeY, relativeZ)) {
                        continue;
                    }

                    carveBlock(level, box, pos);
                }
            }
        }

        carveLowerFluidEllipsoid(level, box, centerX, centerY, centerZ, localVerticalRadius, false);
    }

    private void carveOpenAirEntranceBowl(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ
    ) {
        double localHorizontalRadius = getTunnelHorizontalRadius();
        double localVerticalRadius = getLocalVerticalRadius(centerY);

        int minX = Mth.floor(centerX - localHorizontalRadius) - 1;
        int maxX = Mth.floor(centerX + localHorizontalRadius) + 1;
        int minZ = Mth.floor(centerZ - localHorizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + localHorizontalRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double relativeX = (x + 0.5D - centerX) / localHorizontalRadius;
                double relativeZ = (z + 0.5D - centerZ) / localHorizontalRadius;

                double horizontalDistanceSquared = relativeX * relativeX + relativeZ * relativeZ;

                if (horizontalDistanceSquared >= 1.0D) {
                    continue;
                }

                /*
                 * This mirrors the ellipsoid profile used by shouldSkip():
                 *
                 * relativeX^2 + relativeY^4 + relativeZ^2 = 1
                 *
                 * Solving for the lower half gives a smooth bowl floor.
                 */
                double verticalExtent = Math.pow(1.0D - horizontalDistanceSquared, 0.25D);
                int floorY = Mth.floor(centerY - localVerticalRadius * verticalExtent);

                floorY = Math.max(floorY, TARGET_Y);

                placeEntranceBowlFloorLayer(level, box, x, floorY, z);

                for (int y = floorY + 1; y <= box.maxY(); y++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (oldState.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    if (oldState.isAir()) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        carveLowerFluidEllipsoid(level, box, centerX, centerY, centerZ, localVerticalRadius, true);
    }

    private void carveLowerFluidEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double mainCenterX,
            double mainCenterY,
            double mainCenterZ,
            double mainVerticalRadius,
            boolean useEntranceBowlFloor
    ) {
        if (this.liquidRadius <= 0.0F || this.liquidDepth <= 0.0F) {
            return;
        }

        double bottomOfMainEllipsoid = mainCenterY - mainVerticalRadius;
        double fluidCenterY = bottomOfMainEllipsoid - 1.0D;

        int minX = Mth.floor(mainCenterX - this.liquidRadius) - 1;
        int maxX = Mth.floor(mainCenterX + this.liquidRadius) + 1;
        int minY = Mth.floor(fluidCenterY - this.liquidDepth);
        int maxY = Mth.floor(fluidCenterY) + 1;
        int minZ = Mth.floor(mainCenterZ - this.liquidRadius) - 1;
        int maxZ = Mth.floor(mainCenterZ + this.liquidRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockState fluidState = getFluidStateForRiverColumn(bottomOfMainEllipsoid);

                if (fluidState == null) {
                    continue;
                }

                boolean placedAnyFluidInColumn = false;
                int lowestPlacedFluidY = Integer.MAX_VALUE;

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    if (!isInsideLowerFluidEllipsoid(
                            x,
                            y,
                            z,
                            mainCenterX,
                            fluidCenterY,
                            mainCenterZ
                    )) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (!canReplaceWithFluid(oldState)) {
                        continue;
                    }

                    level.setBlock(pos, fluidState, Block.UPDATE_CLIENTS);

                    if (fluidState.is(Blocks.LAVA)) {
                        replaceFloorBelowLavaIfNeeded(level, box, pos);
                    }

                    clearBlockAboveFluid(level, box, pos);

                    placedAnyFluidInColumn = true;
                    lowestPlacedFluidY = Math.min(lowestPlacedFluidY, y);
                }

                if (placedAnyFluidInColumn) {
                    if (useEntranceBowlFloor) {
                        placeEntranceBowlFloorLayer(level, box, x, lowestPlacedFluidY - 1, z);
                    } else {
                        decorateDeepDarkRiverColumnSurfaces(level, box, x, z, minY, maxY, mainCenterX, fluidCenterY, mainCenterZ);
                    }
                }
            }
        }
    }

    private void placeEntranceBowlFloorLayer(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int topY,
            int z
    ) {
        placeEntranceBowlFloorBlock(level, box, x, topY, z, Blocks.SAND.defaultBlockState());
        placeEntranceBowlFloorBlock(level, box, x, topY - 1, z, Blocks.SAND.defaultBlockState());
        placeEntranceBowlFloorBlock(level, box, x, topY - 2, z, Blocks.SANDSTONE.defaultBlockState());
        placeEntranceBowlFloorBlock(level, box, x, topY - 3, z, Blocks.SANDSTONE.defaultBlockState());
    }

    private void placeEntranceBowlFloorBlock(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int y,
            int z,
            BlockState replacement
    ) {
        BlockPos pos = new BlockPos(x, y, z);

        if (!box.isInside(pos)) {
            return;
        }

        BlockState oldState = level.getBlockState(pos);

        if (!canReplaceEntranceBowlFloorBlock(oldState)) {
            return;
        }

        level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);
    }

    private boolean canReplaceEntranceBowlFloorBlock(BlockState state) {
        return !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.SCULK_CATALYST)
                && !state.is(Blocks.SCULK_SENSOR)
                && !state.is(Blocks.SCULK_SHRIEKER);
    }

    private void decorateDeepDarkRiverColumnSurfaces(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int z,
            int minY,
            int maxY,
            double fluidCenterX,
            double fluidCenterY,
            double fluidCenterZ
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            if (getLayerAtY(y) != NetherLayer.DEEP_DARK) {
                continue;
            }

            if (!isInsideLowerFluidEllipsoid(x, y, z, fluidCenterX, fluidCenterY, fluidCenterZ)) {
                continue;
            }

            pos.set(x, y, z);

            placeSculkIfNaturalSolid(level, box, pos.below());
            placeSculkIfNaturalSolid(level, box, pos.north());
            placeSculkIfNaturalSolid(level, box, pos.south());
            placeSculkIfNaturalSolid(level, box, pos.east());
            placeSculkIfNaturalSolid(level, box, pos.west());
        }
    }

    private void placeSculkIfNaturalSolid(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) {
            return;
        }

        if (getLayerAtY(pos.getY()) != NetherLayer.DEEP_DARK) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (!isNaturalReplaceableSolid(state)) {
            return;
        }

        level.setBlock(pos, Blocks.SCULK.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean isInsideLowerFluidEllipsoid(
            int x,
            int y,
            int z,
            double fluidCenterX,
            double fluidCenterY,
            double fluidCenterZ
    ) {
        double relativeX = (x + 0.5D - fluidCenterX) / this.liquidRadius;
        double relativeY = (y + 0.5D - fluidCenterY) / this.liquidDepth;
        double relativeZ = (z + 0.5D - fluidCenterZ) / this.liquidRadius;

        if (relativeY > 0.0D) {
            return false;
        }

        return relativeX * relativeX
                + relativeY * relativeY * relativeY * relativeY
                + relativeZ * relativeZ <= 1.0D;
    }

    private BlockState getFluidStateForRiverColumn(double landFloorY) {
        boolean bottomIsInLavaRegion = landFloorY - liquidDepth <= this.maxSoulSludgeY;
        boolean topIsInWaterRegion = landFloorY > this.maxSoulSludgeY;

        if (bottomIsInLavaRegion && topIsInWaterRegion) {
            return null;
        }

        if (bottomIsInLavaRegion) {
            return Blocks.LAVA.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }

    private double getLocalVerticalRadius(double centerY) {
        double localVerticalRadius = this.verticalRadius;

        if(centerY > maxDeepDarkY){
            return verticalRadiusUpper;
        }

//        if (centerY <= this.lavaLevel) {
//            double minCeilingY = this.lavaLevel;
//
//            if (centerY + localVerticalRadius < minCeilingY) {
//                localVerticalRadius = minCeilingY - centerY;
//            }
//        }

        return localVerticalRadius;
    }
}