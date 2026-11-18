package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Authored camp protection arrives with chunks before client mining prediction. */
public final class SavannaCampProtectionSnapshot {
    private static final String KEY = "aoemod_savanna_camp_protection";
    private record Entry(BoundingBox bounds, boolean cleared) { }
    private static final Map<LevelChunk, List<Entry>> RECEIVED = Collections.synchronizedMap(new WeakHashMap<>());
    private SavannaCampProtectionSnapshot() { }

    public static boolean protects(Level level, BlockPos pos) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || !level.hasChunkAt(pos)) return false;
        for (var entry : RECEIVED.getOrDefault(level.getChunkAt(pos), List.of())) if (entry.bounds.isInside(pos))
            return !entry.cleared || !SavannaCampProgress.isRewardBlock(level.getBlockState(pos), pos, entry.bounds);
        return false;
    }

    public static void write(LevelChunk chunk, CompoundTag packet) {
        if (!(chunk.getLevel() instanceof ServerLevel level) || !GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return;
        SavannaCampProgress.discover(level, chunk.getPos());
        var entries = new ListTag();
        for (var camp : SavannaCampProgress.inChunk(level, chunk.getPos())) {
            var box = SavannaCampProgress.bounds(camp);
            var tag = new CompoundTag();
            tag.putIntArray("Bounds", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            tag.putBoolean("Cleared", camp.cleared()); entries.add(tag);
        }
        if (!entries.isEmpty()) packet.put(KEY, entries);
    }

    public static void read(LevelChunk chunk, CompoundTag packet) {
        if (!packet.contains(KEY, Tag.TAG_LIST)) { RECEIVED.remove(chunk); return; }
        var entries = new ArrayList<Entry>();
        for (var value : packet.getList(KEY, Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag) value; var box = tag.getIntArray("Bounds");
            if (box.length == 6) entries.add(new Entry(new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]), tag.getBoolean("Cleared")));
        }
        RECEIVED.put(chunk, List.copyOf(entries));
    }

    public static void refresh(ServerLevel level, BoundingBox bounds) {
        var source = level.getChunkSource();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
            var pos = new net.minecraft.world.level.ChunkPos(x, z);
            var players = source.chunkMap.getPlayers(pos, false);
            if (players.isEmpty()) continue;
            var chunk = source.getChunkNow(x, z);
            if (chunk == null) continue;
            var packet = new ClientboundLevelChunkWithLightPacket(chunk, source.getLightEngine(), null, null);
            for (var player : players) player.connection.send(packet);
        }
    }
}
