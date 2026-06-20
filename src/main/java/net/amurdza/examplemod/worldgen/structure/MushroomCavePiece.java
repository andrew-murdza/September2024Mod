package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
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

    public MushroomCavePiece(
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
            int maxMushroomCaves,
            HolderSet<PlacedFeature> placedFeatures
    ) {
        super(
                ModStructures.MUSHROOM_CAVE_PIECE.get(),
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
    protected void decorateCaveSurface(WorldGenLevel level, BlockPos carvedPos) {
        boolean isMushroomLayer = carvedPos.getY() <= this.maxMushroomCaves;

        BlockPos top = carvedPos.below();
        BlockState topState = level.getBlockState(top);

        if (!isNaturalReplaceableSurface(level, top, topState)) {
            return;
        }

        BlockState surfaceState = isMushroomLayer
                ? Blocks.MYCELIUM.defaultBlockState()
                : Blocks.GRASS_BLOCK.defaultBlockState();

        level.setBlock(top, surfaceState, Block.UPDATE_CLIENTS);

        BlockPos backing = top.below();
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
        BlockPos riverFloor = new BlockPos(x, lowestPlacedFluidY - 1, z);

        placeMudRiverFloor(level, box, riverFloor);
        placeMudRiverFloor(level, box, riverFloor.below());
        placeMudRiverFloor(level, box, riverFloor.below(2));
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