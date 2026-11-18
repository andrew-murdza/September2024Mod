package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.block.state.BlockState;

/** Compatibility entry points for protecting the ocean bed without excluding the caves underneath. */
public final class SnowyLakeCaves {
    private SnowyLakeCaves() { }

    public static boolean lakeColumn(RandomState random, int x, int z) {
        return SnowyOceanFloor.submerged(SnowyOceanFloor.floor(random, x, z));
    }

    public static void closeBuriedPockets(ChunkAccess chunk, RandomState random, BlockState rock) {
        SnowyOceanFloor.restoreCoverAndWater(chunk, random, rock);
    }

    public static void protectCarverColumns(ChunkAccess chunk, RandomState random, CarvingMask mask) {
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            if (!lakeColumn(random, minX + dx, minZ + dz)) continue;
            int floor = SnowyOceanFloor.floor(random, minX + dx, minZ + dz);
            for (int y = floor - SnowyOceanFloor.COVER_BLOCKS + 1; y <= floor; y++) mask.set(dx, y, dz);
        }
    }
}
