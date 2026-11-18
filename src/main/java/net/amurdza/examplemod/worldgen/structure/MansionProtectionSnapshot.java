package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact, chunk-scoped geometry sent before client block prediction can begin. */
public record MansionProtectionSnapshot(List<BoundingBox> rooms, List<MansionBasementPiece> basements,
                                        List<BoundingBox> unlockedRewards) {
    public MansionProtectionSnapshot(List<BoundingBox> rooms, List<MansionBasementPiece> basements) {
        this(rooms, basements, List.of());
    }
    public boolean rewardsUnlocked(BlockPos pos) { return unlockedRewards.stream().anyMatch(box -> box.isInside(pos)); }
    private static final String KEY = "aoemod_mansion_protection";
    private static final Map<LevelChunk, MansionProtectionSnapshot> RECEIVED = Collections.synchronizedMap(new WeakHashMap<>());

    public boolean protects(BlockPos pos) {
        return rooms.stream().anyMatch(box -> box.isInside(pos))
                || basements.stream().anyMatch(piece -> piece.getBoundingBox().isInside(pos) && piece.isBuiltCell(pos));
    }

    public static boolean protects(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var snapshot = RECEIVED.get(level.getChunkAt(pos));
        return snapshot != null && snapshot.protects(pos)
                && !(AuthoredRewardLocks.isRewardBlock(level.getBlockState(pos)) && snapshot.rewardsUnlocked(pos));
    }

    public static MansionProtectionSnapshot capture(ServerLevel level, ChunkPos chunk) {
        var rooms = new ArrayList<BoundingBox>();
        var basements = new ArrayList<MansionBasementPiece>();
        var unlocked = new ArrayList<BoundingBox>();
        for (var layout : PlacedMansions.get(level).at(chunk.getMiddleBlockPosition(0))) {
            if (rewardsAvailable(level, layout.basement())) {
                for (var room : layout.rooms()) if (intersects(room, chunk)) unlocked.add(room);
                if (intersects(layout.basement().getBoundingBox(), chunk)) unlocked.add(layout.basement().getBoundingBox());
            }
            for (var room : layout.rooms()) if (intersects(room, chunk)) rooms.add(room);
            if (intersects(layout.basement().getBoundingBox(), chunk)) basements.add(layout.basement());
        }
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        for (var start : level.structureManager().startsForStructure(chunk, structure -> structure == mansion)) {
            var cellar = start.getPieces().stream().filter(MansionBasementPiece.class::isInstance)
                    .map(MansionBasementPiece.class::cast).findFirst().orElse(null);
            if (cellar != null && rewardsAvailable(level, cellar))
                for (var piece : start.getPieces()) if (intersects(piece.getBoundingBox(), chunk)
                        && (piece instanceof WoodlandMansionPiece || piece instanceof MansionBasementPiece))
                    unlocked.add(piece.getBoundingBox());
            for (var piece : start.getPieces()) if (intersects(piece.getBoundingBox(), chunk)) {
                if (piece instanceof WoodlandMansionPiece) rooms.add(piece.getBoundingBox());
                else if (piece instanceof MansionBasementPiece basement) basements.add(basement);
            }
        }
        return new MansionProtectionSnapshot(List.copyOf(rooms), List.copyOf(basements), List.copyOf(unlocked));
    }

    private static boolean rewardsAvailable(ServerLevel level, MansionBasementPiece basement) {
        return basement.encounterId() == null || AuthoredEncounterProgress.get(level).isCleared(basement.encounterId());
    }

    private static boolean intersects(BoundingBox box, ChunkPos chunk) {
        return box.intersects(chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ());
    }

    public CompoundTag encode() {
        var tag = new CompoundTag();
        var boxes = new ListTag();
        for (var box : rooms) boxes.add(new IntArrayTag(new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}));
        tag.put("Rooms", boxes);
        var unlocked = new ListTag();
        for (var box : unlockedRewards) unlocked.add(new IntArrayTag(new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}));
        tag.put("UnlockedRewards", unlocked);
        var cellars = new ListTag();
        for (var piece : basements) {
            var cellar = new CompoundTag();
            cellar.putLong("Floor", piece.keyAnchor().asLong());
            cellar.putString("Rotation", piece.stairRotation().name());
            cellar.putInt("Version", piece.layoutVersion());
            cellar.putBoolean("DecorativeWalls", piece.decorativeWalls());
            cellars.add(cellar);
        }
        tag.put("Basements", cellars);
        return tag;
    }

    public static MansionProtectionSnapshot decode(CompoundTag tag) {
        var rooms = new ArrayList<BoundingBox>();
        var boxes = tag.getList("Rooms", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < boxes.size(); i++) {
            var box = boxes.getIntArray(i);
            if (box.length == 6) rooms.add(new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]));
        }
        var basements = new ArrayList<MansionBasementPiece>();
        for (var entry : tag.getList("Basements", Tag.TAG_COMPOUND)) {
            var cellar = (CompoundTag) entry;
            basements.add(new MansionBasementPiece(BlockPos.of(cellar.getLong("Floor")).above(MansionBasementPiece.mainFloorOffset(cellar.getInt("Version"))),
                    Rotation.valueOf(cellar.getString("Rotation")), cellar.getInt("Version"),
                    !cellar.contains("DecorativeWalls") || cellar.getBoolean("DecorativeWalls")));
        }
        var unlocked = new ArrayList<BoundingBox>();
        var rewardBoxes = tag.getList("UnlockedRewards", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < rewardBoxes.size(); i++) {
            var box = rewardBoxes.getIntArray(i);
            if (box.length == 6) unlocked.add(new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]));
        }
        return new MansionProtectionSnapshot(List.copyOf(rooms), List.copyOf(basements), List.copyOf(unlocked));
    }

    public static void write(LevelChunk chunk, CompoundTag packet) {
        if (chunk.getLevel() instanceof ServerLevel level) {
            var snapshot = capture(level, chunk.getPos());
            if (!snapshot.rooms.isEmpty() || !snapshot.basements.isEmpty()) packet.put(KEY, snapshot.encode());
        }
    }

    public static void read(LevelChunk chunk, CompoundTag packet) {
        if (packet.contains(KEY, Tag.TAG_COMPOUND)) RECEIVED.put(chunk, decode(packet.getCompound(KEY)));
        else RECEIVED.remove(chunk);
    }

    /** Commands can build inside chunks the player is already watching; resend their exact new snapshot. */
    public static void refreshPlaced(ServerLevel level, StructureStart start) {
        if (start.getPieces().stream().noneMatch(MansionBasementPiece.class::isInstance)) return;
        var bounds = replacementRefreshBounds(level, start);
        refreshBounds(level, bounds);
    }

    public static void refreshEncounter(ServerLevel level, java.util.UUID id) {
        var encounter = AuthoredEncounterProgress.get(level).find(id);
        if (encounter == null) return;
        var origin = encounter.origin();
        // Native references and command placements are both covered; only watched loaded chunks are sent.
        refreshBounds(level, new BoundingBox(origin.getX() - 256, level.getMinBuildHeight(), origin.getZ() - 256,
                origin.getX() + 256, level.getMaxBuildHeight() - 1, origin.getZ() + 256));
    }

    private static void refreshBounds(ServerLevel level, BoundingBox bounds) {
        var source = level.getChunkSource();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
            var pos = new ChunkPos(x, z);
            var players = source.chunkMap.getPlayers(pos, false);
            if (players.isEmpty()) continue;
            var chunk = source.getChunkNow(x, z);
            if (chunk == null) continue;
            var packet = new ClientboundLevelChunkWithLightPacket(chunk, source.getLightEngine(), null, null);
            for (var player : players) player.connection.send(packet);
        }
    }

    /** Include abandoned chunks so their next packet actively removes the old protection snapshot. */
    public static BoundingBox replacementRefreshBounds(ServerLevel level, StructureStart start) {
        var box = start.getBoundingBox();
        var bounds = new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        for (var piece : start.getPieces()) if (piece instanceof MansionBasementPiece basement) {
            var previous = PlacedMansions.get(level).consumeReplacedBounds(basement.keyAnchor());
            if (previous != null) bounds.encapsulate(previous);
        }
        return bounds;
    }
}
