package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;

/** Complete Y75 bamboo patches occupy chunks without a cherry tree on that terrace. */
public final class TropicalBamboo {
    public static final int PATCH_WIDTH = 6;
    public static final int PATCH_STEMS = 10;
    public static final int TERRACE_Y = 75;
    private TropicalBamboo() { }

    public static int placeChunk(WorldGenLevel level, ChunkPos chunk) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return 0;
        boolean[][] roots = new boolean[16][16];
        int available = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            BlockPos base = new BlockPos(chunk.getMinBlockX() + x, TERRACE_Y + 1, chunk.getMinBlockZ() + z);
            var soil = level.getBlockState(base.below());
            roots[x][z] = (soil.is(BlockTags.DIRT) || soil.is(Blocks.MOSS_BLOCK)) && openStemCell(level, base)
                    && GameplayDimensions.biomeIs(level, base, "cherry_grove")
                    && base.equals(TropicalVegetation.findGround(level, base.getX(), base.getZ()));
            if (roots[x][z]) available++;
        }
        if (available == 0 || hasTerraceCherry(level, chunk)) return 0;
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ chunk.toLong() ^ 0x91E10DA5C79E7B1DL));
        // Prefer a full patch, then the largest smaller footprint in a narrow terrace fragment.
        int width = 17 - PATCH_WIDTH;
        int start = random.nextInt(width * width), bestIndex = 0, bestCount = 0;
        for (int attempt = 0; attempt < width * width; attempt++) {
            int index = (start + attempt * 37) % (width * width), count = 0;
            for (int x = index % width; x < index % width + PATCH_WIDTH; x++) {
                for (int z = index / width; z < index / width + PATCH_WIDTH; z++) if (roots[x][z]) count++;
            }
            if (count > bestCount) { bestCount = count; bestIndex = index; }
            if (bestCount >= PATCH_STEMS) break;
        }
        return placePatch(level, chunk.getMinBlockX() + bestIndex % width,
                chunk.getMinBlockZ() + bestIndex / width, random, true, Math.min(PATCH_STEMS, bestCount));
    }

    public static boolean hasTerraceCherry(WorldGenLevel level, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                BlockPos base = new BlockPos(x, TERRACE_Y + 1, z);
                if (level.getBlockState(base).is(Blocks.CHERRY_LOG)
                        && !level.getBlockState(base.below()).is(Blocks.CHERRY_LOG)
                        && GameplayDimensions.biomeIs(level, base, "cherry_grove")) return true;
            }
        }
        return false;
    }

    public static int placePatch(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        return placePatch(level, minX, minZ, random, false, PATCH_STEMS);
    }

    private static int placePatch(WorldGenLevel level, int minX, int minZ, RandomSource random, boolean terraceOnly, int minimumStems) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return 0;
        int[] order = new int[PATCH_WIDTH * PATCH_WIDTH];
        for (int i = 0; i < order.length; i++) order[i] = i;
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = order[i]; order[i] = order[j]; order[j] = swap;
        }
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        int stems = 0;
        for (int index : order) {
            BlockPos base = TropicalVegetation.findGround(level, minX + index % PATCH_WIDTH, minZ + index / PATCH_WIDTH);
            if (base == null || terraceOnly && base.getY() != TERRACE_Y + 1 || !GameplayDimensions.biomeIs(level, base, "cherry_grove")
                    || !openStemCell(level, base) || !Blocks.BAMBOO.defaultBlockState().canSurvive(level, base)) continue;
            int clearance = 0;
            while (base.getY() + clearance < level.getMaxBuildHeight()
                    && openStemCell(level, base.above(clearance))) clearance++;
            if (clearance == 0) continue;
            // A real overhead obstacle, including leaves, determines height even above sixteen.
            // Open sky keeps the native feature's five-to-sixteen-block height range.
            int height = base.getY() + clearance < level.getMaxBuildHeight()
                    ? clearance : Math.min(clearance, 5 + random.nextInt(12));
            for (int y = 0; y < height; y++) {
                int fromTop = height - 1 - y;
                BlockState state = Blocks.BAMBOO.defaultBlockState()
                        .setValue(BambooStalkBlock.AGE, height > 2 ? 1 : 0)
                        .setValue(BambooStalkBlock.LEAVES, fromTop <= 1 ? BambooLeaves.LARGE
                                : fromTop == 2 ? BambooLeaves.SMALL : BambooLeaves.NONE)
                        .setValue(BambooStalkBlock.STAGE, fromTop == 0 ? 1 : 0);
                buffer.setBlock(base.above(y), state, 2);
            }
            if (++stems == PATCH_STEMS) return buffer.commit() ? stems : 0;
        }
        // Partial patches are confined to the small terrace fragments selected by placeChunk.
        return stems >= minimumStems && buffer.commit() ? stems : 0;
    }

    private static boolean openStemCell(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)) return false;
        return state.isAir() || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.VINE)
                || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)
                || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.HANGING_ROOTS);
    }

    /** Neighboring chunks can finish a canopy after this chunk's final bamboo pass. */
    public static void trimForObstacle(LevelAccessor level, BlockPos pos, BlockState previous, BlockState placed) {
        if (!previous.is(Blocks.BAMBOO) || placed.is(Blocks.BAMBOO) || !GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                || !placed.isSolid() && !placed.is(BlockTags.LEAVES)) return;
        // Remove the detached upper stalk without drops, inside the same tree transaction.
        // Otherwise a later neighboring crown leaves bamboo protruding through its top.
        BlockPos cursor = pos.above();
        while (!level.isOutsideBuildHeight(cursor) && level.getBlockState(cursor).is(Blocks.BAMBOO)) {
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 18);
            cursor = cursor.above();
        }
        // Finish the surviving stalk immediately below the obstruction and stop its growth.
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos tip = pos.below(depth);
            BlockState bamboo = level.getBlockState(tip);
            if (!bamboo.is(Blocks.BAMBOO)) break;
            level.setBlock(tip, bamboo.setValue(BambooStalkBlock.STAGE, depth == 1 ? 1 : 0)
                    .setValue(BambooStalkBlock.LEAVES, depth <= 2 ? BambooLeaves.LARGE : BambooLeaves.SMALL), 18);
        }
    }
}
