package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
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

public abstract class AbstractSpiralCavePiece extends StructurePiece {

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
                liquidRadius,
                endY,
                endX
        ));

        this.origin = origin;
        this.endY = endY;
        this.endX= endX;
        this.seed = seed;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.centralPillarDiameter = centralPillarDiameter;
        this.minFloorThickness = minFloorThickness;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;
        this.placedFeatures = placedFeatures;

        double pathRadius = getPathRadius(
                horizontalRadius,
                centralPillarDiameter,
                liquidRadius
        );

        this.pillarCenterX = origin.getX();
        this.pillarCenterZ = origin.getZ() + pathRadius;
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

        double pathRadius = getPathRadius(
                horizontalRadius,
                centralPillarDiameter,
                liquidRadius
        );

        this.pillarCenterX = origin.getX();
        this.pillarCenterZ = origin.getZ() + pathRadius;
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
        tag.putInt("EndX", this.endX); // <-- missing

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

    protected float getTunnelHorizontalRadius() {
        return this.horizontalRadius + this.liquidRadius;
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

    protected boolean isNaturalReplaceableSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
        return isNaturalReplaceableSolid(state)
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    protected boolean isNaturalReplaceableSolid(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
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

        int maxSteps = 4096;

        for (int step = 0; step < maxSteps; step++) {
            double x = this.pillarCenterX + Math.cos(yaw) * pathRadius;
            double z = this.pillarCenterZ + Math.sin(yaw) * pathRadius;

            if (wouldBottomOfEllipsoidCarveIntoTargetYPlusOne(y)) {
                break;
            }

            int maxCarveY = this.origin.getY();

            carveEllipsoid(level, box, x, y, z, maxCarveY);

            lastX = x;
            lastY = y;
            lastZ = z;

            y -= minDropPerStep;
            yaw += turnPerStep;
        }
        placePlacedFeaturesForCurrentStructureChunk(level, generator, chunkPos);
    }

    protected void continueSurfaceRiverPositiveX(
            WorldGenLevel level,
            BoundingBox box,
            double startX,
            double fluidCenterY,
            double centerZ
    ) {
        if (this.endX <= 0 || this.liquidRadius <= 0.0F || this.liquidDepth <= 0.0F) {
            return;
        }

        double targetX = startX + this.endX;
        int maxClearY = this.origin.getY();

        for (double centerX = startX; centerX <= targetX; centerX += 1.0D) {
            carveStraightRiverColumn(level, box, centerX, fluidCenterY, centerZ, maxClearY);
        }
    }

    protected void carveStraightRiverColumn(
            WorldGenLevel level,
            BoundingBox box,
            double centerX,
            double fluidCenterY,
            double centerZ,
            int maxClearY
    ) {
        int minX = Mth.floor(centerX - this.liquidRadius) - 1;
        int maxX = Mth.floor(centerX + this.liquidRadius) + 1;
        int minY = Mth.floor(fluidCenterY - this.liquidDepth);
        int maxY = Mth.floor(fluidCenterY) + 1;
        int minZ = Mth.floor(centerZ - this.liquidRadius) - 1;
        int maxZ = Mth.floor(centerZ + this.liquidRadius) + 1;

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

                    if (!isInsideStraightRiverEllipsoid(
                            x,
                            y,
                            z,
                            centerX,
                            fluidCenterY,
                            centerZ
                    )) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (!canReplaceWithFluid(oldState)) {
                        continue;
                    }

                    level.setBlock(pos, fluidState, Block.UPDATE_CLIENTS);
                    clearBlockAboveFluid(level, box, pos, maxClearY);

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
                            centerX,
                            fluidCenterY,
                            centerZ
                    );
                }
            }
        }
    }

    protected boolean isInsideStraightRiverEllipsoid(
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

    protected static BoundingBox makeBoundingBox(
            BlockPos origin,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
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

        double pillarCenterX = origin.getX();
        double pillarCenterZ = origin.getZ() + pathRadius;

        double spiralOuterRadius = pathRadius + tunnelHorizontalRadius;

        int padding = 3;

        int minX = Mth.floor(pillarCenterX - spiralOuterRadius) - padding;
        int maxX = Mth.ceil(pillarCenterX + spiralOuterRadius) + padding;
        int minZ = Mth.floor(pillarCenterZ - spiralOuterRadius) - padding;
        int maxZ = Mth.ceil(pillarCenterZ + spiralOuterRadius) + padding;

        int minY = endY
                - Mth.ceil(verticalRadius)
                - Mth.ceil(liquidRadius)
                - padding;

        int maxY = origin.getY() + Mth.ceil(verticalRadius) + 48;

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
        double bottomOfEllipsoid = centerY - this.verticalRadius;
        return bottomOfEllipsoid <= endY + 1;
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
                centerY,
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