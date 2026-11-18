package net.amurdza.examplemod.worldgen;

import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartState;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

/** At most one heart per selected chunk, enclosed in an existing pale oak trunk. */
public final class SoulCreakingHearts {
    private SoulCreakingHearts() { }

    public static boolean place(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (random.nextInt(8) != 0) return false;
        Block heart = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:creaking_heart"));
        if (!(heart instanceof CreakingHeartBlock)) return false;
        BlockPos selected = null;
        int candidates = 0;
        int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = level.getMinBuildHeight() + 2; y < level.getMaxBuildHeight() - 2; y++) {
                    cursor.set(x, y, z);
                    if (!verticalPaleLog(level.getBlockState(cursor))
                            || !verticalPaleLog(level.getBlockState(cursor.above()))
                            || !verticalPaleLog(level.getBlockState(cursor.below()))
                            || !GameplayDimensions.biomeIs(level, cursor, "soul_sand_valley")) continue;
                    if (random.nextInt(++candidates) == 0) selected = cursor.immutable();
                }
            }
        }
        if (selected == null) return false;
        return level.setBlock(selected, heart.defaultBlockState()
                .setValue(CreakingHeartBlock.AXIS, Direction.Axis.Y)
                .setValue(CreakingHeartBlock.NATURAL, true)
                .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT), Block.UPDATE_CLIENTS);
    }

    private static boolean verticalPaleLog(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && (id.getPath().equals("pale_oak_log") || id.getPath().equals("stripped_pale_oak_log"))
                && state.hasProperty(BlockStateProperties.AXIS) && state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }
}
