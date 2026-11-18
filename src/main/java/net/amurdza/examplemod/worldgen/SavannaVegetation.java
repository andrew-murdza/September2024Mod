package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

/** Flower stripes, a 3x3 crop cluster, and mushrooms per 288 square. */
public final class SavannaVegetation {
    public static final int CROP_REGION_BLOCKS = CropTiles.SAVANNA_TILE_SIZE;
    public static final int PATCHES_PER_SPECIES = 3;
    public static final int CROPS_PER_PATCH = CropTiles.PLANTS_PER_PATCH;
    public static final int CROP_PATCHES_PER_REGION = 11;
    public static final double MINIMUM_DRY_HEIGHT = 61.5;
    public static final List<String> PATCH_SPECIES = CropTiles.SAVANNA_SPECIES;

    private SavannaVegetation() { }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int chunkX = Math.floorDiv(context.origin().getX(), 16), chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        return place(context, cropPatchesForChunk(context.level(), chunkX, chunkZ));
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context, DoubleBinaryOperator surfaceHeight) {
        long seed = context.level().getSeed();
        SavannaFlowerBands bands = new SavannaFlowerBands(seed);
        int chunkX = Math.floorDiv(context.origin().getX(), 16), chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        java.util.function.BiPredicate<Integer, Integer> suitable =
                (x, z) -> bands.colour(x, z) < 0 && surfaceHeight.applyAsDouble(x, z) > MINIMUM_DRY_HEIGHT;
        return place(context, adapt(CropTiles.forChunk(seed, chunkX, chunkZ, PATCH_SPECIES, suitable)));
    }

    private static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context, List<CropPatch> patches) {
        WorldGenLevel level = context.level();
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        SavannaFlowerBands bands = new SavannaFlowerBands(level.getSeed());
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            if (bands.hasFlower(x, z)) placed |= placeTulip(level, x, z, bands.tulip(x, z), context.random());
        }
        List<CropTiles.Patch> reservations = patches.stream().map(p -> new CropTiles.Patch(p.x(), p.z(), p.species())).toList();
        for (CropTiles.Patch patch : reservations) placed |= CropTiles.place(level, patch, reservations) > 0;
        return SavannaSeafloor.fillShallowGaps(level, minX, minZ) || placed;
    }

    public static List<CropPatch> cropPatchesForChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        return adapt(CropTiles.forChunk(level, chunkX, chunkZ, true));
    }

    public static List<CropPatch> cropPatches(WorldGenLevel level, int tileX, int tileZ) {
        return adapt(CropTiles.plan(level, tileX, tileZ, true));
    }

    public static List<CropPatch> cropPatches(long seed, int tileX, int tileZ, DoubleBinaryOperator surfaceHeight) {
        SavannaFlowerBands bands = new SavannaFlowerBands(seed);
        return adapt(CropTiles.plan(seed, tileX, tileZ, PATCH_SPECIES,
                (x, z) -> bands.colour(x, z) < 0 && surfaceHeight.applyAsDouble(x, z) > MINIMUM_DRY_HEIGHT));
    }

    private static List<CropPatch> adapt(List<CropTiles.Patch> patches) {
        return patches.stream().map(p -> new CropPatch(p.x(), p.z(), p.species())).toList();
    }

    public static int placeCropPatch(WorldGenLevel level, CropPatch patch, RandomSource random) {
        return CropTiles.place(level, new CropTiles.Patch(patch.x(), patch.z(), patch.species()));
    }

    public static String tulip(long seed, int x, int z) { return new SavannaFlowerBands(seed).tulip(x, z); }

    /** Neighbouring grass patches can run first; they must not cancel this column's reserved flower. */
    public static boolean placeTulip(WorldGenLevel level, int x, int z, String tulip, RandomSource random) {
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        BlockState previous = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        boolean tall = previous.is(Blocks.TALL_GRASS)
                && previous.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        boolean upperGrass = tall && above.is(Blocks.TALL_GRASS)
                && above.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        if ((!previous.isAir() && !previous.is(Blocks.GRASS) && !tall)
                || (!above.isAir() && !above.is(BlockTags.LEAVES) && !upperGrass)
                || !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                || !GameplayDimensions.biomeIs(level, pos, "savanna")) return false;
        // Place first: single-block tulips need only their own cell. Vanilla shape
        // updates usually remove the old upper grass half; fixtures need the explicit cleanup.
        if (DocumentedFloraFeature.placePlant(level, pos, tulip, random)) {
            if (upperGrass && level.getBlockState(pos.above()).is(Blocks.TALL_GRASS)) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return true;
        }
        // A rejected write must leave both halves intact. Known-shape restoration
        // avoids a temporary missing half deleting its partner during rollback.
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.getBlockState(pos).equals(previous)) level.setBlock(pos, previous, flags);
        if (upperGrass && !level.getBlockState(pos.above()).equals(above)) level.setBlock(pos.above(), above, flags);
        return false;
    }

    public record CropPatch(int x, int z, String species) { }
}
