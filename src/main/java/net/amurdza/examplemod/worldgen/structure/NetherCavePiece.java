package net.amurdza.examplemod.worldgen.structure;

import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NetherCavePiece extends AbstractSpiralCavePiece {
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

    private final int maxDeepDarkY;
    private final int maxSoulSludgeY;
    private final int maxSoulSoilY;
    private final int maxSoulSandY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    public NetherCavePiece(
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
            int maxDeepDarkY,
            int maxSoulSludgeY,
            int maxSoulSoilY,
            int maxSoulSandY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.NETHER_CAVE_PIECE.get(),
                origin,
                seed,
                lowerHorizontalRadius,
                lowerVerticalRadius,
                upperHorizontalRadius,
                upperVerticalRadius,
                centralPillarDiameter,
                minFloorThickness,
                upperPitch,
                liquidDepth,
                liquidRadius,
                placedFeatures
        );

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSludgeY = maxSoulSludgeY;
        this.maxSoulSoilY = maxSoulSoilY;
        this.maxSoulSandY = maxSoulSandY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltasY = maxBasaltDeltasY;
    }

    @SuppressWarnings("unused")
    public NetherCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxSoulSludgeY = tag.getInt("MaxSoulSludgeY");
        this.maxSoulSoilY = tag.getInt("MaxSoulSoilY");
        this.maxSoulSandY = tag.getInt("MaxSoulSandY");
        this.maxWarpedForestY = tag.getInt("MaxWarpedForestY");
        this.maxCrimsonForestY = tag.getInt("MaxCrimsonForestY");
        this.maxBasaltDeltasY = tag.getInt("MaxBasaltDeltasY");
    }

    @Override
    protected void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("MaxDeepDarkY", this.maxDeepDarkY);
        tag.putInt("MaxSoulSludgeY", this.maxSoulSludgeY);
        tag.putInt("MaxSoulSoilY", this.maxSoulSoilY);
        tag.putInt("MaxSoulSandY", this.maxSoulSandY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltasY);
    }

    @Override
    protected boolean useUpperShape(double centerY) {
        return centerY > this.maxDeepDarkY;
    }

    @Override
    protected boolean useUpperPitch(double centerY) {
        double bottomOfEllipsoid = centerY - getLocalVerticalRadius(centerY);
        return bottomOfEllipsoid > this.maxDeepDarkY;
    }

    @Override
    protected void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos) {
        NetherLayer layer = getLayerAtY(carvedPos.getY());

        if (layer == NetherLayer.DEEP_DARK) {
            replaceAdjacentSolidsWithSculkAndEndStoneBacking(level, carvedPos);
            return;
        }

        NetherLayer belowLayer = getLayerAtY(carvedPos.getY() - 1);
        BlockState floorState = getFloorState(belowLayer);

        if (floorState == null) {
            return;
        }

        BlockPos below = carvedPos.below();
        BlockState belowState = level.getBlockState(below);

        if (isNaturalReplaceableSurface(level, below, belowState)) {
            level.setBlock(below, floorState, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState getRiverFluidState(double landFloorY) {
        boolean bottomIsInLavaRegion = landFloorY - this.liquidDepth <= this.maxSoulSludgeY;
        boolean topIsInWaterRegion = landFloorY > this.maxSoulSludgeY + 1;

        if (bottomIsInLavaRegion && topIsInWaterRegion) {
            return null;
        }

        if (bottomIsInLavaRegion) {
            return Blocks.LAVA.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }

    @Override
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
        replaceFloorBelowLavaIfNeeded(level, box, new BlockPos(x, lowestPlacedFluidY, z));
        decorateDeepDarkRiverColumnSurfaces(level, box, x, z, minY, maxY, fluidCenterX, fluidCenterY, fluidCenterZ);
    }

    @Override
    protected boolean isNaturalReplaceableSolid(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER)
                && !state.is(Blocks.SCULK_CATALYST)
                && !state.is(Blocks.SCULK_SENSOR)
                && !state.is(Blocks.SCULK_SHRIEKER)
                && !state.is(Blocks.BEDROCK);
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
        if (y > this.maxBasaltDeltasY) {
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
}