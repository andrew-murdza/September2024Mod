package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Continuous hanging columns with a mature tip and room to walk beneath them. */
public final class CaveVinePlacement {
    private static final int FLOOR_CLEARANCE = 4;

    private CaveVinePlacement() { }

    public static boolean place(WorldGenLevel level, BlockPos start, int fallbackLength, boolean allowWater) {
        if (!keepColumn(level, start)) return false;
        List<BlockPos> positions = positions(level, start, fallbackLength, allowWater);
        if (positions.isEmpty()) return false;
        write(level, positions, (pos, state) -> level.setBlock(pos, state, 2));
        return true;
    }

    public static void place(TreeDecorator.Context context, BlockPos start, int fallbackLength) {
        if (!keepColumn(context.level(), start)) return;
        write(context.level(), positions(context.level(), start, fallbackLength, false), context::setBlock);
    }

    /** One seeded decision per horizontal column, shared by every tropical generation entry point. */
    public static boolean keepColumn(LevelSimulatedReader level, BlockPos start) {
        if (!(level instanceof LevelReader reader) || !GameplayDimensions.is(reader, GameplayDimensions.TROPICAL)) return true;
        long seed = level instanceof WorldGenLevel world ? world.getSeed() : 0;
        return keepTropicalColumn(seed, start.getX(), start.getZ());
    }

    /** The proposed tip must leave the same four empty cells used during generation. */
    public static boolean hasFloorClearance(LevelSimulatedReader level, BlockPos tip) {
        for (int dy = 1; dy <= FLOOR_CLEARANCE; dy++) {
            if (level.isStateAtPosition(tip.below(dy), state -> state.isSolid() || state.is(BlockTags.LEAVES))) return false;
        }
        return true;
    }

    public static boolean keepTropicalColumn(long seed, int x, int z) {
        long hash = seed ^ (long) x * 0x632BE59BD9B4E019L ^ (long) z * 0x9E3779B97F4A7C15L;
        hash = (hash ^ hash >>> 30) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ hash >>> 27) * 0x94D049BB133111EBL;
        return Math.floorMod(hash ^ hash >>> 31, 24) == 0;
    }

    public static List<BlockPos> positions(LevelSimulatedReader level, BlockPos start,
                                          int fallbackLength, boolean allowWater) {
        int minimumY = level instanceof LevelHeightAccessor heights ? heights.getMinBuildHeight() : start.getY() - 256;
        if (level instanceof LevelHeightAccessor heights && heights.isOutsideBuildHeight(start)) return List.of();
        int bottomY = Math.max(minimumY, start.getY() - Math.max(2, fallbackLength) + 1);
        BlockPos.MutableBlockPos cursor = start.mutable();
        boolean protectPlants = level instanceof LevelReader reader
                && GameplayDimensions.is(reader, GameplayDimensions.TROPICAL);
        // A sampled length is only a fallback when the column has no floor.
        for (int y = start.getY(); y >= minimumY; y--) {
            cursor.setY(y);
            if (!level.isStateAtPosition(cursor, state -> passable(state, allowWater, protectPlants))) {
                // Four empty cells separate the obstacle from the final vine cell.
                bottomY = y + FLOOR_CLEARANCE + 1;
                break;
            }
        }
        if (bottomY > start.getY()) return List.of();
        List<BlockPos> positions = new ArrayList<>();
        for (int y = start.getY(); y >= bottomY; y--) positions.add(new BlockPos(start.getX(), y, start.getZ()));
        return positions;
    }

    private static boolean passable(BlockState state, boolean allowWater, boolean protectPlants) {
        // A later ceiling/tree pass must not cut through an existing column or a double plant.
        if (state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) return !protectPlants;
        if (protectPlants && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return false;
        if (allowWater && state.is(Blocks.WATER)) return true;
        return state.getFluidState().isEmpty() && !state.isSolid() && !state.is(BlockTags.LEAVES)
                && (state.isAir() || state.canBeReplaced());
    }

    /** A later tree pass must shorten an earlier vine when its canopy enters the column. */
    public static void trimForGeneratedObstacle(WorldGenLevel level, BlockPos pos,
                                                BlockState previous, BlockState placed) {
        if (!placed.isSolid() && !placed.is(BlockTags.LEAVES)) return;
        if (previous.isSolid() || previous.is(BlockTags.LEAVES)) return;
        if (TropicalPlantIntegrity.isCaveVine(previous)
                && !(placed.is(BlockTags.LEAVES) && GameplayDimensions.is(level, GameplayDimensions.TROPICAL))) {
            TropicalPlantIntegrity.clearDetachedVines(level, pos.below());
        }
        // Remove bottom-up so support cleanup cannot invalidate a surviving upper section.
        for (int dy = 1; dy <= FLOOR_CLEARANCE; dy++) {
            BlockPos clearance = pos.above(dy);
            BlockState state = level.getBlockState(clearance);
            if (TropicalPlantIntegrity.isCaveVine(state)) {
                level.setBlock(clearance, state.getFluidState().createLegacyBlock(), 18);
            }
        }
        BlockPos tip = pos.above(FLOOR_CLEARANCE + 1);
        BlockState state = level.getBlockState(tip);
        if (state.is(Blocks.CAVE_VINES_PLANT)) {
            level.setBlock(tip, Blocks.CAVE_VINES.defaultBlockState()
                    .setValue(BlockStateProperties.BERRIES, state.getValue(BlockStateProperties.BERRIES))
                    .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED))
                    .setValue(BlockStateProperties.AGE_25, 25), 18);
        }
    }

    private static void write(LevelSimulatedReader level, List<BlockPos> positions, BiConsumer<BlockPos, BlockState> setter) {
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            boolean last = i == positions.size() - 1;
            BlockState state = (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                    .setValue(BlockStateProperties.BERRIES, true);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED,
                        level.isFluidAtPosition(pos, fluid -> fluid.is(FluidTags.WATER)));
            }
            if (last) state = state.setValue(BlockStateProperties.AGE_25, 25);
            setter.accept(pos, state);
        }
    }
}
