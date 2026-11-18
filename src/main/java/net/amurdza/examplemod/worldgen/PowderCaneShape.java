package net.amurdza.examplemod.worldgen;

import com.soytutta.mynethersdelight.common.registry.MNDBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A pepper cap, small leaves, two large rows, one small row, then a bare stalk. */
public final class PowderCaneShape {
    private PowderCaneShape() { }

    public static boolean isStem(BlockState state) {
        return state.is(MNDBlocks.POWDERY_CANE.get()) || state.is(MNDBlocks.POWDERY_CANNON.get());
    }

    public static boolean isPlant(BlockState state) {
        return isStem(state) || state.is(MNDBlocks.BULLET_PEPPER.get());
    }

    public static BambooLeaves leaves(int depth) {
        return depth == 1 || depth == 4 ? BambooLeaves.SMALL
                : depth == 2 || depth == 3 ? BambooLeaves.LARGE : BambooLeaves.NONE;
    }

    public static BlockState shape(BlockGetter level, BlockPos pos, BlockState state) {
        if (!isPlant(state)) return state;
        int above = 0;
        while (above < 64 && isPlant(level.getBlockState(pos.above(above + 1)))) above++;
        if (state.hasProperty(BlockStateProperties.BAMBOO_LEAVES)) {
            state = state.setValue(BlockStateProperties.BAMBOO_LEAVES, leaves(above));
        }
        if (above > 0) {
            if (state.hasProperty(BlockStateProperties.LIT)) state = state.setValue(BlockStateProperties.LIT, false);
            // A pepper that grows upward becomes a mature lower stalk. The
            // blockstate override leaves its silhouette controlled by LEAVES.
            if (state.is(MNDBlocks.POWDERY_CANE.get())) state = state.setValue(BlockStateProperties.AGE_3, 3);
        }
        return state;
    }

    public static void normalize(LevelAccessor level, BlockPos pos) {
        BlockPos top = pos;
        for (int i = 0; i < 64 && isPlant(level.getBlockState(top.above())); i++) top = top.above();
        for (int i = 0; i < 64 && isPlant(level.getBlockState(top)); i++, top = top.below()) {
            BlockState previous = level.getBlockState(top), next = shape(level, top, previous);
            if (!previous.equals(next)) level.setBlock(top, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    public static boolean placeTree(WorldGenLevel level, BlockPos root, boolean thick, RandomSource random) {
        int clearance = 0;
        while (clearance < 12 && !level.isOutsideBuildHeight(root.above(clearance))
                && level.isEmptyBlock(root.above(clearance))) clearance++;
        if (clearance < 7) return false;
        int height = 7 + random.nextInt(clearance - 6);
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        BlockState stem = (thick ? MNDBlocks.POWDERY_CANNON.get() : MNDBlocks.POWDERY_CANE.get()).defaultBlockState();
        if (!stem.canSurvive(buffer, root)) return false;
        stem = stem.trySetValue(BlockStateProperties.AGE_1, 1).trySetValue(BlockStateProperties.AGE_3, 3)
                .trySetValue(BlockStateProperties.STAGE, 1)
                .trySetValue(BlockStateProperties.LIT, false);
        for (int i = 0; i < height - 1; i++) {
            buffer.setBlock(root.above(i), stem.trySetValue(BlockStateProperties.BAMBOO_LEAVES, leaves(height - 1 - i)),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        buffer.setBlock(root.above(height - 1), MNDBlocks.BULLET_PEPPER.get().defaultBlockState()
                        .setValue(BlockStateProperties.AGE_3, 3).setValue(BlockStateProperties.LIT, true),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return buffer.commit();
    }
}
