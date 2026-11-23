package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Compact shore patches; desert roots keep their original water and form one connected cluster. */
public final class ShoreSugarCane {
    public static final int PATCH_WIDTH = 6;
    public static final int ROOTS_PER_PATCH = 5;
    public static final int STALK_HEIGHT = 3;

    private ShoreSugarCane() { }

    public static boolean hasDryShoreGeneration(WorldGenLevel level) {
        return GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                || GameplayDimensions.is(level, GameplayDimensions.SAVANNA)
                || GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                || GameplayDimensions.is(level, GameplayDimensions.DESERT);
    }

    public static boolean isHabitat(WorldGenLevel level, BlockPos pos) {
        if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return GameplayDimensions.biomeIs(level, pos, "plains");
        if (GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return GameplayDimensions.biomeIs(level, pos, "savanna");
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return GameplayDimensions.biomeIs(level, pos, "dappled_forest")
                || GameplayDimensions.biomeIs(level, pos, "cold_ocean")
                || GameplayDimensions.biomeIs(level, pos, "mountain_lake");
        return GameplayDimensions.is(level, GameplayDimensions.DESERT) && pos.getY() >= 0
                && (GameplayDimensions.biomeIs(level, pos, "desert") || GameplayDimensions.biomeIs(level, pos, "mesa"));
    }

    /** Vanilla placed-feature frequency and uniform chunk origins, with the existing configured patch. */
    public static int placeChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!hasDryShoreGeneration(level)) return 0;
        RandomSource random = chunkRandom(level.getSeed(), chunkX, chunkZ);
        // Sampling the origin before the biome-dependent rarity retains the same
        // independent uniform distributions as vanilla's rarity/in-square chain.
        int x = chunkX * 16 + random.nextInt(16), z = chunkZ * 16 + random.nextInt(16);
        BlockPos origin = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        String biome = level.getBiome(origin).unwrapKey().map(key -> key.location().getPath()).orElse("");
        int rarity = vanillaRarity(biome);
        if (rarity == 0 || random.nextInt(rarity) != 0 || !isHabitat(level, origin)) return 0;
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:sugar_cane_patch"));
        return feature != null && feature.place(level, level.getLevel().getChunkSource().getGenerator(), random, origin)
                ? ROOTS_PER_PATCH : 0;
    }

    /** Ordinary cane uses vanilla rarity. Desert and mesa receive half the vanilla attempts. */
    public static int vanillaRarity(String biome) {
        return switch (biome) {
            case "plains", "savanna", "dappled_forest", "cold_ocean", "mountain_lake" -> 6;
            case "desert" -> 2;
            case "mesa" -> 10;
            default -> 0;
        };
    }

    public static RandomSource chunkRandom(long seed, int chunkX, int chunkZ) {
        return RandomSource.create(TropicalLayout.mix(seed ^ 0x4b78e2bdc619L
                ^ TropicalLayout.mix(chunkX * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(chunkZ * 0x9e3779b97f4a7c15L)));
    }

    /** The configured feature keeps the same compact five-root, three-high patch. */
    public static int placeConfiguredPatch(WorldGenLevel level, BlockPos origin) {
        return hasDryShoreGeneration(level) ? placePatch(level, origin.getX() - 2, origin.getZ() - 2, PATCH_WIDTH) : 0;
    }

    /** Direct patch helper used by placement checks; natural generation uses placeChunk's vanilla distribution. */
    public static int placePatchInChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!hasDryShoreGeneration(level)) return 0;
        int minX = chunkX * 16, minZ = chunkZ * 16;
        return placePatch(level, minX, minZ, 16);
    }

    private static int placePatch(WorldGenLevel level, int minX, int minZ, int width) {
        BlockPos[] sites = new BlockPos[width * width];
        int viable = 0;
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < width; dz++) {
            int x = minX + dx, z = minZ + dz;
            BlockPos pos = new BlockPos(x, level.getHeight(GameplayDimensions.is(level, GameplayDimensions.DESERT)
                    ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            // A repeated decoration pass must not add another cluster to this reservation.
            if (level.getBlockState(pos).is(Blocks.SUGAR_CANE) || level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)) return 0;
            if (canPlaceStalk(level, pos)) { sites[dx + width * dz] = pos; viable++; }
        }
        if (viable < ROOTS_PER_PATCH) return 0;
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ 0x4b78e2bdc619L
                ^ TropicalLayout.mix(minX * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(minZ * 0x9e3779b97f4a7c15L)));
        int origins = width - PATCH_WIDTH + 1;
        for (int origin : shuffled(origins * origins, random)) {
            int dx = origin % origins, dz = origin / origins;
            List<BlockPos> roots = new ArrayList<>();
            for (int offset : shuffled(PATCH_WIDTH * PATCH_WIDTH, random)) {
                BlockPos pos = sites[dx + offset % PATCH_WIDTH + width * (dz + offset / PATCH_WIDTH)];
                if (pos == null) continue;
                if (GameplayDimensions.is(level, GameplayDimensions.DESERT)) {
                    roots = connectedRoots(sites, width, dx, dz, pos);
                    if (roots.size() == ROOTS_PER_PATCH) break;
                } else {
                    roots.add(pos);
                    if (roots.size() == ROOTS_PER_PATCH) break;
                }
            }
            if (roots.size() != ROOTS_PER_PATCH) continue;
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            boolean valid = true;
            for (BlockPos root : roots) valid &= stageStalk(buffer, root);
            if (valid && commit(buffer)) return ROOTS_PER_PATCH;
        }
        return 0;
    }

    private static List<BlockPos> connectedRoots(BlockPos[] sites, int width, int dx, int dz, BlockPos start) {
        List<BlockPos> roots = new ArrayList<>();
        roots.add(start);
        for (int index = 0; index < roots.size() && roots.size() < ROOTS_PER_PATCH; index++) {
            BlockPos current = roots.get(index);
            for (int x = dx; x < dx + PATCH_WIDTH; x++) for (int z = dz; z < dz + PATCH_WIDTH; z++) {
                BlockPos candidate = sites[x + width * z];
                if (candidate != null && !roots.contains(candidate)
                        && Math.abs(candidate.getX() - current.getX()) + Math.abs(candidate.getZ() - current.getZ()) == 1
                        && candidate.getY() == current.getY()) {
                    roots.add(candidate);
                    if (roots.size() == ROOTS_PER_PATCH) return roots;
                }
            }
        }
        return roots;
    }

    /** Generation-only single-column entry point; block-item placement is deliberately unchanged. */
    public static boolean placeStalk(WorldGenLevel level, BlockPos pos) {
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        return stageStalk(buffer, pos) && commit(buffer);
    }

    private static boolean stageStalk(GrowthPlacementBuffer buffer, BlockPos pos) {
        if (!canPlaceStalk(buffer, pos)) return false;
        BlockState cane = Blocks.SUGAR_CANE.defaultBlockState();
        for (int y = 0; y < STALK_HEIGHT; y++) buffer.setBlock(pos.above(y),
                AquaticPlantPlacement.copyWater(buffer, pos.above(y), cane), Block.UPDATE_CLIENTS);
        return true;
    }

    private static boolean canPlaceStalk(WorldGenLevel level, BlockPos pos) {
        if (!isHabitat(level, pos) || level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)) return false;
        // Camps author their crop-pond cane in the template. Random shore
        // clusters must never add cane to the livestock ponds or their banks.
        if (GameplayDimensions.is(level, GameplayDimensions.SAVANNA) && StructurePlantExclusion.excludes(level, pos)) return false;
        boolean shore = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shore |= level.getFluidState(pos.below().relative(direction)).is(FluidTags.WATER);
        }
        boolean submerged = GameplayDimensions.is(level, GameplayDimensions.DESERT);
        if (submerged) {
            if (!level.getBlockState(pos).is(Blocks.WATER) || !level.getFluidState(pos).isSource()) return false;
            if (!touchesDryShore(level, pos)) return false;
        } else if (!shore) return false;
        // Plains has already assembled complete four-to-six-root tall-grass
        // patches. A later shore pass must not consume part of one cluster.
        boolean preserveTallGrass = GameplayDimensions.is(level, GameplayDimensions.PLAINS);
        for (int y = 0; y < STALK_HEIGHT; y++) {
            BlockPos target = pos.above(y);
            BlockState existing = level.getBlockState(target);
            if (!isHabitat(level, target)) return false;
            if (submerged && y == 0) continue;
            if (!level.getFluidState(target).isEmpty() || !replaceableGrass(existing)
                    || preserveTallGrass && existing.is(Blocks.TALL_GRASS)) return false;
        }
        return AquaticPlantPlacement.copyWater(level, pos, Blocks.SUGAR_CANE.defaultBlockState()).canSurvive(level, pos);
    }

    /** Every submerged desert root touches a dry bank, including diagonally. */
    public static boolean touchesDryShore(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos shore = pos.offset(dx, 0, dz);
            if (level.getFluidState(shore).isEmpty()
                    && level.getBlockState(shore).isFaceSturdy(level, shore, Direction.UP)
                    && level.getFluidState(shore.above()).isEmpty()) return true;
        }
        return false;
    }

    private static boolean replaceableGrass(BlockState state) {
        return state.isAir() || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN);
    }

    private static boolean commit(GrowthPlacementBuffer buffer) {
        return buffer.isValid((pos, state) -> DocumentedPlacementRules.isGenerationBiomeAllowed(buffer, pos, state, false)
                && state.canSurvive(buffer, pos)) && buffer.commit();
    }

    private static int[] shuffled(int size, RandomSource random) {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = i;
        for (int i = size - 1; i > 0; i--) {
            int other = random.nextInt(i + 1), value = result[i]; result[i] = result[other]; result[other] = value;
        }
        return result;
    }
}
