package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;

import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Persisted reservations protect every cell of a proven surface-water path until its chunk decorates. */
public final class PlainsWaterProof {
    public static final String NBT_KEY = "aoemod:surface_water_paths";
    // A standard sea-level column needs at most 63 * 256 bits = 2016 bytes per chunk.
    // Weak keys release the in-memory index when the owning chunk unloads.
    private static final Map<ChunkAccess, BitSet> PATHS = new WeakHashMap<>();

    private PlainsWaterProof() { }

    private static ChunkAccess owner(ChunkAccess chunk) {
        return chunk instanceof ImposterProtoChunk wrapper ? wrapper.getWrapped() : chunk;
    }

    private static int index(BlockPos pos) {
        return (pos.getY() << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
    }

    public static synchronized boolean contains(ChunkAccess chunk, BlockPos pos) {
        BitSet paths = PATHS.get(owner(chunk));
        return paths != null && pos.getY() >= 0 && pos.getY() < 64 && paths.get(index(pos));
    }

    public static synchronized void mark(ChunkAccess chunk, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= 64) return;
        ChunkAccess owner = owner(chunk);
        PATHS.computeIfAbsent(owner, ignored -> new BitSet()).set(index(pos));
        owner.setUnsaved(true);
    }

    public static synchronized long[] save(ChunkAccess chunk) {
        BitSet paths = PATHS.get(owner(chunk));
        return paths == null ? new long[0] : paths.toLongArray();
    }

    public static synchronized void load(ChunkAccess chunk, long[] data) {
        ChunkAccess owner = owner(chunk);
        if (data.length == 0) PATHS.remove(owner);
        else PATHS.put(owner, BitSet.valueOf(data.length <= 256 ? data : java.util.Arrays.copyOf(data, 256)));
    }

    public static synchronized void copy(ChunkAccess from, ChunkAccess to) {
        BitSet paths = PATHS.get(owner(from));
        if (paths != null) PATHS.put(owner(to), (BitSet) paths.clone());
    }
}
