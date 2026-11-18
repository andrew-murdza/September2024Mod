package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;

import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact camp footprint available even in the one-chunk region used for population. */
public final class SavannaCampColumns {
    public static final String NBT_KEY = "aoemod:savanna_camp_columns";
    public static final String VEGETATION_NBT_KEY = "aoemod:savanna_camp_vegetation_columns";
    private static final Map<ChunkAccess, BitSet> COLUMNS = new WeakHashMap<>();
    // Groundcover reservations in bits 0..255; the wider tree reservations in 256..511.
    private static final Map<ChunkAccess, BitSet> VEGETATION = new WeakHashMap<>();

    private SavannaCampColumns() { }

    private static ChunkAccess owner(ChunkAccess chunk) {
        return chunk instanceof ImposterProtoChunk wrapper ? wrapper.getWrapped() : chunk;
    }

    public static synchronized boolean recorded(ChunkAccess chunk) {
        return COLUMNS.containsKey(owner(chunk));
    }

    public static synchronized boolean contains(ChunkAccess chunk, BlockPos pos) {
        BitSet columns = COLUMNS.get(owner(chunk));
        return columns != null && chunk.getPos().x == pos.getX() >> 4 && chunk.getPos().z == pos.getZ() >> 4
                && columns.get(((pos.getZ() & 15) << 4) | (pos.getX() & 15));
    }

    public static synchronized void record(ChunkAccess chunk, SavannaOrcCampPiece piece) {
        ChunkAccess owner = owner(chunk);
        BitSet columns = COLUMNS.computeIfAbsent(owner, ignored -> new BitSet(256));
        BitSet vegetation = piece.usesNativeVegetation()
                ? VEGETATION.computeIfAbsent(owner, ignored -> new BitSet(512)) : null;
        var pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            pos.set(owner.getPos().getMinBlockX() + x, piece.templatePosition().getY(), owner.getPos().getMinBlockZ() + z);
            if (piece.containsSurfaceColumn(pos)) columns.set((z << 4) | x);
            if (vegetation != null) {
                int index = (z << 4) | x;
                // A replacement command physically rewrites only its mound columns.
                // Retain neighboring camps' reservations outside that overwritten area.
                if (piece.hasMoundColumn(pos)) {
                    vegetation.clear(index);
                    vegetation.clear(index + 256);
                }
                if (SavannaCampVegetation.reserved(piece, pos, false)) vegetation.set(index);
                if (SavannaCampVegetation.reserved(piece, pos, true)) vegetation.set(index + 256);
            }
        }
        owner.setUnsaved(true);
    }

    /** Null means no snapshot; an empty array is a recorded, empty footprint. */
    public static synchronized long[] save(ChunkAccess chunk) {
        BitSet columns = COLUMNS.get(owner(chunk));
        return columns == null ? null : columns.toLongArray();
    }

    public static synchronized void load(ChunkAccess chunk, long[] values) {
        if (values == null) COLUMNS.remove(owner(chunk));
        else COLUMNS.put(owner(chunk), BitSet.valueOf(values.length <= 4 ? values : java.util.Arrays.copyOf(values, 4)));
    }

    public static synchronized void copy(ChunkAccess from, ChunkAccess to) {
        BitSet columns = COLUMNS.get(owner(from));
        if (columns != null) COLUMNS.put(owner(to), (BitSet) columns.clone());
        BitSet vegetation = VEGETATION.get(owner(from));
        if (vegetation != null) VEGETATION.put(owner(to), (BitSet) vegetation.clone());
    }

    public static synchronized boolean vegetationRecorded(ChunkAccess chunk) {
        return VEGETATION.containsKey(owner(chunk));
    }

    public static synchronized boolean reservesVegetation(ChunkAccess chunk, BlockPos pos, boolean tree) {
        BitSet columns = VEGETATION.get(owner(chunk));
        return columns != null && chunk.getPos().x == pos.getX() >> 4 && chunk.getPos().z == pos.getZ() >> 4
                && columns.get(((pos.getZ() & 15) << 4) | (pos.getX() & 15) | (tree ? 256 : 0));
    }

    public static synchronized long[] saveVegetation(ChunkAccess chunk) {
        BitSet columns = VEGETATION.get(owner(chunk));
        return columns == null ? null : columns.toLongArray();
    }

    public static synchronized void loadVegetation(ChunkAccess chunk, long[] values) {
        if (values == null) VEGETATION.remove(owner(chunk));
        else VEGETATION.put(owner(chunk), BitSet.valueOf(values.length <= 8 ? values : java.util.Arrays.copyOf(values, 8)));
    }
}
