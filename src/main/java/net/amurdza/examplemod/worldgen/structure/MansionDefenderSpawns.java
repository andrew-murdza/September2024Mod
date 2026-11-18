package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persist failed original spawns, without ever replacing unloaded or defeated defenders. */
public final class MansionDefenderSpawns extends SavedData {
    public record Plan(UUID encounter, BlockPos origin, int expected, int index, String type, BlockPos preferred, BoundingBox room) { }
    private final Map<UUID, Plan> plans = new HashMap<>();

    public static synchronized MansionDefenderSpawns get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MansionDefenderSpawns::load, MansionDefenderSpawns::new, "aoemod_mansion_defender_spawns");
    }
    public static UUID entityId(UUID encounter, int index) {
        return UUID.nameUUIDFromBytes(("mansion:" + encounter + ":" + index).getBytes(StandardCharsets.UTF_8));
    }
    public synchronized void plan(ServerLevel level, Plan plan) {
        AuthoredEncounterProgress.get(level).register(plan.encounter, plan.origin, plan.expected, "mansion");
        if (plans.putIfAbsent(entityId(plan.encounter, plan.index), plan) == null) setDirty();
    }
    public synchronized List<Plan> plans() { return List.copyOf(plans.values()); }
    private synchronized void remove(Plan plan) {
        if (plans.remove(entityId(plan.encounter, plan.index)) != null) setDirty();
    }

    public static boolean retry(ServerLevel level, Plan plan) {
        var progress = AuthoredEncounterProgress.get(level);
        if (level.getEntity(entityId(plan.encounter, plan.index)) instanceof Mob existing && !existing.isRemoved()) {
            progress.track(existing); get(level).remove(plan); return false;
        }
        if (!progress.canPlaceDefender(plan.encounter, plan.index)) { get(level).remove(plan); return false; }
        if (level.getChunkSource().getChunkNow(plan.preferred.getX() >> 4, plan.preferred.getZ() >> 4) == null) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(plan.type));
        if (type == null || !(type.create(level) instanceof Mob mob)) return false;
        for (int radius = 0; radius <= 6; radius++) for (int dy : new int[]{0, 1, -1, 2, -2})
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                var pos = plan.preferred.offset(dx, dy, dz);
                if (!plan.room.isInside(pos) || !plan.room.isInside(pos.above(2))
                        || level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                if (!level.noCollision(mob) || !level.getFluidState(pos).isEmpty()) continue;
                mob.setUUID(entityId(plan.encounter, plan.index));
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                StructureSpawnedMobData.mark(mob, pos);
                AuthoredEncounterProgress.markDefender(mob, plan.encounter, plan.origin, plan.expected, "mansion", plan.index);
                boolean placed = level.addFreshEntity(mob);
                if (placed) { progress.track(mob); get(level).remove(plan); }
                return placed;
            }
        mob.discard();
        return false;
    }

    public static void retryLoaded(ServerLevel level) {
        var progress = AuthoredEncounterProgress.get(level);
        var activeOrigins = new java.util.HashSet<BlockPos>();
        var activeIds = new java.util.HashSet<UUID>();
        for (var encounter : progress.encounters("mansion"))
            if (activeOrigins.add(encounter.origin()) && !encounter.cleared()) activeIds.add(encounter.id());
        var queue = get(level);
        for (var plan : queue.plans()) {
            if (!activeIds.contains(plan.encounter) || !progress.canPlaceDefender(plan.encounter, plan.index)) queue.remove(plan);
            else if (level.getChunkSource().getChunkNow(plan.preferred.getX() >> 4, plan.preferred.getZ() >> 4) != null) retry(level, plan);
        }
    }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        var entries = new ListTag();
        for (var plan : plans.values()) {
            var entry = new CompoundTag(); entry.putUUID("Encounter", plan.encounter); entry.putLong("Origin", plan.origin.asLong());
            entry.putInt("Expected", plan.expected); entry.putInt("Index", plan.index); entry.putString("Type", plan.type);
            entry.putLong("Preferred", plan.preferred.asLong()); var box = plan.room;
            entry.putIntArray("Room", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}); entries.add(entry);
        }
        tag.put("Plans", entries); return tag;
    }
    public static MansionDefenderSpawns load(CompoundTag tag) {
        var data = new MansionDefenderSpawns();
        for (var value : tag.getList("Plans", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value; var box = entry.getIntArray("Room");
            if (!entry.hasUUID("Encounter") || box.length != 6) continue;
            var plan = new Plan(entry.getUUID("Encounter"), BlockPos.of(entry.getLong("Origin")), entry.getInt("Expected"), entry.getInt("Index"),
                    entry.getString("Type"), BlockPos.of(entry.getLong("Preferred")), new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]));
            data.plans.put(entityId(plan.encounter, plan.index), plan);
        }
        return data;
    }
}
