package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;

import java.util.HashMap;
import java.util.Map;

/** Snow belongs to the entire completed crown, including leaves across a decoration boundary. */
public final class SnowyCanopySnow {
    private SnowyCanopySnow() { }

    public static void cover(GrowthPlacementBuffer level) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return;
        Map<Long, BlockPos> tops = new HashMap<>();
        for (BlockPos leaf : level.placedBlocks(state -> state.is(BlockTags.LEAVES))) {
            long column = ((long) leaf.getX() << 32) ^ (leaf.getZ() & 0xffffffffL);
            tops.merge(column, leaf, (first, second) -> first.getY() > second.getY() ? first : second);
        }
        for (BlockPos top : tops.values()) {
            if (GameplayDimensions.biomeIs(level, top, "snowy_forest")) {
                SnowyPlantSnowlogging.coverSurface(level, top.above());
            }
        }
    }
}
