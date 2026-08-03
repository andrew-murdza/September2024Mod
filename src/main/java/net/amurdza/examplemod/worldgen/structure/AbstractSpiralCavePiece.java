package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractSpiralCavePiece extends StructurePiece {

    private static final Map<GuaranteedMobSpawnKey, Integer> GUARANTEED_MOB_SPAWN_COUNTS = new HashMap<>();

    protected static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] {
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
    protected double activeCarveLowerVerticalRadius;
    protected double activeCarveUpperVerticalRadius;
    protected final List<CarveEllipsoid> activePieceCarveEllipsoids = new ArrayList<>();

    protected record CarveEllipsoid(
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double lowerVerticalRadius,
            double upperVerticalRadius,
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
        this(
                type,
                origin,
                seed,
                endY,
                endX,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                liquidDepth,
                liquidRadius,
                placedFeatures,
                Integer.MIN_VALUE
        );
    }

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
            HolderSet<PlacedFeature> placedFeatures,
            int upwardSpiralSurfaceY
    ) {
        super(type, 0, makeBoundingBox(
                origin,
                horizontalRadius,
                verticalRadius,
                centralPillarDiameter,
                liquidDepth,
                liquidRadius,
                endY,
                endX,
                upwardSpiralSurfaceY
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

    protected abstract BlockState getRiverFluidState(double caveCenterY, int x, int y, int z);

    protected float getTunnelHorizontalRadius() {
        return this.horizontalRadius + this.liquidRadius;
    }

    protected boolean shouldSkip(double relativeX, double relativeY, double relativeZ) {
        double relativeYSquared = relativeY * relativeY;
        return relativeX * relativeX
                + relativeYSquared * relativeYSquared
                + relativeZ * relativeZ >= 1.0D;
    }

    protected double maximumSpiralTurns() {
        return Double.POSITIVE_INFINITY;
    }

    protected double upwardSpiralSurfaceY() {
        return Double.NEGATIVE_INFINITY;
    }

    protected double upwardSpiralUpperVerticalRadius(double upwardTurns) {
        return this.verticalRadius;
    }

    protected boolean activeEllipsoidWouldCarve(BlockPos pos) {
        if (this.activeCarveHorizontalRadius <= 0.0D
                || this.activeCarveLowerVerticalRadius <= 0.0D
                || this.activeCarveUpperVerticalRadius <= 0.0D) {
            return false;
        }

        return ellipsoidWouldCarve(
                pos,
                this.activeCarveCenterX,
                this.activeCarveCenterY,
                this.activeCarveCenterZ,
                this.activeCarveHorizontalRadius,
                this.activeCarveLowerVerticalRadius,
                this.activeCarveUpperVerticalRadius
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
                    ellipsoid.lowerVerticalRadius(),
                    ellipsoid.upperVerticalRadius()
            )) {
                return true;
            }
        }

        return false;
    }

    protected int lowestCarvedFloorYAt(
            int blockX,
            int blockZ,
            double minimumEllipsoidCenterY,
            int fallbackY
    ) {
        int lowestFloorY = fallbackY;

        for (CarveEllipsoid ellipsoid : this.activePieceCarveEllipsoids) {
            if (ellipsoid.centerY() < minimumEllipsoidCenterY) {
                continue;
            }

            double relativeX = (blockX + 0.5D - ellipsoid.centerX()) / ellipsoid.horizontalRadius();
            double relativeZ = (blockZ + 0.5D - ellipsoid.centerZ()) / ellipsoid.horizontalRadius();
            double remainingVerticalShape = 1.0D - relativeX * relativeX - relativeZ * relativeZ;

            if (remainingVerticalShape <= 0.0D) {
                continue;
            }

            double verticalScale = Math.pow(remainingVerticalShape, 0.25D);
            double lowerBoundaryY = ellipsoid.centerY()
                    - ellipsoid.lowerVerticalRadius() * verticalScale;
            lowestFloorY = Math.min(lowestFloorY, Mth.floor(lowerBoundaryY - 0.5D));
        }

        return lowestFloorY;
    }

    private boolean ellipsoidWouldCarve(
            BlockPos pos,
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double lowerVerticalRadius,
            double upperVerticalRadius
    ) {
        if (horizontalRadius <= 0.0D
                || lowerVerticalRadius <= 0.0D
                || upperVerticalRadius <= 0.0D) {
            return false;
        }

        double relativeX = (pos.getX() + 0.5D - centerX) / horizontalRadius;
        double blockCenterY = pos.getY() + 0.5D;
        double localVerticalRadius = blockCenterY < centerY
                ? lowerVerticalRadius
                : upperVerticalRadius;
        double relativeY = (blockCenterY - centerY) / localVerticalRadius;
        double relativeZ = (pos.getZ() + 0.5D - centerZ) / horizontalRadius;

        return !shouldSkip(relativeX, relativeY, relativeZ);
    }

    protected static double turnPerStepForPathRadius(double pathRadius) {
        return 1.0D / Math.max(pathRadius, 1.0D);
    }

    protected void carveBlock(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos.MutableBlockPos pos
    ) {
        BlockState oldState = level.getBlockState(pos);

        if (!canReplace(oldState)) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        clearUnsupportedBlockAbove(level, box, pos);
        decorateCaveBoundaries(level, box, pos);
        afterCarveBlock(level, pos, oldState);
    }

    protected void afterCarveBlock(WorldGenLevel level, BlockPos carvedPos, BlockState oldState) {
    }

    private void clearUnsupportedBlockAbove(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos carvedPos
    ) {
        BlockPos above = carvedPos.above();
        if (!box.isInside(above)) {
            return;
        }

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
        Block oreHostBlock = oreHostReplacementForState(state);

        if (oreHostBlock != null) {
            return oreHostBlock;
        }

        return replacementBlock;
    }

    protected Block oreHostReplacementForState(BlockState state) {
        if (state.is(ModTags.Blocks.stoneOres)) {
            return Blocks.STONE;
        }

        if (state.is(ModTags.Blocks.deepslateOres)) {
            return Blocks.DEEPSLATE;
        }

        if (state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS)) {
            return Blocks.NETHERRACK;
        }

        if (state.is(ModBlocks.SOUL_SOIL_GOLD_ORE.get())
                || state.is(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get())) {
            return Blocks.SOUL_SOIL;
        }

        if (state.is(ModBlocks.SOUL_SAND_GOLD_ORE.get())
                || state.is(ModBlocks.SOUL_SAND_QUARTZ_ORE.get())) {
            return Blocks.SOUL_SAND;
        }

        if (state.is(ModBlocks.BLACKSTONE_GOLD_ORE.get())
                || state.is(ModBlocks.BLACKSTONE_QUARTZ_ORE.get())) {
            return Blocks.BLACKSTONE;
        }

        if (state.is(ModBlocks.BASALT_GOLD_ORE.get())
                || state.is(ModBlocks.BASALT_QUARTZ_ORE.get())) {
            return Blocks.BASALT;
        }

        return null;
    }

    protected Block overworldStoneReplacement(BlockPos pos, BlockState state) {
        if (pos.getY() < 0) {
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

    private static boolean isProtectedDecoratedSurface(BlockState state) {
        return state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER)
                || state.is(Blocks.END_STONE);
    }

    protected void decorateCaveBoundaries(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos carvedPos
    ) {
        BlockPos surfacePos = carvedPos.below();

        if (box.isInside(surfacePos) && canReplace(level, surfacePos)) {
            decorateCaveFloor(level, surfacePos);
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos wallPos = carvedPos.relative(direction);

            if (box.isInside(wallPos) && canReplace(level, wallPos)) {
                decorateCaveWall(level, wallPos, direction);
            }
        }

        BlockPos ceilingPos = carvedPos.above();

        if (box.isInside(ceilingPos) && canReplace(level, ceilingPos)) {
            decorateCaveCeiling(level, ceilingPos);
        }
    }

    protected void decorateRiverColumnBoundaries(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos carvedPos
    ) {
        BlockPos surfacePos = carvedPos.below();

        if (box.isInside(surfacePos) && canReplace(level, surfacePos)) {
            decorateRiverFloor(level, surfacePos);
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos wallPos = carvedPos.relative(direction);

            if (box.isInside(wallPos) && canReplace(level, wallPos)) {
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
        decorateCaveBoundaries(level, box, above);
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
        List<CarveEllipsoid> chunkEllipsoids = new ArrayList<>();

        double finalCenterY = this.endY + this.verticalRadius + this.liquidDepth + 2.0D;
        int maxSteps = Math.max(1, Mth.ceil((y - finalCenterY) / minDropPerStep) + 1);
        double maximumTurns = maximumSpiralTurns();
        if (Double.isFinite(maximumTurns)) {
            maxSteps = Math.min(maxSteps, Math.max(1, Mth.ceil(stepsPerTurn * maximumTurns)));
        }

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

            CarveEllipsoid ellipsoid = new CarveEllipsoid(
                    x,
                    y,
                    z,
                    getTunnelHorizontalRadius(),
                    this.verticalRadius,
                    this.verticalRadius,
                    maxCarveYForEllipsoid(y)
            );

            if (ellipsoidIntersectsBox(ellipsoid, box, 2)) {
                chunkEllipsoids.add(ellipsoid);
            }

            y -= minDropPerStep;
            yaw += turnPerStep;
        }

        if (capturedFirstStep) {
            addStraightExitTunnelPositiveXEllipsoids(chunkEllipsoids, box, firstX, firstY, firstZ);
        }

        addUpwardSpiralEllipsoids(
                chunkEllipsoids,
                box,
                pathRadius,
                turnPerStep,
                minDropPerStep,
                3 * Math.PI * 0.5D,
                this.origin.getY()
        );

        this.activePieceCarveEllipsoids.clear();
        this.activePieceCarveEllipsoids.addAll(chunkEllipsoids);

        for (CarveEllipsoid ellipsoid : chunkEllipsoids) {
            carveEllipsoid(
                    level,
                    box,
                    ellipsoid.centerX(),
                    ellipsoid.centerY(),
                    ellipsoid.centerZ(),
                    ellipsoid.horizontalRadius(),
                    ellipsoid.lowerVerticalRadius(),
                    ellipsoid.upperVerticalRadius(),
                    ellipsoid.maxCarveY()
            );
        }

        afterCarvingBeforeFeatures(
                level,
                random,
                box,
                chunkPos,
                List.copyOf(chunkEllipsoids)
        );
        placePlacedFeaturesForCurrentStructureChunk(level, generator, chunkPos);
        afterPostProcess(level, random, box, chunkPos);
        this.activePieceCarveEllipsoids.clear();
    }

    private void addUpwardSpiralEllipsoids(
            List<CarveEllipsoid> ellipsoids,
            BoundingBox box,
            double pathRadius,
            double turnPerStep,
            double risePerStep,
            double startYaw,
            double startCenterY
    ) {
        double targetSurfaceY = upwardSpiralSurfaceY();
        if (!Double.isFinite(targetSurfaceY)) {
            return;
        }

        double targetCenterY = targetSurfaceY + this.verticalRadius + 1.0D;
        if (targetCenterY <= startCenterY) {
            return;
        }

        double stepsPerTurn = Math.PI * 2.0D / Math.abs(turnPerStep);
        int upwardSteps = Math.max(1, Mth.ceil((targetCenterY - startCenterY) / risePerStep));
        for (int step = 1; step <= upwardSteps; step++) {
            double centerY = Math.min(startCenterY + step * risePerStep, targetCenterY);
            double fractionalStep = (centerY - startCenterY) / risePerStep;
            double upwardTurns = fractionalStep / stepsPerTurn;
            double upperVerticalRadius = upwardSpiralUpperVerticalRadius(upwardTurns);
            double yaw = startYaw - turnPerStep * fractionalStep;
            double x = this.pillarCenterX + Math.cos(yaw) * pathRadius;
            double z = this.pillarCenterZ + Math.sin(yaw) * pathRadius;
            CarveEllipsoid ellipsoid = new CarveEllipsoid(
                    x,
                    centerY,
                    z,
                    getTunnelHorizontalRadius(),
                    this.verticalRadius,
                    upperVerticalRadius,
                    maxCarveYForEllipsoid(centerY, upperVerticalRadius)
            );

            if (ellipsoidIntersectsBox(ellipsoid, box, 2)) {
                ellipsoids.add(ellipsoid);
            }
        }
    }

    private boolean ellipsoidIntersectsBox(
            CarveEllipsoid ellipsoid,
            BoundingBox box,
            int margin
    ) {
        double horizontalReach = ellipsoid.horizontalRadius() + margin;
        double lowerVerticalReach = ellipsoid.lowerVerticalRadius() + this.liquidDepth + margin;
        double upperVerticalReach = ellipsoid.upperVerticalRadius() + margin;

        return ellipsoid.centerX() + horizontalReach >= box.minX()
                && ellipsoid.centerX() - horizontalReach <= box.maxX()
                && ellipsoid.centerY() + upperVerticalReach >= box.minY()
                && ellipsoid.centerY() - lowerVerticalReach <= box.maxY()
                && ellipsoid.centerZ() + horizontalReach >= box.minZ()
                && ellipsoid.centerZ() - horizontalReach <= box.maxZ();
    }

    protected void afterPostProcess(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos
    ) {
    }

    protected void afterCarvingBeforeFeatures(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            List<CarveEllipsoid> chunkEllipsoids
    ) {
    }

    protected boolean isOriginChunk(ChunkPos chunkPos) {
        return chunkPos.x == (this.origin.getX() >> 4)
                && chunkPos.z == (this.origin.getZ() >> 4);
    }

    protected void spawnGuaranteedMobsOncePerStructure(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int totalCount,
            Predicate<BlockState> preferredFloor
    ) {
        spawnGuaranteedMobsOncePerStructure(level, random, box, type, totalCount, preferredFloor, true);
    }

    protected void spawnGuaranteedMobsStrictlyOncePerStructure(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int totalCount,
            Predicate<BlockState> preferredFloor
    ) {
        spawnGuaranteedMobsOncePerStructure(level, random, box, type, totalCount, preferredFloor, false);
    }

    protected void spawnGuaranteedMobsStrictlyOncePerStructure(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int totalCount,
            Predicate<BlockState> preferredFloor,
            Predicate<BlockPos> positionFilter
    ) {
        spawnGuaranteedMobsOncePerStructure(
                level,
                random,
                box,
                type,
                totalCount,
                preferredFloor,
                false,
                positionFilter
        );
    }

    private void spawnGuaranteedMobsOncePerStructure(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int totalCount,
            Predicate<BlockState> preferredFloor,
            boolean allowFallback
    ) {
        spawnGuaranteedMobsOncePerStructure(
                level,
                random,
                box,
                type,
                totalCount,
                preferredFloor,
                allowFallback,
                this::isOnInnerSideOfRiver
        );
    }

    private void spawnGuaranteedMobsOncePerStructure(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int totalCount,
            Predicate<BlockState> preferredFloor,
            boolean allowFallback,
            Predicate<BlockPos> positionFilter
    ) {
        if (totalCount <= 0) {
            return;
        }

        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (entityKey == null) {
            return;
        }

        GuaranteedMobSpawnKey key = new GuaranteedMobSpawnKey(this.seed, this.origin.asLong(), entityKey);
        synchronized (GUARANTEED_MOB_SPAWN_COUNTS) {
            int alreadySpawned = GUARANTEED_MOB_SPAWN_COUNTS.getOrDefault(key, 0);
            int remaining = totalCount - alreadySpawned;
            if (remaining <= 0) {
                return;
            }

            int spawned = spawnGuaranteedMobs(
                    level,
                    random,
                    box,
                    type,
                    remaining,
                    preferredFloor,
                    allowFallback,
                    alreadySpawned,
                    positionFilter
            );
            if (spawned > 0) {
                GUARANTEED_MOB_SPAWN_COUNTS.put(key, alreadySpawned + spawned);
            }
        }
    }

    protected int spawnGuaranteedMobs(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int count,
            Predicate<BlockState> preferredFloor
    ) {
        return spawnGuaranteedMobs(level, random, box, type, count, preferredFloor, true, 0);
    }

    protected int spawnGuaranteedMobs(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int count,
            Predicate<BlockState> preferredFloor,
            boolean allowFallback
    ) {
        return spawnGuaranteedMobs(level, random, box, type, count, preferredFloor, allowFallback, 0);
    }

    private int spawnGuaranteedMobs(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int count,
            Predicate<BlockState> preferredFloor,
            boolean allowFallback,
            int firstStructureSpawnIndex
    ) {
        return spawnGuaranteedMobs(
                level,
                random,
                box,
                type,
                count,
                preferredFloor,
                allowFallback,
                firstStructureSpawnIndex,
                this::isOnInnerSideOfRiver
        );
    }

    private int spawnGuaranteedMobs(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox box,
            EntityType<? extends Mob> type,
            int count,
            Predicate<BlockState> preferredFloor,
            boolean allowFallback,
            int firstStructureSpawnIndex,
            Predicate<BlockPos> positionFilter
    ) {
        if (count <= 0) {
            return 0;
        }

        List<BlockPos> preferred = new ArrayList<>();
        List<BlockPos> fallback = new ArrayList<>();
        collectMobSpawnPositions(level, box, preferredFloor, positionFilter, preferred, fallback);

        shuffle(preferred, random);
        shuffle(fallback, random);

        Set<BlockPos> used = new HashSet<>();
        int spawned = spawnFromCandidates(
                level,
                random,
                type,
                count,
                preferred,
                used,
                false,
                firstStructureSpawnIndex
        );

        if (allowFallback && spawned < count) {
            spawned += spawnFromCandidates(
                    level,
                    random,
                    type,
                    count - spawned,
                    fallback,
                    used,
                    true,
                    firstStructureSpawnIndex + spawned
            );
        }

        return spawned;
    }

    private void collectMobSpawnPositions(
            WorldGenLevel level,
            BoundingBox box,
            Predicate<BlockState> preferredFloor,
            Predicate<BlockPos> positionFilter,
            List<BlockPos> preferred,
            List<BlockPos> fallback
    ) {
        BoundingBox pieceBox = this.getBoundingBox();

        int minX = Math.max(box.minX(), pieceBox.minX());
        int maxX = Math.min(box.maxX(), pieceBox.maxX());
        int minY = Math.max(box.minY() + 1, pieceBox.minY() + 1);
        int maxY = Math.min(box.maxY() - 1, pieceBox.maxY() - 1);
        int minZ = Math.max(box.minZ(), pieceBox.minZ());
        int maxZ = Math.min(box.maxZ(), pieceBox.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos feet = new BlockPos(x, y, z);
                    BlockPos floorPos = feet.below();
                    BlockState floor = level.getBlockState(floorPos);

                    if (!isValidMobSpawnSpace(level, feet, floorPos, floor)) {
                        continue;
                    }

                    if (!positionFilter.test(feet)) {
                        continue;
                    }

                    fallback.add(feet);
                    if (preferredFloor.test(floor)) {
                        preferred.add(feet);
                    }
                }
            }
        }
    }

    private boolean isOnInnerSideOfRiver(BlockPos pos) {
        double dx = pos.getX() + 0.5D - this.pillarCenterX;
        double dz = pos.getZ() + 0.5D - this.pillarCenterZ;
        double radialDistance = Math.sqrt(dx * dx + dz * dz);
        double innerRiverBankRadius = getPathRadius(
                this.horizontalRadius,
                this.centralPillarDiameter,
                this.liquidRadius
        ) - this.liquidRadius;

        return radialDistance < innerRiverBankRadius;
    }

    private static boolean isValidMobSpawnSpace(
            WorldGenLevel level,
            BlockPos feet,
            BlockPos floorPos,
            BlockState floor
    ) {
        return level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir()
                && floor.getFluidState().isEmpty()
                && floor.isFaceSturdy(level, floorPos, Direction.UP);
    }

    protected void configureGuaranteedMob(Mob mob, int structureSpawnIndex) {
    }

    private int spawnFromCandidates(
            WorldGenLevel level,
            RandomSource random,
            EntityType<? extends Mob> type,
            int count,
            List<BlockPos> candidates,
            Set<BlockPos> used,
            boolean allowRepeats,
            int firstStructureSpawnIndex
    ) {
        if (candidates.isEmpty()) {
            return 0;
        }

        int spawned = 0;
        int attempts = allowRepeats ? count * candidates.size() : candidates.size();

        for (int i = 0; i < attempts && spawned < count; i++) {
            BlockPos pos = candidates.get(i % candidates.size());
            if (!allowRepeats && !used.add(pos)) {
                continue;
            }

            Mob mob = type.create(level.getLevel());
            if (mob == null) {
                continue;
            }

            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
            configureGuaranteedMob(mob, firstStructureSpawnIndex + spawned);
            StructureSpawnedMobData.mark(mob, pos);

            if (level.addFreshEntity(mob)) {
                used.add(pos);
                spawned++;
            }
        }

        return spawned;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            Collections.swap(values, i, random.nextInt(i + 1));
        }
    }

    private record GuaranteedMobSpawnKey(
            long structureSeed,
            long origin,
            ResourceLocation entityType
    ) {
    }

    protected void addStraightExitTunnelPositiveXEllipsoids(
            List<CarveEllipsoid> ellipsoids,
            BoundingBox box,
            double startX,
            double centerY,
            double centerZ
    ) {
        if (this.endX <= 0) {
            return;
        }

        double targetX = startX + this.endX;
        int maxCarveY = maxCarveYForEllipsoid(centerY);

        for (double centerX = startX; centerX <= targetX; centerX += 1.0D) {
            CarveEllipsoid ellipsoid = new CarveEllipsoid(
                    centerX,
                    centerY,
                    centerZ,
                    getTunnelHorizontalRadius(),
                    this.verticalRadius,
                    this.verticalRadius,
                    maxCarveY
            );

            if (ellipsoidIntersectsBox(ellipsoid, box, 2)) {
                ellipsoids.add(ellipsoid);
            }
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
            int endX,
            int upwardSpiralSurfaceY
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

        int carveMargin = 2;

        int spiralMinX = Mth.floor(pillarCenterX - spiralOuterRadius) - carveMargin;
        int spiralMaxX = Mth.ceil(pillarCenterX + spiralOuterRadius) + carveMargin;
        int spiralMinZ = Mth.floor(pillarCenterZ - spiralOuterRadius) - carveMargin;
        int spiralMaxZ = Mth.ceil(pillarCenterZ + spiralOuterRadius) + carveMargin;

        int minX = spiralMinX;
        int maxX = spiralMaxX;
        int minZ = spiralMinZ;
        int maxZ = spiralMaxZ;

        /*
         * The straight exit starts at yaw 3pi/2: origin X and one path radius
         * north of the pillar. Bound that actual segment instead of adding endX
         * to the entire spiral box.
         */
        if (endX != 0) {
            double straightStartX = pillarCenterX;
            double straightEndX = straightStartX + endX;
            double straightCenterZ = pillarCenterZ - pathRadius;
            double straightCrossSectionRadius = tunnelHorizontalRadius;
            minX = Math.min(
                    minX,
                    Mth.floor(Math.min(straightStartX, straightEndX) - tunnelHorizontalRadius) - carveMargin
            );
            maxX = Math.max(
                    maxX,
                    Mth.ceil(Math.max(straightStartX, straightEndX) + tunnelHorizontalRadius) + carveMargin
            );
            minZ = Math.min(
                    minZ,
                    Mth.floor(straightCenterZ - straightCrossSectionRadius) - carveMargin
            );
            maxZ = Math.max(
                    maxZ,
                    Mth.ceil(straightCenterZ + straightCrossSectionRadius) + carveMargin
            );
        }

        /*
         * Main tunnel carving clamps its lower bound to endY, but the lower fluid
         * ellipsoid can extend below that by liquidDepth.
         */
        int minY = mainAirCarveMinY(endY)
                - Mth.ceil(liquidDepth)
                - 2;

        int maxY = origin.getY()
                + Mth.ceil(verticalRadius)
                + 8;
        if (upwardSpiralSurfaceY != Integer.MIN_VALUE) {
            maxY = Math.max(
                    maxY,
                    upwardSpiralSurfaceY + Mth.ceil(2.0F * verticalRadius) + 3
            );
        }

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
        double bottomOfEllipsoid = centerY - this.verticalRadius - this.liquidDepth - 2;
        return bottomOfEllipsoid <= endY;
    }

    protected int maxCarveYForEllipsoid(double centerY) {
        return maxCarveYForEllipsoid(centerY, this.verticalRadius);
    }

    protected int maxCarveYForEllipsoid(double centerY, double upperVerticalRadius) {
        return Mth.ceil(centerY + upperVerticalRadius + 2.0D);
    }

    protected static int mainAirCarveMinY(int endY) {
        return endY - 2;
    }

    protected void carveEllipsoid(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double centerY,
            double centerZ,
            double horizontalRadius,
            double lowerVerticalRadius,
            double upperVerticalRadius,
            int maxCarveY
    ) {
        this.activeCarveCenterX = centerX;
        this.activeCarveCenterY = centerY;
        this.activeCarveCenterZ = centerZ;
        this.activeCarveHorizontalRadius = horizontalRadius;
        this.activeCarveLowerVerticalRadius = lowerVerticalRadius;
        this.activeCarveUpperVerticalRadius = upperVerticalRadius;

        int minX = Mth.floor(centerX - horizontalRadius) - 1;
        int maxX = Mth.floor(centerX + horizontalRadius) + 1;
        int minY = Mth.floor(centerY - lowerVerticalRadius) - 1;
        int maxY = Mth.floor(centerY + upperVerticalRadius) + 1;
        int minZ = Mth.floor(centerZ - horizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + horizontalRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        minY = Math.max(minY, mainAirCarveMinY(this.endY));
        maxY = Math.min(maxY, maxCarveY);

        minX = Math.max(minX, box.minX());
        maxX = Math.min(maxX, box.maxX());
        minY = Math.max(minY, box.minY());
        maxY = Math.min(maxY, box.maxY());
        minZ = Math.max(minZ, box.minZ());
        maxZ = Math.min(maxZ, box.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);

                    double relativeX = (x + 0.5D - centerX) / horizontalRadius;
                    double blockCenterY = y + 0.5D;
                    double localVerticalRadius = blockCenterY < centerY
                            ? lowerVerticalRadius
                            : upperVerticalRadius;
                    double relativeY = (blockCenterY - centerY) / localVerticalRadius;
                    double relativeZ = (z + 0.5D - centerZ) / horizontalRadius;

                    if (shouldSkip(relativeX, relativeY, relativeZ)) {
                        continue;
                    }

                    carveBlock(level, box, pos);
                }
            }
        }

        carveLowerFluidEllipsoid(
                level,
                box,
                centerX,
                centerY - 1,
                centerZ,
                lowerVerticalRadius,
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

        minX = Math.max(minX, box.minX());
        maxX = Math.min(maxX, box.maxX());
        minY = Math.max(minY, box.minY());
        maxY = Math.min(maxY, box.maxY());
        minZ = Math.max(minZ, box.minZ());
        maxZ = Math.min(maxZ, box.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean placedAnyFluidInColumn = false;
                int lowestPlacedFluidY = Integer.MAX_VALUE;

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);

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

                    BlockState fluidState = getRiverFluidState(mainCenterY, x, y, z);

                    if (fluidState == null) {
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

            decorateRiverColumnBoundaries(level, box, pos);
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
                + relativeY * relativeY
                + relativeZ * relativeZ <= 1.0D;
    }
}
