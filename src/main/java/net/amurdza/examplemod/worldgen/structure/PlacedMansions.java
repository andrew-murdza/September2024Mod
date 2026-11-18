package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vanilla does not register starts for /place structure. Remember only those manually placed mansions. */
public final class PlacedMansions extends SavedData {
    public record Layout(MansionBasementPiece basement, List<BoundingBox> rooms) {}
    private final Map<BlockPos, Layout> layouts = new HashMap<>();
    private final Map<Long, List<Layout>> byChunk = new HashMap<>();
    private final Map<BlockPos, BoundingBox> replacedBounds = new HashMap<>();

    public static PlacedMansions get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlacedMansions::load, PlacedMansions::new, "aoemod_placed_mansions");
    }

    public void record(MansionBasementPiece basement, List<BoundingBox> rooms) {
        var previous = layouts.get(basement.keyAnchor());
        BlockPos estate = estateAnchor(basement, rooms);
        if (previous == null) previous = layouts.values().stream().filter(layout ->
                estate.equals(estateAnchor(layout.basement, layout.rooms))).findFirst().orElse(null);
        if (previous == null) previous = layouts.values().stream().filter(layout ->
                layout.basement.keyAnchor().atY(layout.basement.mainFloorY())
                        .equals(basement.keyAnchor().atY(basement.mainFloorY()))).findFirst().orElse(null);
        if (previous != null) {
            if (previous.basement.layoutVersion() == basement.layoutVersion()
                    && previous.basement.decorativeWalls() == basement.decorativeWalls()
                    && previous.basement.stairRotation() == basement.stairRotation()
                    && previous.rooms.equals(rooms)
                    && java.util.Objects.equals(previous.basement.encounterId(), basement.encounterId())) return;
            // A deliberate replacement command may have a newer blueprint at the same entrance.
            var oldBounds = BoundingBox.encapsulatingBoxes(previous.rooms).orElseThrow().encapsulate(previous.basement.getBoundingBox());
            replacedBounds.merge(basement.keyAnchor(), oldBounds, (first, next) -> first.encapsulate(next));
            var replaced = previous;
            layouts.remove(previous.basement.keyAnchor());
            byChunk.values().forEach(entries -> entries.remove(replaced));
            byChunk.values().removeIf(List::isEmpty);
        }
        index(new Layout(basement, List.copyOf(rooms)));
        setDirty();
    }

    private static BlockPos estateAnchor(MansionBasementPiece basement, List<BoundingBox> rooms) {
        return BoundingBox.encapsulatingBoxes(rooms).map(box -> box.getCenter().atY(basement.groundsY()))
                .orElse(basement.keyAnchor().atY(basement.groundsY()));
    }

    private void index(Layout layout) {
        layouts.put(layout.basement.keyAnchor(), layout);
        var bounds = BoundingBox.encapsulatingBoxes(layout.rooms).orElseThrow().encapsulate(layout.basement.getBoundingBox());
        int margin = layout.basement.layoutVersion() >= MansionEstateShape.SQUARE_VERSION
                ? MansionEstateShape.SQUARE_MARGIN + Math.max(0, layout.basement.groundsY() + 64) : MansionGrounds.OUTER_MARGIN;
        for (int x = (bounds.minX() - margin) >> 4; x <= (bounds.maxX() + margin) >> 4; x++) {
            for (int z = (bounds.minZ() - margin) >> 4; z <= (bounds.maxZ() + margin) >> 4; z++)
                byChunk.computeIfAbsent(ChunkPos.asLong(x, z), unused -> new ArrayList<>()).add(layout);
        }
    }

    public List<Layout> at(BlockPos pos) {
        return byChunk.getOrDefault(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), List.of());
    }

    /** Transient packet invalidation only; the current layout remains the sole persisted geometry. */
    public BoundingBox consumeReplacedBounds(BlockPos anchor) {
        return replacedBounds.remove(anchor);
    }

    @Override public CompoundTag save(CompoundTag data) {
        var entries = new ListTag();
        for (var layout : layouts.values()) {
            var entry = new CompoundTag();
            entry.putLong("Floor", layout.basement.keyAnchor().asLong());
            entry.putString("Rotation", layout.basement.stairRotation().name());
            entry.putInt("LayoutVersion", layout.basement.layoutVersion());
            entry.putBoolean("DecorativeWalls", layout.basement.decorativeWalls());
            layout.basement.saveEncounter(entry);
            var rooms = new ListTag();
            for (var b : layout.rooms) rooms.add(new net.minecraft.nbt.IntArrayTag(new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}));
            entry.put("Rooms", rooms);
            entries.add(entry);
        }
        data.put("Mansions", entries);
        return data;
    }

    public static PlacedMansions load(CompoundTag data) {
        var result = new PlacedMansions();
        for (var element : data.getList("Mansions", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var boxes = new ArrayList<BoundingBox>();
            var rooms = entry.getList("Rooms", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < rooms.size(); i++) {
                int[] b = rooms.getIntArray(i);
                if (b.length == 6) boxes.add(new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
            if (boxes.isEmpty()) continue;
            int version = entry.contains("LayoutVersion") ? entry.getInt("LayoutVersion") : 1;
            var basement = new MansionBasementPiece(BlockPos.of(entry.getLong("Floor")).above(MansionBasementPiece.mainFloorOffset(version)),
                    Rotation.valueOf(entry.getString("Rotation")), version,
                    !entry.contains("DecorativeWalls") || entry.getBoolean("DecorativeWalls"));
            basement.readEncounter(entry);
            result.index(new Layout(basement, boxes));
        }
        return result;
    }
}
