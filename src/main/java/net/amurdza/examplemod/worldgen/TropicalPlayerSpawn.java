package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A persistent tropical world spawn, without generating terrain in the unused Overworld. */
public final class TropicalPlayerSpawn {
    private static final String DATA_NAME = "aoemod_tropical_player_spawn";
    private static final int SEARCH_RADIUS = 128;

    private TropicalPlayerSpawn() { }

    public static ServerLevel level(MinecraftServer server) {
        if (server.getWorldData().isDebugWorld()) return server.overworld();
        ServerLevel tropical = server.getLevel(GameplayDimensions.TROPICAL);
        return tropical != null ? tropical : server.overworld();
    }

    /** Ordinary spawn queries only read the saved point after its first initialization. */
    public static BlockPos position(ServerLevel tropical) {
        SpawnData data = data(tropical);
        if (data.initialAnchor != null) return data.initialAnchor;
        if (data.position == null) {
            var border = tropical.getWorldBorder();
            double centerX = border.getCenterX();
            double centerZ = border.getCenterZ();
            TropicalLayout layout = DimensionTerrain.tropicalLayout(
                    tropical.getChunkSource().randomState().router().temperature());
            TropicalLayout.Island island = layout.nearest(centerX, centerZ);
            int x = Mth.floor(Mth.clamp(island.x(), border.getMinX() + 1, border.getMaxX() - 1));
            int z = Mth.floor(Mth.clamp(island.z(), border.getMinZ() + 1, border.getMaxZ() - 1));
            // Decoration may ask for the world's spawn while the first search is generating
            // chunks. Give that nested query a temporary anchor, never a persisted unsafe point.
            data.initialAnchor = new BlockPos(x,
                    Math.max(tropical.getSeaLevel() + 1, Mth.floor(layout.surfaceHeight(x, z)) + 1), z);
            try {
                BlockPos selected = findSafePosition(tropical, data.initialAnchor);
                data.initialAnchor = selected;
                if (tropical.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
                    var chest = tropical.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                            .get(MiscOverworldFeatures.BONUS_CHEST);
                    if (chest != null) {
                        chest.place(tropical, tropical.getChunkSource().getGenerator(), tropical.random, selected);
                        if (!isSafe(tropical, selected)) selected = findSafePosition(tropical, selected);
                    }
                }
                data.setPosition(selected);
            } finally {
                // On failure, position and the previous dirty state remain unchanged.
                data.initialAnchor = null;
            }
        }
        return data.position;
    }

    /** Validate on player arrival, rather than loading chunks for background spawn queries. */
    public static BlockPos safePosition(ServerLevel tropical) {
        BlockPos saved = position(tropical);
        if (isSafe(tropical, saved)) return saved;
        BlockPos replacement = findSafePosition(tropical, saved);
        data(tropical).setPosition(replacement);
        return replacement;
    }

    public static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 2 > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockPos floor = feet.below();
        BlockState support = level.getBlockState(floor);
        if (!support.isFaceSturdy(level, floor, Direction.UP) || !support.getFluidState().isEmpty()
                || support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS)
                || TropicalSpawnSurfaces.giantMushroom(support) || hazardous(support)) return false;
        for (int dy = 0; dy < 2; dy++) {
            BlockState state = level.getBlockState(feet.above(dy));
            if (!state.getFluidState().isEmpty() || hazardous(state)) return false;
        }
        AABB bounds = new AABB(feet.getX() + 0.2D, feet.getY(), feet.getZ() + 0.2D,
                feet.getX() + 0.8D, feet.getY() + 1.8D, feet.getZ() + 0.8D);
        return level.getWorldBorder().isWithinBounds(bounds) && level.noCollision(null, bounds);
    }

    /** Search complete chunks in nearest-first order; never build a platform or alter terrain. */
    public static BlockPos findSafePosition(ServerLevel level, BlockPos near) {
        var border = level.getWorldBorder();
        int x = Mth.floor(Mth.clamp(near.getX(), border.getMinX() + 1, border.getMaxX() - 1));
        int z = Mth.floor(Mth.clamp(near.getZ(), border.getMinZ() + 1, border.getMaxZ() - 1));
        int minX = x - SEARCH_RADIUS, maxX = x + SEARCH_RADIUS;
        int minZ = z - SEARCH_RADIUS, maxZ = z + SEARCH_RADIUS;
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) chunks.add(new ChunkPos(cx, cz));
        }
        chunks.sort(Comparator.comparingDouble(chunk -> distanceSquared(
                Mth.clamp(x, chunk.getMinBlockX(), chunk.getMaxBlockX()),
                Mth.clamp(z, chunk.getMinBlockZ(), chunk.getMaxBlockZ()), x, z)));
        int minY = Math.max(level.getMinBuildHeight() + 1, level.getSeaLevel());
        for (ChunkPos chunk : chunks) {
            if (!border.isWithinBounds(chunk)) continue;
            level.getChunk(chunk.x, chunk.z);
            List<BlockPos> columns = new ArrayList<>();
            for (int bx = Math.max(minX, chunk.getMinBlockX()); bx <= Math.min(maxX, chunk.getMaxBlockX()); bx++) {
                for (int bz = Math.max(minZ, chunk.getMinBlockZ()); bz <= Math.min(maxZ, chunk.getMaxBlockZ()); bz++) {
                    columns.add(new BlockPos(bx, 0, bz));
                }
            }
            columns.sort(Comparator.comparingDouble(column -> distanceSquared(column.getX(), column.getZ(), x, z)));
            for (BlockPos column : columns) {
                int top = Math.min(level.getMaxBuildHeight() - 2,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()));
                for (int y = top; y >= minY; y--) {
                    BlockPos candidate = new BlockPos(column.getX(), y, column.getZ());
                    if (isSafe(level, candidate)) return candidate;
                }
            }
        }
        throw new IllegalStateException("No safe tropical player spawn within " + SEARCH_RADIUS
                + " blocks of " + x + ", " + z + "; clear a dry, supported two-block-high space near the spawn");
    }

    private static boolean hazardous(BlockState state) {
        return NetherRampDecorationGuard.hazardous(state) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.COBWEB) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
    }

    private static double distanceSquared(int x, int z, int originX, int originZ) {
        double dx = x - originX, dz = z - originZ;
        return dx * dx + dz * dz;
    }

    private static SpawnData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SpawnData::load, SpawnData::new, DATA_NAME);
    }

    private static final class SpawnData extends SavedData {
        private BlockPos position;
        private BlockPos initialAnchor;

        private static SpawnData load(CompoundTag tag) {
            SpawnData data = new SpawnData();
            if (tag.contains("Position", Tag.TAG_LONG)) data.position = BlockPos.of(tag.getLong("Position"));
            return data;
        }

        private void setPosition(BlockPos position) {
            this.position = position.immutable();
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            if (position != null) tag.putLong("Position", position.asLong());
            return tag;
        }
    }
}
