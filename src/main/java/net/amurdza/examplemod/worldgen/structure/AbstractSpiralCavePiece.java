package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.worldgen.feature.AllSurfacesFeatureConfig;
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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractSpiralCavePiece extends StructurePiece {
    protected static final int TARGET_Y = -126;
    private static final double FIXED_STARTING_YAW = 0.0D;

    protected final BlockPos origin;
    protected final long seed;

    protected final float lowerHorizontalRadius;
    protected final float lowerVerticalRadius;
    protected final float upperHorizontalRadius;
    protected final float upperVerticalRadius;

    protected final float centralPillarDiameter;
    protected final float minFloorThickness;
    protected final float upperPitch;

    protected final float liquidDepth;
    protected final float liquidRadius;

    protected final List<AllSurfacesFeatureConfig> allSurfaceFeatures;
    protected final List<ExactSurfaceFeatureConfig> exactSurfaceFeatures;

    protected AbstractSpiralCavePiece(
            StructurePieceType type,
            BlockPos origin,
            long seed,
            float lowerHorizontalRadius,
            float lowerVerticalRadius,
            float upperHorizontalRadius,
            float upperVerticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float upperPitch,
            float liquidDepth,
            float liquidRadius,
            List<AllSurfacesFeatureConfig> allSurfaceFeatures,
            List<ExactSurfaceFeatureConfig> exactSurfaceFeatures
    ) {
        super(type, 0, makeBoundingBox(
                origin,
                lowerHorizontalRadius,
                lowerVerticalRadius,
                upperHorizontalRadius,
                upperVerticalRadius,
                centralPillarDiameter,
                liquidRadius
        ));

        this.origin = origin;
        this.seed = seed;
        this.lowerHorizontalRadius = lowerHorizontalRadius;
        this.lowerVerticalRadius = lowerVerticalRadius;
        this.upperHorizontalRadius = upperHorizontalRadius;
        this.upperVerticalRadius = upperVerticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.upperPitch = upperPitch;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.allSurfaceFeatures = allSurfaceFeatures;
        this.exactSurfaceFeatures = exactSurfaceFeatures;
    }

    protected AbstractSpiralCavePiece(
            StructurePieceType type,
            CompoundTag tag
    ) {
        super(type, tag);

        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.seed = tag.getLong("Seed");

        this.lowerHorizontalRadius = tag.contains("LowerHorizontalRadius")
                ? tag.getFloat("LowerHorizontalRadius")
                : tag.getFloat("HorizontalRadius");

        this.lowerVerticalRadius = tag.contains("LowerVerticalRadius")
                ? tag.getFloat("LowerVerticalRadius")
                : tag.getFloat("VerticalRadius");

        this.upperHorizontalRadius = tag.contains("UpperHorizontalRadius")
                ? tag.getFloat("UpperHorizontalRadius")
                : this.lowerHorizontalRadius;

        this.upperVerticalRadius = tag.contains("UpperVerticalRadius")
                ? tag.getFloat("UpperVerticalRadius")
                : tag.getFloat("VerticalRadiusUpper");

        this.centralPillarDiameter = tag.getFloat("CentralPillarDiameter");
        this.minFloorThickness = tag.getFloat("MinFloorThickness");
        this.upperPitch = tag.getFloat("UpperPitch");
        this.liquidDepth = tag.getFloat("LiquidDepth");

        this.liquidRadius = tag.contains("LiquidRadius")
                ? tag.getFloat("LiquidRadius")
                : tag.getFloat("LiquidWidth");

        /*
         * These feature lists normally come from the structure config when the
         * piece is first created. They are not serialized into NBT because they
         * contain holders/predicates/codecs.
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

        tag.putFloat("LowerHorizontalRadius", this.lowerHorizontalRadius);
        tag.putFloat("LowerVerticalRadius", this.lowerVerticalRadius);
        tag.putFloat("UpperHorizontalRadius", this.upperHorizontalRadius);
        tag.putFloat("UpperVerticalRadius", this.upperVerticalRadius);

        tag.putFloat("CentralPillarDiameter", this.centralPillarDiameter);
        tag.putFloat("MinFloorThickness", this.minFloorThickness);
        tag.putFloat("UpperPitch", this.upperPitch);
        tag.putFloat("LiquidDepth", this.liquidDepth);
        tag.putFloat("LiquidRadius", this.liquidRadius);

        addSubclassSaveData(context, tag);
    }

    protected abstract void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    );

    protected abstract boolean useUpperShape(double centerY);

    protected abstract boolean useUpperPitch(double centerY);

    protected abstract void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos);

    protected abstract BlockState getRiverFluidState(double landFloorY);

    protected abstract void decorateRiverColumnSurfaces(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int z,
            int minY,
            int maxY,
            int lowestPlacedFluidY,
            double fluidCenterX,
            double fluidCenterY,
            double fluidCenterZ
    );

    protected void decorateEntranceRiverFloor(
            WorldGenLevel level,
            BoundingBox box,
            int x,
            int lowestPlacedFluidY,
            int z
    ) {
        placeEntranceBowlFloorLayer(level, box, x, lowestPlacedFluidY - 1, z);
    }

    protected float getLowerTunnelHorizontalRadius() {
        return this.lowerHorizontalRadius + this.liquidRadius;
    }

    protected float getUpperTunnelHorizontalRadius() {
        return this.upperHorizontalRadius + this.liquidRadius;
    }

    protected float getMaxTunnelHorizontalRadius() {
        return Math.max(getLowerTunnelHorizontalRadius(), getUpperTunnelHorizontalRadius());
    }

    protected double getLocalTunnelHorizontalRadius(double centerY) {
        if (useUpperShape(centerY)) {
            return getUpperTunnelHorizontalRadius();
        }

        return getLowerTunnelHorizontalRadius();
    }

    protected double getLocalVerticalRadius(double centerY) {
        if (useUpperShape(centerY)) {
            return this.upperVerticalRadius;
        }

        return this.lowerVerticalRadius;
    }

    protected boolean shouldSkip(double relativeX, double relativeY, double relativeZ) {
        return relativeX * relativeX
                + relativeY * relativeY * relativeY * relativeY
                + relativeZ * relativeZ >= 1.0D;
    }

    protected static double turnPerStepForPathRadius(double pathRadius) {
        return 1.0D / Math.max(pathRadius, 1.0D);
    }

    protected void carveBlock(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos
    ) {
        BlockState oldState = level.getBlockState(pos);

        if (!canReplace(oldState)) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        decorateCaveSurface(level, pos);
    }

    protected boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }

    protected boolean canReplaceWithFluid(BlockState state) {
        return canReplace(state) || state.is(Blocks.LAVA) || state.is(Blocks.WATER);
    }

    protected void clearBlockAboveFluid(WorldGenLevel level, BoundingBox box, BlockPos fluidPos) {
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

    protected boolean isNaturalReplaceableSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
        return isNaturalReplaceableSolid(state)
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    protected boolean isNaturalReplaceableSolid(BlockState state){
        return !state.is(Blocks.BEDROCK)&&state.getFluidState().isEmpty();
    }

    protected static boolean isExposedToAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));

            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    protected void tryPlaceAllSurfaceFeatureConfigAt(
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

    protected Holder<PlacedFeature> getAllSurfaceFeatureForTargetPosition(
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

    protected boolean isSafeAirOrigin(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getFluidState(pos).isEmpty();
    }

    protected void placeExactSurfaceFeatureConfigs(
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

    protected void placeExactSurfaceFeatureConfigForWholePiece(
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

            if (!isValidExactSurfaceFeatureCandidate(level, selectedPos, cfg)) {
                continue;
            }

            cfg.feature.value().place(
                    level,
                    generator,
                    RandomSource.create(getExactSurfacePlacementSeed(fullPieceBox, configIndex)),
                    selectedPos
            );
        }
    }

    protected List<BlockPos> selectGlobalExactSurfaceFeaturePositions(
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

    protected BlockPos randomLandRadialPositionInWholePiece(
            RandomSource random,
            BoundingBox fullPieceBox,
            int minY,
            int maxY
    ) {
        double tunnelHorizontalRadius = getMaxTunnelHorizontalRadius();

        double centralPillarRadius = this.centralPillarDiameter * 0.5D;
        double pathRadius = centralPillarRadius + tunnelHorizontalRadius;

        double innerOffset = this.liquidRadius + 1.0D;
        double outerOffset = tunnelHorizontalRadius - 1.0D;

        if (outerOffset <= innerOffset) {
            innerOffset = 0.0D;
            outerOffset = tunnelHorizontalRadius;
        }

        double pathAngle = random.nextDouble() * Math.PI * 2.0D;

        double pathCenterX = this.origin.getX() + Math.cos(pathAngle) * pathRadius;
        double pathCenterZ = this.origin.getZ() + Math.sin(pathAngle) * pathRadius;

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

    protected boolean isValidExactSurfaceFeatureCandidate(
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

    protected boolean isLandFeatureOrigin(WorldGenLevel level, BlockPos pos) {
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

    protected long getExactSurfacePlacementSeed(BoundingBox box, int configIndex) {
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
        float tunnelHorizontalRadius = getMaxTunnelHorizontalRadius();

        double pathRadius = this.centralPillarDiameter * 0.5D + tunnelHorizontalRadius;
        double turnPerStep = turnPerStepForPathRadius(pathRadius);

        double stepsPerTurn = (Math.PI * 2.0D) / Math.abs(turnPerStep);
        int flatEntranceSteps = Math.max(1, Mth.ceil(stepsPerTurn * 0.75D));
        int entranceRiverSteps = Math.max(1, Mth.ceil(stepsPerTurn * 0.25D));

        double carvedHeight = 2.0D * Math.max(this.lowerVerticalRadius, this.upperVerticalRadius);
        double requiredVerticalSeparation = carvedHeight + this.minFloorThickness + 1.0D + this.liquidDepth;

        double minDropPerStep = requiredVerticalSeparation / stepsPerTurn;
        minDropPerStep = Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        float lowerPitch = (float) -Math.asin(minDropPerStep);

        double yaw = FIXED_STARTING_YAW;
        double y = this.origin.getY();

        if (shouldMakeBowl()) {
            double entranceStartYaw = yaw;

            for (int step = 0; step < flatEntranceSteps; step++) {
                double x = this.origin.getX() + Math.cos(yaw) * pathRadius;
                double z = this.origin.getZ() + Math.sin(yaw) * pathRadius;

                boolean carveEntranceRiver = step < entranceRiverSteps;

                carveOpenAirEntranceBowl(level, box, x, y, z, carveEntranceRiver);

                yaw += turnPerStep;
            }

            /*
             * This second pass fills/clears the whole entrance disk so the middle
             * pillar above the sand is removed. It does not place river blocks.
             */
            carveFlatEntranceBowlInteriorDisk(
                    level,
                    box,
                    pathRadius + getLocalTunnelHorizontalRadius(this.origin.getY()),
                    entranceStartYaw,
                    yaw
            );
        }

        int maxSteps = 4096;

        for (int step = 0; step < maxSteps; step++) {
            double x = this.origin.getX() + Math.cos(yaw) * pathRadius;
            double z = this.origin.getZ() + Math.sin(yaw) * pathRadius;

            if (wouldBottomOfEllipsoidCarveIntoTargetYPlusOne(y)) {
                break;
            }

            carveEllipsoid(level, box, x, y, z);

            double verticalStep;

            if (useUpperPitch(y)) {
                verticalStep = -this.upperPitch;
            } else {
                verticalStep = Mth.sin(lowerPitch);
            }

            y += verticalStep;
            yaw += turnPerStep;
        }

        placeExactSurfaceFeatureConfigs(level, generator, box);
        placeAllSurfaceFeatureConfigs(level, generator, random, box);
    }

    protected boolean shouldMakeBowl(){
        return true;
    }

    protected static BoundingBox makeBoundingBox(
            BlockPos origin,
            float lowerHorizontalRadius,
            float lowerVerticalRadius,
            float upperHorizontalRadius,
            float upperVerticalRadius,
            float centralPillarDiameter,
            float liquidRadius
    ) {
        float maxTunnelHorizontalRadius = Math.max(lowerHorizontalRadius, upperHorizontalRadius) + liquidRadius;
        float maxVerticalRadius = Math.max(lowerVerticalRadius, upperVerticalRadius);

        int totalRadius = Mth.ceil(centralPillarDiameter * 0.5F + 2.0F * maxTunnelHorizontalRadius);

        int maxY = origin.getY() + Mth.ceil(maxVerticalRadius) + 48;

        return new BoundingBox(
                origin.getX() - totalRadius - 3,
                TARGET_Y,
                origin.getZ() - totalRadius - 3,
                origin.getX() + totalRadius + 3,
                maxY,
                origin.getZ() + totalRadius + 3
        );
    }

    protected void placeAllSurfaceFeatureConfigs(
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

    protected void carveFlatEntranceRiver(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            int floorTopY,
            double centerZ
    ) {
        if (this.liquidRadius <= 0.0F || this.liquidDepth <= 0.0F) {
            return;
        }

        BlockState fluidState = getRiverFluidState(floorTopY);

        if (fluidState == null) {
            return;
        }

        int minX = Mth.floor(centerX - this.liquidRadius) - 1;
        int maxX = Mth.floor(centerX + this.liquidRadius) + 1;
        int minZ = Mth.floor(centerZ - this.liquidRadius) - 1;
        int maxZ = Mth.floor(centerZ + this.liquidRadius) + 1;

        int fluidTopY = floorTopY;
        int fluidBottomY = floorTopY - Mth.ceil(this.liquidDepth) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double relativeX = (x + 0.5D - centerX) / this.liquidRadius;
                double relativeZ = (z + 0.5D - centerZ) / this.liquidRadius;

                if (relativeX * relativeX + relativeZ * relativeZ > 1.0D) {
                    continue;
                }

                for (int y = fluidBottomY; y <= fluidTopY; y++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (!canReplaceWithFluid(oldState)) {
                        continue;
                    }

                    level.setBlock(pos, fluidState, Block.UPDATE_CLIENTS);
                }

                decorateEntranceRiverFloor(level, box, x, fluidBottomY, z);
            }
        }
    }

    protected boolean wouldBottomOfEllipsoidCarveIntoTargetYPlusOne(double centerY) {
        double bottomOfEllipsoid = centerY - getLocalVerticalRadius(centerY);
        return bottomOfEllipsoid <= TARGET_Y + 1;
    }

    protected void carveEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ
    ) {
        double localHorizontalRadius = getLocalTunnelHorizontalRadius(centerY);
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

                    carveBlock(level, pos);
                }
            }
        }

        carveLowerFluidEllipsoid(level, box, centerX, centerY, centerZ, localVerticalRadius, false);
    }

    protected void carveOpenAirEntranceBowl(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ,
            boolean carveEntranceRiver
    ) {
        double localHorizontalRadius = getLocalTunnelHorizontalRadius(centerY);

        int minX = Mth.floor(centerX - localHorizontalRadius) - 1;
        int maxX = Mth.floor(centerX + localHorizontalRadius) + 1;
        int minZ = Mth.floor(centerZ - localHorizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + localHorizontalRadius) + 1;

        int floorTopY = getEntranceBowlFloorTopY();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double relativeX = (x + 0.5D - centerX) / localHorizontalRadius;
                double relativeZ = (z + 0.5D - centerZ) / localHorizontalRadius;

                double horizontalDistanceSquared = relativeX * relativeX + relativeZ * relativeZ;

                if (horizontalDistanceSquared >= 1.0D) {
                    continue;
                }

                placeEntranceBowlFloorLayer(level, box, x, floorTopY, z);

                for (int y = floorTopY + 1; y <= box.maxY(); y++) {
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

        if (carveEntranceRiver) {
            carveFlatEntranceRiver(level, box, centerX, floorTopY, centerZ);
        }
    }

    protected void carveLowerFluidEllipsoid(
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
                BlockState fluidState = getRiverFluidState(fluidCenterY);

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
                    clearBlockAboveFluid(level, box, pos);

                    placedAnyFluidInColumn = true;
                    lowestPlacedFluidY = Math.min(lowestPlacedFluidY, y);
                }

                if (placedAnyFluidInColumn) {
                    if (useEntranceBowlFloor) {
                        decorateEntranceRiverFloor(level, box, x, lowestPlacedFluidY, z);
                    } else {
                        decorateRiverColumnSurfaces(
                                level,
                                box,
                                x,
                                z,
                                minY,
                                maxY,
                                lowestPlacedFluidY,
                                mainCenterX,
                                fluidCenterY,
                                mainCenterZ
                        );
                    }
                }
            }
        }
    }

    protected void placeEntranceBowlFloorLayer(
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

    protected void placeEntranceBowlFloorBlock(
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

    protected boolean canReplaceEntranceBowlFloorBlock(BlockState state) {
        return !state.is(Blocks.BEDROCK)&&state.getFluidState().isEmpty();
    }

    protected void carveFlatEntranceBowlInteriorDisk(
            WorldGenLevel level,
            BoundingBox box,
            double radius,
            double startYaw,
            double endYaw
    ) {
        int floorTopY = getEntranceBowlFloorTopY();

        int minX = Mth.floor(this.origin.getX() - radius) - 1;
        int maxX = Mth.floor(this.origin.getX() + radius) + 1;
        int minZ = Mth.floor(this.origin.getZ() - radius) - 1;
        int maxZ = Mth.floor(this.origin.getZ() + radius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x + 0.5D - this.origin.getX();
                double dz = z + 0.5D - this.origin.getZ();

                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                placeEntranceBowlFloorLayer(level, box, x, floorTopY, z);

                for (int y = floorTopY + 1; y <= box.maxY(); y++) {
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
    }

    protected int getEntranceBowlFloorTopY() {
        return this.origin.getY();
    }

    protected boolean isInsideLowerFluidEllipsoid(
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
}