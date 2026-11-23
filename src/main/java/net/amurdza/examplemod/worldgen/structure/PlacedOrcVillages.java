package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Command-placed camps have no vanilla structure start; retain their crop exclusion after reload. */
public final class PlacedOrcVillages extends SavedData {
    private record Footprint(int radius, String shape) { }
    private final Map<BlockPos, Footprint> centers = new HashMap<>();
    private long version;
    public record Village(BlockPos center, int radius) { }
    public long version() { return version; }
    public java.util.List<Village> villages() {
        return centers.entrySet().stream().map(entry -> new Village(entry.getKey(), entry.getValue().radius())).toList();
    }

    public static PlacedOrcVillages get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlacedOrcVillages::load, PlacedOrcVillages::new, "aoemod_placed_orc_villages");
    }

    public void record(BlockPos center) {
        record(center, SavannaOrcVillage.RADIUS);
    }

    public void record(BlockPos center, int radius) {
        record(center, radius, "square");
    }

    public void record(BlockPos center, int radius, boolean circular) {
        record(center, radius, circular ? "circle" : "cubic");
    }

    public void record(BlockPos center, int radius, String shape) {
        if (!java.util.Set.of("square", "circle", "cubic").contains(shape)) throw new IllegalArgumentException(shape);
        var footprint = new Footprint(radius, shape);
        Footprint previous = centers.put(center.immutable(), footprint);
        if (!footprint.equals(previous)) { version++; setDirty(); }
    }

    public boolean contains(BlockPos pos) {
        return centers.entrySet().stream().anyMatch(entry -> {
            long x = Math.abs((long) pos.getX() - entry.getKey().getX());
            long z = Math.abs((long) pos.getZ() - entry.getKey().getZ());
            long r = entry.getValue().radius();
            if (x > r || z > r) return false;
            return switch (entry.getValue().shape()) {
                case "square" -> true;
                case "circle" -> x * x + z * z <= r * r;
                default -> x * x * x + z * z * z <= r * r * r;
            };
        });
    }

    @Override public CompoundTag save(CompoundTag tag) {
        var villages = new net.minecraft.nbt.ListTag();
        centers.forEach((center, footprint) -> {
            var village = new CompoundTag();
            village.putLong("Center", center.asLong());
            village.putInt("Radius", footprint.radius());
            village.putString("Shape", footprint.shape());
            villages.add(village);
        });
        tag.put("Villages", villages);
        return tag;
    }

    public static PlacedOrcVillages load(CompoundTag tag) {
        var result = new PlacedOrcVillages();
        for (long packed : tag.getLongArray("Centers")) result.centers.put(BlockPos.of(packed),
                new Footprint(LegacySavannaOrcVillageV3.RADIUS, "cubic"));
        for (var entry : tag.getList("Villages", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var village = (CompoundTag) entry;
            result.centers.put(BlockPos.of(village.getLong("Center")),
                    new Footprint(village.getInt("Radius"), village.contains("Shape") ? village.getString("Shape")
                            : village.getBoolean("Circular") ? "circle" : "cubic"));
        }
        result.version = result.centers.size();
        return result;
    }
}
