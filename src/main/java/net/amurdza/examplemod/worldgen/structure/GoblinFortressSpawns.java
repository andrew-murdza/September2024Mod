package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persist original spawn plans until insertion succeeds; unloading and deaths never replenish a roster. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class GoblinFortressSpawns extends SavedData {
    public record Plan(UUID encounter, BlockPos origin, int index, String type, BlockPos preferred,
                       BoundingBox room, boolean livestock, boolean aquatic) { }
    private final Map<UUID, Plan> pending = new LinkedHashMap<>();
    private final Set<UUID> placedLivestock = new HashSet<>();

    public static synchronized GoblinFortressSpawns get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(GoblinFortressSpawns::load, GoblinFortressSpawns::new,
                "aoemod_goblin_fortress_spawns");
    }

    public static UUID entityId(Plan plan) {
        return UUID.nameUUIDFromBytes(("goblin_fortress:" + plan.encounter + ":" + plan.index).getBytes(StandardCharsets.UTF_8));
    }

    public synchronized void plan(ServerLevel level, Plan plan) {
        AuthoredEncounterProgress.get(level).register(plan.encounter, plan.origin,
                GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.ENCOUNTER_KIND);
        if (placedLivestock.contains(entityId(plan))) return;
        if (!plan.livestock && !AuthoredEncounterProgress.get(level).canPlaceDefender(plan.encounter, plan.index)) return;
        if (pending.putIfAbsent(entityId(plan), plan) == null) setDirty();
    }

    public synchronized List<Plan> plans() { return List.copyOf(pending.values()); }

    private synchronized void finish(Plan plan) {
        pending.remove(entityId(plan));
        if (plan.livestock) placedLivestock.add(entityId(plan));
        setDirty();
    }

    public static boolean retry(ServerLevel level, Plan plan) {
        var data = get(level);
        var progress = AuthoredEncounterProgress.get(level);
        if (level.getEntity(entityId(plan)) instanceof Mob existing && !existing.isRemoved()) {
            progress.track(existing); data.finish(plan); return false;
        }
        if (data.placedLivestock.contains(entityId(plan))
                || !plan.livestock && !progress.canPlaceDefender(plan.encounter, plan.index)) {
            data.finish(plan); return false;
        }
        if (!entitiesReady(level, plan.preferred)) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(plan.type));
        if (type == null || !(type.create(level) instanceof Mob mob)) return false;
        for (int radius = 0; radius <= 5; radius++) for (int dy : new int[]{0, 1, -1})
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                var pos = plan.preferred.offset(dx, dy, dz);
                if (!plan.room.isInside(pos) || !plan.room.isInside(pos.above((int) Math.ceil(mob.getBbHeight()) - 1))
                        || !entitiesReady(level, pos)) continue;
                if (plan.aquatic ? !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        : !level.getFluidState(pos).isEmpty() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                if (!level.noCollision(mob)) continue;
                // Vanilla noCollision filters ordinary living mobs out of its entity collision shapes.
                // Explicit occupancy prevents two pending residents from claiming the same alternate floor.
                if (!level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox(),
                        occupant -> occupant.isAlive() && !occupant.isSpectator()).isEmpty()) continue;
                mob.setUUID(entityId(plan));
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                StructureSpawnedMobData.mark(mob, pos);
                if (plan.livestock) {
                    AuthoredEncounterProgress.markLivestock(mob, plan.encounter, plan.origin,
                            GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.ENCOUNTER_KIND);
                    // Pens provide containment. Livestock must not acquire a permanent home leash.
                    if (mob instanceof net.minecraft.world.entity.PathfinderMob animal) animal.clearRestriction();
                } else AuthoredEncounterProgress.markDefender(mob, plan.encounter, plan.origin,
                        GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.ENCOUNTER_KIND, plan.index);
                if (level.addFreshEntity(mob)) { progress.track(mob); data.finish(plan); return true; }
                mob.discard();
                return false;
            }
        mob.discard();
        return false;
    }

    private static boolean entitiesReady(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && level.areEntitiesLoaded(ChunkPos.asLong(pos)) && level.isPositionEntityTicking(pos);
    }

    public static void retryLoaded(ServerLevel level) {
        var queue = get(level);
        int attempted = 0;
        for (var plan : queue.plans()) {
            if (level.getChunkSource().getChunkNow(plan.preferred.getX() >> 4, plan.preferred.getZ() >> 4) == null) continue;
            retry(level, plan);
            // Blocked doorways or a canceled insertion must not starve later rooms.
            synchronized (queue) {
                var deferred = queue.pending.remove(entityId(plan));
                if (deferred != null) queue.pending.put(entityId(plan), deferred);
            }
            if (++attempted == 16) break;
        }
    }

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 20 == 0)
            retryLoaded(level);
    }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        var entries = new ListTag();
        for (var plan : pending.values()) {
            var entry = new CompoundTag();
            entry.putUUID("Encounter", plan.encounter); entry.putLong("Origin", plan.origin.asLong());
            entry.putInt("Index", plan.index); entry.putString("Type", plan.type);
            entry.putLong("Preferred", plan.preferred.asLong());
            entry.putBoolean("Livestock", plan.livestock); entry.putBoolean("Aquatic", plan.aquatic);
            var box = plan.room;
            entry.putIntArray("Room", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            entries.add(entry);
        }
        tag.put("Pending", entries);
        var animals = new ListTag();
        for (var id : placedLivestock) { var entry = new CompoundTag(); entry.putUUID("Id", id); animals.add(entry); }
        tag.put("PlacedLivestock", animals);
        return tag;
    }

    public static GoblinFortressSpawns load(CompoundTag tag) {
        var data = new GoblinFortressSpawns();
        for (var value : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value; var box = entry.getIntArray("Room");
            if (!entry.hasUUID("Encounter") || box.length != 6) continue;
            var plan = new Plan(entry.getUUID("Encounter"), BlockPos.of(entry.getLong("Origin")), entry.getInt("Index"),
                    entry.getString("Type"), BlockPos.of(entry.getLong("Preferred")),
                    new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]),
                    entry.getBoolean("Livestock"), entry.getBoolean("Aquatic"));
            data.pending.put(entityId(plan), plan);
        }
        for (var value : tag.getList("PlacedLivestock", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (entry.hasUUID("Id")) data.placedLivestock.add(entry.getUUID("Id"));
        }
        return data;
    }
}
