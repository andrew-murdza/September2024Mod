package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.eventbus.api.Event;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A fixed authored roster unlocks only through recorded deaths, never an empty loaded area. */
public final class AuthoredEncounterProgress extends SavedData {
    private static final String DATA = "aoemod_authored_encounters";
    private static final String ID = "aoemod.encounterId", ORIGIN = "aoemod.encounterOrigin";
    private static final String EXPECTED = "aoemod.encounterExpected", KIND = "aoemod.encounterKind";
    private static final String INDEX = "aoemod.encounterDefender", LIVESTOCK = "aoemod.encounterLivestock";
    private final Map<UUID, Encounter> encounters = new HashMap<>();
    private long sequence;

    public static synchronized AuthoredEncounterProgress get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AuthoredEncounterProgress::load, AuthoredEncounterProgress::new, DATA);
    }

    public static final class Encounter {
        private final UUID id;
        private final BlockPos origin;
        private final int expected;
        private final String kind;
        private long order;
        private final Map<Integer, UUID> defenders = new HashMap<>();
        private final BitSet defeated = new BitSet();
        private final BitSet pending = new BitSet();

        private Encounter(UUID id, BlockPos origin, int expected, String kind) {
            this.id = id; this.origin = origin.immutable(); this.expected = expected; this.kind = kind;
        }

        public UUID id() { return id; }
        public BlockPos origin() { return origin; }
        public int expected() { return expected; }
        public String kind() { return kind; }
        public int defeatedCount() { return defeated.cardinality(); }
        public boolean cleared() { return expected > 0 && defeated.cardinality() == expected; }
    }

    public synchronized Encounter register(UUID id, BlockPos origin, int expected, String kind) {
        if (id == null || expected <= 0 || expected > 10000) throw new IllegalArgumentException("Invalid authored encounter");
        var existing = encounters.get(id);
        if (existing != null) {
            if (!existing.origin.equals(origin) || existing.expected != expected || !existing.kind.equals(kind))
                throw new IllegalStateException("Authored encounter identity changed: " + id);
            return existing;
        }
        var created = new Encounter(id, origin, expected, kind);
        created.order = ++sequence;
        encounters.put(id, created);
        setDirty();
        return created;
    }

    public synchronized Encounter find(UUID id) { return encounters.get(id); }
    public synchronized long registrationVersion() { return sequence; }
    public synchronized List<Encounter> encounters(String kind) {
        return encounters.values().stream().filter(encounter -> encounter.kind.equals(kind))
                .sorted(java.util.Comparator.comparingLong((Encounter encounter) -> encounter.order).reversed()).toList();
    }
    public synchronized boolean isCleared(UUID id) {
        var encounter = encounters.get(id);
        return encounter != null && encounter.cleared();
    }
    public synchronized int defeatedCount(UUID id) {
        var encounter = encounters.get(id);
        return encounter == null ? 0 : encounter.defeatedCount();
    }

    /** Queue only never-observed defenders; unloaded or dead entities are never replacements. */
    public synchronized void queueMissingDefenders(UUID id) {
        var encounter = encounters.get(id);
        if (encounter == null || encounter.cleared()) return;
        boolean changed = false;
        for (int index = 0; index < encounter.expected; index++) {
            if (!encounter.defenders.containsKey(index) && !encounter.defeated.get(index) && !encounter.pending.get(index)) {
                encounter.pending.set(index);
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    public synchronized BitSet pendingDefenders(UUID id) {
        var encounter = encounters.get(id);
        return encounter == null ? new BitSet() : (BitSet) encounter.pending.clone();
    }

    public synchronized boolean canPlaceDefender(UUID id, int index) {
        var encounter = encounters.get(id);
        return encounter != null && index >= 0 && index < encounter.expected
                && !encounter.defeated.get(index) && !encounter.defenders.containsKey(index);
    }

    public static void markDefender(Mob mob, UUID id, BlockPos origin, int expected, String kind, int index) {
        if (index < 0 || index >= expected) throw new IllegalArgumentException("Defender index outside authored roster");
        mark(mob, id, origin, expected, kind);
        mob.getPersistentData().putInt(INDEX, index);
        mob.getPersistentData().remove(LIVESTOCK);
    }

    public static void markLivestock(Mob mob, UUID id, BlockPos origin, int expected, String kind) {
        mark(mob, id, origin, expected, kind);
        mob.getPersistentData().putBoolean(LIVESTOCK, true);
        mob.getPersistentData().remove(INDEX);
    }

    private static void mark(Mob mob, UUID id, BlockPos origin, int expected, String kind) {
        var tag = mob.getPersistentData();
        tag.putUUID(ID, id); tag.putLong(ORIGIN, origin.asLong()); tag.putInt(EXPECTED, expected); tag.putString(KIND, kind);
        mob.setPersistenceRequired();
    }

    public static UUID encounterId(Entity entity) {
        var tag = entity.getPersistentData();
        return tag.hasUUID(ID) ? tag.getUUID(ID) : null;
    }
    public static String kind(Entity entity) { return entity.getPersistentData().getString(KIND); }
    public static boolean isLivestock(Entity entity) { return entity.getPersistentData().getBoolean(LIVESTOCK); }
    public static boolean isDefender(Entity entity) {
        return encounterId(entity) != null && entity.getPersistentData().contains(INDEX, Tag.TAG_INT);
    }

    public synchronized void track(Mob mob) {
        var tag = mob.getPersistentData();
        if (!tag.hasUUID(ID) || !tag.contains(ORIGIN, Tag.TAG_LONG) || tag.getInt(EXPECTED) <= 0) return;
        var encounter = register(tag.getUUID(ID), BlockPos.of(tag.getLong(ORIGIN)), tag.getInt(EXPECTED), tag.getString(KIND));
        if (!tag.contains(INDEX, Tag.TAG_INT)) return;
        int index = tag.getInt(INDEX);
        if (index >= 0 && index < encounter.expected && !encounter.defenders.containsKey(index)) {
            encounter.defenders.put(index, mob.getUUID());
            encounter.pending.clear(index);
            setDirty();
        }
    }

    /** Returns true exactly once, when the final distinct authored defender dies. */
    public synchronized boolean defeat(Mob mob) {
        if (!isDefender(mob)) return false;
        track(mob);
        var encounter = encounters.get(encounterId(mob));
        int index = mob.getPersistentData().getInt(INDEX);
        if (encounter == null || index < 0 || index >= encounter.expected || encounter.defeated.get(index)
                || !mob.getUUID().equals(encounter.defenders.get(index))) return false;
        encounter.defeated.set(index);
        encounter.pending.clear(index);
        setDirty();
        return encounter.cleared();
    }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        var entries = new ListTag();
        for (var encounter : encounters.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Id", encounter.id); entry.putLong("Origin", encounter.origin.asLong());
            entry.putInt("Expected", encounter.expected); entry.putString("Kind", encounter.kind);
            entry.putLong("Order", encounter.order);
            entry.putLongArray("Defeated", encounter.defeated.toLongArray());
            entry.putLongArray("Pending", encounter.pending.toLongArray());
            var defenders = new ListTag();
            encounter.defenders.forEach((index, id) -> {
                var defender = new CompoundTag(); defender.putInt("Index", index); defender.putUUID("Id", id); defenders.add(defender);
            });
            entry.put("Defenders", defenders); entries.add(entry);
        }
        tag.put("Encounters", entries);
        return tag;
    }

    public static AuthoredEncounterProgress load(CompoundTag tag) {
        var result = new AuthoredEncounterProgress();
        for (var value : tag.getList("Encounters", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            int expected = entry.getInt("Expected");
            if (!entry.hasUUID("Id") || expected <= 0 || expected > 10000) continue;
            var encounter = new Encounter(entry.getUUID("Id"), BlockPos.of(entry.getLong("Origin")), expected, entry.getString("Kind"));
            encounter.order = entry.contains("Order") ? entry.getLong("Order") : result.sequence + 1;
            result.sequence = Math.max(result.sequence, encounter.order);
            for (var defenderValue : entry.getList("Defenders", Tag.TAG_COMPOUND)) {
                var defender = (CompoundTag) defenderValue;
                int index = defender.getInt("Index");
                if (index >= 0 && index < expected && defender.hasUUID("Id")) encounter.defenders.put(index, defender.getUUID("Id"));
            }
            var defeated = BitSet.valueOf(entry.getLongArray("Defeated"));
            for (int index = defeated.nextSetBit(0); index >= 0 && index < expected; index = defeated.nextSetBit(index + 1))
                if (encounter.defenders.containsKey(index)) encounter.defeated.set(index);
            var pending = BitSet.valueOf(entry.getLongArray("Pending"));
            for (int index = pending.nextSetBit(0); index >= 0 && index < expected; index = pending.nextSetBit(index + 1))
                if (!encounter.defenders.containsKey(index) && !encounter.defeated.get(index)) encounter.pending.set(index);
            result.encounters.put(encounter.id, encounter);
        }
        return result;
    }

    public static final class Cleared extends Event {
        private final ServerLevel level;
        private final UUID id;
        private final String kind;
        public Cleared(ServerLevel level, UUID id, String kind) { this.level = level; this.id = id; this.kind = kind; }
        public ServerLevel level() { return level; }
        public UUID id() { return id; }
        public String kind() { return kind; }
    }
}
