package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NetherCavePiece extends AbstractSpiralCavePiece {
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

    @Override
    protected void decorateCaveFloor(WorldGenLevel level, BlockPos surfacePos) {
        int y = surfacePos.above().getY();

        if (y <= maxDeepDarkY && y > maxSoulSandValleyY) {
            setSculk(level, surfacePos, Direction.DOWN);
        }

        else if (y <= maxWarpedForestY && y > maxCrimsonForestY) {
            setBlock(level, surfacePos, Blocks.WARPED_NYLIUM);
        }
        else if (y <= maxCrimsonForestY && y > maxBasaltDeltasY) {
            setBlock(level, surfacePos, Blocks.CRIMSON_NYLIUM);
        }

        replaceExposedUndesirableBoundaryBlock(level, surfacePos, netherBoundaryReplacement(surfacePos), false);
    }

    @Override
    protected void decorateRiverFloor(WorldGenLevel level, BlockPos surfacePos) {
        if (surfacePos.above().getY() > maxDeepDarkY) {
            setBlock(level, surfacePos, Blocks.SAND);
            setBlock(level, surfacePos.below(), Blocks.SANDSTONE);
        } else {
            decorateCaveFloor(level, surfacePos);
        }
    }

    @Override
    protected void decorateCaveWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        if (wallPos.above().getY() <= maxDeepDarkY && wallPos.above().getY() > maxSoulSandValleyY) {
            setSculk(level, wallPos, wallDirection);
        }

        replaceExposedUndesirableBoundaryBlock(level, wallPos, netherBoundaryReplacement(wallPos), false);
    }

    @Override
    protected void decorateRiverWall(WorldGenLevel level, BlockPos wallPos, Direction wallDirection) {
        decorateCaveWall(level, wallPos, wallDirection);
    }

    @Override
    protected void decorateCaveCeiling(WorldGenLevel level, BlockPos ceilingPos) {
        if (ceilingPos.above().getY() <= maxDeepDarkY && ceilingPos.above().getY() > maxSoulSandValleyY) {
            setSculk(level, ceilingPos, Direction.UP);
        }

        replaceExposedUndesirableBoundaryBlock(level, ceilingPos, netherBoundaryReplacement(ceilingPos), true);
    }

    private Block netherBoundaryReplacement(BlockPos pos) {
        int y = pos.above().getY();

        if (y > maxDeepDarkY) {
            return Blocks.STONE;
        }

        if (y <= maxDeepDarkY && y > maxSoulSandValleyY) {
            return Blocks.SCULK;
        }

        if (y <= maxSoulSandValleyY && y > maxWarpedForestY) {
            return ((pos.getX() + pos.getZ()) & 1) == 0 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL;
        }

        if (y <= maxBasaltDeltasY) {
            return ((pos.getX() + pos.getY() + pos.getZ()) & 1) == 0 ? Blocks.BASALT : Blocks.BLACKSTONE;
        }

        return Blocks.NETHERRACK;
    }

    protected void setSculk(WorldGenLevel level, BlockPos pos, Direction direction){
        for(int i=0; i<4; i++){
            if(i>1 && (direction == Direction.DOWN || direction == Direction.UP)){
                return;
            }
            BlockPos pos1 = pos.relative(direction,i);
            if(pos1.above().getY() > maxDeepDarkY || pos1.above().getY() <= maxSoulSandValleyY){
                return;
            }
            BlockState state = level.getBlockState(pos1);
            if(!state.is(Blocks.SCULK_CATALYST)&&!state.is(Blocks.SCULK_SHRIEKER)
                    &&!state.is(Blocks.SCULK_SENSOR)
                    &&!state.is(Blocks.CHORUS_FLOWER)
                    &&!state.is(Blocks.CHORUS_PLANT)&&!state.is(Blocks.SCULK)){
                Block replaceBlock = i < 2 ? Blocks.SCULK : Blocks.END_STONE;
                setBlock(level, pos1, replaceBlock);
            }
            else {
                break;
            }
        }
    }

    @Override
    protected BlockState getRiverFluidState(double landFloorY) {
        boolean bottomIsInLavaRegion = landFloorY - this.liquidDepth - 2 <= this.maxSoulSandValleyY;
        boolean topIsInWaterRegion = landFloorY > this.maxSoulSandValleyY - 2;

        if (bottomIsInLavaRegion && topIsInWaterRegion) {
            return null;
        }

        if (bottomIsInLavaRegion) {
            return Blocks.LAVA.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }
}
