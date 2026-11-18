package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Complete mushroom patches on underground Plains floors. */
public final class PlainsCaveVegetation {
    // Three dispersed opportunities per nine chunk columns keep the old 1/3 mean.
    public static final int PATCH_RARITY = 3;
    public static final int MUSHROOMS_PER_PATCH = 5;
    public static final int COLUMN_ATTEMPTS = 16;
    private static final int[][] PATCH_COLUMNS = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
    };

    private PlainsCaveVegetation() { }

    /** One candidate in each row and column of a seeded three-by-three chunk tile. */
    public static boolean patchChunk(long seed, int chunkX, int chunkZ) {
        int tileX = Math.floorDiv(chunkX, PATCH_RARITY), tileZ = Math.floorDiv(chunkZ, PATCH_RARITY);
        long hash = TropicalLayout.mix(seed ^ 0x506C61696E734675L
                ^ tileX * 341873128712L ^ tileZ * 132897987541L);
        int[] columns = PATCH_COLUMNS[Math.floorMod(hash, PATCH_COLUMNS.length)];
        return Math.floorMod(chunkX, PATCH_RARITY) == columns[Math.floorMod(chunkZ, PATCH_RARITY)];
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return false;
        int chunkX = Math.floorDiv(context.origin().getX(), 16), chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        int minX = chunkX * 16, minZ = chunkZ * 16;
        if (!patchChunk(level.getSeed(), chunkX, chunkZ)) return false;
        // Species, floor search and patch jitter are independent of other flora's
        // random consumption and of the order neighboring chunks decorate.
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ 0x4361766550617463L
                ^ chunkX * 341873128712L ^ chunkZ * 132897987541L));
        Block mushroom = random.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM;
        {
            // Bounded stratified search: each column is scanned once. The old
            // 121-column search performed repeated full-height ceiling and
            // biome queries, making empty chunks disproportionately expensive.
            int start = random.nextInt(121);
            boolean planted = false;
            for (int attempt = 0; attempt < COLUMN_ATTEMPTS && !planted; attempt++) {
                int cell = (start + attempt * 37) % 121;
                int x = minX + 2 + cell % 11, z = minZ + 2 + cell / 11;
                List<BlockPos> floors = new ArrayList<>();
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                boolean roof = false;
                for (int y = surface - 1; y > level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isTerrainRoof(state)) roof = true;
                    if (roof && y < surface - 2 && state.isAir() && isSupportedFloor(level, pos)) floors.add(pos);
                }
                while (!floors.isEmpty() && !planted) {
                    BlockPos anchor = floors.remove(random.nextInt(floors.size()));
                    planted = placePatch(level, anchor, mushroom, random) > 0;
                }
            }
            return planted;
        }
    }

    public static int placePatch(WorldGenLevel level, BlockPos anchor, Block mushroom, RandomSource random) {
        if (mushroom != Blocks.BROWN_MUSHROOM && mushroom != Blocks.RED_MUSHROOM) return 0;
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS) || !isCaveFloor(level, anchor)) return 0;
        int[] offsets = new int[36];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = offsets[i]; offsets[i] = offsets[j]; offsets[j] = swap;
        }
        List<BlockPos> roots = new ArrayList<>();
        BlockState state = mushroom.defaultBlockState();
        for (int offset : offsets) {
            int x = anchor.getX() - 2 + offset % 6;
            int z = anchor.getZ() - 2 + offset / 6;
            // Follow small steps in the same cave floor without reaching another
            // gallery or placing multiple mushrooms in one vertical column.
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos pos = new BlockPos(x, anchor.getY() + dy, z);
                if (isCaveFloor(level, pos) && state.canSurvive(level, pos)) {
                    roots.add(pos);
                    break;
                }
            }
            if (roots.size() == MUSHROOMS_PER_PATCH) break;
        }
        if (roots.size() != MUSHROOMS_PER_PATCH) return 0;
        List<BlockPos> planted = new ArrayList<>();
        for (BlockPos pos : roots) {
            if (!level.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
                for (BlockPos previous : planted) level.setBlock(previous, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                return 0;
            }
            planted.add(pos);
        }
        return planted.size();
    }

    public static boolean isCaveFloor(WorldGenLevel level, BlockPos pos) {
        if (!isSupportedFloor(level, pos)) return false;
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
        if (pos.getY() >= surface - 2) return false;
        for (int y = pos.getY() + 1; y < surface; y++) {
            if (isTerrainRoof(level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())))) return true;
        }
        return false;
    }

    private static boolean isSupportedFloor(WorldGenLevel level, BlockPos pos) {
        if (StructurePlantExclusion.excludes(level, pos)) return false;
        if (!level.isEmptyBlock(pos)) return false;
        BlockState support = level.getBlockState(pos.below());
        if (!support.isFaceSturdy(level, pos.below(), Direction.UP)
                || support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS)
                || support.is(Blocks.MUSHROOM_STEM)) return false;
        return GameplayDimensions.biomeIs(level, pos, "plains");
    }

    private static boolean isTerrainRoof(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL) || state.is(Blocks.BEDROCK);
    }
}
