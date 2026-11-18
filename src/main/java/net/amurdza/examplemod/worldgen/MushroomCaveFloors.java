package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Finish soil exposed by vanilla carvers, which execute after the normal surface rules. */
public final class MushroomCaveFloors {
    private MushroomCaveFloors() { }

    public static void prepare(WorldGenLevel level, int minX, int minZ) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = -32; y < 0; y++) {
                BlockPos root = new BlockPos(x, y, z);
                if (!level.isEmptyBlock(root) || !GameplayDimensions.biomeIs(level, root, "mushroom_caves")) continue;
                BlockPos floor = root.below();
                BlockState ground = level.getBlockState(floor);
                if (ground.is(Blocks.MYCELIUM) || !(naturalGround(ground) || ground.is(Tags.Blocks.ORES))) continue;
                level.setBlock(floor, Blocks.MYCELIUM.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int layer = 1; layer <= 2; layer++) {
                    BlockPos below = floor.below(layer);
                    if (!naturalGround(level.getBlockState(below))) break;
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static boolean naturalGround(BlockState state) {
        // Soil under the surface preserves ore veins, aquifer mud and bedrock.
        // The exposed surface itself may replace an ore with mycelium.
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.CALCITE) || state.is(Blocks.POLISHED_BASALT) || state.is(Blocks.BASALT);
    }
}
