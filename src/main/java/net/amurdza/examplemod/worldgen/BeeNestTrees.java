package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.ToIntBiFunction;

/** One reserved tree site per 288-square land tile; entirely wet tiles remain untouched. */
public final class BeeNestTrees {
    public static final int TILE_SIZE = CropTiles.TILE_SIZE;
    private static final int CHUNKS_PER_TILE = TILE_SIZE / 16;
    private static final int CHUNK_COUNT = CHUNKS_PER_TILE * CHUNKS_PER_TILE;
    private static final long SALT = 0x4F1BBCDCBFA54001L;
    private static final Map<RandomState, Map<Key, Optional<BlockPos>>> SITES = new WeakHashMap<>();

    private BeeNestTrees() { }

    public static boolean applies(WorldGenLevel level) {
        return GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                || GameplayDimensions.is(level, GameplayDimensions.SAVANNA);
    }

    public static boolean ownsChunk(WorldGenLevel level, int chunkX, int chunkZ) {
        if (!applies(level)) return false;
        BlockPos site = site(level, Math.floorDiv(chunkX, CHUNKS_PER_TILE), Math.floorDiv(chunkZ, CHUNKS_PER_TILE));
        return site != null && Math.floorDiv(site.getX(), 16) == chunkX && Math.floorDiv(site.getZ(), 16) == chunkZ;
    }

    public static BlockPos site(WorldGenLevel level, int tileX, int tileZ) {
        var source = level.getLevel().getChunkSource();
        RandomState state = source.randomState();
        Key key = new Key(level.getSeed(), tileX, tileZ);
        synchronized (SITES) {
            var cache = SITES.get(state);
            if (cache != null && cache.containsKey(key)) return cache.get(key).orElse(null);
        }
        var generator = source.getGenerator();
        var heights = generator instanceof NoiseBasedChunkGenerator noise ? new CropTerrainHeights(level, noise, state) : null;
        BlockPos result = plan(level.getSeed(), tileX, tileZ, generator.getSeaLevel(), (x, z) -> heights == null
                ? generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state) : heights.height(x, z));
        synchronized (SITES) {
            var cache = SITES.computeIfAbsent(state, ignored -> new LinkedHashMap<>(64, .75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Optional<BlockPos>> eldest) {
                    return size() > 256;
                }
            });
            cache.putIfAbsent(key, Optional.ofNullable(result));
            return cache.get(key).orElse(null);
        }
    }

    public static BlockPos plan(long seed, int tileX, int tileZ, int seaLevel, ToIntBiFunction<Integer, Integer> height) {
        long hash = TropicalLayout.mix(seed ^ SALT ^ tileX * 341873128712L ^ tileZ * 132897987541L);
        int start = Math.floorMod(hash, CHUNK_COUNT);
        BlockPos fallback = null;
        for (int i = 0; i < CHUNK_COUNT; i++) {
            int cell = (start + i * 97) % CHUNK_COUNT;
            int x = tileX * TILE_SIZE + cell % CHUNKS_PER_TILE * 16 + 8;
            int z = tileZ * TILE_SIZE + cell / CHUNKS_PER_TILE * 16 + 8;
            int y = height.applyAsInt(x, z);
            if (y < seaLevel) continue;
            if (fallback == null) fallback = new BlockPos(x, y, z);
            boolean flat = true;
            for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-3, 3}) {
                int edge = height.applyAsInt(x + dx, z + dz);
                if (edge < seaLevel || Math.abs(edge - y) > 2) flat = false;
            }
            if (flat) return new BlockPos(x, y, z);
        }
        return fallback;
    }

    public static boolean place(FeaturePlaceContext<?> context) {
        WorldGenLevel level = context.level();
        int cx = Math.floorDiv(context.origin().getX(), 16), cz = Math.floorDiv(context.origin().getZ(), 16);
        if (!ownsChunk(level, cx, cz)) return false;
        return placeAt(context, site(level, Math.floorDiv(cx, CHUNKS_PER_TILE), Math.floorDiv(cz, CHUNKS_PER_TILE)));
    }

    public static boolean placeAt(FeaturePlaceContext<?> context, BlockPos planned) {
        WorldGenLevel level = context.level();
        int cx = Math.floorDiv(context.origin().getX(), 16), cz = Math.floorDiv(context.origin().getZ(), 16);
        if (!applies(level) || planned == null || Math.floorDiv(planned.getX(), 16) != cx
                || Math.floorDiv(planned.getZ(), 16) != cz) return false;
        boolean savanna = GameplayDimensions.is(level, GameplayDimensions.SAVANNA);
        var tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:trees/" + (savanna ? "acacia_bees" : "birch_bees")));
        if (tree == null) return false;
        // Re-entry must not add a second tree/nest. Search only this reserved chunk.
        for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
            for (int y = planned.getY() - 3; y <= planned.getY() + 16; y++) {
                if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.BEE_NEST)) return true;
            }
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ SALT ^ cx * 341873128712L ^ cz * 132897987541L);
        for (int attempt = 0; attempt < 128; attempt++) {
            int cell = (36 + attempt * 37) % 64;
            int x = cx * 16 + 4 + cell % 8, z = cz * 16 + 4 + cell / 8;
            BlockPos root = ground(level, x, z, planned.getY());
            if (root == null) continue;
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            if (!tree.place(buffer, context.chunkGenerator(), random, root)) continue;
            boolean[] nest = {false};
            boolean valid = buffer.isValid((pos, state) -> {
                if (state.is(Blocks.BEE_NEST)) {
                    if (Math.floorDiv(pos.getX(), 16) != cx || Math.floorDiv(pos.getZ(), 16) != cz) return false;
                    nest[0] = buffer.getBlockEntity(pos) instanceof BeehiveBlockEntity hive && !hive.isEmpty();
                }
                return BiomePlacementRestrictions.isAllowed(buffer, pos, state)
                        && TreeGrowthCollisionRules.isAllowed(level, pos, state);
            });
            if (valid && nest[0] && buffer.commit()) return true;
        }
        return false;
    }

    private static BlockPos ground(WorldGenLevel level, int x, int z, int expectedY) {
        // The reserved tile site is planned from noise before structures exist.
        // A newly raised camp must use its real floor, just like ordinary acacias.
        if (GameplayDimensions.is(level, GameplayDimensions.SAVANNA)
                && StructurePlantExclusion.excludes(level, new BlockPos(x, expectedY, z))) {
            expectedY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        for (int y = expectedY + 4; y >= expectedY - 4; y--) {
            BlockPos root = new BlockPos(x, y, z);
            if (level.getBlockState(root.below()).is(BlockTags.DIRT)
                    && level.getFluidState(root).isEmpty() && level.getBlockState(root).canBeReplaced()) return root;
        }
        return null;
    }

    private record Key(long seed, int x, int z) { }
}
