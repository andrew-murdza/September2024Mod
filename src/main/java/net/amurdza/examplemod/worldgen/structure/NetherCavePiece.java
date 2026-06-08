package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.block.ModBlocks;
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
        SOUL_SAND_VALLEY,
        WARPED_FOREST,
        CRIMSON_FOREST,
        BASALT_DELTAS,
        NONE
    }

    private final int maxDeepDarkY;
    private final int maxSoulSandValleyY;
    private final int maxWarpedForestY;
    private final int maxCrimsonForestY;
    private final int maxBasaltDeltasY;

    public NetherCavePiece(
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
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.NETHER_CAVE_PIECE.get(),
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
                placedFeatures
        );

        this.maxDeepDarkY = maxDeepDarkY;
        this.maxSoulSandValleyY = maxSoulSandValleyY;
        this.maxWarpedForestY = maxWarpedForestY;
        this.maxCrimsonForestY = maxCrimsonForestY;
        this.maxBasaltDeltasY = maxBasaltDeltasY;
    }

    @SuppressWarnings("unused")
    public NetherCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NETHER_CAVE_PIECE.get(), tag);

        this.maxDeepDarkY = tag.getInt("MaxDeepDarkY");
        this.maxSoulSandValleyY = tag.getInt("MaxSoulSandValleyY");
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
        tag.putInt("MaxSoulSandValleyY", this.maxSoulSandValleyY);
        tag.putInt("MaxWarpedForestY", this.maxWarpedForestY);
        tag.putInt("MaxCrimsonForestY", this.maxCrimsonForestY);
        tag.putInt("MaxBasaltDeltasY", this.maxBasaltDeltasY);
    }

    protected boolean isNaturalReplaceableSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
        return isNaturalReplaceableSolid(state)
                && state.isFaceSturdy(level, pos, Direction.UP);
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

    private static BlockState getFloorState(NetherLayer layer) {
        return switch (layer) {
            case CRIMSON_FOREST -> Blocks.CRIMSON_NYLIUM.defaultBlockState();
            case WARPED_FOREST -> Blocks.WARPED_NYLIUM.defaultBlockState();
            case BASALT_DELTAS, DEEP_DARK, SOUL_SAND_VALLEY , NONE -> null;
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

        if (!lavaState.is(Blocks.LAVA)
                &&(!lavaState.is(Blocks.WATER)||getLayerAtY(lavaPos.below().getY())!=NetherLayer.NONE)) {
            return;
        }

        BlockPos below = lavaPos.below();

        if (!box.isInside(below)) {
            return;
        }

        BlockState belowState = level.getBlockState(below);

        if(shouldNotReplaceFluidFloor(belowState)){
            return;
        }

        if (!belowState.getFluidState().isEmpty()) {
            return;
        }

        BlockState floorState = getFluidFloorState(below.getY());

        level.setBlock(below, floorState, Block.UPDATE_CLIENTS);
    }

    private static boolean shouldNotReplaceFluidFloor(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.is(ModBlocks.BASALT_GOLD_ORE.get())
                || state.is(ModBlocks.BASALT_GOLD_ORE.get()) || state.is(ModBlocks.BLACKSTONE_GOLD_ORE.get())
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