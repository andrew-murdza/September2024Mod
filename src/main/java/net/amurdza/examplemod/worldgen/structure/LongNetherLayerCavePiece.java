package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LongNetherLayerCavePiece extends StructurePiece {
    private static final int TARGET_Y = -126;
    private static final float FLOOR_LEVEL = -1.0F;

    private enum NetherLayer {
        DEEP_DARK,
        SOUL_SAND_VALLEY,
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
    private final float centralPillarDiameterExtra;
    private final float minFloorThickness;
    private final float pitchLower;
    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxDeepDarkY;
    private final int maxSoulSandValleyY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltas;

    public LongNetherLayerCavePiece(
            BlockPos origin,
            long seed,
            float horizontalRadius,
            float verticalRadius,
            int lavaLevel,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            float liquidDepth,
            float liquidRadius,
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltas
    ) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), 0, makeBoundingBox(
                origin,
                horizontalRadius,
                verticalRadius,
                lavaLevel,
                centralPillarDiameterExtra,
                minFloorThickness,
                pitchLower,
                liquidDepth,
                liquidRadius
        ));

        this.origin = origin;
        this.seed = seed;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.lavaLevel = lavaLevel;
        this.centralPillarDiameterExtra = centralPillarDiameterExtra;
        this.minFloorThickness = minFloorThickness;
        this.pitchLower = pitchLower;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSandValleyY = maxSoulSandValleyY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltas = maxBasaltDeltas;
    }

    @SuppressWarnings("unused")
    public LongNetherLayerCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.seed = tag.getLong("Seed");
        this.horizontalRadius = tag.getFloat("HorizontalRadius");
        this.verticalRadius = tag.getFloat("VerticalRadius");
        this.lavaLevel = tag.getInt("LavaLevel");
        this.centralPillarDiameterExtra = tag.getFloat("CentralPillarDiameterExtra");
        this.minFloorThickness = tag.getFloat("MinFloorThickness");
        this.pitchLower = tag.getFloat("PitchLower");
        this.liquidDepth = tag.getFloat("LiquidDepth");

        this.liquidRadius = tag.contains("LiquidRadius")
                ? tag.getFloat("LiquidRadius")
                : tag.getFloat("LiquidWidth");

        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxSoulSandValleyY = tag.getInt("MaxSoulSandValleyY");
        this.maxWarpedForestY = tag.getInt("MaxWarpedForestY");
        this.maxCrimsonForestY = tag.getInt("MaxCrimsonForestY");
        this.maxBasaltDeltas = tag.getInt("MaxBasaltDeltasY");
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
        tag.putFloat("CentralPillarDiameterExtra", this.centralPillarDiameterExtra);
        tag.putFloat("MinFloorThickness", this.minFloorThickness);
        tag.putFloat("PitchLower", this.pitchLower);
        tag.putFloat("LiquidDepth", this.liquidDepth);
        tag.putFloat("LiquidRadius", this.liquidRadius);

        tag.putInt("MaxDeepDarkY", this.maxDeepDarkY);
        tag.putInt("MaxSoulSandValleyY", this.maxSoulSandValleyY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltas);
    }

    private boolean shouldSkip(double relativeX, double relativeY, double relativeZ) {
        if (relativeY <= FLOOR_LEVEL) {
            return true;
        }

        return relativeX * relativeX
                + relativeY * relativeY
                + relativeZ * relativeZ >= 1.0D;
    }

    private static float actualCentralPillarDiameter(
            float horizontalRadius,
            float centralPillarDiameterExtra,
            float minFloorThickness
    ) {
        return 2.0F * horizontalRadius
                + minFloorThickness
                + centralPillarDiameterExtra;
    }

    private static double turnPerStepForPathRadius(double pathRadius) {
        return 1.0D / Math.max(pathRadius, 1.0D);
    }

    private void carveBlock(WorldGenLevel level, BlockPos.MutableBlockPos pos) {
        BlockState oldState = level.getBlockState(pos);

        if (!canReplace(oldState) || pos.getY() < TARGET_Y) {
            return;
        }

        BlockState carvedState = pos.getY() <= this.lavaLevel
                ? Blocks.LAVA.defaultBlockState()
                : Blocks.AIR.defaultBlockState();

        level.setBlock(pos, carvedState, Block.UPDATE_CLIENTS);
        decorateCaveSurface(level, pos);
    }

    private boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }

    private void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos) {
        NetherLayer layer = getLayerAtY(carvedPos.getY());

        if (layer == NetherLayer.DEEP_DARK) {
            replaceAdjacentSolids(level, carvedPos, Blocks.SCULK.defaultBlockState());
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
        if (y > this.maxSoulSandValleyY) {
            return NetherLayer.DEEP_DARK;
        }
        if (y > this.maxWarpedForestY) {
            return NetherLayer.SOUL_SAND_VALLEY;
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
            case CRIMSON_FOREST -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
            case WARPED_FOREST -> Blocks.WARPED_NYLIUM.defaultBlockState();
            case SOUL_SAND_VALLEY, BASALT_DELTAS, DEEP_DARK, NONE -> null;
        };
    }

    private static void replaceAdjacentSolids(WorldGenLevel level, BlockPos carvedPos, BlockState replacement) {
        for (Direction direction : Direction.values()) {
            BlockPos sidePos = carvedPos.relative(direction);
            BlockState sideState = level.getBlockState(sidePos);

            if (isNaturalReplaceableSurface(level, sidePos, sideState)) {
                level.setBlock(sidePos, replacement, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isNaturalReplaceableSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.BEDROCK)
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private void replaceLavaFloorsWithBlackstone(WorldGenLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    pos.set(x, y, z);

                    BlockState state = level.getBlockState(pos);

                    if (!state.is(Blocks.LAVA)) {
                        continue;
                    }

                    BlockPos below = pos.below();
                    BlockState belowState = level.getBlockState(below);

                    if (shouldReplaceFloor(belowState)) {
                        level.setBlock(below, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static boolean shouldReplaceFloor(BlockState state) {
        return state.is(Blocks.GRAVEL)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT);
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
        RandomSource pathRandom = RandomSource.create(this.seed);

        float centralPillarDiameter = actualCentralPillarDiameter(
                this.horizontalRadius,
                this.centralPillarDiameterExtra,
                this.minFloorThickness
        );

        double pathRadius = centralPillarDiameter * 0.5D + this.horizontalRadius;
        double turnMagnitude = turnPerStepForPathRadius(pathRadius);
        double turnPerStep = pathRandom.nextBoolean() ? turnMagnitude : -turnMagnitude;

        double stepsPerTurn = (Math.PI * 2.0D) / Math.abs(turnPerStep);

        double carvedHeight = (1.0D - FLOOR_LEVEL) * this.verticalRadius;
        double requiredVerticalSeparation = carvedHeight * 1.1D + this.minFloorThickness;

        double minDropPerStep = requiredVerticalSeparation / stepsPerTurn;
        minDropPerStep = Mth.clamp(minDropPerStep, 0.01D, 0.85D);

        float pitch = (float) -Math.asin(minDropPerStep);

        double yaw = pathRandom.nextDouble() * Math.PI * 2.0D;
        double x = this.origin.getX();
        double y = this.origin.getY();
        double z = this.origin.getZ();

        int maxSteps = 4096;
        List<RiverPoint> riverPoints = new ArrayList<>();

        for (int step = 0; step < maxSteps && y > TARGET_Y; step++) {
            carveEllipsoid(level, box, x, y, z);

            if (y <= this.lavaLevel) {
                riverPoints.add(new RiverPoint(x, y, z));

                double horizontalStep = 1.0D;
                double verticalDrop = horizontalStep * this.pitchLower;

                x += Math.cos(yaw) * horizontalStep;
                y -= verticalDrop;
                z += Math.sin(yaw) * horizontalStep;
            } else {
                double horizontalStep = Mth.cos(pitch);

                x += Math.cos(yaw) * horizontalStep;
                y += Mth.sin(pitch);
                z += Math.sin(yaw) * horizontalStep;

                yaw += turnPerStep;
            }
        }

        placeLavaRiver(level, box, riverPoints);
        replaceLavaFloorsWithBlackstone(level, box);
    }

    private void carveEllipsoid(WorldGenLevel level, BoundingBox box, double centerX, double centerY, double centerZ) {
        double localVerticalRadius = getLocalVerticalRadius(centerY);

        int minX = Mth.floor(centerX - this.horizontalRadius) - 1;
        int maxX = Mth.floor(centerX + this.horizontalRadius) + 1;
        int minY = Mth.floor(centerY - localVerticalRadius) - 1;
        int maxY = Mth.floor(centerY + localVerticalRadius) + 1;
        int minZ = Mth.floor(centerZ - this.horizontalRadius) - 1;
        int maxZ = Mth.floor(centerZ + this.horizontalRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    double relativeX = (x + 0.5D - centerX) / this.horizontalRadius;
                    double relativeY = (y + 0.5D - centerY) / localVerticalRadius;
                    double relativeZ = (z + 0.5D - centerZ) / this.horizontalRadius;

                    if (shouldSkip(relativeX, relativeY, relativeZ)) {
                        continue;
                    }

                    carveBlock(level, pos);
                }
            }
        }
    }

    private record RiverPoint(double x, double y, double z) {
    }

    private void placeLavaRiver(WorldGenLevel level, BoundingBox box, List<RiverPoint> riverPoints) {
        if (this.liquidDepth <= 0.0F || this.liquidRadius <= 0.0F || riverPoints.size() < 2) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < riverPoints.size() - 1; i++) {
            RiverPoint current = riverPoints.get(i);
            RiverPoint next = riverPoints.get(i + 1);

            double pathDx = next.x - current.x;
            double pathDz = next.z - current.z;
            double pathHorizontalLength = Math.sqrt(pathDx * pathDx + pathDz * pathDz);

            if (pathHorizontalLength < 0.0001D) {
                continue;
            }

            double forwardX = pathDx / pathHorizontalLength;
            double forwardZ = pathDz / pathHorizontalLength;

            double currentOutwardX = current.x - this.origin.getX();
            double currentOutwardZ = current.z - this.origin.getZ();
            double currentOutwardLength = Math.sqrt(
                    currentOutwardX * currentOutwardX
                            + currentOutwardZ * currentOutwardZ
            );

            double nextOutwardX = next.x - this.origin.getX();
            double nextOutwardZ = next.z - this.origin.getZ();
            double nextOutwardLength = Math.sqrt(
                    nextOutwardX * nextOutwardX
                            + nextOutwardZ * nextOutwardZ
            );

            if (currentOutwardLength < 0.0001D || nextOutwardLength < 0.0001D) {
                continue;
            }

            currentOutwardX /= currentOutwardLength;
            currentOutwardZ /= currentOutwardLength;
            nextOutwardX /= nextOutwardLength;
            nextOutwardZ /= nextOutwardLength;

            int currentFloorY = getApproximateCarvedFloorY(current.y);
            int nextFloorY = getApproximateCarvedFloorY(next.y);

            int lavaSurfaceY = Math.min(currentFloorY, nextFloorY);

            double shorelineInset = this.liquidRadius * 0.65D;
            double riverDistanceFromPathCenter = Math.max(0.0D, this.horizontalRadius - shorelineInset);

            double currentRiverCenterX = current.x + currentOutwardX * riverDistanceFromPathCenter;
            double currentRiverCenterZ = current.z + currentOutwardZ * riverDistanceFromPathCenter;

            double nextRiverCenterX = next.x + nextOutwardX * riverDistanceFromPathCenter;
            double nextRiverCenterZ = next.z + nextOutwardZ * riverDistanceFromPathCenter;

            double riverDx = nextRiverCenterX - currentRiverCenterX;
            double riverDz = nextRiverCenterZ - currentRiverCenterZ;
            double riverLength = Math.sqrt(riverDx * riverDx + riverDz * riverDz);

            if (riverLength < 0.0001D) {
                continue;
            }

            double riverForwardX = riverDx / riverLength;
            double riverForwardZ = riverDz / riverLength;

            double riverHorizontalRadius = this.liquidRadius;
            double riverVerticalRadius = this.liquidDepth;

            int minX = Mth.floor(Math.min(currentRiverCenterX, nextRiverCenterX) - riverHorizontalRadius) - 2;
            int maxX = Mth.floor(Math.max(currentRiverCenterX, nextRiverCenterX) + riverHorizontalRadius) + 2;
            int minY = Mth.floor(lavaSurfaceY - riverVerticalRadius) - 2;
            int maxY = lavaSurfaceY + 2;
            int minZ = Mth.floor(Math.min(currentRiverCenterZ, nextRiverCenterZ) - riverHorizontalRadius) - 2;
            int maxZ = Mth.floor(Math.max(currentRiverCenterZ, nextRiverCenterZ) + riverHorizontalRadius) + 2;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        pos.set(x, y, z);

                        if (!box.isInside(pos)) {
                            continue;
                        }

                        double blockCenterX = x + 0.5D;
                        double blockCenterY = y + 0.5D;
                        double blockCenterZ = z + 0.5D;

                        double relX = blockCenterX - currentRiverCenterX;
                        double relZ = blockCenterZ - currentRiverCenterZ;

                        double along = relX * riverForwardX + relZ * riverForwardZ;

                        if (along < -1.0D || along > riverLength + 1.0D) {
                            continue;
                        }

                        double clampedAlong = Mth.clamp(along, 0.0D, riverLength);

                        double closestCenterX = currentRiverCenterX + riverForwardX * clampedAlong;
                        double closestCenterZ = currentRiverCenterZ + riverForwardZ * clampedAlong;

                        double radialX = blockCenterX - closestCenterX;
                        double radialZ = blockCenterZ - closestCenterZ;
                        double radialDistance = Math.sqrt(radialX * radialX + radialZ * radialZ);

                        double relativeHorizontal = radialDistance / riverHorizontalRadius;
                        double relativeVertical = (blockCenterY - lavaSurfaceY) / riverVerticalRadius;

                        if (relativeHorizontal * relativeHorizontal + relativeVertical * relativeVertical > 1.0D) {
                            continue;
                        }

                        BlockState oldState = level.getBlockState(pos);

                        if (!canReplace(oldState) && !oldState.is(Blocks.LAVA)) {
                            continue;
                        }

                        if (y <= lavaSurfaceY) {
                            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                        } else if (canReplace(oldState) || oldState.is(Blocks.LAVA)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }

            placeRiverFloor(
                    level,
                    box,
                    currentRiverCenterX,
                    lavaSurfaceY,
                    currentRiverCenterZ,
                    riverForwardX,
                    riverForwardZ,
                    riverLength,
                    riverHorizontalRadius,
                    riverVerticalRadius
            );
        }
    }

    private void placeRiverFloor(
            WorldGenLevel level,
            BoundingBox box,
            double riverCenterX,
            int lavaSurfaceY,
            double riverCenterZ,
            double forwardX,
            double forwardZ,
            double horizontalLength,
            double riverHorizontalRadius,
            double riverVerticalRadius
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int minX = Mth.floor(riverCenterX - riverHorizontalRadius) - 2;
        int maxX = Mth.floor(riverCenterX + forwardX * horizontalLength + riverHorizontalRadius) + 2;
        int minZ = Mth.floor(riverCenterZ - riverHorizontalRadius) - 2;
        int maxZ = Mth.floor(riverCenterZ + forwardZ * horizontalLength + riverHorizontalRadius) + 2;

        if (minX > maxX) {
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minZ > maxZ) {
            int temp = minZ;
            minZ = maxZ;
            maxZ = temp;
        }

        int minY = Mth.floor(lavaSurfaceY - riverVerticalRadius) - 3;
        int maxY = lavaSurfaceY;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);

                    if (!box.isInside(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);

                    if (!state.is(Blocks.LAVA)) {
                        continue;
                    }

                    BlockPos below = pos.below();
                    BlockState belowState = level.getBlockState(below);

                    if (canReplace(belowState) || shouldReplaceFloor(belowState)) {
                        level.setBlock(below, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private int getApproximateCarvedFloorY(double centerY) {
        double localVerticalRadius = getLocalVerticalRadius(centerY);
        return Mth.floor(centerY + FLOOR_LEVEL * localVerticalRadius + 0.5D);
    }

    private double getLocalVerticalRadius(double centerY) {
        double localVerticalRadius = this.verticalRadius;

        if (centerY <= this.lavaLevel) {
            double minCeilingY = this.lavaLevel + 9.0D;

            if (centerY + localVerticalRadius < minCeilingY) {
                localVerticalRadius = minCeilingY - centerY;
            }
        }

        return localVerticalRadius;
    }

    private static BoundingBox makeBoundingBox(
            BlockPos origin,
            float horizontalRadius,
            float verticalRadius,
            int lavaLevel,
            float centralPillarDiameterExtra,
            float minFloorThickness,
            float pitchLower,
            float liquidDepth,
            float liquidRadius
    ) {
        float centralPillarDiameter = actualCentralPillarDiameter(
                horizontalRadius,
                centralPillarDiameterExtra,
                minFloorThickness
        );

        float pathRadius = centralPillarDiameter * 0.5F + horizontalRadius;

        double lowerVerticalDrop = Math.max(0.0D, lavaLevel - TARGET_Y);
        double lowerHorizontalTravel = lowerVerticalDrop / pitchLower;

        float maxHorizontalCarveRadius = Math.max(horizontalRadius, liquidRadius);

        int totalRadius = Mth.ceil(pathRadius + maxHorizontalCarveRadius + lowerHorizontalTravel + 32.0D);

        int minY = Math.min(TARGET_Y - Mth.ceil(verticalRadius) - Mth.ceil(liquidDepth) - 16, -126);
        int maxY = Math.max(origin.getY() + Mth.ceil(verticalRadius) + 32, lavaLevel + 16);

        return new BoundingBox(
                origin.getX() - totalRadius,
                minY,
                origin.getZ() - totalRadius,
                origin.getX() + totalRadius,
                maxY,
                origin.getZ() + totalRadius
        );
    }
}