package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Join native diagonal components with upper corners and remove redundant lower logs. */
public final class TreeLogConnections {
    private TreeLogConnections() { }

    public static List<BlockPos> connectors(Set<BlockPos> nativeLogs, Predicate<BlockPos> available) {
        return connectors(nativeLogs, available, pos -> false);
    }

    public static List<BlockPos> connectors(Set<BlockPos> nativeLogs, Predicate<BlockPos> available,
                                             Predicate<BlockPos> inLeaves) {
        Set<BlockPos> logs = new HashSet<>(nativeLogs);
        List<BlockPos[]> edges = new ArrayList<>();
        var ordered = nativeLogs.stream().sorted(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingInt(pos -> pos.getX()).thenComparingInt(pos -> pos.getZ())).toList();
        for (int i = 0; i < ordered.size(); i++) for (int j = i + 1; j < ordered.size(); j++) {
            BlockPos a = ordered.get(i), b = ordered.get(j);
            if (Math.abs(a.getX() - b.getX()) <= 1 && Math.abs(a.getY() - b.getY()) <= 1
                    && Math.abs(a.getZ() - b.getZ()) <= 1 && a.distManhattan(b) > 1) edges.add(new BlockPos[]{a, b});
        }
        edges.sort(Comparator.comparingInt(edge -> edge[0].distManhattan(edge[1])));
        Set<BlockPos> additions = new LinkedHashSet<>();
        for (var edge : edges) {
            if (connected(logs, edge[0], edge[1])) continue;
            List<List<BlockPos>> choices = new ArrayList<>();
            paths(edge[0], edge[1], new ArrayList<>(), choices);
            var individual = choices.stream().flatMap(List::stream).distinct().map(List::of).toList();
            choices.addAll(individual);
            List<BlockPos> best = null;
            long bestScore = Long.MAX_VALUE;
            for (var path : choices) {
                var fresh = path.stream().filter(pos -> !logs.contains(pos)).toList();
                if (fresh.stream().anyMatch(pos -> !available.test(pos))) continue;
                var combined = new HashSet<>(logs); combined.addAll(fresh);
                if (!connected(combined, edge[0], edge[1])) continue;
                int maxColumn = 0, sumColumns = 0, elevation = 0, exposed = 0;
                for (BlockPos pos : fresh) {
                    int column = 1;
                    for (BlockPos at = pos.below(); combined.contains(at); at = at.below()) column++;
                    for (BlockPos at = pos.above(); combined.contains(at); at = at.above()) column++;
                    maxColumn = Math.max(maxColumn, column);
                    sumColumns += column;
                    elevation += pos.getY() - Math.min(edge[0].getY(), edge[1].getY());
                    if (!inLeaves.test(pos)) exposed++;
                }
                // Keep the upper corner of a sloping limb. Lower hanging logs
                // are only needed when the upper route is obstructed.
                long score = fresh.size() * 1_000_000_000L - elevation * 1_000_000L
                        + exposed * 10_000L + maxColumn * 100L + sumColumns;
                if (score < bestScore) { best = fresh; bestScore = score; }
            }
            if (best != null) { additions.addAll(best); logs.addAll(best); }
        }
        return List.copyOf(additions);
    }

    /** Remove lower duplicates only when every remaining log still has a face-connected path. */
    public static Set<BlockPos> withoutRedundantLowerLogs(Set<BlockPos> original, Set<BlockPos> retained) {
        Set<BlockPos> logs = new LinkedHashSet<>(original);
        var ordered = original.stream().sorted(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).toList();
        for (BlockPos lower : ordered) {
            if (retained.contains(lower) || !logs.contains(lower.above())) continue;
            logs.remove(lower);
            BlockPos start = logs.iterator().next();
            if (logs.stream().anyMatch(pos -> !connected(logs, start, pos))) logs.add(lower);
        }
        return logs;
    }

    private static void paths(BlockPos at, BlockPos end, List<BlockPos> path, List<List<BlockPos>> result) {
        if (at.equals(end)) { result.add(List.copyOf(path)); return; }
        for (Direction direction : Direction.values()) {
            BlockPos next = at.relative(direction);
            if (next.distManhattan(end) >= at.distManhattan(end)) continue;
            if (!next.equals(end)) path.add(next);
            paths(next, end, path, result);
            if (!next.equals(end)) path.remove(path.size() - 1);
        }
    }

    public static boolean connected(Set<BlockPos> logs, BlockPos start, BlockPos end) {
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>(); queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!logs.contains(pos) || !visited.add(pos)) continue;
            if (pos.equals(end)) return true;
            for (Direction direction : Direction.values()) queue.add(pos.relative(direction));
        }
        return false;
    }
}
