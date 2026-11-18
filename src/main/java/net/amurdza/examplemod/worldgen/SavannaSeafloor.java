package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Completes the one-block-deep shoreline after the original weighted seafloor pass. */
public final class SavannaSeafloor {
    private SavannaSeafloor() { }

    public static boolean fillShallowGaps(WorldGenLevel level, int minX, int minZ) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        // Savanna lakes fill to sea level. Reading that surface directly also
        // reaches shoreline water below a tree canopy without touching deep beds.
        // Level/WorldGenRegion#getSeaLevel returns vanilla's constant 63;
        // use the custom generator's configured sea level instead.
        int waterY = level.getLevel().getChunkSource().getGenerator().getSeaLevel() - 1;
        BlockState kelp = Blocks.KELP.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25);
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos pos = new BlockPos(x, waterY, z);
            BlockState water = level.getBlockState(pos);
            BlockState bed = level.getBlockState(pos.below());
            if (!water.is(Blocks.WATER) || !water.getFluidState().isSource()
                    || !level.isEmptyBlock(pos.above()) || !bed.getFluidState().isEmpty()
                    || !(bed.is(Blocks.MUD) || bed.is(Blocks.GRASS_BLOCK)
                    || bed.is(Blocks.DIRT) || bed.is(Blocks.ROOTED_DIRT))
                    || !bed.isFaceSturdy(level, pos.below(), Direction.UP)
                    || !GameplayDimensions.biomeIs(level, pos, "savanna") || !kelp.canSurvive(level, pos)) continue;
            // The shared kelp feature reserves a water cell above its head and
            // cannot satisfy its shallow weighted choice. Fill only those bare
            // source cells; kelp retains the exact water that was already here.
            // Only native terrain is a bed: a sponge in deeper water must not
            // turn its remaining surface water into a second plant selection.
            placed |= level.setBlock(pos, kelp, Block.UPDATE_CLIENTS);
        }
        return placed;
    }
}
