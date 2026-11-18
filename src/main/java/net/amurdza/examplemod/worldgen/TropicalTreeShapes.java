package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared diagonal-log repair and tropical shape rules for generation and sapling growth. */
public final class TropicalTreeShapes {
    private static final ThreadLocal<Boolean> TALL_MUSHROOM = ThreadLocal.withInitial(() -> false);

    private TropicalTreeShapes() { }

    public static boolean mangroveLeafAllowed(LevelReader level, BlockPos origin, BlockPos leaf) {
        // Roots lift the first log to origin+7 or origin+8. Keep two complete
        // log layers exposed below the crown, including the higher root roll.
        if (leaf.getY() < origin.getY() + 10 || level.getFluidState(leaf).is(FluidTags.WATER)) return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = leaf.relative(direction);
            if (!level.isOutsideBuildHeight(neighbor) && level.getFluidState(neighbor).is(FluidTags.WATER)) return false;
        }
        return true;
    }

    /** Fill only vertical holes within this crown, preserving its irregular outline and branch logs. */
    public static void fillMangroveColumns(WorldGenLevel level, BlockPos origin, Map<BlockPos, BlockState> foliage,
                                           BiConsumer<BlockPos, BlockState> setter) {
        Map<Long, int[]> columns = new HashMap<>();
        for (BlockPos leaf : foliage.keySet()) {
            long column = ((long) leaf.getX() << 32) ^ (leaf.getZ() & 0xffffffffL);
            int[] range = columns.computeIfAbsent(column, ignored -> new int[]{leaf.getY(), leaf.getY()});
            range[0] = Math.min(range[0], leaf.getY());
            range[1] = Math.max(range[1], leaf.getY());
        }
        for (var column : columns.entrySet()) {
            int x = (int) (column.getKey() >> 32), z = (int) (long) column.getKey();
            for (int y = column.getValue()[0]; y <= column.getValue()[1]; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (mangroveLeafAllowed(level, origin, pos) && level.getBlockState(pos).isAir()) {
                    setter.accept(pos, Blocks.MANGROVE_LEAVES.defaultBlockState());
                }
            }
        }
    }

    /** Add lower supports and upper bridges where native giant-jungle logs meet diagonally. */
    public static boolean connectJungleBranches(WorldGenLevel level, BlockPos origin, Map<BlockPos, BlockState> logs,
                                                 BiConsumer<BlockPos, BlockState> setter) {
        Set<BlockPos> connected = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(origin);
        expand(logs.keySet(), connected, frontier);
        while (connected.size() < logs.size()) {
            BlockPos loose = null, neighbor = null;
            search: for (BlockPos candidate : List.copyOf(logs.keySet())) {
                if (connected.contains(candidate)) continue;
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = candidate.offset(dx, dy, dz);
                    if (connected.contains(check)) {
                        loose = candidate;
                        neighbor = check;
                        break search;
                    }
                }
            }
            if (loose == null) return false;
            for (BlockPos corner : TropicalBranchConnections.limbCorners(neighbor, loose)) {
                if (logs.containsKey(corner)) continue;
                if (!TreeFeature.validTreePos(level, corner)) return false;
                setter.accept(corner, logs.get(loose));
                logs.put(corner, logs.get(loose));
                frontier.add(corner);
            }
            frontier.add(loose);
            expand(logs.keySet(), connected, frontier);
        }
        return true;
    }

    /** Inspect the actual log graph, including branches already connected by another route. */
    public static boolean completeDiagonalBridges(WorldGenLevel level, Map<BlockPos, BlockState> logs,
                                                   BiConsumer<BlockPos, BlockState> setter) {
        Set<BlockPos> nativeLogs = new java.util.LinkedHashSet<>(logs.keySet());
        Set<BlockPos> required = new java.util.LinkedHashSet<>();
        for (BlockPos first : nativeLogs) {
            for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) for (int dy = -1; dy <= 1; dy++) {
                BlockPos second = first.offset(dx, dy, dz);
                if (!nativeLogs.contains(second) || first.asLong() >= second.asLong()) continue;
                BlockPos lower = first.getY() <= second.getY() ? first : second;
                BlockPos higher = lower.equals(first) ? second : first;
                BlockPos firstElbow = new BlockPos(higher.getX(), lower.getY(), lower.getZ());
                BlockPos secondElbow = new BlockPos(lower.getX(), lower.getY(), higher.getZ());
                // A full two-by-two trunk already has both horizontal routes.
                // Do not extend its top merely because its corners are diagonal.
                if (nativeLogs.contains(firstElbow) && nativeLogs.contains(secondElbow)) continue;
                if (lower.getY() == higher.getY() && nativeLogs.contains(secondElbow)) {
                    required.add(secondElbow.above());
                } else {
                    required.addAll(TropicalBranchConnections.limbCorners(lower, higher));
                }
            }
        }
        for (BlockPos pos : required) {
            if (!logs.containsKey(pos) && !TreeFeature.validTreePos(level, pos)) return false;
        }
        BlockState log = logs.values().stream().findFirst().orElse(null);
        if (log == null) return true;
        for (BlockPos pos : required) {
            if (logs.containsKey(pos)) continue;
            setter.accept(pos, log);
            logs.put(pos, log);
        }
        return true;
    }

    private static void expand(Set<BlockPos> logs, Set<BlockPos> connected, ArrayDeque<BlockPos> frontier) {
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.removeFirst();
            if (!logs.contains(pos) || !connected.add(pos)) continue;
            for (Direction direction : Direction.values()) frontier.add(pos.relative(direction));
        }
    }

    public static boolean withMushroomHeight(FeaturePlaceContext<?> context, Supplier<Boolean> generate) {
        boolean previous = TALL_MUSHROOM.get();
        boolean tropical = GameplayDimensions.is(context.level(), GameplayDimensions.TROPICAL)
                && context.config() instanceof HugeMushroomFeatureConfiguration;
        TALL_MUSHROOM.set(tropical);
        try {
            return generate.get();
        } finally {
            TALL_MUSHROOM.set(previous);
        }
    }

    public static boolean tallMushroom() {
        return TALL_MUSHROOM.get();
    }
}
