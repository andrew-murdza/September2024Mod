package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import snownee.snow.block.entity.SnowBlockEntity;

/** One patch of each berry per suitable 120-square tile, with six bushes per patch. */
public final class SnowyVegetation {
    public static final int BERRIES_PER_PATCH = 6;
    public static final int PATCH_WIDTH = 6;
    public static final int DAPPLED_GRASS_ATTEMPTS = 2;
    private static final List<String> BERRIES = List.of("minecraft:sweet_berry_bush", "aoemod:blue_berry_bush");

    private SnowyVegetation() { }

    public record BerryPatch(int x, int z, String species, long seed) { }

    public static List<BerryPatch> berryTilePatches(long seed, int tileX, int tileZ,
                                                   BiPredicate<Integer, Integer> suitable) {
        return berries(seed, SpacedCropPatches.plan(seed ^ "snowy_forest".hashCode(), tileX, tileZ,
                BERRIES, BERRIES_PER_PATCH, suitable));
    }

    public static List<BerryPatch> berryTilePatches(WorldGenLevel level, int tileX, int tileZ) {
        return berries(level.getSeed(), SnowyCropClusters.plan(level, tileX, tileZ,
                "snowy_forest", BERRIES, BERRIES_PER_PATCH));
    }

    public static List<BerryPatch> berryPatches(long seed, int chunkX, int chunkZ) {
        return berriesInChunk(chunkX, chunkZ, (x, z) -> berryTilePatches(seed, x, z, (px, pz) -> true));
    }

    public static List<BerryPatch> berryPatches(WorldGenLevel level, int chunkX, int chunkZ) {
        return berriesInChunk(chunkX, chunkZ, (x, z) -> berryTilePatches(level, x, z));
    }

    private static List<BerryPatch> berriesInChunk(int chunkX, int chunkZ,
            java.util.function.BiFunction<Integer, Integer, List<BerryPatch>> plans) {
        List<BerryPatch> result = new ArrayList<>();
        for (int tx = Math.floorDiv(chunkX * 16, SpacedCropPatches.TILE_SIZE);
             tx <= Math.floorDiv(chunkX * 16 + 15, SpacedCropPatches.TILE_SIZE); tx++) {
            for (int tz = Math.floorDiv(chunkZ * 16, SpacedCropPatches.TILE_SIZE);
                 tz <= Math.floorDiv(chunkZ * 16 + 15, SpacedCropPatches.TILE_SIZE); tz++) {
                for (BerryPatch patch : plans.apply(tx, tz)) {
                    if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) result.add(patch);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<BerryPatch> berries(long seed, List<CropTiles.Patch> patches) {
        return patches.stream().map(p -> new BerryPatch(p.x() - 2, p.z() - 2, p.species(),
                TropicalLayout.mix(seed ^ p.species().hashCode() ^ p.x() * 0x632be59bd9b4e019L
                        ^ p.z() * 0x9e3779b97f4a7c15L))).toList();
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GameplayDimensions.is(context.level(), GameplayDimensions.SNOWY)) return false;
        WorldGenLevel level = context.level();
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        boolean placed = SnowyLakeVegetation.place(level, minX, minZ);
        placed |= placeDappledGrass(level, minX, minZ);
        placed |= placeBerryPatchesInChunk(level, Math.floorDiv(minX, 16), Math.floorDiv(minZ, 16));
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos root = surfaceRoot(level, x, z);
            if (root != null && GameplayDimensions.biomeIs(level, root, "snowy_forest")) {
                BlockState plant = groveGroundCover(level.getSeed(), x, z);
                if (plant != null && level.getBlockState(root.below()).is(Blocks.GRASS_BLOCK)
                        && (level.isEmptyBlock(root) || level.getBlockState(root).is(Blocks.SNOW))) {
                    placed |= SnowyPlantSnowlogging.place(level, root, plant);
                }
                placed |= SnowyPlantSnowlogging.coverSurface(level, root);
            }
            // Keep taiga snow on the exposed canopy/ice as well as the forest floor.
            // Mountain Lake and its legacy alias now have rain instead of snow.
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (GameplayDimensions.biomeIs(level, top, "snowy_forest")) {
                placed |= SnowyPlantSnowlogging.coverSurface(level, top);
            }
        }
        return placed;
    }

    /** Reserve complete berry patches before trees; the later flora pass may retry only missing patches. */
    public static boolean placeBerryPatchesInChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        boolean placed = false;
        for (BerryPatch patch : berryPatches(level, chunkX, chunkZ)) {
            placed |= placeBerryPatchWithRetries(level, patch) == BERRIES_PER_PATCH;
        }
        return placed;
    }

    /** Two native forest patches, anchored below the canopy on the actual soil. */
    public static boolean placeDappledGrass(WorldGenLevel level, int minX, int minZ) {
        var patch = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("minecraft:patch_grass"));
        if (patch == null) return false;
        var random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ 0x5d13a4cbL
                ^ TropicalLayout.mix(minX * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(minZ * 0x9e3779b97f4a7c15L)));
        boolean placed = false;
        for (int attempt = 0; attempt < DAPPLED_GRASS_ATTEMPTS; attempt++) {
            BlockPos root = surfaceRoot(level, minX + random.nextInt(16), minZ + random.nextInt(16));
            if (root != null && GameplayDimensions.biomeIs(level, root, "dappled_forest")) {
                placed |= patch.place(level, level.getLevel().getChunkSource().getGenerator(), random, root);
            }
        }
        return placed;
    }

    /** Historical Overworld grove (e484294^): 6% cover, with grass/fern/large-fern weights 6:10:1. */
    public static BlockState groveGroundCover(long seed, int x, int z) {
        RandomSource random = RandomSource.create(TropicalLayout.mix(seed ^ 0x6bd83140ceL
                ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L)));
        if (random.nextFloat() >= 0.06F) return null;
        int species = random.nextInt(17);
        return (species < 6 ? Blocks.GRASS : species < 16 ? Blocks.FERN : Blocks.LARGE_FERN).defaultBlockState();
    }

    public static int placeBerryPatchWithRetries(WorldGenLevel level, BerryPatch patch) {
        var species = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(patch.species()));
        int ownerX = Math.floorDiv(patch.x(), 16) * 16, ownerZ = Math.floorDiv(patch.z(), 16) * 16;
        for (int x = ownerX; x < ownerX + 16; x++) for (int z = ownerZ; z < ownerZ + 16; z++) {
            BlockPos root = surfaceRoot(level, x, z);
            if (root != null && (level.getBlockState(root).is(species)
                    || level.getBlockEntity(root) instanceof SnowBlockEntity snow && snow.getContainedState().is(species))) return 0;
        }
        if (placeBerryPatch(level, patch) == BERRIES_PER_PATCH) return BERRIES_PER_PATCH;
        int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
        List<BerryPatch> alternatives = new ArrayList<>();
        for (int x = minX; x <= minX + 10; x++) for (int z = minZ; z <= minZ + 10; z++) {
            if (x != patch.x() || z != patch.z()) alternatives.add(new BerryPatch(x, z, patch.species(), patch.seed()));
        }
        alternatives.sort(java.util.Comparator.comparingLong(p -> TropicalLayout.mix(patch.seed()
                ^ p.x() * 0x632be59bd9b4e019L ^ p.z() * 0x9e3779b97f4a7c15L)));
        for (BerryPatch alternate : alternatives) {
            if (placeBerryPatch(level, alternate) == BERRIES_PER_PATCH) return BERRIES_PER_PATCH;
        }
        return 0;
    }

    public static int placeBerryPatch(WorldGenLevel level, BerryPatch patch) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(patch.species()));
        if (block == null || block == Blocks.AIR) return 0;
        BlockState berry = block.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3);
        RandomSource random = RandomSource.create(patch.seed());
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < PATCH_WIDTH * PATCH_WIDTH; i++) offsets.add(i);
        for (int i = offsets.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            int previous = offsets.get(i);
            offsets.set(i, offsets.get(other));
            offsets.set(other, previous);
        }
        List<BlockPos> positions = new ArrayList<>();
        for (int offset : offsets) {
            BlockPos pos = surfaceRoot(level, patch.x() + offset % PATCH_WIDTH, patch.z() + offset / PATCH_WIDTH);
            if (pos != null && GameplayDimensions.biomeIs(level, pos, "snowy_forest")
                    && (level.isEmptyBlock(pos) || level.getBlockState(pos).is(Blocks.SNOW))
                    && berry.canSurvive(level, pos)) {
                positions.add(pos);
                if (positions.size() == BERRIES_PER_PATCH) break;
            }
        }
        if (positions.size() != BERRIES_PER_PATCH) return 0;
        List<BlockState> originals = positions.stream().map(level::getBlockState).toList();
        List<BlockState> soils = positions.stream().map(pos -> level.getBlockState(pos.below())).toList();
        for (int i = 0; i < positions.size(); i++) {
            if (!SnowyPlantSnowlogging.place(level, positions.get(i), berry)) {
                for (int j = 0; j <= i; j++) {
                    level.setBlock(positions.get(j), originals.get(j), 18);
                    level.setBlock(positions.get(j).below(), soils.get(j), 18);
                }
                return 0;
            }
        }
        return BERRIES_PER_PATCH;
    }

    public static BlockPos surfaceRoot(WorldGenLevel level, int x, int z) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        for (int y = top.getY(); y >= Math.max(1, top.getY() - 64); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).is(BlockTags.DIRT)) return pos;
            BlockState below = level.getBlockState(pos.below());
            // Do not tunnel through lake water or rock looking for a cave floor.
            if (!below.getFluidState().isEmpty() || below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE)) return null;
        }
        return null;
    }
}
