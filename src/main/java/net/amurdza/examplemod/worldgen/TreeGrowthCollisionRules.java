package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Compares a staged tropical tree with the world before that whole tree attempt. */
public final class TreeGrowthCollisionRules {
    private TreeGrowthCollisionRules() { }

    public static boolean isAllowed(LevelReader original, BlockPos pos, BlockState proposed) {
        BlockState existing = original.getBlockState(pos);
        // Stems never consume an existing crown, in any dimension. Cap blocks
        // retain their separate leaf-replacement permission below.
        if (proposed.getBlock() instanceof HugeMushroomBlock && isWood(proposed) && !existing.isAir()) return false;
        // Mushroom caps also carry our leaves tag for harvesting. That must not
        // make them replaceable by ordinary foliage during tree generation.
        boolean mushroom = proposed.getBlock() instanceof HugeMushroomBlock;
        if (mushroom && isMangroveRoot(existing)) return false;
        boolean leafReplacingCap = proposed.is(Blocks.RED_MUSHROOM_BLOCK) || proposed.is(Blocks.BROWN_MUSHROOM_BLOCK);
        if (mushroom && !leafReplacingCap && existing.is(BlockTags.LEAVES)) return false;
        if (!mushroom && (isWood(proposed) || proposed.is(BlockTags.LEAVES))
                && (existing.getBlock() instanceof HugeMushroomBlock || RedMushroomCapSpace.isInsideRedCap(pos,
                sample -> original.getBlockState(sample).is(Blocks.RED_MUSHROOM_BLOCK)))) return false;
        if (GameplayDimensions.is(original, GameplayDimensions.NETHER)
                && (isWood(proposed) || proposed.is(BlockTags.LEAVES) || proposed.is(Blocks.NETHER_WART_BLOCK)
                || proposed.is(Blocks.WARPED_WART_BLOCK) || proposed.is(Blocks.SHROOMLIGHT)
                || proposed.is(Blocks.WEEPING_VINES) || proposed.is(Blocks.WEEPING_VINES_PLANT))) {
            // Template fungi do not perform vanilla's whole-canopy clearance test.
            // Keep new cave walls/pillars and the reserved soul crop patches intact.
            if (existing.is(ModBlocks.SOUL_BEETROOTS.get())) return false;
            return existing.isAir() || existing.canBeReplaced() && existing.getFluidState().isEmpty();
        }
        if (!GameplayDimensions.is(original, GameplayDimensions.TROPICAL)) return true;
        // A skipped trunk cell is just as harmful as an overwritten one: vanilla
        // clearance accepts existing logs, so the new branch can continue beyond
        // another tree and graft itself onto that tree. Keep one clear block
        // between independent woody skeletons, not between their leafy crowns.
        if (isWood(existing)) return false;
        boolean wood = isWood(proposed);
        boolean cap = isMushroomCap(proposed);
        if (proposed.is(Blocks.CHERRY_LEAVES)) {
            if (existing.is(Blocks.CHERRY_LEAVES)) return false;
            // Independent cherries keep an air gap between their actual crowns;
            // mixed rainforest crowns may still overlap at the biome boundary.
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!original.isOutsideBuildHeight(neighbor)
                        && original.getBlockState(neighbor).is(Blocks.CHERRY_LEAVES)) return false;
            }
        }
        if ((wood || cap) && (isMushroomCap(existing) || !leafReplacingCap && existing.is(BlockTags.LEAVES))) return false;
        if (wood || cap) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!original.isOutsideBuildHeight(neighbor)) {
                    BlockState adjacent = original.getBlockState(neighbor);
                    if (isWood(adjacent) || mushroom && isMangroveRoot(adjacent)) return false;
                }
            }
        }
        return true;
    }

    public static boolean isWood(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae");
    }

    private static boolean isMangroveRoot(BlockState state) {
        return state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    private static boolean isMushroomCap(BlockState state) {
        return state.getBlock() instanceof HugeMushroomBlock && !isWood(state);
    }
}
