package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
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

public class NetherCaveStraightRiverPiece extends StructurePiece {
    private enum NetherLayer {
        DEEP_DARK,
        SOUL_SAND_VALLEY,
        WARPED_FOREST,
        CRIMSON_FOREST,
        BASALT_DELTAS,
        NONE
    }

    private final double startX;
    private final double endX;
    private final double fluidCenterY;
    private final double centerZ;
    private final int maxClearY;

    private final float liquidDepth;
    private final float liquidRadius;

    private final int maxSoulSandValleyY;
    private final int maxDeepDarkY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    public NetherCaveStraightRiverPiece(
            double startX,
            double endX,
            double fluidCenterY,
            double centerZ,
            int maxClearY,
            float liquidDepth,
            float liquidRadius,
            int maxSoulSandValleyY,
            int maxDeepDarkY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
        super(
                ModStructures.NETHER_CAVE_STRAIGHT_RIVER_PIECE.get(),
                0,
                makeBoundingBox(
                        startX,
                        endX,
                        fluidCenterY,
                        centerZ,
                        maxClearY,
                        liquidDepth,
                        liquidRadius
                )
        );

        this.startX = startX;
        this.endX = endX;
        this.fluidCenterY = fluidCenterY;
        this.centerZ = centerZ;
        this.maxClearY = maxClearY;
        this.liquidDepth = liquidDepth;
        this.liquidRadius = liquidRadius;

        this.maxSoulSandValleyY = maxSoulSandValleyY;
        this.maxDeepDarkY = maxDeepDarkY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltasY = maxBasaltDeltasY;
    }

    @SuppressWarnings("unused")
    public NetherCaveStraightRiverPiece(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        super(ModStructures.NETHER_CAVE_STRAIGHT_RIVER_PIECE.get(), tag);

        this.startX = tag.getDouble("StartX");
        this.endX = tag.getDouble("EndX");
        this.fluidCenterY = tag.getDouble("FluidCenterY");
        this.centerZ = tag.getDouble("CenterZ");
        this.maxClearY = tag.getInt("MaxClearY");

        this.liquidDepth = tag.getFloat("LiquidDepth");
        this.liquidRadius = tag.getFloat("LiquidRadius");

        this.maxSoulSandValleyY = tag.getInt("MaxSoulSandValleyY");
        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxWarpedForestY = tag.getInt("MaxWarpedForestY");
        this.maxCrimsonForestY = tag.getInt("MaxCrimsonForestY");
        this.maxBasaltDeltasY = tag.getInt("MaxBasaltDeltasY");
    }

    private static BoundingBox makeBoundingBox(
            double startX,
            double endX,
            double fluidCenterY,
            double centerZ,
            int maxClearY,
            float liquidDepth,
            float liquidRadius
    ) {
        int padding = 3;

        int minX = Mth.floor(Math.min(startX, endX) - liquidRadius) - padding;
        int maxX = Mth.ceil(Math.max(startX, endX) + liquidRadius) + padding;

        int minY = Mth.floor(fluidCenterY - liquidDepth) - padding;
        int maxY = maxClearY + padding;

        int minZ = Mth.floor(centerZ - liquidRadius) - padding;
        int maxZ = Mth.ceil(centerZ + liquidRadius) + padding;

        return new BoundingBox(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
    }

    @Override
    protected void addAdditionalSaveData(
            @NotNull StructurePieceSerializationContext context,
            @NotNull CompoundTag tag
    ) {
        tag.putDouble("StartX", this.startX);
        tag.putDouble("EndX", this.endX);
        tag.putDouble("FluidCenterY", this.fluidCenterY);
        tag.putDouble("CenterZ", this.centerZ);
        tag.putInt("MaxClearY", this.maxClearY);

        tag.putFloat("LiquidDepth", this.liquidDepth);
        tag.putFloat("LiquidRadius", this.liquidRadius);

        tag.putInt("MaxSoulSandValleyY", this.maxSoulSandValleyY);
        tag.putInt("MaxDeepDarkY", this.maxDeepDarkY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltasY);
    }

    @Override
    public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator generator,
            @NotNull net.minecraft.util.RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos pivot
    ) {
        double step = this.endX >= this.startX ? 1.0D : -1.0D;

        for (
                double centerX = this.startX;
                step > 0.0D ? centerX <= this.endX : centerX >= this.endX;
                centerX += step
        ) {
            carveStraightRiverColumn(level, box, centerX);
        }

        carveStraightRiverColumn(level, box, this.endX);
    }

    private void carveStraightRiverColumn(
            WorldGenLevel level,
            BoundingBox box,
            double centerX
    ) {
        int minX = Mth.floor(centerX - this.liquidRadius) - 1;
        int maxX = Mth.floor(centerX + this.liquidRadius) + 1;
        int minY = Mth.floor(this.fluidCenterY - this.liquidDepth);
        int maxY = Mth.floor(this.fluidCenterY) + 1;
        int minZ = Mth.floor(this.centerZ - this.liquidRadius) - 1;
        int maxZ = Mth.floor(this.centerZ + this.liquidRadius) + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockState fluidState = getRiverFluidState(this.fluidCenterY);

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
                            this.fluidCenterY,
                            this.centerZ
                    )) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (!canReplaceWithFluid(oldState)) {
                        continue;
                    }

                    level.setBlock(pos, fluidState, Block.UPDATE_CLIENTS);
                    clearBlockAboveFluid(level, box, pos, this.maxClearY);

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
                            this.fluidCenterY,
                            this.centerZ
                    );
                }
            }
        }
    }

    private boolean isInsideStraightRiverEllipsoid(
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

    private void clearBlockAboveFluid(
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

    private boolean canReplace(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }

    private boolean canReplaceWithFluid(BlockState state) {
        return canReplace(state) || state.is(Blocks.LAVA) || state.is(Blocks.WATER);
    }

    private BlockState getRiverFluidState(double landFloorY) {
        boolean bottomIsInLavaRegion = landFloorY - this.liquidDepth <= this.maxSoulSandValleyY;
        boolean topIsInWaterRegion = landFloorY > this.maxSoulSandValleyY + 1;

        if (bottomIsInLavaRegion && topIsInWaterRegion) {
            return null;
        }

        if (bottomIsInLavaRegion) {
            return Blocks.LAVA.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }

    private void decorateRiverColumnSurfaces(
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
        replaceFloorBelowLavaIfNeeded(level, box, new BlockPos(x, lowestPlacedFluidY, z));
        decorateDeepDarkRiverColumnSurfaces(level, box, x, z, minY, maxY, fluidCenterX, fluidCenterY, fluidCenterZ);
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
        if (y > this.maxBasaltDeltasY) {
            return NetherLayer.CRIMSON_FOREST;
        }
        return NetherLayer.BASALT_DELTAS;
    }

    private boolean isNaturalReplaceableSolid(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.SCULK_CATALYST)
                && !state.is(Blocks.SCULK_SENSOR)
                && !state.is(Blocks.SCULK_SHRIEKER)
                && !state.is(Blocks.BEDROCK);
    }

    private void replaceFloorBelowLavaIfNeeded(WorldGenLevel level, BoundingBox box, BlockPos lavaPos) {
        if (!box.isInside(lavaPos)) {
            return;
        }

        BlockState lavaState = level.getBlockState(lavaPos);

        if (!lavaState.is(Blocks.LAVA)
                && (!lavaState.is(Blocks.WATER) || getLayerAtY(lavaPos.below().getY()) != NetherLayer.NONE)) {
            return;
        }

        BlockPos below = lavaPos.below();

        if (!box.isInside(below)) {
            return;
        }

        BlockState belowState = level.getBlockState(below);

        if (shouldNotReplaceFluidFloor(belowState)) {
            return;
        }

        if (!belowState.getFluidState().isEmpty()) {
            return;
        }

        BlockState floorState = getFluidFloorState(below.getY());

        level.setBlock(below, floorState, Block.UPDATE_CLIENTS);
    }

    private static boolean shouldNotReplaceFluidFloor(BlockState state) {
        return state.is(Blocks.BEDROCK)
                || state.is(ModBlocks.BASALT_GOLD_ORE.get())
                || state.is(ModBlocks.BLACKSTONE_GOLD_ORE.get());
    }

    private BlockState getFluidFloorState(int y) {
        NetherLayer layer = getLayerAtY(y);

        return switch (layer) {
            case WARPED_FOREST -> Blocks.WARPED_NYLIUM.defaultBlockState();
            case CRIMSON_FOREST -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
            case NONE -> Blocks.SAND.defaultBlockState();
            default -> Blocks.BLACKSTONE.defaultBlockState();
        };
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

            if (!isInsideStraightRiverEllipsoid(x, y, z, fluidCenterX, fluidCenterY, fluidCenterZ)) {
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
}