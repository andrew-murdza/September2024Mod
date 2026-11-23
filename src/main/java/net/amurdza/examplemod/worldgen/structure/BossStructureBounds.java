package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persist boundaries for natural and command-placed boss structures without loading chunks during movement. */
public final class BossStructureBounds extends SavedData {
    public record Boundary(String key, AABB box, UUID encounter) { }
    private final Map<String, Boundary> boundaries = new HashMap<>();
    private final Map<Long, List<Boundary>> chunks = new HashMap<>();
    private final java.util.Set<PlacedMansions.Layout> migrated = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private long villagesVersion = -1, campsVersion = -1;

    public static synchronized BossStructureBounds get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BossStructureBounds::load, BossStructureBounds::new,
                "aoemod_boss_boundaries");
    }

    public synchronized void record(String key, AABB box, UUID encounter) {
        var next = new Boundary(key, box, encounter);
        var previous = boundaries.get(key);
        if (next.equals(previous)) return;
        if (previous != null) {
            chunks.values().forEach(values -> values.remove(previous));
            chunks.values().removeIf(List::isEmpty);
        }
        index(next);
        setDirty();
    }

    public synchronized void remove(String key) {
        var previous = boundaries.remove(key);
        if (previous == null) return;
        chunks.values().forEach(values -> values.remove(previous));
        chunks.values().removeIf(List::isEmpty);
        setDirty();
    }

    private void index(Boundary boundary) {
        boundaries.put(boundary.key, boundary);
        var box = boundary.box;
        for (int x = (int) Math.floor(box.minX) >> 4; x <= (int) Math.floor(box.maxX) >> 4; x++)
            for (int z = (int) Math.floor(box.minZ) >> 4; z <= (int) Math.floor(box.maxZ) >> 4; z++)
                chunks.computeIfAbsent(ChunkPos.asLong(x, z), unused -> new ArrayList<>()).add(boundary);
    }

    public synchronized List<Boundary> nearby(AABB box) {
        int minX = (int) Math.floor(box.minX) >> 4, maxX = (int) Math.floor(box.maxX) >> 4;
        int minZ = (int) Math.floor(box.minZ) >> 4, maxZ = (int) Math.floor(box.maxZ) >> 4;
        if (maxX - minX > 4 || maxZ - minZ > 4)
            return boundaries.values().stream().filter(boundary -> boundary.box.intersects(box)).toList();
        var result = new LinkedHashSet<Boundary>();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            for (var boundary : chunks.getOrDefault(ChunkPos.asLong(x, z), List.of()))
                if (boundary.box.intersects(box)) result.add(boundary);
        return List.copyOf(result);
    }

    public static void mansion(ServerLevel level, MansionBasementPiece basement, List<BoundingBox> rooms) {
        if (rooms.isEmpty()) return;
        var box = BoundingBox.encapsulatingBoxes(rooms).orElseThrow().encapsulate(basement.getBoundingBox());
        get(level).record("mansion:" + basement.keyAnchor().asLong(),
                new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1),
                basement.encounterId());
    }

    /** Structure starts also migrate already-generated worlds when their owner chunk loads. */
    public static void discover(ServerLevel level, StructureStart start) {
        if (start == null || !start.isValid()) return;
        MansionBasementPiece basement = null;
        var rooms = new ArrayList<BoundingBox>();
        for (var piece : start.getPieces()) {
            if (piece instanceof BossLairPiece lair) lair.registerBoundary(level);
            if (piece instanceof NetherCitadelPiece citadel) citadel.registerBoundary(level);
            if (piece instanceof GoblinFortressPiece fortress) fortress.registerBoundary(level);
            if (piece instanceof SavannaOrcCampPiece camp && camp.encounterId() != null)
                SavannaCampProgress.register(level, camp.encounterId(), camp.templatePosition());
            else if (piece instanceof SavannaOrcCampPiece camp) legacyCamp(level, camp);
            if (piece instanceof MansionBasementPiece found) basement = found;
            if (piece instanceof WoodlandMansionPiece) rooms.add(piece.getBoundingBox());
        }
        if (basement != null) {
            // A deliberate /place replacement is authoritative over the older natural start still on disk.
            var anchor = basement.keyAnchor().atY(0);
            var manual = PlacedMansions.get(level).at(basement.keyAnchor()).stream()
                    .filter(layout -> layout.basement().keyAnchor().atY(0).equals(anchor))
                    .findFirst().orElse(null);
            if (manual != null) mansion(level, manual.basement(), manual.rooms());
            else mansion(level, basement, rooms);
        }
    }

    public static void legacyCamp(ServerLevel level, SavannaOrcCampPiece camp) {
        if (camp.encounterId() != null) return;
        var b = camp.getBoundingBox();
        get(level).record("legacy-camp:" + camp.templatePosition().asLong(),
                new AABB(b.minX(), -2048, b.minZ(), b.maxX() + 1, 2048, b.maxZ() + 1), null);
    }

    public static List<Boundary> nearby(ServerLevel level, AABB box) {
        var data = get(level);
        var villages = PlacedOrcVillages.get(level);
        var progress = AuthoredEncounterProgress.get(level);
        if (data.villagesVersion != villages.version() || data.campsVersion != progress.registrationVersion()) {
            data.villagesVersion = villages.version(); data.campsVersion = progress.registrationVersion();
            var modern = progress.encounters(SavannaCampProgress.KIND);
            for (var village : villages.villages()) {
                var center = village.center();
                String key = "legacy-placed-camp:" + center.asLong();
                if (modern.stream().anyMatch(camp -> camp.origin().getX() + SavannaOrcVillage.CENTER == center.getX()
                        && camp.origin().getZ() + SavannaOrcVillage.CENTER == center.getZ())) {
                    data.remove(key); continue;
                }
                int r = village.radius();
                data.record(key, new AABB(center.getX() - r, -2048, center.getZ() - r,
                        center.getX() + r + 1, 2048, center.getZ() + r + 1), null);
            }
        }
        // A placed layout is immutable. Never re-scan its hundreds of rooms on every mob movement.
        for (var layout : PlacedMansions.get(level).at(BlockPos.containing(box.getCenter()))) {
            if (data.migrated.add(layout)) mansion(level, layout.basement(), layout.rooms());
        }
        var result = new ArrayList<>(data.nearby(box));
        for (var camp : SavannaCampProgress.nearby(level, box)) {
            var boundary = SavannaCampProgress.boundary(camp);
            if (boundary.intersects(box)) result.add(new Boundary("camp:" + camp.id(), boundary, camp.id()));
        }
        return List.copyOf(result);
    }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        for (var boundary : boundaries.values()) {
            var entry = new CompoundTag();
            entry.putString("Key", boundary.key);
            var box = boundary.box;
            entry.putIntArray("Bounds", new int[]{(int) box.minX, (int) box.minY, (int) box.minZ,
                    (int) box.maxX, (int) box.maxY, (int) box.maxZ});
            if (boundary.encounter != null) entry.putUUID("Encounter", boundary.encounter);
            list.add(entry);
        }
        tag.put("Boundaries", list);
        return tag;
    }

    public static BossStructureBounds load(CompoundTag tag) {
        var result = new BossStructureBounds();
        for (var value : tag.getList("Boundaries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            var b = entry.getIntArray("Bounds");
            if (b.length == 6) result.index(new Boundary(entry.getString("Key"),
                    new AABB(b[0], b[1], b[2], b[3], b[4], b[5]),
                    entry.hasUUID("Encounter") ? entry.getUUID("Encounter") : null));
        }
        return result;
    }
}
