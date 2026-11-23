package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Chunk-clipped construction; saved encounter slots, never chunk reloads, control the population. */
public final class NetherCitadelPiece extends StructurePiece {
    private static final ResourceLocation LOOT = new ResourceLocation("aoemod", "chests/bosses/nether_citadel");
    private static final ThreadLocal<Boolean> BUILDING = ThreadLocal.withInitial(() -> false);
    private final BlockPos origin;
    private final UUID encounter;
    private final long seed;
    private final Set<Long> completedChunks = new HashSet<>();
    private NetherCitadelApproach approach;

    public NetherCitadelPiece(BlockPos origin, long seed) {
        super(ModStructures.NETHER_CITADEL_PIECE.get(), 0, NetherCitadelProgress.bounds(origin));
        this.origin = origin.atY(NetherCitadelLayout.MIN_Y);
        this.seed = seed;
        encounter = UUID.nameUUIDFromBytes(("aoemod:nether_citadel:" + this.origin.asLong() + ":" + seed)
                .getBytes(StandardCharsets.UTF_8));
    }

    public NetherCitadelPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.NETHER_CITADEL_PIECE.get(), tag);
        origin = BlockPos.of(tag.getLong("CitadelOrigin")).atY(NetherCitadelLayout.MIN_Y);
        seed = tag.getLong("CitadelSeed");
        encounter = tag.getUUID("CitadelEncounter");
        for (long chunk : tag.getLongArray("CitadelChunks")) completedChunks.add(chunk);
        boundingBox = NetherCitadelProgress.bounds(origin);
    }

    public BlockPos origin() { return origin; }
    public UUID encounterId() { return encounter; }
    public static boolean building() { return BUILDING.get(); }

    public void registerBoundary(ServerLevel level) {
        NetherCitadelProgress.register(level, encounter, origin);
    }

    @Override
    protected synchronized void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("CitadelOrigin", origin.asLong());
        tag.putLong("CitadelSeed", seed);
        tag.putUUID("CitadelEncounter", encounter);
        tag.putLongArray("CitadelChunks", completedChunks.stream().mapToLong(Long::longValue).sorted().toArray());
    }

    @Override
    public synchronized void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                        RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        registerBoundary(level.getLevel());
        if (completedChunks.contains(chunk.toLong())) return;
        if (approach == null) approach = NetherCitadelApproach.sample(origin, generator,
                level.getLevel().getChunkSource().randomState(), level.getLevel());
        boolean previous = BUILDING.get();
        BUILDING.set(true);
        try {
            var cursor = new BlockPos.MutableBlockPos();
            for (int x = Math.max(clip.minX(), boundingBox.minX()); x <= Math.min(clip.maxX(), boundingBox.maxX()); x++) {
                for (int z = Math.max(clip.minZ(), boundingBox.minZ()); z <= Math.min(clip.maxZ(), boundingBox.maxZ()); z++) {
                    for (int y = Math.max(clip.minY(), boundingBox.minY()); y <= Math.min(clip.maxY(), boundingBox.maxY()); y++) {
                        int dx = x - origin.getX(), dz = z - origin.getZ();
                        var fitted = approach.stateAt(dx, y, dz);
                        var state = fitted != null ? fitted : NetherCitadelLayout.stateAt(dx, y, dz);
                        if (state != null) level.setBlock(cursor.set(x, y, z), state, 2);
                    }
                }
            }
            for (var chest : NetherCitadelLayout.chests()) {
                var pos = origin.offset(chest.pos().getX(), chest.pos().getY() - origin.getY(), chest.pos().getZ());
                if (clip.isInside(pos) && level.getBlockEntity(pos) instanceof ChestBlockEntity inventory)
                    inventory.setLootTable(LOOT, seed ^ pos.asLong());
            }
            if (clip.minX() <= Math.max(chunk.getMinBlockX(), boundingBox.minX())
                    && clip.maxX() >= Math.min(chunk.getMaxBlockX(), boundingBox.maxX())
                    && clip.minZ() <= Math.max(chunk.getMinBlockZ(), boundingBox.minZ())
                    && clip.maxZ() >= Math.min(chunk.getMaxBlockZ(), boundingBox.maxZ())
                    && clip.minY() <= boundingBox.minY() && clip.maxY() >= boundingBox.maxY())
                completedChunks.add(chunk.toLong());
        } finally {
            BUILDING.set(previous);
        }
    }

    /** Query only already available worldgen chunks; never load neighbors from a feature worker. */
    public static boolean reserves(WorldGenLevel level, BlockPos pos) {
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)
                || pos.getY() < NetherCitadelLayout.MIN_Y || pos.getY() > NetherCitadelLayout.MAX_Y) return false;
        if (level instanceof ServerLevel server) return NetherCitadelProgress.at(server, pos) != null;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "wither_cave"));
        if (structure == null) return false;
        var current = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
        if (current == null) return false;
        for (long reference : current.getReferencesForStructure(structure)) {
            var owner = new ChunkPos(reference);
            var startChunk = level.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, false);
            if (startChunk == null) continue;
            var start = startChunk.getStartForStructure(structure);
            if (start != null && start.isValid()) for (var piece : start.getPieces())
                if (piece instanceof NetherCitadelPiece && piece.getBoundingBox().isInside(pos)) return true;
        }
        return false;
    }
}
