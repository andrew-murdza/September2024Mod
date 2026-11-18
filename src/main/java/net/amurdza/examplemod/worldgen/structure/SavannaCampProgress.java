package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashMap;

/** Version-eleven camp identity and fixed 127-defender progression. */
public final class SavannaCampProgress {
    public static final String KIND = "savanna_camp";
    public static final int DEFENDERS = 127;
    private static final Map<ServerLevel, Index> INDEX = new WeakHashMap<>();
    private static final class Index {
        long version = -1;
        final Map<Long, List<AuthoredEncounterProgress.Encounter>> chunks = new HashMap<>();
        final java.util.Set<Long> discovered = new java.util.HashSet<>();
    }
    private SavannaCampProgress() { }

    public static AuthoredEncounterProgress.Encounter register(ServerLevel level, UUID id, BlockPos origin) {
        var data = AuthoredEncounterProgress.get(level);
        var camp = data.register(id, origin, DEFENDERS, KIND);
        data.queueMissingDefenders(id);
        return camp;
    }

    public static void markResident(Mob mob, UUID id, BlockPos origin, int index) {
        AuthoredEncounterProgress.markDefender(mob, id, origin, DEFENDERS, KIND, index);
    }

    public static void markLivestock(Mob mob, UUID id, BlockPos origin) {
        AuthoredEncounterProgress.markLivestock(mob, id, origin, DEFENDERS, KIND);
    }

    public static BoundingBox bounds(AuthoredEncounterProgress.Encounter camp) {
        var origin = camp.origin();
        int center = SavannaOrcVillage.CENTER, radius = SavannaOrcVillage.RADIUS;
        return new BoundingBox(origin.getX() + center - radius, 0, origin.getZ() + center - radius,
                origin.getX() + center + radius, origin.getY() + 17, origin.getZ() + center + radius);
    }

    public static AABB boundary(AuthoredEncounterProgress.Encounter camp) {
        var bounds = bounds(camp);
        return new AABB(bounds.minX(), -2048, bounds.minZ(), bounds.maxX() + 1, 2048, bounds.maxZ() + 1);
    }

    /** Chunk packets and interaction queries register natural camps before any resident chunk needs to load. */
    public static void discover(ServerLevel level, ChunkPos chunk) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
        if (structure == null) return;
        for (var start : level.structureManager().startsForStructure(chunk, value -> value == structure))
            for (var piece : start.getPieces()) if (piece instanceof SavannaOrcCampPiece camp && camp.encounterId() != null)
                register(level, camp.encounterId(), camp.templatePosition());
    }

    public static AuthoredEncounterProgress.Encounter at(ServerLevel level, BlockPos pos) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return null;
        var chunk = new ChunkPos(pos);
        for (var camp : inChunk(level, chunk)) if (bounds(camp).isInside(pos)) return camp;
        if (!level.hasChunkAt(pos)) return null;
        var index = INDEX.get(level);
        if (index.discovered.size() > 4096) index.discovered.clear();
        if (index.discovered.add(chunk.toLong())) discover(level, chunk);
        for (var camp : inChunk(level, chunk)) if (bounds(camp).isInside(pos)) return camp;
        return null;
    }

    public static synchronized List<AuthoredEncounterProgress.Encounter> inChunk(ServerLevel level, ChunkPos chunk) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return List.of();
        var data = AuthoredEncounterProgress.get(level);
        var index = INDEX.computeIfAbsent(level, ignored -> new Index());
        if (index.version != data.registrationVersion()) {
            index.chunks.clear(); index.version = data.registrationVersion();
        }
        if (index.chunks.size() > 4096) index.chunks.clear();
        return index.chunks.computeIfAbsent(chunk.toLong(), ignored -> {
            var origins = new java.util.HashSet<BlockPos>();
            return data.encounters(KIND).stream().filter(camp -> origins.add(camp.origin())).filter(camp -> bounds(camp).intersects(
                    chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ())).toList();
        });
    }

    public static List<AuthoredEncounterProgress.Encounter> nearby(ServerLevel level, AABB box) {
        var result = new java.util.LinkedHashSet<AuthoredEncounterProgress.Encounter>();
        int minX = net.minecraft.util.Mth.floor(box.minX) >> 4, maxX = net.minecraft.util.Mth.floor(box.maxX) >> 4;
        int minZ = net.minecraft.util.Mth.floor(box.minZ) >> 4, maxZ = net.minecraft.util.Mth.floor(box.maxZ) >> 4;
        // Ordinary movement spans at most a few chunks. Large command teleports
        // use the tick fallback, avoiding scans proportional to travel distance.
        if (maxX - minX > 4 || maxZ - minZ > 4) {
            var origins = new java.util.HashSet<BlockPos>();
            return AuthoredEncounterProgress.get(level).encounters(KIND).stream().filter(camp -> origins.add(camp.origin())).toList();
        }
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) result.addAll(inChunk(level, new ChunkPos(x, z)));
        return List.copyOf(result);
    }

    public static boolean belongs(Mob mob, AuthoredEncounterProgress.Encounter camp) {
        return camp.id().equals(AuthoredEncounterProgress.encounterId(mob));
    }

    /** Only chests and the crops inside the four authored fenced L-shaped fields are rewards. */
    public static boolean isRewardBlock(net.minecraft.world.level.block.state.BlockState state, BlockPos pos, BoundingBox bounds) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) return true;
        int x = pos.getX() - (bounds.minX() + SavannaOrcVillage.RADIUS);
        int z = pos.getZ() - (bounds.minZ() + SavannaOrcVillage.RADIUS);
        int a = x < 0 ? -x - 1 : x, b = z < 0 ? -z - 1 : z;
        return a >= 4 && b >= 4 && a <= 51 && b <= 51 && Math.max(a, b) >= 40
                && AuthoredRewardLocks.isRewardBlock(state);
    }
}
