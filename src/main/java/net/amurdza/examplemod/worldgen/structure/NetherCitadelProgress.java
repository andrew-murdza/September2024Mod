package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** A fixed, persistent roster; unloading or defeating a resident never creates a replacement. */
public final class NetherCitadelProgress {
    public static final String KIND = "nether_citadel";
    public static final int DEFENDERS = 321;

    private NetherCitadelProgress() { }

    public static AuthoredEncounterProgress.Encounter register(ServerLevel level, UUID id, BlockPos origin) {
        var progress = AuthoredEncounterProgress.get(level);
        var encounter = progress.register(id, origin, DEFENDERS, KIND);
        progress.queueMissingDefenders(id);
        BossStructureBounds.get(level).record(KIND + ":" + origin.asLong(), boundary(encounter), id);
        return encounter;
    }

    public static BoundingBox bounds(BlockPos origin) {
        int radius = NetherCitadelLayout.RADIUS;
        return new BoundingBox(origin.getX() - radius, NetherCitadelLayout.MIN_Y, origin.getZ() - radius,
                origin.getX() + radius, NetherCitadelLayout.MAX_Y, origin.getZ() + radius);
    }

    public static BoundingBox bounds(AuthoredEncounterProgress.Encounter encounter) {
        return bounds(encounter.origin());
    }

    public static AABB boundary(AuthoredEncounterProgress.Encounter encounter) {
        var box = bounds(encounter);
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }

    public static AuthoredEncounterProgress.Encounter at(ServerLevel level, BlockPos pos) {
        var progress = AuthoredEncounterProgress.get(level);
        for (var boundary : BossStructureBounds.get(level).nearby(new AABB(pos))) {
            if (!boundary.key().startsWith(KIND + ":") || boundary.encounter() == null) continue;
            var encounter = progress.find(boundary.encounter());
            if (encounter != null && KIND.equals(encounter.kind())) return encounter;
        }
        return null;
    }

    public static List<AuthoredEncounterProgress.Encounter> inChunk(ServerLevel level, ChunkPos chunk) {
        var origins = new HashSet<BlockPos>();
        return AuthoredEncounterProgress.get(level).encounters(KIND).stream()
                .filter(encounter -> origins.add(encounter.origin()))
                .filter(encounter -> bounds(encounter).intersects(chunk.getMinBlockX(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), chunk.getMaxBlockZ())).toList();
    }

    public static UUID entityId(UUID encounter, int index) {
        return UUID.nameUUIDFromBytes((KIND + ":" + encounter + ":" + index).getBytes(StandardCharsets.UTF_8));
    }

    public static BlockPos residentPos(BlockPos origin, NetherCitadelLayout.Resident resident) {
        return new BlockPos(origin.getX() + resident.pos().getX(), resident.pos().getY(),
                origin.getZ() + resident.pos().getZ());
    }

    /** Called only on the server thread, after generated chunks and their entities have finished loading. */
    public static void retryLoaded(ServerLevel level) {
        var progress = AuthoredEncounterProgress.get(level);
        var encounters = progress.encounters(KIND);
        if (encounters.isEmpty()) return;
        var residents = NetherCitadelLayout.residents();
        if (residents.size() != DEFENDERS) throw new IllegalStateException("Citadel roster must contain " + DEFENDERS + " defenders");
        var origins = new HashSet<BlockPos>();
        int spawned = 0;
        for (var encounter : encounters) {
            if (!origins.add(encounter.origin()) || encounter.cleared()) continue;
            var pending = progress.pendingDefenders(encounter.id());
            for (int index = pending.nextSetBit(0); index >= 0; index = pending.nextSetBit(index + 1)) {
                if (index >= residents.size()) break;
                if (retry(level, encounter, index, residents.get(index)) && ++spawned >= 32) return;
            }
        }
    }

    private static boolean retry(ServerLevel level, AuthoredEncounterProgress.Encounter encounter, int index,
            NetherCitadelLayout.Resident resident) {
        var progress = AuthoredEncounterProgress.get(level);
        if (!progress.canPlaceDefender(encounter.id(), index)) return false;
        var id = entityId(encounter.id(), index);
        if (level.getEntity(id) instanceof Mob existing && !existing.isRemoved()) {
            progress.track(existing);
            return false;
        }
        var pos = residentPos(encounter.origin(), resident);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(resident.id()));
        if (type == null || !(type.create(level) instanceof Mob mob)) return false;
        if (mob instanceof MagmaCube cube) cube.setSize(2, true);
        mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        var box = mob.getBoundingBox();
        for (int x = (int) Math.floor(box.minX) >> 4; x <= (int) Math.floor(box.maxX) >> 4; x++)
            for (int z = (int) Math.floor(box.minZ) >> 4; z <= (int) Math.floor(box.maxZ) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.getFluidState(pos).isEmpty() || !level.noCollision(mob)) return false;
        mob.setUUID(id);
        finalizeResident(level, mob, pos);
        StructureSpawnedMobData.mark(mob, pos);
        AuthoredEncounterProgress.markDefender(mob, encounter.id(), encounter.origin(), DEFENDERS, KIND, index);
        if (!level.addFreshEntity(mob)) return false;
        // Track synchronously: a second chunk finishing this tick must never queue a duplicate.
        progress.track(mob);
        return true;
    }

    /** Native initialization may add jockeys or random juveniles; the authored roster has neither. */
    public static void finalizeResident(ServerLevel level, Mob mob, BlockPos pos) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE,
                mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null, null);
        if (mob instanceof Zombie zombie) zombie.setBaby(false);
        if (mob instanceof Hoglin hoglin) hoglin.setBaby(false);
        if (mob instanceof Zoglin zoglin) zoglin.setBaby(false);
        if (mob instanceof MagmaCube cube) cube.setSize(2, true);
    }
}
