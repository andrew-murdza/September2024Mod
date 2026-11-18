package net.amurdza.examplemod.worldgen.feature.features;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Vanilla lichen attachment rules, with ten or twenty connected vein blocks per patch. */
public final class DeepDarkVeinFeature extends Feature<MultifaceGrowthConfiguration> {
    public DeepDarkVeinFeature() { super(MultifaceGrowthConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<MultifaceGrowthConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        MultifaceGrowthConfiguration config = context.config();
        RandomSource random = context.random();
        if (!isAirOrWater(level.getBlockState(origin))) return false;
        List<Direction> directions = config.getShuffledDirections(random);
        if (placePatch(level, origin, config, random, directions) > 0) return true;
        // Keep vanilla MultifaceGrowthFeature's attachment search positions.
        // Its search loop tests the adjacent block repeatedly; search_range is
        // not a patch-size setting, so increasing it cannot double coverage.
        for (Direction direction : directions) {
            BlockPos candidate = origin.relative(direction);
            if (!isAirOrWater(level.getBlockState(candidate))) continue;
            if (placePatch(level, candidate, config, random,
                    config.getShuffledDirectionsExcept(random, direction.getOpposite())) > 0) return true;
        }
        return false;
    }

    public static int placePatch(WorldGenLevel level, BlockPos origin, MultifaceGrowthConfiguration config,
                                 RandomSource random, List<Direction> directions) {
        BlockState initial = attachment(level, origin, config, directions);
        if (initial == null) return 0;
        // Preserve the patch and spread lotteries, enlarging the previous 2/4
        // connected footprint fivefold without adding extra patch starts.
        int size = 10 * (random.nextFloat() < config.chanceOfSpreading ? 2 : 1);
        List<BlockPos> roots = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        roots.add(origin.immutable());
        states.add(initial);
        for (int cursor = 0; cursor < roots.size() && roots.size() < size; cursor++) {
            List<Direction> offsets = new ArrayList<>(List.of(Direction.values()));
            while (!offsets.isEmpty() && roots.size() < size) {
                BlockPos next = roots.get(cursor).relative(offsets.remove(random.nextInt(offsets.size())));
                if (roots.contains(next)) continue;
                BlockState state = attachment(level, next, config, config.getShuffledDirections(random));
                if (state != null) {
                    roots.add(next);
                    states.add(state);
                }
            }
        }
        // Reject a crevice that cannot accommodate the complete connected patch.
        if (roots.size() != size) return 0;
        for (int i = 0; i < size; i++) {
            level.setBlock(roots.get(i), states.get(i), Block.UPDATE_CLIENTS);
            level.getChunk(roots.get(i)).markPosForPostprocessing(roots.get(i));
        }
        return size;
    }

    private static BlockState attachment(WorldGenLevel level, BlockPos pos, MultifaceGrowthConfiguration config,
                                          List<Direction> directions) {
        BlockState existing = level.getBlockState(pos);
        if (!isAirOrWater(existing) || !GameplayDimensions.biomeIs(level, pos, "deep_dark")) return null;
        for (Direction direction : directions) {
            if (direction == Direction.DOWN) continue;
            if (!level.getBlockState(pos.relative(direction)).is(config.canBePlacedOn)) continue;
            BlockState state = config.placeBlock.getStateForPlacement(existing, level, pos, direction);
            if (state != null) return state;
        }
        return null;
    }

    private static boolean isAirOrWater(BlockState state) { return state.isAir() || state.is(Blocks.WATER); }
}
