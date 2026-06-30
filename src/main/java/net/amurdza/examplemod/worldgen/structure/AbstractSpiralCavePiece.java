package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpiralCavePiece extends StructurePiece {

    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    protected final BlockPos origin;
    protected final long seed;
    protected final int endY;
    protected final int endX;

    protected final float horizontalRadius;
    protected final float verticalRadius;

    protected final float centralPillarDiameter;
    protected final float minFloorThickness;

    protected final float liquidDepth;
    protected final float liquidRadius;

    protected final double pillarCenterX;
    protected final double pillarCenterZ;

    protected final HolderSet<PlacedFeature> placedFeatures;

    protected double activeCarveCenterX;
    protected double activeCarveCenterY;
    protected double activeCarveCenterZ;
    protected double activeCarveHorizontalRadius;
    protected double activeCarveVerticalRadius;
    protected final List<CarveEllipsoid> activePieceCarveEllipsoids = new ArrayList<>();

    protected record CarveEllipsoid(
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double verticalRadius,
            int maxCarveY
    ) {}

    protected AbstractSpiralCavePiece(
            StructurePieceType type,
            BlockPos origin,
            long seed,
            int endY,
            int endX,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(type, 0, makeBoundingBox(
                origin,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                liquidDepth,
                liquidRadius,
                endY,
                endX
        ));

        this.origin = origin;
        this.endY = endY;
        this.endX = endX;
        this.seed = seed;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.placedFeatures = placedFeatures;

        this.pillarCenterX = origin.getX();
        this.pillarCenterZ = origin.getZ();
    }

    protected AbstractSpiralCavePiece(
            StructurePieceType type,
            CompoundTag tag
    ) {
        super(type, tag);

        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.endY = tag.getInt("EndY");
        this.endX = tag.getInt("EndX");
        this.seed = tag.getLong("Seed");

        this.horizontalRadius = tag.contains("HorizontalRadius")
                ? tag.getFloat("HorizontalRadius")
                : tag.getFloat("LowerHorizontalRadius");

        this.verticalRadius = tag.contains("VerticalRadius")
                ? tag.getFloat("VerticalRadius")
                : tag.getFloat("LowerVerticalRadius");

        this.centralPillarDiameter = tag.getFloat("CentralPillarDiameter");
        this.minFloorThickness = tag.getFloat("MinFloorThickness");
        this.liquidDepth = tag.getFloat("LiquidDepth");

        this.liquidRadius = tag.contains("LiquidRadius")
                ? tag.getFloat("LiquidRadius")
                : tag.getFloat("LiquidWidth");

        /*
         * These normally come from the structure config when the piece is first
         * created. They are not serialized into NBT because placed feature holders
         * should be read from the config/registry instead.
         */
        this.placedFeatures = HolderSet.direct();

        this.pillarCenterX = origin.getX();
        this.pillarCenterZ = origin.getZ();
    }

    @Override
    protected void addAdditionalSaveData(
            @NotNull StructurePieceSerializationContext context,
            @NotNull CompoundTag tag
    ) {
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());

        tag.putInt("EndY", this.endY);
        tag.putInt("EndX", this.endX);

        tag.putLong("Seed", this.seed);

        tag.putFloat("HorizontalRadius", this.horizontalRadius);
        tag.putFloat("VerticalRadius", this.verticalRadius);

        tag.putFloat("CentralPillarDiameter", this.centralPillarDiameter);
        tag.putFloat("MinFloorThickness", this.minFloorThickness);
        tag.putFloat("LiquidDepth", this.liquidDepth);
        tag.putFloat("LiquidRadius", this.liquidRadius);

        addSubclassSaveData(context, tag);
    }

    protected abstract void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    );

    /*
     * These methods now receive the actual block position to decorate.
     *
     * surfacePos is already carvedPos.below().
     * wallPos is already carvedPos.relative(wallDirection).
     * ceilingPos is already carvedPos.above().
     *
     * The shared solid/natural/replaceable check has already passed before
     * these methods are called.
     */
    protected abstract void decorateCaveFloor(WorldGenLevel level, BlockPos surfacePos);

    protected abstract void decorateRiverFloor(WorldGenLevel level, BlockPos surfacePos);

    protected abstract void decorateCaveWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection);

    protected abstract void decorateRiverWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection);

    protected abstract void decorateCaveCeiling(WorldGenLevel level, BlockPos ceilingPos);

    protected abstract BlockState getRiverFluidState(double landFloorY);

    protected float getTunnelHorizontalRadius() {
        return this.horizontalRadius + this.liquidRadius;
    }

    protected boolean shouldSkip(double relativeX, double relativeY, double relativeZ) {
        return relativeX * relativeX
                + relativeY * relativeY * relativeY * relativeY
                + relativeZ * relativeZ >= 1.0D;
    }

    protected boolean activeEllipsoidWouldCarve(BlockPos pos) {
        if (this.activeCarveHorizontalRadius <= 0.0D || this.activeCarveVerticalRadius <= 0.0D) {
            return false;
        }

        return ellipsoidWouldCarve(
                pos,
                this.activeCarveCenterX,
                this.activeCarveCenterY,
                this.activeCarveCenterZ,
                this.activeCarveHorizontalRadius,
                this.activeCarveVerticalRadius
        );
    }

    protected boolean anyMainEllipsoidWouldCarve(BlockPos pos) {
        if (this.activePieceCarveEllipsoids.isEmpty()) {
            return activeEllipsoidWouldCarve(pos);
        }

        for (CarveEllipsoid ellipsoid : this.activePieceCarveEllipsoids) {
            if (ellipsoidWouldCarve(
                    pos,
                    ellipsoid.centerX(),
                    ellipsoid.centerY(),
                    ellipsoid.centerZ(),
                    ellipsoid.horizontalRadius(),
                    ellipsoid.verticalRadius()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean ellipsoidWouldCarve(
            BlockPos pos,
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double verticalRadius
    ) {
        if (horizontalRadius <= 0.0D || verticalRadius <= 0.0D) {
            return false;
        }

        double relativeX = (pos.getX() + 0.5D - centerX) / horizontalRadius;
        double relativeY = (pos.getY() + 0.5D - centerY) / verticalRadius;
        double relativeZ = (pos.getZ() + 0.5D - centerZ) / horizontalRadius;

        return !shouldSkip(relativeX, relativeY, relativeZ);
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
        clearUnsupportedBlockAbove(level, pos);
        decorateCaveBoundaries(level, pos);
    }

    private void clearUnsupportedBlockAbove(WorldGenLevel level, BlockPos carvedPos) {
        BlockPos above = carvedPos.above();
        BlockState aboveState = level.getBlockState(above);

        if (aboveState.isAir() || !aboveState.getFluidState().isEmpty()) {
            return;
        }

        if (!aboveState.canSurvive(level, above)) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected void setBlock(WorldGenLevel level, BlockPos pos, Block block){
        if(canReplace(level,pos)){
            level.setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS);
        }
    }

    protected void replaceExposedUndesirableBoundaryBlock(
            WorldGenLevel level,
            BlockPos pos,
            Block replacementBlock,
            boolean replaceSandAndSandstone
    ) {
        BlockState state = level.getBlockState(pos);

        if (!canReplace(state)
                || isProtectedDecoratedSurface(state)
                || !isUndesirableVisibleBoundaryBlock(state, replaceSandAndSandstone)) {
            return;
        }

        level.setBlock(pos, boundaryReplacementForState(state, replacementBlock).defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    protected Block boundaryReplacementForState(BlockState state, Block replacementBlock) {
        if (replacementBlock == Blocks.STONE && isDeepslateLikeOre(state)) {
            return Blocks.DEEPSLATE;
        }

        return replacementBlock;
    }

    protected Block overworldStoneReplacement(BlockPos pos, BlockState state) {
        if (pos.getY() < 0 || isDeepslateLikeOre(state)) {
            return Blocks.DEEPSLATE;
        }

        return Blocks.STONE;
    }

    private static boolean isUndesirableVisibleBoundaryBlock(
            BlockState state,
            boolean replaceSandAndSandstone
    ) {
        return isOreLike(state)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SUSPICIOUS_GRAVEL)
                || (replaceSandAndSandstone && isLooseSandOrSandstone(state));
    }

    private static boolean isOreLike(BlockState state) {
        if (state.is(Tags.Blocks.ORES)) {
            return true;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        return key != null && key.getPath().contains("_ore");
    }

    private static boolean isLooseSandOrSandstone(BlockState state) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SUSPICIOUS_SAND)
                || isSandstone(state);
    }

    private static boolean isSandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CHISELED_RED_SANDSTONE)
                || state.is(Blocks.CUT_RED_SANDSTONE)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE);
    }

    private static boolean isDeepslateOre(BlockState state) {
        return state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    private static boolean isDeepslateLikeOre(BlockState state) {
        if (isDeepslateOre(state)) {
            return true;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        return key != null
                && key.getPath().contains("deepslate")
                && key.getPath().contains("_ore");
    }

    private static boolean isProtectedDecoratedSurface(BlockState state) {
        return state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER)
                || state.is(Blocks.END_STONE);
    }

    protected void decorateCaveBoundaries(WorldGenLevel level, BlockPos carvedPos) {
        BlockPos surfacePos = carvedPos.below();

        if (canReplace(level, surfacePos)) {
            decorateCaveFloor(level, surfacePos);
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos wallPos = carvedPos.relative(direction);

            if (canReplace(level, wallPos)) {
                decorateCaveWall(level, wallPos, direction);
            }
        }

        BlockPos ceilingPos = carvedPos.above();

        if (canReplace(level, ceilingPos)) {
            decorateCaveCeiling(level, ceilingPos);
        }
    }

    protected void decorateRiverColumnBoundaries(WorldGenLevel level, BlockPos carvedPos) {
        BlockPos surfacePos = carvedPos.below();

        if (canReplace(level, surfacePos)) {
            decorateRiverFloor(level, surfacePos);
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos wallPos = carvedPos.relative(direction);

            if (canReplace(level, wallPos)) {
                decorateRiverWall(level, wallPos, direction);
            }
        }

        /*
         * No river-column ceiling decoration.
         *
         * River columns are assumed to never be directly underneath a ceiling.
         */
    }

    protected boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty() && state.isSolid();
    }

    protected boolean canReplace(WorldGenLevel level, BlockPos pos) {
        return canReplace(level.getBlockState(pos));
    }

    protected boolean canReplaceWithFluid(BlockState state) {
        return !state.is(Blocks.BEDROCK);
    }

    protected void clearBlockAboveFluid(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos fluidPos,
            int maxClearY
    ) {
        BlockPos above = fluidPos.above();

        if (above.getY() > maxClearY) {
            return;
        }

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

        double pathRadius = this.centralPillarDiameter * 0.5D + tunnelHorizontalRadius;
        double turnPerStep = -turnPerStepForPathRadius(pathRadius);

        double stepsPerTurn = (Math.PI * 2.0D) / Math.abs(turnPerStep);

        double carvedHeight = 2.0D * this.verticalRadius;
        double requiredVerticalSeparation = carvedHeight + this.minFloorThickness + 1.0D + this.liquidDepth;

        double minDropPerStep = requiredVerticalSeparation / stepsPerTurn;
        minDropPerStep = Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        double yaw = 3 * Math.PI * 0.5D;
        double y = this.origin.getY();

        double lastX = this.pillarCenterX + Math.cos(yaw) * pathRadius;
        double lastY = y;
        double lastZ = this.pillarCenterZ + Math.sin(yaw) * pathRadius;

        double firstX = lastX;
        double firstY = lastY;
        double firstZ = lastZ;
        boolean capturedFirstStep = false;
        List<CarveEllipsoid> ellipsoids = new ArrayList<>();

        int maxSteps = 4096;

        for (int step = 0; step < maxSteps; step++) {
            double x = this.pillarCenterX + Math.cos(yaw) * pathRadius;
            double z = this.pillarCenterZ + Math.sin(yaw) * pathRadius;

            if (!capturedFirstStep) {
                firstX = x;
                firstY = y;
                firstZ = z;
                capturedFirstStep = true;
            }

            if (wouldBottomOfEllipsoidCarveIntoTargetYPlusOne(y)) {
                break;
            }

            ellipsoids.add(new CarveEllipsoid(
                    x,
                    y,
                    z,
                    getTunnelHorizontalRadius(),
                    this.verticalRadius,
                    this.origin.getY()
            ));

            y -= minDropPerStep;
            yaw += turnPerStep;
        }

        if (capturedFirstStep) {
            addStraightExitTunnelPositiveXEllipsoids(ellipsoids, firstX, firstY, firstZ);
        }

        this.activePieceCarveEllipsoids.clear();
        this.activePieceCarveEllipsoids.addAll(ellipsoids);

        for (CarveEllipsoid ellipsoid : ellipsoids) {
            carveEllipsoid(
                    level,
                    box,
                    ellipsoid.centerX(),
                    ellipsoid.centerY(),
                    ellipsoid.centerZ(),
                    ellipsoid.maxCarveY()
            );
        }

        this.activePieceCarveEllipsoids.clear();

        placePlacedFeaturesForCurrentStructureChunk(level, generator, chunkPos);
    }

    protected void addStraightExitTunnelPositiveXEllipsoids(
            List<CarveEllipsoid> ellipsoids,
            double startX,
            double centerY,
            double centerZ
    ) {
        if (this.endX <= 0) {
            return;
        }

        double targetX = startX + this.endX;
        int maxCarveY = Math.max(
                this.origin.getY(),
                Mth.ceil(centerY + this.verticalRadius + 2.0D)
        );

        for (double centerX = startX; centerX <= targetX; centerX += 1.0D) {
            ellipsoids.add(new CarveEllipsoid(
                    centerX,
                    centerY,
                    centerZ,
                    getTunnelHorizontalRadius(),
                    this.verticalRadius,
                    maxCarveY
            ));
        }
    }

    protected static BoundingBox makeBoundingBox(
            BlockPos origin,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float liquidDepth,
            float liquidRadius,
            int endY,
            int endX
    ) {
        double tunnelHorizontalRadius = horizontalRadius + liquidRadius;

        double pathRadius = getPathRadius(
                horizontalRadius,
                centralPillarDiameter,
                liquidRadius
        );

        /*
         * This must match the center used by postProcess(). If this uses originZ
         * directly, the spiral can be clipped on the two sides that extend around
         * the real pillar center.
         */
        double pillarCenterX = origin.getX();
        double pillarCenterZ = origin.getZ();

        /*
         * Centerline radius plus the full carved tunnel radius. This covers the
         * whole circle traced by the spiral, not only the pillar itself.
         */
        double spiralOuterRadius = pathRadius + tunnelHorizontalRadius;

        /*
         * A tight padding of 3 is easy to undercount because carving checks block
         * centers, uses floor/ceil, and also places a lower fluid ellipsoid. Use a
         * chunk-sized safety margin while debugging; after confirming the cutoff is
         * gone, this can usually be reduced to 8 or 12.
         */
        int padding = 16;

        int spiralMinX = Mth.floor(pillarCenterX - spiralOuterRadius) - padding;
        int spiralMaxX = Mth.ceil(pillarCenterX + spiralOuterRadius) + padding;
        int spiralMinZ = Mth.floor(pillarCenterZ - spiralOuterRadius) - padding;
        int spiralMaxZ = Mth.ceil(pillarCenterZ + spiralOuterRadius) + padding;

        int minX = spiralMinX;
        int maxX = spiralMaxX;
        int minZ = spiralMinZ;
        int maxZ = spiralMaxZ;

        /*
         * The optional straight river is carved in +X from a point on/near the
         * spiral. Since makeBoundingBox() does not know the exact startX that the
         * subclass will pass to continueSurfaceRiverPositiveX(), the safe bound is
         * the whole spiral X span plus endX.
         */
        int riverExtraRadius = Mth.ceil(liquidRadius) + padding;

        if (endX > 0) {
            maxX = Math.max(maxX, spiralMaxX + endX + riverExtraRadius);
            minZ = Math.min(minZ, spiralMinZ - riverExtraRadius);
            maxZ = Math.max(maxZ, spiralMaxZ + riverExtraRadius);
        } else if (endX < 0) {
            minX = Math.min(minX, spiralMinX + endX - riverExtraRadius);
            minZ = Math.min(minZ, spiralMinZ - riverExtraRadius);
            maxZ = Math.max(maxZ, spiralMaxZ + riverExtraRadius);
        }

        /*
         * Main tunnel carving clamps its lower bound to endY, but the lower fluid
         * ellipsoid can extend below that by liquidDepth.
         */
        int minY = endY
                - Mth.ceil(liquidDepth)
                - padding
                - 2;

        int maxY = origin.getY()
                + Mth.ceil(verticalRadius)
                + padding
                + 2;

        return new BoundingBox(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
    }

    protected void placePlacedFeaturesForCurrentStructureChunk(
            WorldGenLevel level,
            ChunkGenerator generator,
            ChunkPos chunkPos
    ) {
        if (this.placedFeatures.size() == 0) {
            return;
        }

        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        BoundingBox pieceBox = this.getBoundingBox();

        boolean intersectsPiece =
                chunkMaxX >= pieceBox.minX()
                        && chunkMinX <= pieceBox.maxX()
                        && chunkMaxZ >= pieceBox.minZ()
                        && chunkMinZ <= pieceBox.maxZ();

        if (!intersectsPiece) {
            return;
        }

        BlockPos chunkOrigin = new BlockPos(chunkMinX, 0, chunkMinZ);

        for (int i = 0; i < this.placedFeatures.size(); i++) {
            Holder<PlacedFeature> placedFeature = this.placedFeatures.get(i);

            RandomSource featureRandom = RandomSource.create(getPlacedFeatureSeed(chunkPos, i));

            placedFeature.value().place(
                    level,
                    generator,
                    featureRandom,
                    chunkOrigin
            );
        }
    }

    protected static double getPathRadius(
            float horizontalRadius,
            float centralPillarDiameter,
            float liquidRadius
    ) {
        float tunnelHorizontalRadius = horizontalRadius + liquidRadius;
        return centralPillarDiameter * 0.5D + tunnelHorizontalRadius;
    }

    protected long getPlacedFeatureSeed(ChunkPos chunkPos, int featureIndex) {
        long result = this.seed;

        result ^= (long) this.origin.getX() * 341873128712L;
        result ^= (long) this.origin.getY() * 132897987541L;
        result ^= (long) this.origin.getZ() * 42317861L;

        result ^= chunkPos.toLong() * -7046029254386353131L;
        result ^= (long) featureIndex * 82520L;

        return result;
    }

    protected boolean wouldBottomOfEllipsoidCarveIntoTargetYPlusOne(double centerY) {
        double bottomOfEllipsoid = centerY - this.verticalRadius - this.liquidRadius - 2;
        return bottomOfEllipsoid <= endY;
    }

    protected void carveEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ,
            int maxCarveY
    ) {
        double localHorizontalRadius = getTunnelHorizontalRadius();
        double localVerticalRadius = this.verticalRadius;

        this.activeCarveCenterX = centerX;
        this.activeCarveCenterY = centerY;
        this.activeCarveCenterZ = centerZ;
        this.activeCarveHorizontalRadius = localHorizontalRadius;
        this.activeCarveVerticalRadius = localVerticalRadius;

        int minX = Mth.floor(centerX - localHorizontalRadius) - 1;
        int maxX = Mth.floor(centerX + localHorizontalRadius) + 1;
        int minY = Mth.floor(centerY - localVerticalRadius) - 1;
        int maxY = Mth.floor(centerY + localVerticalRadius) + 1;
        int minZ = Mth.floor(centerZ - localHorizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + localHorizontalRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        minY = Math.max(minY, endY);
        maxY = Math.min(maxY, maxCarveY);

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

        carveLowerFluidEllipsoid(
                level,
                box,
                centerX,
                centerY - 1,
                centerZ,
                localVerticalRadius,
                maxCarveY
        );
    }

    protected void carveLowerFluidEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double mainCenterX,
            double mainCenterY,
            double mainCenterZ,
            double mainVerticalRadius,
            int maxClearY
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

        maxY = Math.min(maxY, maxClearY);

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
                    clearBlockAboveFluid(level, box, pos, maxClearY);
                    clearBlockAboveFluid(level, box, pos.above(), maxClearY);

                    placedAnyFluidInColumn = true;
                    lowestPlacedFluidY = Math.min(lowestPlacedFluidY, y);
                }

                if (placedAnyFluidInColumn) {
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

    protected void decorateRiverColumnSurfaces(
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
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = lowestPlacedFluidY; y <= maxY; y++) {
            pos.set(x, y, z);

            if (!box.isInside(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (state.getFluidState().isEmpty()) {
                break;
            }

            decorateRiverColumnBoundaries(level, pos);
        }
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
