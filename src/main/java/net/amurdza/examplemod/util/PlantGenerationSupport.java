package net.amurdza.examplemod.util;

import net.amurdza.examplemod.worldgen.StructurePlantExclusion;
import net.amurdza.examplemod.worldgen.structure.MansionForestFloor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

/** Flowers and wild crops belong on their native ground, never on or inside logs. */
public final class PlantGenerationSupport {
    private PlantGenerationSupport() { }

    public static boolean isAllowed(LevelReader level, BlockPos pos, BlockState state) {
        if ((state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS))
                && level instanceof WorldGenLevel world
                && GameplayDimensions.is(level, GameplayDimensions.PLAINS)) {
            boolean tall = state.is(Blocks.TALL_GRASS);
            BlockPos root = tall && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            if (level.getBlockState(root.below()).is(Blocks.PODZOL) || StructurePlantExclusion.excludes(world, root)) return false;
            if (tall) {
                Integer forestRoot = MansionForestFloor.rootY(world, root.getX(), root.getZ());
                if (forestRoot != null && forestRoot == root.getY()) return false;
            }
        }
        // Vanilla's flowers tag also contains cherry/flowering-azalea leaves
        // and mangrove propagules for bees. Those are tree parts, not ground flowers.
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS)) return true;
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        boolean wildCrop = state.is(Blocks.PUMPKIN) || id != null
                && (id.getPath().startsWith("wild_") || id.getPath().endsWith("_wild_crop"));
        if (wildCrop && level instanceof WorldGenLevel world && GameplayDimensions.is(level, GameplayDimensions.PLAINS)) {
            var wildRoot = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            for (var forest : MansionForestFloor.forChunk(world, new net.minecraft.world.level.ChunkPos(pos)))
                if (forest.contains(pos.getX(), pos.getZ()) && wildRoot.getY() == forest.rootY()) return false;
        }
        if (!state.is(BlockTags.FLOWERS) && (id == null
                || !id.getPath().startsWith("wild_") && !id.getPath().endsWith("_wild_crop"))) return true;
        BlockPos root = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL) && root.getY() == 76) return false;
        return !level.getBlockState(pos).is(BlockTags.LOGS)
                && !level.getBlockState(pos.below()).is(BlockTags.LOGS);
    }
}
