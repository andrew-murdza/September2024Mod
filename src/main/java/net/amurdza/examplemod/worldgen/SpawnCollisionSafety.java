package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

/** Keeps neighbor-dependent collision shapes inside a chunk generation region's available cache. */
public final class SpawnCollisionSafety {
    private SpawnCollisionSafety() { }

    public static boolean hasNoPowderCane(LevelReader level, AABB bounds) {
        if (!canInspect(level, bounds)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(bounds.minX), Mth.floor(bounds.minY), Mth.floor(bounds.minZ),
                Mth.floor(bounds.maxX - 1.0E-7D), Mth.floor(bounds.maxY - 1.0E-7D), Mth.floor(bounds.maxZ - 1.0E-7D))) {
            if (PowderCaneShape.isPlant(level.getBlockState(pos))) return false;
        }
        return true;
    }

    public static boolean canInspect(LevelReader level, BlockPos pos) {
        return canInspect(level, new AABB(pos));
    }

    public static boolean canInspect(LevelReader level, AABB bounds) {
        if (!(level instanceof WorldGenRegion region)) return true;
        // Vanilla scans a one-block collision halo. Hearth & Harvest corn then reads one
        // further horizontal neighbor while building its collision shape. Do not load or
        // invent those chunks during CHUNK_GENERATION: reject this candidate instead.
        int minX = (Mth.floor(bounds.minX - 1.0E-7D) - 2) >> 4;
        int maxX = (Mth.floor(bounds.maxX + 1.0E-7D) + 2) >> 4;
        int minZ = (Mth.floor(bounds.minZ - 1.0E-7D) - 2) >> 4;
        int maxZ = (Mth.floor(bounds.maxZ + 1.0E-7D) + 2) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!region.hasChunk(x, z)) return false;
            }
        }
        return true;
    }
}
