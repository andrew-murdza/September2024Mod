package net.amurdza.examplemod.worldgen;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/** One adjacent chunk cluster per 288-square tile, plus independent Savanna mushroom patches. */
public final class CropTiles {
    public static final int TILE_SIZE = 288;
    public static final int SAVANNA_TILE_SIZE = 288;
    public static final int CELL_SIZE = 8;
    public static final int PLANTS_PER_PATCH = 5;
    public static final int PUMPKINS_PER_PATCH = 2;
    public static final int MUSHROOMS_PER_PATCH = 10;
    public static final String SAVANNA_CORN = "hearthandharvest:corn_stalk";
    public static final List<String> PLAINS_SPECIES = List.of("farmersdelight:wild_carrots",
            "farmersdelight:wild_cabbages", "minecraft:pumpkin");
    public static final List<String> SAVANNA_SPECIES = List.of("farmersdelight:wild_carrots",
            "farmersdelight:wild_cabbages", "minecraft:pumpkin", "minecraft:red_mushroom", "minecraft:brown_mushroom");
    private static final List<String> MUSHROOMS = List.of("minecraft:red_mushroom", "minecraft:brown_mushroom");
    private static final Map<RandomState, Map<Key, CompletableFuture<List<Patch>>>> PLANS = new WeakHashMap<>();

    private CropTiles() { }

    public static List<Patch> plan(WorldGenLevel level, int tileX, int tileZ, boolean savanna) {
        var source = level.getLevel().getChunkSource();
        RandomState state = source.randomState();
        Key key = new Key(level.getSeed(), tileX, tileZ, savanna);
        CompletableFuture<List<Patch>> future;
        boolean compute;
        synchronized (PLANS) {
            Map<Key, CompletableFuture<List<Patch>>> cache = PLANS.computeIfAbsent(state, ignored -> new LinkedHashMap<>(64, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, CompletableFuture<List<Patch>>> eldest) {
                    return size() > 256 && eldest.getValue().isDone();
                }
            });
            future = cache.get(key);
            compute = future == null;
            if (compute) {
                future = new CompletableFuture<>();
                cache.put(key, future);
            }
        }
        // Expensive density sampling must never hold the cache lock across tiles.
        if (compute) {
            try {
                var generator = source.getGenerator();
                CropTerrainHeights heights = generator instanceof NoiseBasedChunkGenerator noise
                        ? new CropTerrainHeights(level, noise, state) : null;
                SavannaFlowerBands bands = savanna ? new SavannaFlowerBands(key.seed()) : null;
                BiPredicate<Integer, Integer> suitable = (x, z) -> {
                    if (bands != null && bands.colour(x, z) >= 0) return false;
                    int height = heights == null ? generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state)
                            : heights.height(x, z);
                    // getBaseHeight is the first empty block; sea-level ground is dry.
                    return height >= generator.getSeaLevel();
                };
                future.complete(plan(key.seed(), tileX, tileZ, savanna ? SAVANNA_SPECIES : PLAINS_SPECIES, suitable));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
                synchronized (PLANS) { PLANS.get(state).remove(key, future); }
            }
        }
        return future.join();
    }

    public static List<Patch> forChunk(WorldGenLevel level, int chunkX, int chunkZ, boolean savanna) {
        return inChunk(chunkX, chunkZ, savanna ? SAVANNA_TILE_SIZE : TILE_SIZE,
                (tx, tz) -> plan(level, tx, tz, savanna)).stream()
                .filter(patch -> !StructurePlantExclusion.excludesPatch(level, patch.x(), patch.z())).toList();
    }

    public static List<Patch> forChunk(long seed, int chunkX, int chunkZ, List<String> species,
                                      BiPredicate<Integer, Integer> suitableColumn) {
        return inChunk(chunkX, chunkZ, species.equals(SAVANNA_SPECIES) || species.equals(PLAINS_SPECIES) ? TILE_SIZE : 144,
                (tx, tz) -> plan(seed, tx, tz, species, suitableColumn));
    }

    private static List<Patch> inChunk(int chunkX, int chunkZ, int tileSize, BiFunction<Integer, Integer, List<Patch>> tiles) {
        int minX = chunkX * 16, minZ = chunkZ * 16;
        List<Patch> patches = new ArrayList<>();
        for (int tx = Math.floorDiv(minX, tileSize); tx <= Math.floorDiv(minX + 15, tileSize); tx++) {
            for (int tz = Math.floorDiv(minZ, tileSize); tz <= Math.floorDiv(minZ + 15, tileSize); tz++) {
                for (Patch patch : tiles.apply(tx, tz)) {
                    if (Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.z(), 16) == chunkZ) patches.add(patch);
                }
            }
        }
        return List.copyOf(patches);
    }

    public static List<Patch> plan(long seed, int tileX, int tileZ, List<String> species,
                                   BiPredicate<Integer, Integer> suitableColumn) {
        boolean savanna = species.equals(SAVANNA_SPECIES);
        // Preserve the small generic layout for callers outside the authored
        // Plains/Savanna cluster palettes.
        if (!savanna && !species.equals(PLAINS_SPECIES)) return planTile(seed, tileX, tileZ, 144, species, suitableColumn);
        Map<Long, Boolean> suitability = new HashMap<>();
        BiPredicate<Integer, Integer> cached = (x, z) -> suitability.computeIfAbsent(
                net.minecraft.world.level.ChunkPos.asLong(x, z), ignored -> suitableColumn.test(x, z));
        List<Patch> patches = new ArrayList<>(planCluster(seed, tileX, tileZ, savanna, cached));
        if (savanna) {
            java.util.Set<Long> occupied = new java.util.HashSet<>();
            patches.forEach(p -> occupied.add(cellKey(p.x(), p.z())));
            patches.addAll(planTile(seed, tileX, tileZ, TILE_SIZE, MUSHROOMS,
                    (x, z) -> !occupied.contains(cellKey(x, z)) && cached.test(x, z)));
        }
        return List.copyOf(patches);
    }

    private static List<Patch> planCluster(long seed, int tileX, int tileZ, boolean savanna,
                                           BiPredicate<Integer, Integer> suitable) {
        int width = 3;
        List<String> palette = new ArrayList<>();
        for (String crop : savanna ? SAVANNA_SPECIES.subList(0, 3) : PLAINS_SPECIES) {
            for (int i = 0; i < width; i++) palette.add(crop);
        }
        return planAdjacentCluster(seed ^ (savanna ? 0x5a14fL : 0x781acL), tileX, tileZ,
                width, palette, PLANTS_PER_PATCH, suitable);
    }

    /** Fill a square, or choose two edge-adjacent cells of a 2x2 square, nearest the tile's center first. */
    public static List<Patch> planAdjacentCluster(long seed, int tileX, int tileZ, int width,
                                                 List<String> species, int target,
                                                 BiPredicate<Integer, Integer> suitable) {
        boolean pair = width == 2 && species.size() == 2;
        if (!pair && species.size() != width * width) throw new IllegalArgumentException("Incomplete crop cluster");
        int chunkX = tileX * (TILE_SIZE / 16), chunkZ = tileZ * (TILE_SIZE / 16);
        long tileSeed = hash(seed, tileX, tileZ);
        List<Cluster> candidates = new ArrayList<>();
        for (int x = 0; x <= TILE_SIZE / 16 - width; x++) for (int z = 0; z <= TILE_SIZE / 16 - width; z++) {
            for (int pattern = 0; pattern < (pair ? 4 : 1); pattern++) {
                List<Patch> chunks = new ArrayList<>();
                for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < width; dz++) {
                    if (pair && !(pattern == 0 && dx == 0 || pattern == 1 && dx == 1
                            || pattern == 2 && dz == 0 || pattern == 3 && dz == 1)) continue;
                    chunks.add(new Patch(chunkX + x + dx, chunkZ + z + dz, ""));
                }
                int central = (int) chunks.stream().filter(patch -> centralChunk(patch.x(), patch.z(), tileX, tileZ)).count();
                candidates.add(new Cluster(chunks, central, hash(tileSeed ^ pattern, chunkX + x, chunkZ + z)));
            }
        }
        candidates.sort(Comparator.comparingInt(Cluster::central).reversed().thenComparingLong(Cluster::order));
        List<String> palette = new ArrayList<>(species);
        RandomSource random = RandomSource.create(tileSeed);
        for (int i = palette.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String old = palette.get(i); palette.set(i, palette.get(j)); palette.set(j, old);
        }
        Map<Long, Patch> chunkSites = new HashMap<>();
        java.util.Set<Long> searched = new java.util.HashSet<>();
        Map<Long, Boolean> columns = new HashMap<>();
        BiPredicate<Integer, Integer> cached = (x, z) -> columns.computeIfAbsent(
                net.minecraft.world.level.ChunkPos.asLong(x, z), ignored -> suitable.test(x, z));
        for (Cluster candidate : candidates) {
            List<Patch> chosen = new ArrayList<>();
            for (Patch chunk : candidate.chunks()) {
                long key = net.minecraft.world.level.ChunkPos.asLong(chunk.x(), chunk.z());
                if (searched.add(key)) {
                    Patch site = findChunkSite(tileSeed, chunk.x(), chunk.z(), target, cached);
                    if (site != null) chunkSites.put(key, site);
                }
                Patch site = chunkSites.get(key);
                if (site == null) break;
                chosen.add(new Patch(site.x(), site.z(), palette.get(chosen.size())));
            }
            if (chosen.size() == palette.size()) return List.copyOf(chosen);
        }
        return List.of();
    }

    private static boolean centralChunk(int x, int z, int tileX, int tileZ) {
        return x * 16 >= tileX * TILE_SIZE + 72 && x * 16 + 15 < tileX * TILE_SIZE + 216
                && z * 16 >= tileZ * TILE_SIZE + 72 && z * 16 + 15 < tileZ * TILE_SIZE + 216;
    }

    public static boolean centralFootprint(Patch patch, int tileX, int tileZ) {
        return patch.x() - 2 >= tileX * TILE_SIZE + 72 && patch.x() + 3 < tileX * TILE_SIZE + 216
                && patch.z() - 2 >= tileZ * TILE_SIZE + 72 && patch.z() + 3 < tileZ * TILE_SIZE + 216;
    }

    private static Patch findChunkSite(long seed, int chunkX, int chunkZ, int target,
                                       BiPredicate<Integer, Integer> suitable) {
        List<Patch> candidates = new ArrayList<>();
        for (int dx : new int[]{0, 8}) for (int dz : new int[]{0, 8}) {
            candidates.addAll(centres(seed, new Patch(chunkX * 16 + dx + 2, chunkZ * 16 + dz + 2, "")));
        }
        candidates.sort(Comparator.comparingLong(patch -> hash(seed ^ 0x191eL, patch.x(), patch.z())));
        for (Patch candidate : candidates) if (hasSites(candidate, suitable, target)) return candidate;
        return null;
    }

    private record Cluster(List<Patch> chunks, int central, long order) { }

    private static long cellKey(int x, int z) {
        return net.minecraft.world.level.ChunkPos.asLong(Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE));
    }

    private static List<Patch> planTile(long seed, int tileX, int tileZ, int tileSize, List<String> species,
                                       BiPredicate<Integer, Integer> suitableColumn) {
        if (species.isEmpty()) return List.of();
        long tileSeed = hash(seed ^ 0x6b593cL, tileX, tileZ);
        List<Patch> candidates = new ArrayList<>();
        for (int x = tileX * tileSize; x < (tileX + 1) * tileSize; x += CELL_SIZE) {
            for (int z = tileZ * tileSize; z < (tileZ + 1) * tileSize; z += CELL_SIZE) {
                long key = hash(tileSeed, x, z);
                candidates.add(new Patch(x + 2 + (int) Long.remainderUnsigned(key, 3),
                        z + 2 + (int) Long.remainderUnsigned(TropicalLayout.mix(key), 3), ""));
            }
        }
        candidates.sort(Comparator.comparingLong(p -> hash(tileSeed ^ 0x47a896L, p.x(), p.z())));
        List<String> palette = new ArrayList<>(species);
        RandomSource random = RandomSource.create(tileSeed ^ 0x765cdL);
        for (int i = palette.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String swap = palette.get(i); palette.set(i, palette.get(j)); palette.set(j, swap);
        }
        Map<Long, Boolean> suitability = new HashMap<>();
        BiPredicate<Integer, Integer> cached = (x, z) -> suitability.computeIfAbsent(
                net.minecraft.world.level.ChunkPos.asLong(x, z), ignored -> suitableColumn.test(x, z));
        List<Patch> chosen = new ArrayList<>();
        // Exhaust every central cell, including its alternate anchors, before
        // moving a mushroom outside the middle 144 square because of water.
        for (int priority = 0; priority < (tileSize == TILE_SIZE ? 2 : 1); priority++) {
            List<Patch> rejected = new ArrayList<>();
            for (Patch candidate : candidates) {
                if (tileSize == TILE_SIZE && centralFootprint(candidate, tileX, tileZ) != (priority == 0)) continue;
                if (hasSites(candidate, cached, plantsPerPatch(palette.get(chosen.size())))) {
                    chosen.add(new Patch(candidate.x(), candidate.z(), palette.get(chosen.size())));
                    if (chosen.size() == palette.size()) return List.copyOf(chosen);
                } else rejected.add(candidate);
            }
            for (Patch candidate : rejected) {
                for (Patch alternative : centres(seed, candidate)) {
                    if (alternative.x() == candidate.x() && alternative.z() == candidate.z()) continue;
                    if (tileSize == TILE_SIZE && centralFootprint(alternative, tileX, tileZ) != (priority == 0)) continue;
                    if (!hasSites(alternative, cached, plantsPerPatch(palette.get(chosen.size())))) continue;
                    chosen.add(new Patch(alternative.x(), alternative.z(), palette.get(chosen.size())));
                    break;
                }
                if (chosen.size() == palette.size()) return List.copyOf(chosen);
            }
        }
        return List.copyOf(chosen);
    }

    private static boolean hasSites(Patch patch, BiPredicate<Integer, Integer> suitable, int target) {
        int count = 0;
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
            if (suitable.test(patch.x() + dx, patch.z() + dz) && ++count == target) return true;
        }
        return false;
    }

    private static List<Patch> centres(long seed, Patch patch) {
        int minX = Math.floorDiv(patch.x(), CELL_SIZE) * CELL_SIZE;
        int minZ = Math.floorDiv(patch.z(), CELL_SIZE) * CELL_SIZE;
        List<Patch> result = new ArrayList<>();
        for (int x = minX + 2; x <= minX + 4; x++) for (int z = minZ + 2; z <= minZ + 4; z++) {
            result.add(new Patch(x, z, patch.species()));
        }
        result.sort(Comparator.comparingLong(p -> hash(seed ^ 0xb3751L, p.x(), p.z())));
        if (result.remove(patch)) result.add(0, patch);
        return result;
    }

    public static int place(WorldGenLevel level, Patch patch) {
        return place(level, patch, List.of(patch));
    }

    /** Retry inside this crop's assigned chunk without consuming another patch's reserved cell. */
    public static int place(WorldGenLevel level, Patch patch, List<Patch> reservations) {
        ResourceLocation id = ResourceLocation.tryParse(patch.species());
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || block == Blocks.AIR || alreadyPlacedInChunk(level, patch, block)) return 0;
        int target = plantsPerPatch(patch.species());
        for (Patch centre : placementCentres(level.getSeed(), patch, reservations)) {
            RandomSource random = RandomSource.create(hash(level.getSeed() ^ patch.species().hashCode(), centre.x(), centre.z()));
            if (placeAt(level, centre, random) == target) return target;
        }
        return 0;
    }

    private static List<Patch> placementCentres(long seed, Patch patch, List<Patch> reservations) {
        List<Patch> result = new ArrayList<>(centres(seed, patch));
        java.util.Set<Long> reserved = new java.util.HashSet<>();
        for (Patch owner : reservations) if (!owner.equals(patch)) reserved.add(cellKey(owner.x(), owner.z()));
        int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
        long original = cellKey(patch.x(), patch.z());
        List<Patch> alternatives = new ArrayList<>();
        for (int dx : new int[]{0, 8}) for (int dz : new int[]{0, 8}) {
            int x = minX + dx + 2, z = minZ + dz + 2;
            if (cellKey(x, z) != original && !reserved.contains(cellKey(x, z))) {
                alternatives.addAll(centres(seed, new Patch(x, z, patch.species())));
            }
        }
        alternatives.sort(Comparator.comparingLong(p -> hash(seed ^ patch.species().hashCode(), p.x(), p.z())));
        result.addAll(alternatives);
        return result;
    }

    private static boolean alreadyPlacedInChunk(WorldGenLevel level, Patch patch, Block species) {
        int minX = Math.floorDiv(patch.x(), 16) * 16, minZ = Math.floorDiv(patch.z(), 16) * 16;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            // Pumpkins move this heightmap upward, while small mushrooms/crops do not.
            if (level.getBlockState(top).is(species) || level.getBlockState(top.below()).is(species)) return true;
        }
        return false;
    }

    public static int placeAt(WorldGenLevel level, Patch patch, RandomSource random) {
        if (StructurePlantExclusion.excludesPatch(level, patch.x(), patch.z())) return 0;
        ResourceLocation id = ResourceLocation.tryParse(patch.species());
        Block species = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        if (species == null || species == Blocks.AIR) return 0;
        SavannaFlowerBands bands = GameplayDimensions.is(level, GameplayDimensions.SAVANNA)
                ? new SavannaFlowerBands(level.getSeed()) : null;
        return tryCropPatch(level, patch, species, bands, random);
    }

    private static int tryCropPatch(WorldGenLevel level, Patch patch, Block species,
                                    SavannaFlowerBands bands, RandomSource random) {
        int target = plantsPerPatch(patch.species());
        int[] offsets = new int[36];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = offsets[i]; offsets[i] = offsets[j]; offsets[j] = swap;
        }
        List<PlantColumn> planted = new ArrayList<>();
        boolean complete = false;
        try {
            // Every column is considered once; rejected writes do not consume
            // the species target or stop the rest of this compact footprint.
            for (int offset : offsets) {
                int x = patch.x() - 2 + offset % 6;
                int z = patch.z() - 2 + offset / 6;
                if (bands != null && bands.colour(x, z) >= 0) continue;
                PlantColumn column = ground(level, x, z);
                if (column != null && replaceGrassWithCrop(level, column, patch.species(), species, random)) {
                    planted.add(column);
                    if (planted.size() == target) {
                        complete = true;
                        return target;
                    }
                }
            }
            return 0;
        } finally {
            // Reject the incomplete footprint without leaving singleton crops or
            // deleting the grass that occupied an incomplete attempt.
            if (!complete) for (PlantColumn column : planted) column.restore(level);
        }
    }

    private static boolean replaceGrassWithCrop(WorldGenLevel level, PlantColumn column, String name,
                                                Block species, RandomSource random) {
        boolean accepted = false;
        try {
            // Clearing grass stays private until the complete replacement is
            // valid. A single-height crop also removes the old upper grass half.
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            buffer.setBlock(column.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (column.upper().is(Blocks.TALL_GRASS)) {
                buffer.setBlock(column.pos().above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            if (!DocumentedFloraFeature.placePlant(buffer, column.pos(), name, random)) return false;
            BlockState root = buffer.getBlockState(column.pos()), upper = buffer.getBlockState(column.pos().above());
            if (!root.is(species)) return false;
            if (root.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                if (root.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER
                        || !upper.is(species) || !upper.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        || upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER) return false;
            } else if (species instanceof CornStalkBlock) {
                BlockState top = buffer.getBlockState(column.pos().above(2));
                if (root.getValue(CornStalkBlock.SECTION) != CornStalkBlock.CornSection.BOTTOM
                        || !upper.is(species) || upper.getValue(CornStalkBlock.SECTION) != CornStalkBlock.CornSection.MIDDLE
                        || !top.is(species) || top.getValue(CornStalkBlock.SECTION) != CornStalkBlock.CornSection.TOP) return false;
            } else if (!upper.isAir()) return false;
            if (!buffer.isValid((pos, state) -> pos.equals(column.pos()) || pos.equals(column.pos().above())
                    || species instanceof CornStalkBlock && pos.equals(column.pos().above(2)))
                    || !buffer.commit()) return false;
            // Some placement interceptors report success without accepting the
            // requested block. Count only the verified, complete live result.
            accepted = level.getBlockState(column.pos()).equals(root)
                    && level.getBlockState(column.pos().above()).equals(upper)
                    && (!(species instanceof CornStalkBlock)
                    || level.getBlockState(column.pos().above(2)).equals(buffer.getBlockState(column.pos().above(2))));
            return accepted;
        } finally {
            if (!accepted) column.restore(level);
        }
    }

    private static PlantColumn ground(WorldGenLevel level, int x, int z) {
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        BlockState root = level.getBlockState(pos), upper = level.getBlockState(pos.above());
        boolean tall = root.is(Blocks.TALL_GRASS)
                && root.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                && upper.is(Blocks.TALL_GRASS)
                && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        if ((!tall && (!(root.isAir() || root.is(Blocks.GRASS)) || !upper.isAir()))
                || !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                || !(GameplayDimensions.biomeIs(level, pos, "savanna") || GameplayDimensions.biomeIs(level, pos, "plains"))) return null;
        return new PlantColumn(pos, root, upper, level.getBlockState(pos.above(2)));
    }

    private record PlantColumn(BlockPos pos, BlockState root, BlockState upper, BlockState third) {
        private void restore(WorldGenLevel level) {
            restoreState(level, pos, root);
            restoreState(level, pos.above(), upper);
            restoreState(level, pos.above(2), third);
        }
        private static void restoreState(WorldGenLevel level, BlockPos pos, BlockState original) {
            if (level.getBlockState(pos).equals(original)) return;
            level.setBlock(pos, original, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (!level.getBlockState(pos).equals(original)) {
                // Restoring an original state must survive an interceptor that
                // rejects new placement, as in GrowthPlacementBuffer rollback.
                level.getChunk(pos).setBlockState(pos, original, false);
                level.getLightEngine().checkBlock(pos);
            }
        }
    }

    private static long hash(long seed, int x, int z) {
        return TropicalLayout.mix(seed ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L)
                ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L));
    }

    public static int plantsPerPatch(String species) {
        return MUSHROOMS.contains(species) ? MUSHROOMS_PER_PATCH
                : species.equals("minecraft:pumpkin") ? PUMPKINS_PER_PATCH : PLANTS_PER_PATCH;
    }

    public record Patch(int x, int z, String species) { }
    private record Key(long seed, int tileX, int tileZ, boolean savanna) { }
}
