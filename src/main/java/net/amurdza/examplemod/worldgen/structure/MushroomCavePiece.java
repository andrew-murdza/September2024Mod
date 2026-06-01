package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class MushroomCavePiece extends AbstractSpiralCavePiece {
    private final int maxMushroomCaves;

    @Override
    protected boolean shouldMakeBowl() {
        return false;
    }

    public MushroomCavePiece(
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
            int maxMushroomCaves,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.MUSHROOM_CAVE_PIECE.get(),
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

        this.maxMushroomCaves = maxMushroomCaves;
    }

    @SuppressWarnings("unused")
    public MushroomCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MUSHROOM_CAVE_PIECE.get(), tag);

        this.maxMushroomCaves = tag.getInt("MaxMushroomCaves");
    }

    @Override
    protected void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("MaxMushroomCaves", this.maxMushroomCaves);
    }

    @Override
    protected boolean useUpperShape(double centerY) {
        return centerY > this.maxMushroomCaves;
    }

    @Override
    protected boolean useUpperPitch(double centerY) {
        return centerY > this.maxMushroomCaves;
    }

    @Override
    protected void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos) {
        if (carvedPos.getY() > this.maxMushroomCaves) {
            return;
        }

        BlockPos below = carvedPos.below();
        BlockState belowState = level.getBlockState(below);

        if (!isNaturalReplaceableSurface(level, below, belowState)) {
            return;
        }

        level.setBlock(below, Blocks.MYCELIUM.defaultBlockState(), Block.UPDATE_CLIENTS);

        BlockPos backing = below.below();
        BlockState backingState = level.getBlockState(backing);

        if (isNaturalReplaceableSolid(backingState)) {
            level.setBlock(backing, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState getRiverFluidState(double landFloorY) {
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
        placeMudRiverFloor(level, box, new BlockPos(x, lowestPlacedFluidY - 1, z));
    }

    private void placeMudRiverFloor(
            WorldGenLevel level,
            BoundingBox box,
            BlockPos pos
    ) {
        if (!box.isInside(pos)) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (!canReplaceMudRiverFloorBlock(state)) {
            return;
        }

        level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean canReplaceMudRiverFloorBlock(BlockState state) {
        return !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }
}