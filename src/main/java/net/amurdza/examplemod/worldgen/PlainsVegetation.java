package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/** Regroups the existing tall-grass budget without consuming or changing short grass. */
public final class PlainsVegetation {
    public static final int CROP_REGION_BLOCKS = CropTiles.TILE_SIZE;
    public static final int PATCHES_PER_SPECIES = 3;
    public static final int CROP_PATCHES_PER_REGION = 9;
    public static final int CROPS_PER_PATCH = 5;
    private static final Set<String> REPLACED_SCATTER = Set.of("farmersdelight:wild_potatoes", "farmersdelight:wild_carrots",
            "farmersdelight:wild_beetroots", "minecraft:pumpkin", "farmersdelight:wild_cabbages",
            "minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:glow_lichen");

    private PlainsVegetation() { }

    public static boolean replacesScatteredPlant(String name) { return REPLACED_SCATTER.contains(name); }

    /** Run after the original floor and 40-column flora pass, retaining their grass lottery. */
    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        boolean placed = false;
        List<CropTiles.Patch> reservations = CropTiles.forChunk(level, Math.floorDiv(minX, 16), Math.floorDiv(minZ, 16), false);
        for (CropTiles.Patch patch : reservations) {
            placed |= CropTiles.place(level, patch, reservations) > 0;
        }
        return relocateTallGrass(level, minX, minZ, context.random()) > 0 || placed;
    }

    /** Three carrot, cabbage and pumpkin chunks, shuffled within one 3x3 cluster per 288 tile. */
    public static List<CropPatch> cropPatches(long seed, int tileX, int tileZ) {
        return cropPatches(seed, tileX, tileZ, (x, z) -> true);
    }

    public static List<CropPatch> cropPatches(long seed, int tileX, int tileZ,
                                             BiPredicate<Integer, Integer> suitableColumn) {
        return adapt(CropTiles.plan(seed, tileX, tileZ, CropTiles.PLAINS_SPECIES, suitableColumn));
    }

    public static List<CropPatch> cropPatches(WorldGenLevel level, int tileX, int tileZ) {
        return adapt(CropTiles.plan(level, tileX, tileZ, false));
    }

    public static List<CropPatch> cropPatchesForChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        return adapt(CropTiles.forChunk(level, chunkX, chunkZ, false));
    }

    public static List<CropPatch> cropPatchesForChunk(long seed, int chunkX, int chunkZ,
                                                    BiPredicate<Integer, Integer> suitableColumn) {
        return adapt(CropTiles.forChunk(seed, chunkX, chunkZ, CropTiles.PLAINS_SPECIES, suitableColumn));
    }

    private static List<CropPatch> adapt(List<CropTiles.Patch> patches) {
        return patches.stream().map(p -> new CropPatch(p.x(), p.z(), p.species())).toList();
    }

    public static int placeCropPatchWithRetries(WorldGenLevel level, CropPatch patch) {
        return CropTiles.place(level, new CropTiles.Patch(patch.x(), patch.z(), patch.species()));
    }

    public static int placeCropPatch(WorldGenLevel level, CropPatch patch, RandomSource random) {
        return CropTiles.placeAt(level, new CropTiles.Patch(patch.x(), patch.z(), patch.species()), random);
    }

    public static int relocateTallGrass(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        List<BlockPos> original = new ArrayList<>();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos pos = surfaceRoot(level, x, z);
            if (pos != null && level.getBlockState(pos).is(Blocks.TALL_GRASS)
                    && level.getBlockState(pos).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                original.add(pos);
            }
        }
        if (original.isEmpty()) return 0;
        clearPlants(level, original);
        int target = original.size();
        // All totals >=4 except seven can be partitioned into groups of 4..6.
        // Unbiased rounding preserves the original mean for the small exceptional totals.
        if (target < 4) target = random.nextInt(4) < target ? 4 : 0;
        if (target == 7) target = random.nextBoolean() ? 6 : 8;
        if (target == 0) return 0;

        List<List<BlockPos>> patches = new ArrayList<>();
        for (int dx : new int[]{1, 9}) for (int dz : new int[]{1, 9}) {
            List<BlockPos> candidates = new ArrayList<>();
            for (int offset : shuffledOffsets(random)) {
                BlockPos pos = surfaceRoot(level, minX + dx + offset % 6, minZ + dz + offset / 6);
                if (pos != null && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) candidates.add(pos);
            }
            patches.add(candidates);
        }
        // Disjoint six-square footprints leave two columns between clusters and a
        // border at chunk edges. Terrain obstruction may reduce the retained budget.
        List<int[]> allocations = new ArrayList<>();
        int available = Math.min(target, 24);
        while (available >= 4 && allocations.isEmpty()) {
            allocate(patches, 0, available, new int[4], allocations);
            if (allocations.isEmpty()) available--;
        }
        if (allocations.isEmpty()) return 0;
        int[] sizes = allocations.get(random.nextInt(allocations.size()));
        int count = 0;
        for (int i = 0; i < patches.size(); i++) {
            List<BlockPos> planted = new ArrayList<>();
            for (BlockPos pos : patches.get(i)) {
                if (planted.size() >= sizes[i]) break;
                if (DocumentedFloraFeature.placePlant(level, pos, "minecraft:tall_grass", random)) planted.add(pos);
            }
            if (planted.size() < 4) clearPlants(level, planted);
            else count += planted.size();
        }
        return count;
    }

    private static void allocate(List<List<BlockPos>> patches, int index, int left, int[] sizes, List<int[]> results) {
        if (index == patches.size()) {
            if (left == 0) results.add(sizes.clone());
            return;
        }
        sizes[index] = 0;
        allocate(patches, index + 1, left, sizes, results);
        for (int size = 4; size <= Math.min(6, patches.get(index).size()) && size <= left; size++) {
            sizes[index] = size;
            allocate(patches, index + 1, left - size, sizes, results);
        }
    }

    private static BlockPos surfaceRoot(WorldGenLevel level, int x, int z) {
        // This heightmap ignores non-solid grass, but excludes cave floors and the
        // bottom of water columns without relying on the modded survival rules.
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                && GameplayDimensions.biomeIs(level, pos, "plains") ? pos : null;
    }

    private static int[] shuffledOffsets(RandomSource random) {
        int[] offsets = new int[36];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = offsets[i]; offsets[i] = offsets[j]; offsets[j] = swap;
        }
        return offsets;
    }

    private static void clearPlants(WorldGenLevel level, List<BlockPos> planted) {
        for (BlockPos pos : planted) {
            if (level.getBlockState(pos).hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static long hash(long seed, int x, int z) {
        return TropicalLayout.mix(seed ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L)
                ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L));
    }

    public record CropPatch(int x, int z, String species) { }
}
