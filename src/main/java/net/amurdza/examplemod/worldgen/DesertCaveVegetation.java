package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/** One red and one brown patch in each seeded surface descent, above the deepslate transition. */
public final class DesertCaveVegetation {
    public static final int MIN_FLOOR_Y = 16;
    public static final int MUSHROOMS_PER_PATCH = 5;
    private static final int RED_DISTANCE = 32;
    private static final int BROWN_DISTANCE = 48;

    private DesertCaveVegetation() { }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT)) return false;
        DimensionTerrain terrain = NetherRampDecorationGuard.terrain(level.getLevel().getChunkSource().randomState()).orElse(null);
        if (terrain == null) return false;
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        boolean placed = false;
        for (TunnelPatch patch : patchesForChunk(terrain, chunkX, chunkZ))
            placed |= placeTunnelPatch(level, patch) > 0;
        return placed;
    }

    /** Resolve only routes whose fixed patch anchors belong to this decorating chunk. */
    public static List<TunnelPatch> patchesForChunk(DimensionTerrain terrain, int chunkX, int chunkZ) {
        List<TunnelPatch> patches = new ArrayList<>();
        int minX = chunkX * 16, minZ = chunkZ * 16;
        for (boolean red : new boolean[]{true, false}) {
            int distance = red ? RED_DISTANCE : BROWN_DISTANCE;
            // Arc length bounds either horizontal displacement, including the
            // one-block clamp that keeps the patch inside its decorating chunk.
            double reachX = 32 + DesertCaveLayout.ENTRANCE_START + distance + 2;
            double reachZ = 96 + 18 + 24;
            int firstCycle = (int) Math.ceil((minX - reachX - 308) / 616.0);
            int lastCycle = (int) Math.floor((minX + 16 + reachX - 308) / 616.0);
            int firstStripe = 2 * Mth.floor((minZ - reachZ) / DesertBands.PERIOD) - 1;
            int lastStripe = 2 * Mth.floor((minZ + 16 + reachZ) / DesertBands.PERIOD) + 2;
            for (int cycle = firstCycle; cycle <= lastCycle; cycle++) for (int stripe = firstStripe; stripe <= lastStripe; stripe++) {
                TunnelPatch patch = patch(terrain, cycle, stripe, red);
                if (patch.owner().x == chunkX && patch.owner().z == chunkZ) patches.add(patch);
            }
        }
        return patches;
    }

    public static TunnelPatch patch(DimensionTerrain terrain, int cycle, int stripe, boolean red) {
        var outlet = terrain.desertOutlet(cycle, stripe);
        int distance = red ? RED_DISTANCE : BROWN_DISTANCE;
        int x = Mth.floor(outlet.xAt(distance)), z = Mth.floor(outlet.zAt(distance));
        int minX = Math.floorDiv(x, 16) * 16, minZ = Math.floorDiv(z, 16) * 16;
        // A 3x3 patch fits even when the ten-block tunnel straddles a chunk
        // boundary. Every read/write stays with one owner; no neighboring
        // feature chunk or generation-order marker is needed.
        x = Mth.clamp(x, minX + 1, minX + 14);
        z = Mth.clamp(z, minZ + 1, minZ + 14);
        int y = Mth.floor(outlet.floorAt(distance) + .5) + 1;
        return new TunnelPatch(cycle, stripe, red, new BlockPos(x, y, z));
    }

    public static int placeTunnelPatch(WorldGenLevel level, TunnelPatch patch) {
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT)) return 0;
        BlockState mushroom = (patch.red ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed()
                ^ patch.cycle * 341873128712L ^ patch.stripe * 132897987541L ^ (patch.red ? 0x524544L : 0x42524F574EL)));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < 9; i++) offsets.add(i);
        List<BlockPos> roots = new ArrayList<>();
        while (!offsets.isEmpty() && roots.size() < MUSHROOMS_PER_PATCH) {
            int offset = offsets.remove(random.nextInt(offsets.size()));
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                BlockPos pos = patch.anchor.offset(offset % 3 - 1, dy, offset / 3 - 1);
                if (isCaveFloor(level, pos, mushroom) && mushroom.canSurvive(level, pos)) {
                    roots.add(pos);
                    break;
                }
            }
        }
        if (roots.size() < MUSHROOMS_PER_PATCH) return 0;
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (BlockPos pos : roots) buffer.setBlock(pos, mushroom, 2);
        return buffer.commit() ? MUSHROOMS_PER_PATCH : 0;
    }

    public record TunnelPatch(int cycle, int stripe, boolean red, BlockPos anchor) {
        public ChunkPos owner() { return new ChunkPos(anchor); }
    }

    public static int placePatch(WorldGenLevel level, ChunkGenerator generator, RandomSource random,
                                 BlockPos anchor, boolean red) {
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT) || !isCaveFloor(level, anchor)) return 0;
        BlockState mushroom = (red ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos pos = anchor.offset(dx, dy, dz);
                if (isCaveFloor(level, pos) && mushroom.canSurvive(level, pos)) {
                    candidates.add(pos);
                    break;
                }
            }
        }
        if (candidates.size() < MUSHROOMS_PER_PATCH) return 0;
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (int i = 0; i < MUSHROOMS_PER_PATCH; i++) {
            buffer.setBlock(candidates.remove(random.nextInt(candidates.size())), mushroom, 2);
        }
        return buffer.commit() ? MUSHROOMS_PER_PATCH : 0;
    }

    public static boolean isCaveFloor(WorldGenLevel level, BlockPos pos) {
        return isCaveFloor(level, pos, Blocks.AIR.defaultBlockState());
    }

    private static boolean isCaveFloor(WorldGenLevel level, BlockPos pos, BlockState existingMushroom) {
        if (pos.getY() <= MIN_FLOOR_Y || !(level.isEmptyBlock(pos) || level.getBlockState(pos).equals(existingMushroom))
                || !(GameplayDimensions.biomeIs(level, pos, "desert") || GameplayDimensions.biomeIs(level, pos, "mesa")
                || GameplayDimensions.biomeIs(level, pos, "desert_lake"))) return false;
        BlockState support = level.getBlockState(pos.below());
        if (support.is(Blocks.DEEPSLATE) || support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS)
                || support.is(Blocks.MUSHROOM_STEM) || !support.isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
        if (pos.getY() >= surface - 1) return false;
        // World generation precedes lighting, and this pack lets mushrooms survive
        // daylight. A physical terrain ceiling is the reliable cave test.
        for (int y = pos.getY() + 1; y < surface; y++) {
            BlockState roof = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            if (roof.is(BlockTags.BASE_STONE_OVERWORLD) || roof.is(BlockTags.SAND)
                    || roof.is(BlockTags.TERRACOTTA) || roof.is(BlockTags.DIRT)
                    || roof.is(Blocks.SANDSTONE) || roof.is(Blocks.RED_SANDSTONE)
                    || roof.is(Blocks.GRAVEL) || roof.is(Blocks.CLAY)) return true;
        }
        return false;
    }
}
