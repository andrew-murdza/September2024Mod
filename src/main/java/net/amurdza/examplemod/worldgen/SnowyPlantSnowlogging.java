package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import snownee.snow.CoreModule;
import snownee.snow.block.entity.SnowBlockEntity;

/** Stores the real plant inside Snow! Real Magic's native snow container, including its age. */
public final class SnowyPlantSnowlogging {
    private SnowyPlantSnowlogging() { }

    public static boolean place(LevelAccessor level, BlockPos pos, BlockState plant) {
        BlockState previous = level.getBlockState(pos);
        if ((!previous.isAir() && !previous.is(Blocks.SNOW) && previous != plant)
                || !(plant.getBlock() instanceof SweetBerryBushBlock
                || plant.getBlock() instanceof TallGrassBlock || plant.getBlock() instanceof DoublePlantBlock)
                || !plant.canSurvive(level, pos) || !Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) return false;
        boolean doublePlant = plant.getBlock() instanceof DoublePlantBlock;
        if (doublePlant && plant.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) return false;
        BlockState previousAbove = level.getBlockState(pos.above());
        BlockState upper = doublePlant ? plant.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER) : null;
        if (doublePlant && !previousAbove.isAir() && !previousAbove.equals(upper)) return false;
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState snowState = CoreModule.TILE_BLOCK.defaultBlockState();
        if (previous.is(Blocks.SNOW)) snowState = snowState.setValue(BlockStateProperties.LAYERS,
                previous.getValue(BlockStateProperties.LAYERS));
        if (!level.setBlock(pos, snowState, flags)) return false;
        if (level.getBlockEntity(pos) instanceof SnowBlockEntity snow) {
            snow.setContainedState(plant, false);
            snow.setChanged();
            if (snow.getContainedState() == plant
                    && (!doublePlant || level.setBlock(pos.above(), upper, flags) || level.getBlockState(pos.above()).equals(upper))) {
                markSnowyGround(level, pos);
                return true;
            }
        }
        // A failed block-entity creation must not eat an existing plant or count as a berry root.
        level.setBlock(pos, previous, flags);
        if (doublePlant) level.setBlock(pos.above(), previousAbove, flags);
        return false;
    }

    public static boolean cover(LevelAccessor level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.SNOW) || level.getBlockState(pos).is(CoreModule.TILE_BLOCK.get())) {
            return markSnowyGround(level, pos);
        }
        return place(level, pos, level.getBlockState(pos));
    }

    /** WorldGenRegion writes skip neighbor updates; mirror vanilla FreezeTopLayer's explicit soil state. */
    public static boolean markSnowyGround(LevelAccessor level, BlockPos snowPos) {
        BlockPos below = snowPos.below();
        BlockState ground = level.getBlockState(below);
        return ground.hasProperty(BlockStateProperties.SNOWY) && !ground.getValue(BlockStateProperties.SNOWY)
                && level.setBlock(below, ground.setValue(BlockStateProperties.SNOWY, true),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    public static boolean coverSurface(LevelAccessor level, BlockPos pos) {
        if (level.isEmptyBlock(pos) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            boolean placed = level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return markSnowyGround(level, pos) || placed;
        }
        return cover(level, pos);
    }
}
