package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/** One complete five-cane grove, at most nine blocks across, per 120-square basalt tile. */
public final class PowderCanePatches {
    public static final int TILE_SIZE = 120;
    public static final int PATCH_RADIUS = 4;
    public static final int PLANTS_PER_PATCH = 5;
    private static final String SPECIES = "mynethersdelight:powdery_cane";
    private static final Map<RandomState, Map<Tile, CompletableFuture<List<CropTiles.Patch>>>> PLANS = new WeakHashMap<>();

    private PowderCanePatches() { }

    public static List<CropTiles.Patch> plan(long seed, int tileX, int tileZ, BiPredicate<Integer, Integer> suitable) {
        List<CropTiles.Patch> candidates = centres(tileX, tileZ);
        candidates.sort(Comparator.comparingLong(patch -> patchSeed(seed, patch)));
        for (CropTiles.Patch candidate : candidates) {
            int sites = 0;
            for (int dx = -PATCH_RADIUS; dx <= PATCH_RADIUS; dx++) {
                for (int dz = -PATCH_RADIUS; dz <= PATCH_RADIUS; dz++) {
                    if (suitable.test(candidate.x() + dx, candidate.z() + dz) && ++sites == PLANTS_PER_PATCH) {
                        return List.of(candidate);
                    }
                }
            }
        }
        return List.of();
    }

    public static List<CropTiles.Patch> plan(WorldGenLevel level, int tileX, int tileZ) {
        var source = level.getLevel().getChunkSource();
        RandomState state = source.randomState();
        Tile tile = new Tile(level.getSeed(), tileX, tileZ);
        CompletableFuture<List<CropTiles.Patch>> future;
        boolean compute;
        synchronized (PLANS) {
            var cache = PLANS.computeIfAbsent(state, ignored -> new LinkedHashMap<>(64, .75f, true));
            future = cache.get(tile);
            compute = future == null;
            if (compute) {
                future = new CompletableFuture<>();
                cache.put(tile, future);
            }
        }
        if (compute) {
            try {
                var generator = source.getGenerator();
                DimensionTerrain terrain = NetherRampDecorationGuard.terrain(state).orElse(null);
                Map<Long, Boolean> columns = new HashMap<>();
                future.complete(plan(tile.seed(), tileX, tileZ, (x, z) -> columns.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> {
                    if (terrain != null && DimensionTerrain.netherRampDistance(z,
                            terrain.rampDetail().compute(new DensityFunction.SinglePointContext(x, 0, z))) < 8) return false;
                    // Actual noise interpolation, including lava, without loading or
                    // waiting for neighboring chunks during parallel decoration.
                    return hasDryClearance(generator.getBaseColumn(x, z, level, state));
                })));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                synchronized (PLANS) { PLANS.get(state).remove(tile, future); }
            } finally {
                synchronized (PLANS) {
                    var cache = PLANS.get(state);
                    var entries = cache.entrySet().iterator();
                    while (cache.size() > 256 && entries.hasNext()) if (entries.next().getValue().isDone()) entries.remove();
                }
            }
        }
        return future.join();
    }

    public static boolean hasDryClearance(NoiseColumn column) {
        for (int y = 2; y < 25; y++) {
            BlockState below = column.getBlock(y - 1);
            if (below.isAir() || !below.getFluidState().isEmpty()) continue;
            int clear = 0;
            // Mature trees adapt their height to available space, down to seven blocks.
            while (clear < 7 && column.getBlock(y + clear).isAir()) clear++;
            if (clear == 7) return true;
        }
        return false;
    }

    /** Ore/blob features write directly into chunk sections, so protect the actual rooting surface there. */
    public static boolean allowsOre(WorldGenLevel level, BlockPos pos, BlockState existing, BlockState replacement) {
        if (!(level instanceof WorldGenRegion) || pos.getY() < 1 || pos.getY() >= 24
                || !(existing.is(Blocks.BASALT) || existing.is(Blocks.BLACKSTONE))
                || replacement.is(Blocks.BASALT) || replacement.is(Blocks.BLACKSTONE)
                || !GameplayDimensions.is(level, GameplayDimensions.NETHER)) return true;
        BlockState above = level.getBlockState(pos.above());
        if (!above.isAir() && !PowderCaneShape.isPlant(above)) return true;
        return !reservedFootprint(level, pos);
    }

    /** A hanging cluster must leave the reserved grove's air available before flora decoration runs. */
    public static boolean allowsGlowstone(WorldGenLevel level, BlockPos pos, BlockState replacement) {
        if (!(level instanceof WorldGenRegion) || pos.getY() < 2 || pos.getY() > 35
                || !replacement.is(Blocks.GLOWSTONE)
                || !GameplayDimensions.is(level, GameplayDimensions.NETHER)) return true;
        return !reservedFootprint(level, pos);
    }

    private static boolean reservedFootprint(WorldGenLevel level, BlockPos pos) {
        // Plans contain only one nine-square footprint per tile and are cached per seeded world.
        // Only a low exposed ore surface or a low hanging glowstone block reaches this lookup.
        for (CropTiles.Patch patch : plan(level, Math.floorDiv(pos.getX(), TILE_SIZE), Math.floorDiv(pos.getZ(), TILE_SIZE))) {
            if (Math.abs(pos.getX() - patch.x()) <= PATCH_RADIUS
                    && Math.abs(pos.getZ() - patch.z()) <= PATCH_RADIUS) return true;
        }
        return false;
    }

    public static boolean placeChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)) return false;
        boolean placed = false;
        for (int tx = Math.floorDiv(chunkX * 16, TILE_SIZE); tx <= Math.floorDiv(chunkX * 16 + 15, TILE_SIZE); tx++) {
            for (int tz = Math.floorDiv(chunkZ * 16, TILE_SIZE); tz <= Math.floorDiv(chunkZ * 16 + 15, TILE_SIZE); tz++) {
                for (CropTiles.Patch patch : plan(level, tx, tz)) {
                    if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) {
                        placed |= placePatch(level, patch) == PLANTS_PER_PATCH;
                    }
                }
            }
        }
        return placed;
    }

    public static int placePatch(WorldGenLevel level, CropTiles.Patch patch) {
        int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
        List<BlockPos> floors = new ArrayList<>();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = 2; y < 25; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (PowderCaneShape.isStem(level.getBlockState(pos))) return 0;
                BlockState ground = level.getBlockState(pos.below());
                if (!(ground.is(Blocks.BASALT) || ground.is(Blocks.BLACKSTONE))
                        || !GameplayDimensions.biomeIs(level, pos, "basalt_deltas")) continue;
                int clear = 0;
                while (clear < 12 && level.isEmptyBlock(pos.above(clear))) clear++;
                if (clear >= 7 && NetherRampDecorationGuard.allows(level, pos,
                        com.soytutta.mynethersdelight.common.registry.MNDBlocks.POWDERY_CANE.get().defaultBlockState())) {
                    floors.add(pos);
                    break;
                }
            }
        }
        if (floors.size() < PLANTS_PER_PATCH) return 0;
        List<CropTiles.Patch> candidates = new ArrayList<>();
        candidates.add(patch);
        for (CropTiles.Patch centre : centres(Math.floorDiv(patch.x(), TILE_SIZE), Math.floorDiv(patch.z(), TILE_SIZE))) {
            if (Math.floorDiv(centre.x(), 16) == Math.floorDiv(patch.x(), 16)
                    && Math.floorDiv(centre.z(), 16) == Math.floorDiv(patch.z(), 16) && !centre.equals(patch)) candidates.add(centre);
        }
        RandomSource random = RandomSource.create(patchSeed(level.getSeed(), patch));
        for (CropTiles.Patch centre : candidates) {
            List<BlockPos> sites = new ArrayList<>();
            for (BlockPos pos : floors) {
                if (Math.abs(pos.getX() - centre.x()) <= PATCH_RADIUS
                        && Math.abs(pos.getZ() - centre.z()) <= PATCH_RADIUS) sites.add(pos);
            }
            if (sites.size() < PLANTS_PER_PATCH) continue;
            for (int i = sites.size() - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                BlockPos previous = sites.set(i, sites.get(other));
                sites.set(other, previous);
            }
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            int planted = 0;
            for (BlockPos pos : sites) {
                if (PowderCaneShape.placeTree(buffer, pos, random.nextBoolean(), random) && ++planted == PLANTS_PER_PATCH) {
                    if (buffer.commit()) return planted;
                    break;
                }
            }
        }
        return 0;
    }

    private static List<CropTiles.Patch> centres(int tileX, int tileZ) {
        List<CropTiles.Patch> centres = new ArrayList<>();
        for (int x = tileX * TILE_SIZE + PATCH_RADIUS; x < (tileX + 1) * TILE_SIZE - PATCH_RADIUS; x++) {
            if (Math.floorMod(x, 16) < PATCH_RADIUS || Math.floorMod(x, 16) >= 16 - PATCH_RADIUS) continue;
            for (int z = tileZ * TILE_SIZE + PATCH_RADIUS; z < (tileZ + 1) * TILE_SIZE - PATCH_RADIUS; z++) {
                if (Math.floorMod(z, 16) >= PATCH_RADIUS && Math.floorMod(z, 16) < 16 - PATCH_RADIUS) {
                    centres.add(new CropTiles.Patch(x, z, SPECIES));
                }
            }
        }
        return centres;
    }

    private static long patchSeed(long seed, CropTiles.Patch patch) {
        return TropicalLayout.mix(seed ^ 0x506f776465724361L ^ patch.x() * 341873128712L ^ patch.z() * 132897987541L);
    }

    private record Tile(long seed, int x, int z) { }
}
