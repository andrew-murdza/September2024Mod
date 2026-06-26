package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class MushroomCavePiece extends AbstractSpiralCavePiece {
    private final int maxMushroomCaves;
    private final int minMushroomCaves;

    private boolean hasCeilingAbove(WorldGenLevel level, BlockPos pos) {
        int maxY = 63;
        BlockPos pos1 = new BlockPos(pos.getX(), maxY, pos.getZ());
        BlockState state = level.getBlockState(pos1);
        return !state.isAir();
    }

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
            int minMushroomCaves,
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
        this.minMushroomCaves = minMushroomCaves;
    }

    @SuppressWarnings("unused")
    public MushroomCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MUSHROOM_CAVE_PIECE.get(), tag);

        this.maxMushroomCaves = tag.getInt("MaxMushroomCaves");
        this.minMushroomCaves = tag.getInt("MinMushroomCaves");
    }

    @Override
    protected void addSubclassSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("MaxMushroomCaves", this.maxMushroomCaves);
        tag.putInt("MinMushroomCaves", this.minMushroomCaves);
    }

    @Override
    protected void decorateCaveFloor(WorldGenLevel level, BlockPos top) {
        boolean isMushroomLayer =
                top.above().getY() <= this.maxMushroomCaves &&
                        top.above().getY() >= this.minMushroomCaves;

        if (isMushroomLayer) {
            setBlock(level, top, Blocks.MYCELIUM);
            setBlock(level, top.below(), Blocks.DIRT);
            setBlock(level, top.below(2), Blocks.CLAY);
        }

        else if (top.above().getY() > minMushroomCaves && !hasCeilingAbove(level, top)) {
            setBlock(level, top, Blocks.GRASS_BLOCK);
            setBlock(level, top.below(), Blocks.DIRT);
        }

        else if (top.above().getY() == minMushroomCaves - 1) {
            setBlock(level, top, Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.CALCITE);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
        }

        else if (top.above().getY() == minMushroomCaves - 2) {
            setBlock(level, top, Blocks.CALCITE);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(5), Blocks.SMOOTH_BASALT);
        }

        else if (top.above().getY() == minMushroomCaves - 3) {
            setBlock(level, top, bordersOneBlockHigher(top) ? Blocks.BUDDING_AMETHYST : Blocks.AMETHYST_BLOCK);
            setBlock(level, top.below(), Blocks.CALCITE);
            setBlock(level, top.below(2), Blocks.CALCITE);
            setBlock(level, top.below(3), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(4), Blocks.SMOOTH_BASALT);
            setBlock(level, top.below(5), Blocks.SMOOTH_BASALT);
        }
    }

    private boolean bordersOneBlockHigher(BlockPos top) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!activeEllipsoidWouldCarve(top.offset(dx, 1, dz))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void decorateRiverFloor(WorldGenLevel level, BlockPos surfacePos) {
        setBlock(level, surfacePos, Blocks.MUD);
        setBlock(level, surfacePos.below(), Blocks.MUD);
    }

    @Override
    protected void decorateCaveWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {}

    @Override
    protected void decorateRiverWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {}

    @Override
    protected void decorateCaveCeiling(WorldGenLevel level, BlockPos ceilingPos) {}

    @Override
    protected BlockState getRiverFluidState(double landFloorY) {
        return Blocks.WATER.defaultBlockState();
    }
}
