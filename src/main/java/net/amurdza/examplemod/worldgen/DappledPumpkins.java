package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/** Early crop pass: reserves Dappled pumpkins and Snowy Taiga berries before tree and shrub decoration. */
public final class DappledPumpkins extends Feature<NoneFeatureConfiguration> {
    private static final String PUMPKIN = "minecraft:pumpkin";
    public static final int PUMPKINS_PER_PATCH = 2;
    public static final int PATCH_WIDTH = 2;

    public DappledPumpkins() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GameplayDimensions.is(context.level(), GameplayDimensions.SNOWY)) return false;
        boolean placed = SnowyVegetation.placeBerryPatchesInChunk(context.level(),
                Math.floorDiv(context.origin().getX(), 16), Math.floorDiv(context.origin().getZ(), 16));
        for (var patch : forChunk(context.level(), Math.floorDiv(context.origin().getX(), 16), Math.floorDiv(context.origin().getZ(), 16))) {
            placed |= placePatch(context.level(), patch) > 0;
        }
        return placed;
    }

    public static List<CropTiles.Patch> plan(long seed, int tileX, int tileZ, BiPredicate<Integer, Integer> suitable) {
        return SpacedCropPatches.plan(seed ^ "dappled_forest".hashCode(), tileX, tileZ,
                List.of(PUMPKIN), PUMPKINS_PER_PATCH, suitable);
    }

    public static List<CropTiles.Patch> plan(WorldGenLevel level, int tileX, int tileZ) {
        return SnowyCropClusters.plan(level, tileX, tileZ, "dappled_forest", List.of(PUMPKIN), PUMPKINS_PER_PATCH);
    }

    public static List<CropTiles.Patch> forChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        return inChunk(chunkX, chunkZ, (x, z) -> plan(level, x, z));
    }

    public static List<CropTiles.Patch> forChunk(long seed, int chunkX, int chunkZ, BiPredicate<Integer, Integer> suitable) {
        return inChunk(chunkX, chunkZ, (x, z) -> plan(seed, x, z, suitable));
    }

    private static List<CropTiles.Patch> inChunk(int chunkX, int chunkZ, BiFunction<Integer, Integer, List<CropTiles.Patch>> plans) {
        int minX = chunkX * 16, minZ = chunkZ * 16;
        List<CropTiles.Patch> result = new ArrayList<>();
        for (int tx = Math.floorDiv(minX, SpacedCropPatches.TILE_SIZE); tx <= Math.floorDiv(minX + 15, SpacedCropPatches.TILE_SIZE); tx++) {
            for (int tz = Math.floorDiv(minZ, SpacedCropPatches.TILE_SIZE); tz <= Math.floorDiv(minZ + 15, SpacedCropPatches.TILE_SIZE); tz++) {
                for (var patch : plans.apply(tx, tz)) {
                    if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) result.add(patch);
                }
            }
        }
        return List.copyOf(result);
    }

    public static int placePatch(WorldGenLevel level, CropTiles.Patch patch) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY) || !patch.species().equals(PUMPKIN)) return 0;
        int minX = Math.floorDiv(patch.x(), 8) * 8, minZ = Math.floorDiv(patch.z(), 8) * 8;
        List<CropTiles.Patch> anchors = new ArrayList<>();
        for (int x = minX; x <= minX + 8 - PATCH_WIDTH; x++) for (int z = minZ; z <= minZ + 8 - PATCH_WIDTH; z++) {
            anchors.add(new CropTiles.Patch(x, z, PUMPKIN));
        }
        anchors.sort(Comparator.comparingLong(p -> hash(level.getSeed(), p.x(), p.z())));
        if (anchors.remove(patch)) anchors.add(0, patch);
        for (var anchor : anchors) if (placeAt(level, anchor)) return PUMPKINS_PER_PATCH;
        return 0;
    }

    private static boolean placeAt(WorldGenLevel level, CropTiles.Patch patch) {
        RandomSource random = RandomSource.create(hash(level.getSeed(), patch.x(), patch.z()));
        int[] offsets = new int[PATCH_WIDTH * PATCH_WIDTH];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = offsets[i]; offsets[i] = offsets[j]; offsets[j] = swap;
        }
        var buffer = new GrowthPlacementBuffer(level);
        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        List<BlockPos> roots = new ArrayList<>();
        boolean accepted = false;
        try {
            for (int offset : offsets) {
                int x = patch.x() + offset % PATCH_WIDTH, z = patch.z() + offset / PATCH_WIDTH;
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                BlockState root = level.getBlockState(pos), upper = level.getBlockState(pos.above());
                boolean tall = root.is(Blocks.TALL_GRASS) && root.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                        && upper.is(Blocks.TALL_GRASS) && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                if (!GameplayDimensions.biomeIs(level, pos, "dappled_forest") || !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                        || !tall && (!(root.isAir() || root.is(Blocks.GRASS)) || !upper.isAir())) continue;
                original.put(pos, root); original.put(pos.above(), upper);
                buffer.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                buffer.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (!DocumentedFloraFeature.placePlant(buffer, pos, PUMPKIN, random)) return false;
                roots.add(pos);
                if (roots.size() == PUMPKINS_PER_PATCH) break;
            }
            if (roots.size() != PUMPKINS_PER_PATCH || !buffer.isValid((pos, state) -> original.containsKey(pos))) return false;
            for (BlockPos root : roots) {
                if (!buffer.getBlockState(root).is(Blocks.PUMPKIN) || !buffer.getBlockState(root.above()).isAir()) return false;
            }
            if (!buffer.commit()) return false;
            accepted = roots.stream().allMatch(pos -> level.getBlockState(pos).is(Blocks.PUMPKIN) && level.getBlockState(pos.above()).isAir());
            return accepted;
        } finally {
            if (!accepted) original.forEach((pos, state) -> restore(level, pos, state));
        }
    }

    private static void restore(WorldGenLevel level, BlockPos pos, BlockState original) {
        if (level.getBlockState(pos).equals(original)) return;
        level.setBlock(pos, original, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (!level.getBlockState(pos).equals(original)) {
            level.getChunk(pos).setBlockState(pos, original, false);
            level.getLightEngine().checkBlock(pos);
        }
    }

    private static long hash(long seed, int x, int z) {
        return TropicalLayout.mix(seed ^ 0x1679acL ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L)
                ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L));
    }

}
