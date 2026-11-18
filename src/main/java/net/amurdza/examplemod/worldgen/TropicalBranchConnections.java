package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Lower supports and upper bridges join rounded native limbs by block faces. */
public final class TropicalBranchConnections {
    private TropicalBranchConnections() { }

    /** Minimal face connections: no upper bridges or extra top row. */
    public static List<BlockPos> lowerLimbCorners(BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX(), dy = end.getY() - start.getY(), dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return List.of();
        List<BlockPos> nativeLogs = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            nativeLogs.add(start.offset(Mth.floor(.5F + step * (float) dx / steps),
                    Mth.floor(.5F + step * (float) dy / steps), Mth.floor(.5F + step * (float) dz / steps)));
        }
        LinkedHashSet<BlockPos> corners = new LinkedHashSet<>();
        for (int step = 1; step < nativeLogs.size(); step++) {
            BlockPos first = nativeLogs.get(step - 1), second = nativeLogs.get(step);
            BlockPos lower = first.getY() <= second.getY() ? first : second;
            BlockPos higher = lower.equals(first) ? second : first;
            if (lower.getX() == higher.getX() && lower.getZ() == higher.getZ()) continue;
            // An X/Z/Y corner needs one horizontal elbow plus the support under
            // the upper log. Both additions stay at the lower native elevation.
            corners.add(new BlockPos(higher.getX(), lower.getY(), lower.getZ()));
            corners.add(new BlockPos(higher.getX(), lower.getY(), higher.getZ()));
        }
        corners.removeAll(nativeLogs);
        return List.copyOf(corners);
    }

    public static List<BlockPos> limbCorners(BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX(), dy = end.getY() - start.getY(), dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return List.of();
        float stepX = (float) dx / steps, stepY = (float) dy / steps, stepZ = (float) dz / steps;
        List<BlockPos> nativeLogs = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            nativeLogs.add(start.offset(Mth.floor(.5F + step * stepX), Mth.floor(.5F + step * stepY),
                    Mth.floor(.5F + step * stepZ)));
        }
        LinkedHashSet<BlockPos> corners = new LinkedHashSet<>();
        for (int step = 1; step < nativeLogs.size(); step++) {
            BlockPos first = nativeLogs.get(step - 1), second = nativeLogs.get(step);
            BlockPos lower = first.getY() <= second.getY() ? first : second;
            BlockPos higher = lower.equals(first) ? second : first;
            // Move horizontally at the lower elevation, then rise. This fills
            // the inside lower corner visible under each diagonal branch step.
            BlockPos corner = new BlockPos(higher.getX(), lower.getY(), lower.getZ());
            if (!corner.equals(first) && !corner.equals(second)) corners.add(corner);
            corner = new BlockPos(higher.getX(), lower.getY(), higher.getZ());
            if (!corner.equals(first) && !corner.equals(second)) corners.add(corner);
            // Also bridge above the lower end. A limb that changes X and Z
            // receives an upper horizontal elbow instead of a corner-only join.
            corner = new BlockPos(lower.getX(), higher.getY(), lower.getZ());
            if (!corner.equals(first) && !corner.equals(second)) corners.add(corner);
            corner = new BlockPos(higher.getX(), higher.getY(), lower.getZ());
            if (!corner.equals(first) && !corner.equals(second)) corners.add(corner);
            if (lower.getY() == higher.getY() && lower.getX() != higher.getX() && lower.getZ() != higher.getZ()) {
                // A flat X/Z diagonal needs its elbow and the log immediately
                // above it; straight horizontal limbs need no extra top row.
                corners.add(corner.above());
            }
        }
        corners.removeAll(nativeLogs);
        return List.copyOf(corners);
    }
}
